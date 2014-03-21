// Notification-Push-API.h : main header file for the Notification-Push-API DLL
//

#pragma once

#ifndef __AFXWIN_H__
	#error "include 'stdafx.h' before including this file for PCH"
#endif

#define MYLIBRARY_EXPORTS

#include "resource.h"		// main symbols
#include "protocol.h"
#include "NotificationPushAPI_proto.h"
#include "MyConnection.h"

extern std::string g_strAppId;
extern std::string g_strAppPassword;
extern std::string g_strServerHost;
extern int g_nServerPort;

extern TextReceivedCallbackFunc g_lpTextReceivedCallbackFunc;
extern TextSentCallbackFunc g_lpTextSentCallbackFunc;

extern LoginStatusCallbackFunc g_lpLoginStatusCallbackFunc;
extern LogCallbackFunc g_lpLogCallbackFunc;
extern MsgKeyReceivedCallbackFunc g_lpMsgKeyReceivedCallbackFunc;
extern MaxInactiveTimeReceivedCallbackFunc g_lpMaxInactiveTimeReceivedCallbackFunc;
extern MsgReceivedCallbackFunc g_lpMsgReceivedCallbackFunc;
extern MsgRepliedCallbackFunc g_lpMsgRepliedCallbackFunc;
extern LogCallbackFunc g_lpLogCallbackFunc;

extern std::map<long, MyConnection*> g_connections;

// CNotificationPushAPIApp
//
class CNotificationPushAPIApp : public CWinApp
{
public:
	CNotificationPushAPIApp();

// Overrides
public:
	virtual BOOL InitInstance();
	virtual BOOL ExitInstance();

	DECLARE_MESSAGE_MAP()
};
