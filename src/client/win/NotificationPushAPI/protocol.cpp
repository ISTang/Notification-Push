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
			// δ���յ�������utf8�ַ�
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
			// ��ȡͷ��
			headInput += newInput;
			delete[] newInput;
			// �����ַ���ʼ����
			auto emptyLineFound = false;
			do {
				// Ѱ���н�����
				auto pos = headInput.find(INPUT_RETURN);
				if (pos == -1) {
					// δ�ҵ��н�����
					return;
				}
				// �ҵ��н�����
				// ��¼������(�������н�����)
				std::string line(headInput.c_str(), pos);
				// ����ͷ����
				if (!actionLineFound) {
					// ������
					debug("[Action line]"+line);
					const char *pszTemp = line.c_str();
					while (NULL != *pszTemp && !isspace(*pszTemp)) ++pszTemp;
					if (NULL == *pszTemp) {
						// ��ʽ����
						if (TRACK_SOCKET) trace(std::string("[SOCKET] write: Invalid action line(") + line + ")");
						std::string buf = string_format(CLOSE_CONN_RES, strlen(INVALID_ACTION_LINE_MSG), INVALID_ACTION_LINE_MSG);
						write(buf, true);
						throw INVALID_ACTION_LINE;
					}
					action = std::string(line.c_str(), pszTemp++); // ����
					while (NULL != *pszTemp && isspace(*pszTemp)) ++pszTemp;
					target = pszTemp; // Ŀ��
					debug("Action: " + action + ", target: " + target);
					actionLineFound = true;
				}
				else if (*line.c_str() != NULL) {
					// ������
					debug("[Field line]" + std::string(line));
					const char *pszColon = strchr(line.c_str(), ':');
					if (NULL == pszColon) {
						// ��ʽ����
						if (TRACK_SOCKET) trace(std::string("[SOCKET] write: Invalid field line(") + line + ")");
						std::string buf = string_format(CLOSE_CONN_RES, strlen(INVALID_FIELD_LINE_MSG), INVALID_FIELD_LINE_MSG);
						write(buf, true);
						throw INVALID_FIELD_LINE;
					}
					const char *pszNameEnd = pszColon;
					while (isspace(*(pszNameEnd - 1))) --pszNameEnd;
					auto name = std::string(line.c_str(), pszNameEnd); // ����
					const char *pszValueStart = pszColon + 1;
					while (isspace(*pszValueStart)) ++pszValueStart;
					auto value = pszValueStart; // ֵ
					fields[name] = value;
					debug("Field: " + name + ", value: " + value);
				}
				else {
					// �ҵ�����
					debug("[Empty line]");
					emptyLineFound = true;
				}
				// ��¼ͷ��
				head += line;
				head += INPUT_RETURN;
				// ΪѰ����һ���н�����׼��
				headInput.erase(0, pos + strlen(INPUT_RETURN));
			} while (!emptyLineFound); // ֱ����������
			// ͷ����ȡ���
			waitForHead = false;
			// ȷ���岿����
			if (fields.find(FIELD_BODY_LENGTH) != fields.end()) {
				bodyLength = atoi(fields[FIELD_BODY_LENGTH].c_str());
				if (bodyLength < 0) {
					// �岿�����ֶ�ֵ��Ч
					if (TRACK_SOCKET) trace(std::string("[SOCKET] write: ") + INVALID_LENGTH_VALUE_MSG);
					std::string buf = string_format(CLOSE_CONN_RES, strlen(INVALID_LENGTH_VALUE_MSG), INVALID_LENGTH_VALUE_MSG);
					write(buf, true);
					throw INVALID_BODY_LENGTH;
				}
			}
			// ����������������Ϊ�岿
			body = headInput;
		}
		else {
			// ��ȡ�岿
			body += newInput;
			delete[] newInput;
		}
		// ����岿����
		unsigned int bodySize = strlen_utf8(body.c_str());
		if (bodySize < bodyLength) {
			// ��δ��ȡ���岿
			return;
		}
		else if (bodySize > bodyLength) {
			// �������������Ϊ��һ�����ĵ�ͷ������
			const char *p = pointer_add_utf8(body.c_str(), bodyLength);
			headInput = std::string(p);
			body.erase(p - body.c_str());
		}
		else {
			// û�ж��������
			headInput = "";
		}
		debug("body: "+body);
		if (SHOW_PACKET) trace(head + body);
		// �����µı���
		handlePacket(action, target, fields, body);
		// ׼��������һ������
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
			// ���ӳɹ�
			onConnected();
			break;
		case EVT_CONFAILURE:
			// ����ʧ��
			CloseComm();
			onConnectFailed();
			break;
		case EVT_CONDROP:
			// ���ӶϿ�
			CloseComm();
			onDisconnected(false);
			break;
		case EVT_ZEROLENGTH:
			// �յ��շ���
			break;
		default:
			break;
	}
}

void Connection::initPacket(void)
{
	if (unhandleInput != NULL) delete[] unhandleInput;
	unhandleInput = NULL;

	waitForHead = true; // �ȴ�ͷ��(false��ʾ�ȴ��岿����Ҫ�ٵȴ�)
	headInput = "";
	actionLineFound = false; // �Ƿ����ҵ�������
	//
	head = ""; // ͷ������
	body = ""; // �岿����
	//
	action = ""; // ����
	target = ""; // Ŀ��
	fields.clear(); // �ֶα�
	bodyLength = 0; // �岿����
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
		// TODO ֧�ְ�ȫ��Ϣ
		error("��Ӧ���ݲ�֧�ְ�ȫ��Ϣ");
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
		// TODO ֧�ְ�ȫ��Ϣ
		error("��Ӧ���ݲ�֧�ְ�ȫ��Ϣ");
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
		warn("д�׽���ʧ��");
		return false;
	}

	return true;
}

bool ClientConnection::broadcast(const std::string &sendId, const std::string &msg, bool secure)
{
	if (secure) {
		// TODO ֧�ְ�ȫ��Ϣ
		error("��Ӧ���ݲ�֧�ְ�ȫ��Ϣ");
		return false;
	}

	debug("Broadcasting message ...");
	std::string buf = string_format(BROADCAST_MSG_REQ, sendId.c_str(), secure ? "true" : "false", strlen_utf8(msg.c_str()), msg.c_str());
	if (!write(buf))
	{
		warn("д�׽���ʧ��");
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
		// Ӧ����֤����, ���͵�ǰӦ��ID������
		auto password = md5.CalcMD5FromString(appInfo.password.c_str());
        debug("Received application certificate request, sending...");
		std::string buf = string_format(GET_APPID_RES, appInfo.id.length()+1+strlen(password), appInfo.id.c_str(), password);
		if (!write(buf)) throw SOCKET_WRITE_ERROR;
	}
	else if (action == "SET" && target == "APPID") {
        // Ӧ����֤�ظ�
        auto appOk = (fields[FIELD_ACTION_SUCCESS] == "true");
        if (appOk) {
            debug("Application certificate passed");
        } else {
            error("Application certificate failed: " + body);
			onAppCheckFailed(body);
			throw INVALID_APP_INFO;
        }
    } else if (action == "GET" && target == "USERNAME") {
        // �û���֤����, �����û���������
        debug("Received user certificate request, sending...");
        if (fields[FIELD_LOGIN_SECURE] == "true") {
            // ��ȫ��¼: ��ʱ��֧��
			onUserCheckFailed("Secure login not supported yet!");
			throw FEATURE_NOT_SUPPORTED;
        } else {
            // �ǰ�ȫ��¼
            if (fields[FIELD_LOGIN_PASSWORD] == "true") {
                // ��Ҫ��¼����
				auto password = md5.CalcMD5FromString(loginInfo.password.c_str());
                debug("Getting username...");
				std::string buf = string_format(GET_USERNAME_RES, "false", "true", loginInfo.username.length()+1+strlen(password),
					(loginInfo.username+","+password).c_str());
				if (!write(buf)) throw SOCKET_WRITE_ERROR;
			}
			else {
                // ����Ҫ��¼����
                debug("Getting username...");
				std::string buf = string_format(GET_USERNAME_RES, "false", "false", loginInfo.username.length(),
					loginInfo.username.c_str());
				if (!write(buf)) throw SOCKET_WRITE_ERROR;
			}
        }
    } else if (action == "SET" && target == "USERNAME") {
        // �û���֤�ظ�
        auto usernameOk = (fields[FIELD_ACTION_SUCCESS] == "true");
        if (usernameOk) {
            info("User certificate passed");
        } else {
            error("User certificate failed: " + body);
			onUserCheckFailed(body);
			throw INVALID_USER_INFO;
		}
    } else if (action == "SET" && target == "MSGKEY") {
        // �յ���Ϣ��Կ
        if (fields[FIELD_LOGIN_SECURE] == "true") {
            // �ݲ�֧�ְ�ȫ��Ϣ
			onUserCheckFailed("Secure message not supported yet!");
			throw FEATURE_NOT_SUPPORTED;
		}
		if (!write(SET_MSGKEY_ACK)) throw SOCKET_WRITE_ERROR;
        debug("Received message key: " + body);
		onMsgKeyReceived(body);
    } else if (action == "SET" && target == "ALIVEINT") {
        // ������������
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
        // �յ������ظ��ź�
        time(&lastActiveTime);
		debug("Keep alive replied");
	}
	else if (action == "PUSH" && target == "MSG") {
        // �յ���Ϣ
        auto secure = (fields[FIELD_MSG_SECURE] == "true");
        auto receipt = (fields[FIELD_MSG_RECEIPT] == "true");
        auto msg = body;
        if (receipt) {
            // ȷ�����յ���Ϣ
			if (!write(PUSH_MSG_ACK)) throw SOCKET_WRITE_ERROR;
        }
        //debug("Message received: "+msg);
        time(&lastActiveTime);
		if (secure) {
			// �ݲ�֧�ְ�ȫ��Ϣ
			error("Secure message not supported yet!");
			throw FEATURE_NOT_SUPPORTED;
		}
		onMsgReceived(msg);
    } else if (action == "BROADCAST" && target == "MSG") {
        // �յ���Ϣ�㲥�ظ�
        auto msgId = fields[FIELD_ACTION_ID];
        auto success = (fields[FIELD_ACTION_SUCCESS] == "true");
        info("message " + msgId + (success ? " broadcasted." : " broadcast failed: " + body));
		onMsgReplied(msgId, success, body);
    } else if (action == "MULTICAST" && target == "MSG") {
        // �յ���ϢȺ���ظ�
        auto msgId = fields[FIELD_ACTION_ID];
        auto success = (fields[FIELD_ACTION_SUCCESS] == "true");
        info("message " + msgId + (success ? " multicasted." : " multicast failed: " + body));
		onMsgReplied(msgId, success, body);
    } else if (action == "SEND" && target == "MSG") {
        // �յ���Ϣ���ͻظ�
        auto msgId = fields[FIELD_ACTION_ID];
        auto success = (fields[FIELD_ACTION_SUCCESS] == "true");
        info("message " + msgId + (success ? " sent." : " send failed: " + body));
		onMsgReplied(msgId, success, body);
    } else if (action == "QUERY" && target == "PUBLIC") {
        // �յ����ںŲ�ѯ�ظ�
    } else if (action == "FOLLOW" && target == "PUBLIC") {
        // �յ����ںŹ�ע�ظ�
    } else if (action == "UNFOLLOW" && target == "PUBLIC") {
        // �յ����ں�ȡ����ע�ظ�
    } else if (action == "GET" && target == "FOLLOWED") {
        // �յ���ȡ�ѹ�ע���ںŻظ�
    } else if (action == "CLOSE" && target == "CONN") {
        // �����������Ͽ�����
        warn("Server kickoff me: " + body);
		throw SERVER_KICKOFF_ME;
    } else {
        // �޷����Ķ���
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
