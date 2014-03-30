// Notification-Push-API_proto.h

#pragma once

#ifndef __AFXWIN_H__
	#error "include 'stdafx.h' before including this file for PCH"
#endif

enum LoginStatus { LOGOUT=0, LOGINING=1, LOGON=2 };
enum LogLevel { LOG_TRACE=0, LOG_DEBUG=1, LOG_INFO=2, LOG_WARN=3, LOG_ERROR=4 };

typedef void (CALLBACK *TextReceivedCallbackFunc)(long connId, LPCSTR lpszText);
typedef void (CALLBACK *TextSentCallbackFunc)(long connId, LPCSTR lpszText);

typedef void (CALLBACK *LoginStatusCallbackFunc)(long connId, int nStatus);
typedef void (CALLBACK *LoginFailedCallbackFunc)(long connId, LPCSTR lpszReason);
typedef void (CALLBACK *LogCallbackFunc)(long connId, LPCSTR lpszLogText, int nLogLevel);
typedef void (CALLBACK *MsgKeyReceivedCallbackFunc)(long connId, LPCSTR lpszMsgKey);
typedef void (CALLBACK *MaxInactiveTimeReceivedCallbackFunc)(long connId, int nMaxInactiveTime);
typedef void (CALLBACK *MsgReceivedCallbackFunc)(long connId, LPCSTR lpszMsg);
typedef void (CALLBACK *MsgRepliedCallbackFunc)(long connId, LPCSTR lpszMsgId, bool bSuccess, LPCSTR lpszError);

#ifdef __cplusplus
extern "C" {
#endif

#ifdef  _BUILD_DLL_
#define FUNCTION    __declspec(dllexport)
#else
#define FUNCTION    __declspec(dllimport)
#endif

// ����Ӧ����Ϣ(ID���������Կ)
FUNCTION void CALLBACK SetAppInfo(LPCSTR lpszId, LPCSTR lpszPassword, LPCSTR lpszProtectKey);

// ���÷�������Ϣ(�����Ͷ˿�)
FUNCTION void CALLBACK SetServerInfo(LPCSTR lpszHost, int nPort);

// ���ûص�����
FUNCTION void CALLBACK SetTextReceivedCallbackFunc(TextReceivedCallbackFunc lpTextReceivedCallbackFunc);
FUNCTION void CALLBACK SetTextSentCallbackFunc(TextSentCallbackFunc lpTextSentCallbackFunc);
//
FUNCTION void CALLBACK SetMsgKeyReceivedCallbackFunc(MsgKeyReceivedCallbackFunc lpMsgKeyReceivedCallbackFunc);
FUNCTION void CALLBACK SetMaxInactiveTimeReceivedCallbackFunc(MaxInactiveTimeReceivedCallbackFunc lpMaxInactiveTimeReceivedCallbackFunc);
//
FUNCTION void CALLBACK SetMsgReceivedCallbackFunc(MsgReceivedCallbackFunc lpMsgReceivedCallbackFunc);
FUNCTION void CALLBACK SetMsgRepliedCallbackFunc(MsgRepliedCallbackFunc lpMsgRepliedCallbackFunc);
//
FUNCTION void CALLBACK SetLoginStatusCallbackFunc(LoginStatusCallbackFunc lpLoginStatusCallbackFunc);
FUNCTION void CALLBACK SetLoginFailedCallbackFunc(LoginFailedCallbackFunc lpLoginFailedCallbackFunc);
FUNCTION void CALLBACK SetLogCallbackFunc(LogCallbackFunc lpLogCallbackFunc);

// �û���¼
FUNCTION bool CALLBACK LoginAsUser(long connId, LPCSTR lpszUsername, LPCSTR lpszPassword, 
	bool bAutoReconnect = false, int nReconnectDelay=1000, bool bTrackPacket = true);
// �Ͽ�����
FUNCTION bool CALLBACK Disconnect(long connId);

// ������Ϣ
FUNCTION bool CALLBACK SendMessageTo(long connId, LPCSTR lpszReceiver, LPCSTR lpszMsgId, LPCSTR lpszMsg, bool bSecure = false);
// Ⱥ����Ϣ
FUNCTION bool CALLBACK MulticastMessage(long connId, LPCSTR *lpszReceivers, LPCSTR lpszMsgId, LPCSTR lpszMsg, bool bSecure = false);
// �㲥��Ϣ
FUNCTION bool CALLBACK BroadcastMessage(long connId, LPCSTR lpszMsgId, LPCSTR lpszMsg, bool bSecure = false);

#ifdef __cplusplus
}
#endif
