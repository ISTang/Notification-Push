#include "stdafx.h"
#include "protocol.h"
#include "is_utf8.h"
#include "strlen_utf8.h"
#include "pointer_add_utf8.h"
#include "json/json.h"

Connection::Connection(void)
{
	memset(unhandleInput, 0, sizeof unhandleInput);
	initPacket();
}


Connection::~Connection(void)
{
}


void Connection::onConnected(void)
{
	//std::cout<<"Connected to "<<peerAddress<<std::endl;
}


void Connection::onConnectFailed(void)
{
	//std::cerr<<"Connect failed. "<<std::endl;
}

void Connection::onTextReceived(std::string text)
{
	//std::cout << "Received text: " << text << std::endl;
}

void Connection::onTextSent(std::string text)
{
	//std::cout << "Sent text: " << text << std::endl;
}

void Connection::onDisconnected(bool passive)
{
	//std::cerr<<"Disconnected("<<peerAddress<<(passive?"passive":"active")<<")"<<std::endl;
}


void Connection::handlePacket(const std::string &action, const std::string &target, 
	std::map<std::string, std::string> &fields, const std::string &body)
{
	//std::cout<<"Received action: "<<action<<" "<<target<<std::endl;
}


void Connection::error(const std::string& log)
{
	//std::cerr<<"[error] "<<log<<std::endl;
}


void Connection::warn(const std::string& log)
{
	//std::cerr<<"[warn] "<<log<<std::endl;
}


void Connection::info(const std::string& log)
{
	//std::cout<<"[info] "<<log<<std::endl;
}


void Connection::debug(const std::string& log)
{
	//std::cout<<"[debug] "<<log<<std::endl;
}


void Connection::trace(const std::string& log)
{
	//std::cout<<"[trace] "<<log<<std::endl;
}

bool Connection::write(const std::string& msg, bool end)
{
	onTextSent(msg);

	DWORD dwBytesWritten = WriteComm((LPBYTE)msg.c_str(), msg.size(), SOCKET_WRITE_TIMEOUT);
	if (end) {
		StopComm();
		CloseComm();
		onDisconnected();
	}
	return (dwBytesWritten == msg.size());
}

void Connection::OnDataReceived(const LPBYTE lpBuffer, DWORD dwCount)
{
	try
	{
		strncat_s(unhandleInput, (char *)lpBuffer, dwCount);

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
			trace("[SOCKET] read from " + peerAddress + ": " + strText2.m_psz);
		}

		char newInput[MAX_BUF+1];
		memcpy(newInput, unhandleInput, sizeof newInput);
		memset(unhandleInput, 0, sizeof unhandleInput);

		char buf[MAX_BUF + 1];

		if (waitForHead) {
			// ��ȡͷ��
			headInput += newInput;
			// �����ַ���ʼ����
			auto emptyLineFound = false;
			do {
				// ��ʼ��
				char line[MAX_LINE + 1];
				memset(line, 0, sizeof line);
				// Ѱ���н�����
				auto pos = headInput.find(INPUT_RETURN);
				if (pos == -1) {
					// δ�ҵ��н�����
					return;
				}
				// �ҵ��н�����
				// ��¼������(�������н�����)
				strncpy_s(line, headInput.c_str(), pos);
				// ����ͷ����
				if (!actionLineFound) {
					// ������
					//debug("[Action line]"+std::string(line));
					const char *pszTemp = line;
					while (NULL != *pszTemp && !isspace(*pszTemp)) ++pszTemp;
					if (NULL == *pszTemp) {
						// ��ʽ����
						if (TRACK_SOCKET) trace("[SOCKET] write to client " + peerAddress + ": Invalid action line(" + line + ")");
						sprintf_s(buf, CLOSE_CONN_RES, strlen(INVALID_ACTION_LINE_MSG), INVALID_ACTION_LINE_MSG);
						if (!write(buf, true)) throw SOCKET_WRITE_ERROR;
						return;
					}
					action = std::string(line, pszTemp++); // ����
					while (NULL != *pszTemp && isspace(*pszTemp)) ++pszTemp;
					target = pszTemp; // Ŀ��
					//debug("Action: " + action + ", target: " + target);
					actionLineFound = true;
				}
				else if (*line != NULL) {
					// ������
					//debug("[Field line]" + std::string(line));
					const char *pszColon = strchr(line, ':');
					if (NULL == pszColon) {
						// ��ʽ����
						if (TRACK_SOCKET) trace("[SOCKET] write to client " + peerAddress + ": Invalid field line(" + line + ")");
						sprintf_s(buf, CLOSE_CONN_RES, strlen(INVALID_FIELD_LINE_MSG), INVALID_FIELD_LINE_MSG);
						if (!write(buf, true)) throw SOCKET_WRITE_ERROR;
						return;
					}
					const char *pszNameEnd = pszColon;
					while (isspace(*(pszNameEnd - 1))) --pszNameEnd;
					auto name = std::string(line, pszNameEnd); // ����
					const char *pszValueStart = pszColon + 1;
					while (isspace(*pszValueStart)) ++pszValueStart;
					auto value = pszValueStart; // ֵ
					fields[name] = value;
					//debug("Field: " + name + ", value: " + value);
				}
				else {
					// �ҵ�����
					//debug("[Empty line]");
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
					if (TRACK_SOCKET) trace("[SOCKET] write to client " + peerAddress + ": " + INVALID_LENGTH_VALUE_MSG);
					sprintf_s(buf, CLOSE_CONN_RES, strlen(INVALID_LENGTH_VALUE_MSG), INVALID_LENGTH_VALUE_MSG);
					if (!write(buf, true)) throw SOCKET_WRITE_ERROR;
					return;
				}
			}
			// ����������������Ϊ�岿
			body = headInput;
		}
		else {
			// ��ȡ�岿
			body += newInput;
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
		//debug("body: "+body);
		if (SHOW_PACKET) trace(head + body);
		// �����µı���
		handlePacket(action, target, fields, body);
		// ׼��������һ������
		initPacket();
	}
	catch (int e)
	{
		if (e == SOCKET_WRITE_ERROR)
		{
			StopComm();
			CloseComm();
			onDisconnected();
		}
	}
}

void Connection::OnEvent(UINT uEvent, LPVOID lpvData)
{
	switch( uEvent )
	{
		case EVT_CONSUCCESS:
			// ���ӳɹ�
			peerAddress = ""; // TODO remoteAddress + "[" + socket.remotePort + "]";
			onConnected();
			break;
		case EVT_CONFAILURE:
			// ����ʧ��
			StopComm();
			CloseComm();
			onConnectFailed();
			break;
		case EVT_CONDROP:
			// ���ӶϿ�
			StopComm();
			CloseComm();
			onDisconnected();
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
	waitForHead = true; // �ȴ�ͷ��(false��ʾ�ȴ��岿����Ҫ�ٵȴ�)
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
, m_hKeepAlive(NULL)
{
}


ClientConnection::~ClientConnection(void)
{
	if (NULL != m_hKeepAlive) CloseHandle(m_hKeepAlive);
}

void ClientConnection::setAppInfo(const AppInfo &appInfo)
{
	this->appInfo = appInfo;
}

void ClientConnection::setLoginInfo(const LoginInfo &loginInfo)
{
	this->loginInfo = loginInfo;
}

bool ClientConnection::connect(const std::string &server, int port)
{
	CA2T strServer(server.c_str());
	if (!ConnectTo(strServer, std::to_wstring(port).c_str(), AF_INET, SOCK_STREAM)) return false;
	if (!WatchComm()) return false;

	onLoginStatus(LOGINING);
	return true;
}

void ClientConnection::onLoginStatus(int nStatus)
{
	switch (nStatus)
	{
	case LOGOUT:
		std::cout << "Logout" << std::endl;
		break;
	case LOGINING:
		std::cout << "Logining..." << std::endl;
		break;
	case LOGON:
		std::cout << "Logon" << std::endl;
		break;
	}
}

void ClientConnection::onMsgKeyReceived(void)
{
	std::cout << "MsgKey: " << msgKey << std::endl;
}

void ClientConnection::onMaxInactiveTimeReceived(void)
{
	std::cout << "MaxInactiveTime: " << maxInactiveTime << "ms" << std::endl;
}

void ClientConnection::onMsgReceived(std::string msg)
{
	std::cout << "Received msg: " << msg << std::endl;
}

void ClientConnection::onConnected(void)
{
	Connection::onConnected();

	clientLogining = true;
	onLoginStatus(LOGINING);
}

void ClientConnection::onConnectFailed(void)
{
	Connection::onConnectFailed();

	onLoginStatus(LOGOUT);
}

void ClientConnection::onDisconnected(bool passive)
{
	Connection::onDisconnected(passive);

	clientLogon = false;
	clientLogining = false;
	onLoginStatus(LOGOUT);
}

void ClientConnection::handlePacket(const std::string &action, const std::string &target,
	std::map<std::string, std::string> &fields, const std::string &body)
{
	char buf[MAX_BUF + 1];
	memset(buf, 0, sizeof buf);

	if (action == "GET" && target == "APPID") {
		// Ӧ����֤����, ���͵�ǰӦ��ID������
		auto password = md5.CalcMD5FromString(appInfo.password.c_str());
        debug("Received application certificate request, sending...");
		sprintf_s(buf, GET_APPID_RES, appInfo.id.length()+1+strlen(password), appInfo.id.c_str(), password);
		if (!write(buf)) throw SOCKET_WRITE_ERROR;
	}
	else if (action == "SET" && target == "APPID") {
        // Ӧ����֤�ظ�
        auto appOk = (fields[FIELD_ACTION_SUCCESS] == "true");
        if (appOk) {
            debug("Application certificate passed");
        } else {
            error("Application certificate failed: " + body);
        }
    } else if (action == "GET" && target == "USERNAME") {
        // �û���֤����, �����û���������
        debug("Received user certificate request, sending...");
        if (fields[FIELD_LOGIN_SECURE] == "true") {
            // TODO ��ȫ��¼: ��ʱ��֧��
			throw "��Ӧ���ݲ�֧�ְ�ȫ��¼";
        } else {
            // �ǰ�ȫ��¼
            if (fields[FIELD_LOGIN_PASSWORD] == "true") {
                // ��Ҫ��¼����
				auto password = md5.CalcMD5FromString(loginInfo.password.c_str());
                debug("Getting username...");
				sprintf_s(buf, GET_USERNAME_RES, "false", "true", loginInfo.username.length()+1+strlen(password),
					(loginInfo.username+","+password).c_str());
				if (!write(buf)) throw SOCKET_WRITE_ERROR;
			}
			else {
                // ����Ҫ��¼����
                debug("Getting username...");
				sprintf_s(buf, GET_USERNAME_RES, "false", "false", loginInfo.username.length(),
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
        }
    } else if (action == "SET" && target == "MSGKEY") {
        // �յ���Ϣ��Կ
        if (fields[FIELD_LOGIN_SECURE] == "true") {
            // TODO �ݲ�֧�ְ�ȫ��Ϣ
			throw "��Ӧ���ݲ�֧�ְ�ȫ��Ϣ";
        }
		if (!write(SET_MSGKEY_ACK)) throw SOCKET_WRITE_ERROR;
        msgKey = body;
        debug("Received message key: " + msgKey);
		onMsgKeyReceived();
    } else if (action == "SET" && target == "ALIVEINT") {
        // ������������
		if (!write(SET_ALIVEINT_ACK)) throw SOCKET_WRITE_ERROR;
        clientLogon = true;
		clientLogining = false;
        maxInactiveTime = atoi(body.c_str());
        debug("Max inactive time(ms): " + body);
		onMaxInactiveTimeReceived();
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
			// TODO �ݲ�֧��
			throw "��Ӧ�ò�֧�ּ�����Ϣ";
		}
		onMsgReceived(msg);
    } else if (action == "BROADCAST" && target == "MSG") {
        // �յ���Ϣ�㲥�ظ�
        auto msgId = fields[FIELD_ACTION_ID];
        auto success = (fields[FIELD_ACTION_SUCCESS] == "true");
        info("message " + msgId + (success ? " broadcasted." : " broadcast failed: " + body));
    } else if (action == "MULTICAST" && target == "MSG") {
        // �յ���ϢȺ���ظ�
        auto msgId = fields[FIELD_ACTION_ID];
        auto success = (fields[FIELD_ACTION_SUCCESS] == "true");
        info("message " + msgId + (success ? " multicasted." : " multicast failed: " + body));
    } else if (action == "SEND" && target == "MSG") {
        // �յ���Ϣ���ͻظ�
        auto msgId = fields[FIELD_ACTION_ID];
        auto success = (fields[FIELD_ACTION_SUCCESS] == "true");
        info("message " + msgId + (success ? " sent." : " send failed: " + body));
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
		StopComm();
		CloseComm();
		onDisconnected();
        warn("Server kickoff me: " + body);
    } else {
        // �޷����Ķ���
        error("Unknown action: " + action + " " + target);
    }
}

void ClientConnection::startKeepAlive(void)
{
	if (NULL != m_hKeepAlive) CloseHandle(m_hKeepAlive);

	unsigned nThreadId;
	m_hKeepAlive = (HANDLE)_beginthreadex(NULL, 0, keepAlive, this, CREATE_SUSPENDED, &nThreadId);
	ResumeThread(m_hKeepAlive);
}

unsigned __stdcall ClientConnection::keepAlive(void * pThis)
{
	ClientConnection *pConn = static_cast<ClientConnection *>(pThis);
	int& maxInactiveTime = pConn->maxInactiveTime;
	time_t& lastActiveTime = pConn->lastActiveTime;

	const auto checkInterval = 100;
	auto checkedCount = 0;
	auto maxCheckCount = maxInactiveTime / checkInterval / 2;

	char buf[MAX_BUF + 1];

	while (pConn->clientLogon)
	{
		Sleep(checkInterval);

		if (!pConn->IsOpen())
		{
			pConn->StopComm();
			pConn->CloseComm();
			pConn->onDisconnected();
			pConn->warn("Connection broken");
			return 1;
		}

		time_t now;
		time(&now);
		if (now - lastActiveTime > maxInactiveTime / 1000)
		{
			// server gone?
			if (TRACK_SOCKET) pConn->trace("[SOCKET] write to client " + pConn->peerAddress + ": server gone?");
			sprintf_s(buf, CLOSE_CONN_RES, strlen(INACTIVE_TIMEOUT_MSG), INACTIVE_TIMEOUT_MSG);
			if (!pConn->write(buf, true))
			{
				pConn->StopComm();
				pConn->CloseComm();
				pConn->onDisconnected();
				pConn->warn("Write socket failed");
				return 2;
			}
			return 3;
		}

		if (++checkedCount == maxCheckCount)
		{
			pConn->debug("Keep alive...");
			if (TRACK_SOCKET) pConn->trace("[SOCKET] write to client " + pConn->peerAddress + ": " + SET_ALIVE_REQ);
			if (!pConn->write(SET_ALIVE_REQ))
			{
				pConn->StopComm();
				pConn->CloseComm();
				pConn->onDisconnected();
				pConn->warn("Write socket failed");
				return 2;
			}

			checkedCount = 0;
		}
	}
	return 0;
}
