#include "stdafx.h"
#include "ClientThread.h"
#include "Notification-Push-API.h"

IMPLEMENT_DYNCREATE(CClientThread, CWinThread)

CClientThread::CClientThread()
: m_bTrackPacket(true)
{
}

CClientThread::~CClientThread()
{
}

BOOL CClientThread::InitInstance()
{
	char szPort[10];
	_itoa_s(g_nServerPort, szPort, 10);
	if (!connect())
	{
		return FALSE;
	}

	return TRUE;
}

int CClientThread::ExitInstance()
{
	return 0;
}

void CClientThread::onTextReceived(const std::string& text)
{
	if (m_bTrackPacket && g_lpTextReceivedCallbackFunc!=NULL)
	{
		CA2T strText(text.c_str(), CP_UTF8);
		CT2A strText2(strText.m_psz);
		g_lpTextReceivedCallbackFunc(loginInfo.username.c_str(), std::string(strText2.m_psz).c_str());
	}
}

void CClientThread::onTextSent(const std::string& text)
{
	if (m_bTrackPacket && g_lpTextSentCallbackFunc != NULL)
	{
		g_lpTextSentCallbackFunc(loginInfo.username.c_str(), text.c_str());
	}
}

void CClientThread::onLoginStatus(int nStatus)
{
	if (g_lpLoginStatusCallbackFunc != NULL)
	{
		g_lpLoginStatusCallbackFunc(loginInfo.username.c_str(), nStatus);
	}
}

void CClientThread::onMsgKeyReceived(void)
{
	if (g_lpMsgKeyReceivedCallbackFunc != NULL)
	{
		g_lpMsgKeyReceivedCallbackFunc(loginInfo.username.c_str());
	}
}

void CClientThread::onMaxInactiveTimeReceived(void)
{
	if (g_lpMaxInactiveTimeReceivedCallbackFunc != NULL)
	{
		g_lpMaxInactiveTimeReceivedCallbackFunc(loginInfo.username.c_str());
	}
}

void CClientThread::onMsgReceived(const std::string& msg)
{
	if (g_lpMsgReceivedCallbackFunc != NULL)
	{
		CA2T strText(msg.c_str(), CP_UTF8);
		CT2A strText2(strText.m_psz);
		g_lpMsgReceivedCallbackFunc(loginInfo.username.c_str(), std::string(strText2.m_psz).c_str());
	}
}

void CClientThread::error(const std::string& log)
{
	TRACE("error: %s", log);
}

void CClientThread::warn(const std::string& log)
{
	TRACE("warn: %s", log);
}

void CClientThread::info(const std::string& log)
{
	TRACE("info: %s", log);
}

void CClientThread::debug(const std::string& log)
{
	TRACE("debug: %s", log);
}

void CClientThread::trace(const std::string& log)
{
	TRACE("trace: %s", log);
}

BEGIN_MESSAGE_MAP(CClientThread, CWinThread)
END_MESSAGE_MAP()
