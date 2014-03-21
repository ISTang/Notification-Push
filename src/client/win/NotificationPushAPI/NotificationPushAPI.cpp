// Notification-Push-API.cpp : Defines the initialization routines for the DLL.
//

#include "stdafx.h"
#define _BUILD_DLL_
#include "NotificationPushAPI.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#endif

std::string g_strAppId;
std::string g_strAppPassword;
std::string g_strServerHost;
int g_nServerPort;

TextReceivedCallbackFunc g_lpTextReceivedCallbackFunc;
TextSentCallbackFunc g_lpTextSentCallbackFunc;

LoginStatusCallbackFunc g_lpLoginStatusCallbackFunc;
LogCallbackFunc g_lpLogCallbackFunc;
MsgKeyReceivedCallbackFunc g_lpMsgKeyReceivedCallbackFunc;
MaxInactiveTimeReceivedCallbackFunc g_lpMaxInactiveTimeReceivedCallbackFunc;
MsgReceivedCallbackFunc g_lpMsgReceivedCallbackFunc;
MsgRepliedCallbackFunc g_lpMsgRepliedCallbackFunc;

std::map<long, MyConnection*> g_connections;

// ����Ӧ����Ϣ(ID���������Կ)
void CALLBACK SetAppInfo(LPCSTR lpszId, LPCSTR lpszPassword, LPCSTR lpszProtectKey)
{
	g_strAppId = lpszId;
	g_strAppPassword = lpszPassword;
}

// ���÷�������Ϣ(�����Ͷ˿�)
void CALLBACK SetServerInfo(LPCSTR lpszHost, int nPort)
{
	g_strServerHost = lpszHost;
	g_nServerPort = nPort;
}

// ���ûص�����
void CALLBACK SetTextReceivedCallbackFunc(TextReceivedCallbackFunc lpTextReceivedCallbackFunc)
{
	g_lpTextReceivedCallbackFunc = lpTextReceivedCallbackFunc;
}
void CALLBACK SetTextSentCallbackFunc(TextSentCallbackFunc lpTextSentCallbackFunc)
{
	g_lpTextSentCallbackFunc = lpTextSentCallbackFunc;
}
void CALLBACK SetLoginStatusCallbackFunc(LoginStatusCallbackFunc lpLoginStatusCallbackFunc)
{
	g_lpLoginStatusCallbackFunc = lpLoginStatusCallbackFunc;
}
void CALLBACK SetLogCallbackFunc(LogCallbackFunc lpLogCallbackFunc)
{
	g_lpLogCallbackFunc = lpLogCallbackFunc;
}
void CALLBACK SetMsgKeyReceivedCallbackFunc(MsgKeyReceivedCallbackFunc lpMsgKeyReceivedCallbackFunc)
{
	g_lpMsgKeyReceivedCallbackFunc = lpMsgKeyReceivedCallbackFunc;
}
void CALLBACK SetMaxInactiveTimeReceivedCallbackFunc(MaxInactiveTimeReceivedCallbackFunc lpMaxInactiveTimeReceivedCallbackFunc)
{
	g_lpMaxInactiveTimeReceivedCallbackFunc = lpMaxInactiveTimeReceivedCallbackFunc;
}
void CALLBACK SetMsgReceivedCallbackFunc(MsgReceivedCallbackFunc lpMsgReceivedCallbackFunc)
{
	g_lpMsgReceivedCallbackFunc = lpMsgReceivedCallbackFunc;
}
void CALLBACK SetMsgRepliedCallbackFunc(MsgRepliedCallbackFunc lpMsgRepliedCallbackFunc)
{
	g_lpMsgRepliedCallbackFunc = lpMsgRepliedCallbackFunc;
}

// �û���¼
bool CALLBACK LoginAsUser(long connId, LPCSTR lpszUsername, LPCSTR lpszPassword, bool bTrackPacket)
{
	AFX_MANAGE_STATE(AfxGetStaticModuleState());

	// �������Ӷ���
	MyConnection *pMyConnection = new MyConnection();

	// �������Ӳ���
	pMyConnection->setAppInfo(AppInfo(g_strAppId, g_strAppPassword, ""));
	pMyConnection->setLoginInfo(LoginInfo(lpszUsername, lpszPassword));
	pMyConnection->setServerInfo(g_strServerHost, g_nServerPort);
	pMyConnection->SetTrackPacket(bTrackPacket);
	pMyConnection->SetConnId(connId);

	// ��ʼ����
	bool ok = pMyConnection->connect();
	if (!ok) return false;

	// ��������
	g_connections[connId] = pMyConnection;

	return true;
}

// �Ͽ�����
bool CALLBACK Disconnect(long connId)
{
	AFX_MANAGE_STATE(AfxGetStaticModuleState());

	MyConnection *pConnection = g_connections[connId];
	if (pConnection==NULL) return false;

	pConnection->disconnect();
	return true;
}

// ������Ϣ
bool CALLBACK SendMessageTo(long connId, LPCSTR lpszReceiver, LPCSTR lpszMsgId, LPCSTR lpszMsg, bool bSecure)
{
	AFX_MANAGE_STATE(AfxGetStaticModuleState());

	MyConnection *pConnection = g_connections[connId];
	if (pConnection==NULL) return false;

	return pConnection->send(lpszReceiver, lpszMsgId, lpszMsg, bSecure);
}

// Ⱥ����Ϣ
bool CALLBACK MulticastMessage(long connId, LPCSTR *lpszReceivers, LPCSTR lpszMsgId, LPCSTR lpszMsg, bool bSecure)
{
	AFX_MANAGE_STATE(AfxGetStaticModuleState());

	MyConnection *pConnection = g_connections[connId];
	if (pConnection==NULL) return false;

	std::vector<std::string> receivers;
	for (LPCSTR lpszReceiver = *lpszReceivers; *lpszReceiver!=NULL; lpszReceiver++) {
		receivers.push_back(lpszReceiver);
	}
	return pConnection->multicast(receivers, lpszMsgId, lpszMsg, bSecure);
}

// �㲥��Ϣ
bool CALLBACK BroadcastMessage(long connId, LPCSTR lpszMsgId, LPCSTR lpszMsg, bool bSecure)
{
	AFX_MANAGE_STATE(AfxGetStaticModuleState());

	MyConnection *pConnection = g_connections[connId];
	if (pConnection==NULL) return false;

	return pConnection->broadcast(lpszMsgId, lpszMsg, bSecure);
}


// CNotificationPushAPIApp

BEGIN_MESSAGE_MAP(CNotificationPushAPIApp, CWinApp)
END_MESSAGE_MAP()


// CNotificationPushAPIApp construction
CNotificationPushAPIApp::CNotificationPushAPIApp()
{
}


// The one and only CNotificationPushAPIApp objec
CNotificationPushAPIApp theApp;


// CNotificationPushAPIApp initialization
BOOL CNotificationPushAPIApp::InitInstance()
{
	CWinApp::InitInstance();

	if (!AfxSocketInit())
	{
		AfxMessageBox(IDP_SOCKETS_INIT_FAILED);
		return FALSE;
	}

	return TRUE;
}

// CNotificationPushAPIApp destroy
int CNotificationPushAPIApp::ExitInstance()
{
	for (auto i=g_connections.begin(); i!=g_connections.end(); ++i) {
		MyConnection *pConnection = i->second;
		delete pConnection;
	}

	return CWinApp::ExitInstance();
}
