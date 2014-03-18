// Notification-Push-API.h : main header file for the Notification-Push-API DLL
//

#pragma once

#ifndef __AFXWIN_H__
	#error "include 'stdafx.h' before including this file for PCH"
#endif

#include "resource.h"		// main symbols
#include "protocol.h"

typedef void (CALLBACK *TextReceivedCallbackFunc)(LPCSTR lpszUsername, LPCSTR lpszText); // utf8
typedef void (CALLBACK *TextSentCallbackFunc)(LPCSTR lpszUsername, LPCSTR lpszText);

typedef void (CALLBACK *LoginStatusCallbackFunc)(LPCSTR lpszUsername, int nStatus);
typedef void (CALLBACK *MsgKeyReceivedCallbackFunc)(LPCSTR lpszUsername);
typedef void (CALLBACK *MaxInactiveTimeReceivedCallbackFunc)(LPCSTR lpszUsername);
typedef void (CALLBACK *MsgReceivedCallbackFunc)(LPCSTR lpszUsername, LPCSTR lpszMsg); // utf8

extern std::string g_strAppId;
extern std::string g_strAppPassword;
extern std::string g_strServerHost;
extern int g_nServerPort;

extern TextReceivedCallbackFunc g_lpTextReceivedCallbackFunc;
extern TextSentCallbackFunc g_lpTextSentCallbackFunc;

extern LoginStatusCallbackFunc g_lpLoginStatusCallbackFunc;
extern MsgKeyReceivedCallbackFunc g_lpMsgKeyReceivedCallbackFunc;
extern MaxInactiveTimeReceivedCallbackFunc g_lpMaxInactiveTimeReceivedCallbackFunc;
extern MsgReceivedCallbackFunc g_lpMsgReceivedCallbackFunc;

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
