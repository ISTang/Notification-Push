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

// ����Ӧ����Ϣ(ID���������Կ)
DllImport void PASCAL SetAppInfo(LPCSTR lpszId, LPCSTR lpszPassword, LPCSTR lpszProtectKey);

// ���÷�������Ϣ(�����Ͷ˿�)
DllImport void PASCAL SetServerInfo(LPCSTR lpszHost, int nPort);

// ���ûص�����
DllImport void PASCAL SetTextReceivedCallbackFunc(TextReceivedCallbackFunc lpTextReceivedCallbackFunc);
DllImport void PASCAL SetTextSentCallbackFunc(TextSentCallbackFunc lpTextSentCallbackFunc);
DllImport void PASCAL SetMsgKeyReceivedCallbackFunc(MsgKeyReceivedCallbackFunc lpMsgKeyReceivedCallbackFunc);
DllImport void PASCAL SetMaxInactiveTimeReceivedCallbackFunc(MaxInactiveTimeReceivedCallbackFunc lpMaxInactiveTimeReceivedCallbackFunc);
DllImport void PASCAL SetMsgReceivedCallbackFunc(MsgReceivedCallbackFunc lpMsgReceivedCallbackFunc);

// ���÷�������Ϣ(�����Ͷ˿�)
DllImport bool PASCAL LoginAsUser(LPCSTR lpszUsername, LPCSTR lpszPassword, bool bTrackPacket = true);
