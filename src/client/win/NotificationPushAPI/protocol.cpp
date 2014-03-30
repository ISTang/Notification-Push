#include "stdafx.h"
#include "protocol.h"
#include "is_utf8.h"
#include "strlen_utf8.h"
#include "pointer_add_utf8.h"
#include "string_format.h"

Connection::Connection(void)
: canReconnect(true)
{
	unhandleInput = NULL;
	initPacket();
}


Connection::~Connection(void)
{
	if (unhandleInput!=NULL) delete[] unhandleInput;
}

bool Connection::write(const std::string& msg, bool end)
{
	if (!IsOpen()) return false;

	DWORD dwBytesWritten = WriteComm((LPBYTE)msg.c_str(), msg.size(), SOCKET_WRITE_TIMEOUT);
	if (end) {
		CloseComm();
		onDisconnected(true);
	}
	bool ok = (dwBytesWritten == msg.size());
	if (ok) onTextSent(msg);
	return ok;
}

void Connection::OnDataReceived(const LPBYTE lpBuffer, DWORD dwCount)
{
	if (dwCount==0) return;

	try
	{
		int newSize = ((unhandleInput!=NULL)?strlen(unhandleInput):0)+dwCount;
		char *newBuf = new char[newSize+1];
		memset(newBuf, 0, newSize);
		if (unhandleInput!=NULL)
		{
			strcpy_s(newBuf, newSize, unhandleInput);
		}
		strncat_s(newBuf, newSize+1, (char *)lpBuffer, dwCount);
		//
		if (unhandleInput!=NULL) delete[] unhandleInput;
		unhandleInput = newBuf;

		int  read_retval = 0;
		if (is_utf8((unsigned char *)unhandleInput, read_retval) != 0) {
			// 未接收到完整的utf8字符
			return;
		}

		onTextReceived(unhandleInput);
		if (TRACK_SOCKET)
		{
			CA2T strText(unhandleInput, CP_UTF8);
			CT2A strText2(strText.m_psz);
			trace(std::string("[SOCKET] received: ") + strText2.m_psz);
		}

		int unhandleSize = strlen(unhandleInput);
		char *newInput = new char[unhandleSize+1];
		memcpy(newInput, unhandleInput, unhandleSize);
		newInput[unhandleSize] = '\0';
		//
		unhandleInput = NULL;

		if (waitForHead) {
			// 读取头部
			headInput += newInput;
			delete[] newInput;
			// 从首字符开始处理
			auto emptyLineFound = false;
			do {
				// 寻找行结束符
				auto pos = headInput.find(INPUT_RETURN);
				if (pos == -1) {
					// 未找到行结束符
					return;
				}
				// 找到行结束符
				// 记录行内容(不包括行结束符)
				std::string line(headInput.c_str(), pos);
				// 解析头部行
				if (!actionLineFound) {
					// 动作行
					debug("[Action line]"+line);
					const char *pszTemp = line.c_str();
					while (NULL != *pszTemp && !isspace(*pszTemp)) ++pszTemp;
					if (NULL == *pszTemp) {
						// 格式不对
						if (TRACK_SOCKET) trace(std::string("[SOCKET] write: Invalid action line(") + line + ")");
						std::string buf = string_format(CLOSE_CONN_RES, strlen(INVALID_ACTION_LINE_MSG), INVALID_ACTION_LINE_MSG);
						write(buf, true);
						throw INVALID_ACTION_LINE;
					}
					action = std::string(line.c_str(), pszTemp++); // 动作
					while (NULL != *pszTemp && isspace(*pszTemp)) ++pszTemp;
					target = pszTemp; // 目标
					debug("Action: " + action + ", target: " + target);
					actionLineFound = true;
				}
				else if (*line.c_str() != NULL) {
					// 属性行
					debug("[Field line]" + std::string(line));
					const char *pszColon = strchr(line.c_str(), ':');
					if (NULL == pszColon) {
						// 格式不对
						if (TRACK_SOCKET) trace(std::string("[SOCKET] write: Invalid field line(") + line + ")");
						std::string buf = string_format(CLOSE_CONN_RES, strlen(INVALID_FIELD_LINE_MSG), INVALID_FIELD_LINE_MSG);
						write(buf, true);
						throw INVALID_FIELD_LINE;
					}
					const char *pszNameEnd = pszColon;
					while (isspace(*(pszNameEnd - 1))) --pszNameEnd;
					auto name = std::string(line.c_str(), pszNameEnd); // 名字
					const char *pszValueStart = pszColon + 1;
					while (isspace(*pszValueStart)) ++pszValueStart;
					auto value = pszValueStart; // 值
					fields[name] = value;
					debug("Field: " + name + ", value: " + value);
				}
				else {
					// 找到空行
					debug("[Empty line]");
					emptyLineFound = true;
				}
				// 记录头部
				head += line;
				head += INPUT_RETURN;
				// 为寻找下一个行结束符准备
				headInput.erase(0, pos + strlen(INPUT_RETURN));
			} while (!emptyLineFound); // 直到遇到空行
			// 头部读取完毕
			waitForHead = false;
			// 确定体部长度
			if (fields.find(FIELD_BODY_LENGTH) != fields.end()) {
				bodyLength = atoi(fields[FIELD_BODY_LENGTH].c_str());
				if (bodyLength < 0) {
					// 体部长度字段值无效
					if (TRACK_SOCKET) trace(std::string("[SOCKET] write: ") + INVALID_LENGTH_VALUE_MSG);
					std::string buf = string_format(CLOSE_CONN_RES, strlen(INVALID_LENGTH_VALUE_MSG), INVALID_LENGTH_VALUE_MSG);
					write(buf, true);
					throw INVALID_BODY_LENGTH;
				}
			}
			// 将余下输入内容作为体部
			body = headInput;
		}
		else {
			// 读取体部
			body += newInput;
			delete[] newInput;
		}
		// 检查体部内容
		unsigned int bodySize = strlen_utf8(body.c_str());
		if (bodySize < bodyLength) {
			// 尚未读取完体部
			return;
		}
		else if (bodySize > bodyLength) {
			// 将多余的内容作为下一个报文的头部输入
			const char *p = pointer_add_utf8(body.c_str(), bodyLength);
			headInput = std::string(p);
			body.erase(p - body.c_str());
		}
		else {
			// 没有多余的输入
			headInput = "";
		}
		debug("body: "+body);
		if (SHOW_PACKET) trace(head + body);
		// 处理新的报文
		handlePacket(action, target, fields, body);
		// 准备处理下一个报文
		initPacket();
	}
	catch (int e)
	{
		switch (e)
		{
		case INVALID_ACTION_LINE:
		case INVALID_FIELD_LINE:
		case INVALID_BODY_LENGTH:
		case SOCKET_WRITE_ERROR:
		case SERVER_KICKOFF_ME:
			canReconnect = true;
			break;
		case INVALID_APP_INFO:
		case INVALID_USER_INFO:
		case FEATURE_NOT_SUPPORTED:
		default:
			canReconnect = false;
			break;
		}
		initPacket();
		CloseComm();
		onDisconnected(false);
	}
}

void Connection::OnEvent(UINT uEvent, LPVOID lpvData)
{
	switch( uEvent )
	{
		case EVT_CONSUCCESS:
			// 连接成功
			onConnected();
			break;
		case EVT_CONFAILURE:
			// 连接失败
			CloseComm();
			onConnectFailed();
			break;
		case EVT_CONDROP:
			// 连接断开
			CloseComm();
			onDisconnected(false);
			break;
		case EVT_ZEROLENGTH:
			// 收到空分组
			break;
		default:
			break;
	}
}

void Connection::initPacket(void)
{
	if (unhandleInput != NULL) delete[] unhandleInput;
	unhandleInput = NULL;

	waitForHead = true; // 等待头部(false表示等待体部或不需要再等待)
	headInput = "";
	actionLineFound = false; // 是否已找到动作行
	//
	head = ""; // 头部内容
	body = ""; // 体部内容
	//
	action = ""; // 动作
	target = ""; // 目标
	fields.clear(); // 字段表
	bodyLength = 0; // 体部长度
}

///////////////////////////////////////////////////////////////////////////////

ClientConnection::ClientConnection()
: clientLogon(false)
, clientLogining(false)
, keepAliveHandle(NULL)
, connectHandle(NULL)
, disconnectNow(false)
{
}


ClientConnection::~ClientConnection(void)
{
	disconnect();
}

void ClientConnection::setAppInfo(const AppInfo &appInfo)
{
	this->appInfo = appInfo;
}

void ClientConnection::setLoginInfo(const LoginInfo &loginInfo)
{
	this->loginInfo = loginInfo;
}

void ClientConnection::setServerInfo(const std::string &serverHost, int serverPort)
{
	this->serverHost = serverHost;
	this->serverPort = serverPort;
}

void ClientConnection::SetAutoReconnect(bool autoReconnect, int reconnectDelay) {
	this->autoReconnect = autoReconnect;
	this->reconnectDelay = reconnectDelay;
}

bool ClientConnection::connect(void)
{
	if (!autoReconnect) return (doConnect(this)==0);

	HANDLE hThread;
	UINT uiThreadId = 0;
	hThread = (HANDLE)_beginthreadex(NULL,  // Security attributes
		0,    // stack
		doConnect,   // Thread proc
		this,   // Thread param
		0,   // creation mode(old:CREATE_SUSPENDED)
		&uiThreadId);   // Thread ID

	if (NULL != hThread)
	{
		//SetThreadPriority(m_hconnectHandle, THREAD_PRIORITY_ABOVE_NORMAL);
		//ResumeThread( m_hconnectHandle );
		connectHandle = hThread;
		return true;
	}

	return false;
}

UINT WINAPI  ClientConnection::doConnect(LPVOID pParam)
{
	ClientConnection* pThis = reinterpret_cast<ClientConnection*>(pParam);
	_ASSERTE(pThis != NULL);

	do {
		if (pThis->disconnectNow) break;
		if (!pThis->IsOpen() && pThis->canReconnect)
		{
			pThis->stopKeepAlive();
			pThis->StopComm();
			pThis->initPacket();

			pThis->onLoginStatus(LOGINING);
			CA2T strServer(pThis->serverHost.c_str());
			if (!pThis->ConnectTo(strServer, std::to_wstring(pThis->serverPort).c_str(), AF_INET, SOCK_STREAM))
			{
				if (pThis->disconnectNow) break;
				Sleep(pThis->reconnectDelay);
				continue;
			}
			if (!pThis->WatchComm())
			{
				pThis->CloseComm();
				if (pThis->disconnectNow) break;
				Sleep(pThis->reconnectDelay);
				continue;
			}
		}
		else
		{
			Sleep(100);
		}
	} while (true);

	return 0;
}

void ClientConnection::disconnect(void)
{
	disconnectNow = true;
	if (WaitForSingleObject(connectHandle, 1000L) == WAIT_TIMEOUT)
		TerminateThread(connectHandle, 1L);
	CloseHandle(connectHandle);
	connectHandle = NULL;

	stopKeepAlive();
	
	if (IsStart()) {
		StopComm();
		onDisconnected(true);
	}
}

bool ClientConnection::send(const std::string &receiver, const std::string &sendId, const std::string &msg, bool secure)
{
	if (secure) {
		// TODO 支持安全消息
		error("本应用暂不支持安全消息");
		return false;
	}

	debug("Sending message to " + receiver + "...");
	std::string buf = string_format(SEND_MSG_REQ, receiver.c_str(), sendId.c_str(), secure ? "true" : "false", strlen_utf8(msg.c_str()), msg.c_str());
	if (!write(buf)) return false;

	return true;
}

bool ClientConnection::multicast(const std::vector<std::string> &receivers, const std::string &sendId, const std::string &msg, bool secure)
{
	if (secure) {
		// TODO 支持安全消息
		error("本应用暂不支持安全消息");
		return false;
	}

	if (receivers.size()<2) return false;

	std::string strReceivers = receivers[0];
	for (unsigned int i=1; i<receivers.size(); i++) {
		strReceivers += ","+receivers[i];
	}

	debug("Multicasting message to " + strReceivers + "...");
	std::string buf = string_format(MULTICAST_MSG_REQ, strReceivers.c_str(), sendId.c_str(), secure ? "true" : "false", strlen_utf8(msg.c_str()), msg.c_str());
	if (!write(buf))
	{
		warn("写套接字失败");
		return false;
	}

	return true;
}

bool ClientConnection::broadcast(const std::string &sendId, const std::string &msg, bool secure)
{
	if (secure) {
		// TODO 支持安全消息
		error("本应用暂不支持安全消息");
		return false;
	}

	debug("Broadcasting message ...");
	std::string buf = string_format(BROADCAST_MSG_REQ, sendId.c_str(), secure ? "true" : "false", strlen_utf8(msg.c_str()), msg.c_str());
	if (!write(buf))
	{
		warn("写套接字失败");
		return false;
	}

	return true;
}

void ClientConnection::onConnected(void)
{
	clientLogining = true;
	onLoginStatus(LOGINING);
}

void ClientConnection::onConnectFailed(void)
{
	onLoginStatus(LOGOUT);
}

void ClientConnection::onDisconnected(bool passive)
{
	clientLogon = false;
	clientLogining = false;
	onLoginStatus(LOGOUT);
}

void ClientConnection::handlePacket(const std::string &action, const std::string &target,
	std::map<std::string, std::string> &fields, const std::string &body)
{
	if (action == "GET" && target == "APPID") {
		// 应用认证请求, 发送当前应用ID及密码
		auto password = md5.CalcMD5FromString(appInfo.password.c_str());
        debug("Received application certificate request, sending...");
		std::string buf = string_format(GET_APPID_RES, appInfo.id.length()+1+strlen(password), appInfo.id.c_str(), password);
		if (!write(buf)) throw SOCKET_WRITE_ERROR;
	}
	else if (action == "SET" && target == "APPID") {
        // 应用认证回复
        auto appOk = (fields[FIELD_ACTION_SUCCESS] == "true");
        if (appOk) {
            debug("Application certificate passed");
        } else {
            error("Application certificate failed: " + body);
			onAppCheckFailed(body);
			throw INVALID_APP_INFO;
        }
    } else if (action == "GET" && target == "USERNAME") {
        // 用户认证请求, 发送用户名及密码
        debug("Received user certificate request, sending...");
        if (fields[FIELD_LOGIN_SECURE] == "true") {
            // 安全登录: 暂时不支持
			onUserCheckFailed("Secure login not supported yet!");
			throw FEATURE_NOT_SUPPORTED;
        } else {
            // 非安全登录
            if (fields[FIELD_LOGIN_PASSWORD] == "true") {
                // 需要登录密码
				auto password = md5.CalcMD5FromString(loginInfo.password.c_str());
                debug("Getting username...");
				std::string buf = string_format(GET_USERNAME_RES, "false", "true", loginInfo.username.length()+1+strlen(password),
					(loginInfo.username+","+password).c_str());
				if (!write(buf)) throw SOCKET_WRITE_ERROR;
			}
			else {
                // 不需要登录密码
                debug("Getting username...");
				std::string buf = string_format(GET_USERNAME_RES, "false", "false", loginInfo.username.length(),
					loginInfo.username.c_str());
				if (!write(buf)) throw SOCKET_WRITE_ERROR;
			}
        }
    } else if (action == "SET" && target == "USERNAME") {
        // 用户认证回复
        auto usernameOk = (fields[FIELD_ACTION_SUCCESS] == "true");
        if (usernameOk) {
            info("User certificate passed");
        } else {
            error("User certificate failed: " + body);
			onUserCheckFailed(body);
			throw INVALID_USER_INFO;
		}
    } else if (action == "SET" && target == "MSGKEY") {
        // 收到消息密钥
        if (fields[FIELD_LOGIN_SECURE] == "true") {
            // 暂不支持安全消息
			onUserCheckFailed("Secure message not supported yet!");
			throw FEATURE_NOT_SUPPORTED;
		}
		if (!write(SET_MSGKEY_ACK)) throw SOCKET_WRITE_ERROR;
        debug("Received message key: " + body);
		onMsgKeyReceived(body);
    } else if (action == "SET" && target == "ALIVEINT") {
        // 设置心跳周期
		if (!write(SET_ALIVEINT_ACK)) throw SOCKET_WRITE_ERROR;
        clientLogon = true;
		clientLogining = false;
        maxInactiveTime = atoi(body.c_str());
        debug("Max inactive time(ms): " + body);
		onMaxInactiveTimeReceived(maxInactiveTime);
		onLoginStatus(LOGON);
		time(&lastActiveTime);
		startKeepAlive();
    } else if (action == "SET" && target == "ALIVE") {
        // 收到心跳回复信号
        time(&lastActiveTime);
		debug("Keep alive replied");
	}
	else if (action == "PUSH" && target == "MSG") {
        // 收到消息
        auto secure = (fields[FIELD_MSG_SECURE] == "true");
        auto receipt = (fields[FIELD_MSG_RECEIPT] == "true");
        auto msg = body;
        if (receipt) {
            // 确认已收到消息
			if (!write(PUSH_MSG_ACK)) throw SOCKET_WRITE_ERROR;
        }
        //debug("Message received: "+msg);
        time(&lastActiveTime);
		if (secure) {
			// 暂不支持安全消息
			error("Secure message not supported yet!");
			throw FEATURE_NOT_SUPPORTED;
		}
		onMsgReceived(msg);
    } else if (action == "BROADCAST" && target == "MSG") {
        // 收到消息广播回复
        auto msgId = fields[FIELD_ACTION_ID];
        auto success = (fields[FIELD_ACTION_SUCCESS] == "true");
        info("message " + msgId + (success ? " broadcasted." : " broadcast failed: " + body));
		onMsgReplied(msgId, success, body);
    } else if (action == "MULTICAST" && target == "MSG") {
        // 收到消息群发回复
        auto msgId = fields[FIELD_ACTION_ID];
        auto success = (fields[FIELD_ACTION_SUCCESS] == "true");
        info("message " + msgId + (success ? " multicasted." : " multicast failed: " + body));
		onMsgReplied(msgId, success, body);
    } else if (action == "SEND" && target == "MSG") {
        // 收到消息发送回复
        auto msgId = fields[FIELD_ACTION_ID];
        auto success = (fields[FIELD_ACTION_SUCCESS] == "true");
        info("message " + msgId + (success ? " sent." : " send failed: " + body));
		onMsgReplied(msgId, success, body);
    } else if (action == "QUERY" && target == "PUBLIC") {
        // 收到公众号查询回复
    } else if (action == "FOLLOW" && target == "PUBLIC") {
        // 收到公众号关注回复
    } else if (action == "UNFOLLOW" && target == "PUBLIC") {
        // 收到公众号取消关注回复
    } else if (action == "GET" && target == "FOLLOWED") {
        // 收到获取已关注公众号回复
    } else if (action == "CLOSE" && target == "CONN") {
        // 服务器主动断开连接
        warn("Server kickoff me: " + body);
		throw SERVER_KICKOFF_ME;
    } else {
        // 无法理解的动作
        error("Unknown action: " + action + " " + target);
		throw FEATURE_NOT_SUPPORTED;
    }
}

void ClientConnection::startKeepAlive(void)
{
	unsigned nThreadId;
	keepAliveHandle = (HANDLE)_beginthreadex(NULL, 0, keepAlive, this, CREATE_SUSPENDED, &nThreadId);
	ResumeThread(keepAliveHandle);
}

void ClientConnection::stopKeepAlive(void)
{
	if (keepAliveHandle == NULL) return;

	if (clientLogon) {
		TerminateThread(keepAliveHandle, 0);
	}
	CloseHandle(keepAliveHandle);

	keepAliveHandle = NULL;
}

unsigned __stdcall ClientConnection::keepAlive(void * pThis)
{
	ClientConnection *pConn = static_cast<ClientConnection *>(pThis);
	int& maxInactiveTime = pConn->maxInactiveTime;
	time_t& lastActiveTime = pConn->lastActiveTime;

	const auto checkInterval = 100;
	auto checkedCount = 0;
	auto maxCheckCount = maxInactiveTime / checkInterval / 2;

	while (pConn->clientLogon)
	{
		Sleep(checkInterval);

		if (!pConn->IsOpen())
		{
			pConn->warn("Connection broken");
			pConn->clientLogon = false;
			pConn->CloseComm();
			pConn->onDisconnected(false);
			return 1;
		}

		time_t now;
		time(&now);
		if (now - lastActiveTime > maxInactiveTime / 1000)
		{
			// server gone?
			if (TRACK_SOCKET) pConn->trace("[SOCKET] write: server gone?");
			std::string buf = string_format(CLOSE_CONN_RES, strlen(INACTIVE_TIMEOUT_MSG), INACTIVE_TIMEOUT_MSG);
			if (!pConn->write(buf, true))
			{
				pConn->warn("Write socket failed");
				pConn->clientLogon = false;
				pConn->CloseComm();
				pConn->onDisconnected(false);
				return 2;
			}
			return 3;
		}

		if (++checkedCount == maxCheckCount)
		{
			pConn->debug("Keep alive...");
			if (TRACK_SOCKET) pConn->trace(std::string("[SOCKET] write: ") + SET_ALIVE_REQ);
			if (!pConn->write(SET_ALIVE_REQ))
			{
				pConn->warn("Write socket failed");
				pConn->clientLogon = false;
				pConn->CloseComm();
				pConn->onDisconnected(false);
				return 2;
			}

			checkedCount = 0;
		}
	}
	return 0;
}
