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


// 设置应用信息(ID、密码和密钥)
extern "C" void PASCAL EXPORT SetAppInfo(LPCSTR lpszId, LPCSTR lpszPassword, LPCSTR lpszProtectKey)
{
	g_strAppId = lpszId;
	g_strAppPassword = lpszPassword;
}

// 设置服务器信息(主机和端口)
extern "C" void PASCAL EXPORT SetServerInfo(LPCSTR lpszHost, int nPort)
{
	g_strServerHost = lpszHost;
	g_nServerPort = nPort;
}

// 设置回调函数
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

// 设置服务器信息(主机和端口)
extern "C" bool PASCAL EXPORT LoginAsUser(LPCSTR lpszUsername, LPCSTR lpszPassword, bool bTrackPacket=true)
{
	AFX_MANAGE_STATE(AfxGetStaticModuleState());

	// 创建客户端UI线程
	CClientThread *pClientThread = (CClientThread *)AfxBeginThread(RUNTIME_CLASS(CClientThread),
		THREAD_PRIORITY_NORMAL, CREATE_SUSPENDED);

	// 设置线程参数
	pClientThread->setAppInfo(AppInfo(g_strAppId, g_strAppPassword, ""));
	pClientThread->setLoginInfo(LoginInfo(lpszUsername, lpszPassword));
	pClientThread->setServerInfo(g_strServerHost, g_nServerPort);
	pClientThread->SetTrackPacket(bTrackPacket);

	// 启动线程
	pClientThread->ResumeThread();

	// TODO 记录新的线程
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
	// TODO 自动关闭活动线程

	return CWinApp::ExitInstance();
}
