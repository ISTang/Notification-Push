// SocketManager.h: interface for the CSocketManager class.
//
//////////////////////////////////////////////////////////////////////

#if !defined(AFX_SOCKETMANAGER_H__7403BD71_338A_4531_BD91_3D7E5B505793__INCLUDED_)
#define AFX_SOCKETMANAGER_H__7403BD71_338A_4531_BD91_3D7E5B505793__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "SocketComm.h"

using namespace std;

#define WM_UPDATE_CONNECTION	WM_APP+0x1234

#define NEWLINE "\r\n"
#define MAX_INACTIVE_TIME 1000 * 60 // ms
#define KEEP_ALIVE_COUNT 3
#define KEEP_ALIVE_INTERVAL MAX_INACTIVE_TIME / KEEP_ALIVE_COUNT // ms

#define SERVER_BUSY_MSG "Server busy"
#define ID_PROMPT "Your ID: "
#define PASSWORD_PROMPT "Your password: "
#define INVALID_ID_MSG "Invalid ID. Your ID(6 digits): "
#define INVALID_PASSWORD_MSG "Invalid password. Your password: "
#define WAIT_NOTIFY_MSG "Waiting please..."
#define KEEP_ALIVE_MSG "I'm alive"
#define CLIENT_OFFLINE_MSG "Are you offline?"

#define SOCKET_WRITE_TIMEOUT 1000*10

#define ERROR_CANNOT_CONNECT		1
#define ERROR_INVALID_CLIENTINFO	2
#define ERROR_CANNOT_SEND			3
#define ERROR_DISCONNECTED			4

struct ClientInfo {
	string clientId; // 客户ID
	bool serverBusy; // 服务器忙
	bool invalidClientInfo; // 客户信息无效
	bool clientLogon; // 已登录
	bool clientLogining; // 登录中
	bool disconnected; // 连接已断开

	string oldInput; // 已有输入

	ClientInfo(string clientId) {
		this->clientId = clientId;
		this->serverBusy = false;
		this->invalidClientInfo = false;
		this->clientLogon = false;
		this->clientLogining = false;
		this->disconnected = false;

		this->oldInput = "";
	}
};

extern HANDLE g_oLocker;

void RequestLocker();
void ReleaseLocker();

class CSocketManager : public CSocketComm  
{
public:
	CSocketManager(ClientInfo* pClientInfo);
	virtual ~CSocketManager();

public:

	virtual void OnDataReceived(const LPBYTE lpBuffer, DWORD dwCount);
	virtual void OnEvent(UINT uEvent, LPVOID lpvData);

private:
	ClientInfo *m_pClientInfo;
};

#endif // !defined(AFX_SOCKETMANAGER_H__7403BD71_338A_4531_BD91_3D7E5B505793__INCLUDED_)
