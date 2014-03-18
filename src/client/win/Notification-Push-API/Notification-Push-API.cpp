// Notification-Push-API.cpp : Defines the initialization routines for the DLL.
//

#include "stdafx.h"
#include "Notification-Push-API.h"
#include "ClientThread.h"

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
MsgKeyReceivedCallbackFunc g_lpMsgKeyReceivedCallbackFunc;
MaxInactiveTimeReceivedCallbackFunc g_lpMaxInactiveTimeReceivedCallbackFunc;
MsgReceivedCallbackFunc g_lpMsgReceivedCallbackFunc;


// ����Ӧ����Ϣ(ID���������Կ)
extern "C" void PASCAL EXPORT SetAppInfo(LPCSTR lpszId, LPCSTR lpszPassword, LPCSTR lpszProtectKey)
{
	g_strAppId = lpszId;
	g_strAppPassword = lpszPassword;
}

// ���÷�������Ϣ(�����Ͷ˿�)
extern "C" void PASCAL EXPORT SetServerInfo(LPCSTR lpszHost, int nPort)
{
	g_strServerHost = lpszHost;
	g_nServerPort = nPort;
}

// ���ûص�����
extern "C" void PASCAL EXPORT SetTextReceivedCallbackFunc(TextReceivedCallbackFunc lpTextReceivedCallbackFunc)
{
	g_lpTextReceivedCallbackFunc = lpTextReceivedCallbackFunc;
}
extern "C" void PASCAL EXPORT SetTextSentCallbackFunc(TextSentCallbackFunc lpTextSentCallbackFunc)
{
	g_lpTextSentCallbackFunc = lpTextSentCallbackFunc;
}
extern "C" void PASCAL EXPORT SetLoginStatusCallbackFunc(LoginStatusCallbackFunc lpLoginStatusCallbackFunc)
{
	g_lpLoginStatusCallbackFunc = lpLoginStatusCallbackFunc;
}
extern "C" void PASCAL EXPORT SetMsgKeyReceivedCallbackFunc(MsgKeyReceivedCallbackFunc lpMsgKeyReceivedCallbackFunc)
{
	g_lpMsgKeyReceivedCallbackFunc = lpMsgKeyReceivedCallbackFunc;
}
extern "C" void PASCAL EXPORT SetMaxInactiveTimeReceivedCallbackFunc(MaxInactiveTimeReceivedCallbackFunc lpMaxInactiveTimeReceivedCallbackFunc)
{
	g_lpMaxInactiveTimeReceivedCallbackFunc = lpMaxInactiveTimeReceivedCallbackFunc;
}
extern "C" void PASCAL EXPORT SetMsgReceivedCallbackFunc(MsgReceivedCallbackFunc lpMsgReceivedCallbackFunc)
{
	g_lpMsgReceivedCallbackFunc = lpMsgReceivedCallbackFunc;
}

// ���÷�������Ϣ(�����Ͷ˿�)
extern "C" bool PASCAL EXPORT LoginAsUser(LPCSTR lpszUsername, LPCSTR lpszPassword, bool bTrackPacket=true)
{
	AFX_MANAGE_STATE(AfxGetStaticModuleState());

	// �����ͻ���UI�߳�
	CClientThread *pClientThread = (CClientThread *)AfxBeginThread(RUNTIME_CLASS(CClientThread),
		THREAD_PRIORITY_NORMAL, CREATE_SUSPENDED);

	// �����̲߳���
	pClientThread->setAppInfo(AppInfo(g_strAppId, g_strAppPassword, ""));
	pClientThread->setLoginInfo(LoginInfo(lpszUsername, lpszPassword));
	pClientThread->setServerInfo(g_strServerHost, g_nServerPort);
	pClientThread->SetTrackPacket(bTrackPacket);

	// �����߳�
	pClientThread->ResumeThread();

	// TODO ��¼�µ��߳�
	return true;
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
	// TODO �Զ��رջ�߳�

	return CWinApp::ExitInstance();
}
