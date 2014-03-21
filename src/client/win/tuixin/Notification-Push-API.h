// Notification-Push-API.h : main header file for the Notification-Push-API DLL
//

#pragma once

#ifndef __AFXWIN_H__
	#error "include 'stdafx.h' before including this file for PCH"
#endif

#define DllImport   __declspec( dllimport )

typedef void (CALLBACK *TextReceivedCallbackFunc)(LPCSTR lpszUsername, LPCSTR lpszText); // utf8
typedef void (CALLBACK *TextSentCallbackFunc)(LPCSTR lpszUsername, LPCSTR lpszText);

typedef void (CALLBACK *LoginStatusCallbackFunc)(LPCSTR lpszUsername, int nStatus);
typedef void (CALLBACK *MsgKeyReceivedCallbackFunc)(LPCSTR lpszUsername);
typedef void (CALLBACK *MaxInactiveTimeReceivedCallbackFunc)(LPCSTR lpszUsername);
typedef void (CALLBACK *MsgReceivedCallbackFunc)(LPCSTR lpszUsername, LPCSTR lpszMsg); // utf8

// 设置应用信息(ID、密码和密钥)
DllImport void PASCAL SetAppInfo(LPCSTR lpszId, LPCSTR lpszPassword, LPCSTR lpszProtectKey);

// 设置服务器信息(主机和端口)
DllImport void PASCAL SetServerInfo(LPCSTR lpszHost, int nPort);

// 设置回调函数
DllImport void PASCAL SetTextReceivedCallbackFunc(TextReceivedCallbackFunc lpTextReceivedCallbackFunc);
DllImport void PASCAL SetTextSentCallbackFunc(TextSentCallbackFunc lpTextSentCallbackFunc);
DllImport void PASCAL SetMsgKeyReceivedCallbackFunc(MsgKeyReceivedCallbackFunc lpMsgKeyReceivedCallbackFunc);
DllImport void PASCAL SetMaxInactiveTimeReceivedCallbackFunc(MaxInactiveTimeReceivedCallbackFunc lpMaxInactiveTimeReceivedCallbackFunc);
DllImport void PASCAL SetMsgReceivedCallbackFunc(MsgReceivedCallbackFunc lpMsgReceivedCallbackFunc);

// 设置服务器信息(主机和端口)
DllImport bool PASCAL LoginAsUser(LPCSTR lpszUsername, LPCSTR lpszPassword, bool bTrackPacket = true);
