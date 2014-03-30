#include "stdafx.h"
#include "MyConnection.h"
#include "NotificationPushAPI.h"

MyConnection::MyConnection()
: m_bTrackPacket(true)
{
}

MyConnection::~MyConnection()
{
}

void MyConnection::onTextReceived(const std::string& text)
{
	if (m_bTrackPacket && g_lpTextReceivedCallbackFunc!=NULL)
	{
		CA2T strText(text.c_str(), CP_UTF8);
		CT2A strText2(strText.m_psz);
		g_lpTextReceivedCallbackFunc(m_connId, std::string(strText2.m_psz).c_str());
	}
}

void MyConnection::onTextSent(const std::string& text)
{
	if (m_bTrackPacket && g_lpTextSentCallbackFunc != NULL)
	{
		g_lpTextSentCallbackFunc(m_connId, text.c_str());
	}
}

void MyConnection::onLoginStatus(int nStatus)
{
	if (g_lpLoginStatusCallbackFunc != NULL)
	{
		g_lpLoginStatusCallbackFunc(m_connId, nStatus);
	}
}

void MyConnection::onAppCheckFailed(const std::string& reason)
{
	if (g_lpLoginFailedCallbackFunc != NULL)
	{
		CA2T strText(reason.c_str(), CP_UTF8);
		CT2A strText2(strText.m_psz);
		g_lpLoginFailedCallbackFunc(m_connId, std::string(strText2.m_psz).c_str());
	}
}

void MyConnection::onUserCheckFailed(const std::string& reason)
{
	if (g_lpLoginFailedCallbackFunc != NULL)
	{
		CA2T strText(reason.c_str(), CP_UTF8);
		CT2A strText2(strText.m_psz);
		g_lpLoginFailedCallbackFunc(m_connId, std::string(strText2.m_psz).c_str());
	}
}

void MyConnection::onMsgKeyReceived(const std::string& msgKey)
{
	if (g_lpMsgKeyReceivedCallbackFunc != NULL)
	{
		CA2T strText(msgKey.c_str(), CP_UTF8);
		CT2A strText2(strText.m_psz);
		g_lpMsgKeyReceivedCallbackFunc(m_connId, std::string(strText2.m_psz).c_str());
	}
}

void MyConnection::onMaxInactiveTimeReceived(int nMaxInactiveTime)
{
	if (g_lpMaxInactiveTimeReceivedCallbackFunc != NULL)
	{
		g_lpMaxInactiveTimeReceivedCallbackFunc(m_connId, nMaxInactiveTime);
	}
}

void MyConnection::onMsgReceived(const std::string& msg)
{
	if (g_lpMsgReceivedCallbackFunc != NULL)
	{
		CA2T strText(msg.c_str(), CP_UTF8);
		CT2A strText2(strText.m_psz);
		g_lpMsgReceivedCallbackFunc(m_connId, std::string(strText2.m_psz).c_str());
	}
}

void MyConnection::onMsgReplied(const std::string& msgId, bool success, const std::string& error)
{
	if (g_lpMsgRepliedCallbackFunc != NULL)
	{
		CA2T strMsgId(msgId.c_str(), CP_UTF8);
		CT2A strMsgId2(strMsgId.m_psz);
		CA2T strError(error.c_str(), CP_UTF8);
		CT2A strError2(strError.m_psz);
		g_lpMsgRepliedCallbackFunc(m_connId, std::string(strMsgId2.m_psz).c_str(), success, std::string(strError2.m_psz).c_str());
	}
}

void MyConnection::error(const std::string& log)
{
	if (g_lpLogCallbackFunc!=NULL)
	{
		CA2T strLog(log.c_str(), CP_UTF8);
		CT2A strLog2(strLog.m_psz);
		g_lpLogCallbackFunc(m_connId, strLog2.m_psz, LOG_ERROR);
	}
}

void MyConnection::warn(const std::string& log)
{
	if (g_lpLogCallbackFunc!=NULL)
	{
		CA2T strLog(log.c_str(), CP_UTF8);
		CT2A strLog2(strLog.m_psz);
		g_lpLogCallbackFunc(m_connId, strLog2.m_psz, LOG_WARN);
	}
}

void MyConnection::info(const std::string& log)
{
	if (g_lpLogCallbackFunc!=NULL)
	{
		CA2T strLog(log.c_str(), CP_UTF8);
		CT2A strLog2(strLog.m_psz);
		g_lpLogCallbackFunc(m_connId, strLog2.m_psz, LOG_INFO);
	}
}

void MyConnection::debug(const std::string& log)
{
	if (g_lpLogCallbackFunc!=NULL)
	{
		CA2T strLog(log.c_str(), CP_UTF8);
		CT2A strLog2(strLog.m_psz);
		g_lpLogCallbackFunc(m_connId, strLog2.m_psz, LOG_DEBUG);
	}
}

void MyConnection::trace(const std::string& log)
{
	if (g_lpLogCallbackFunc!=NULL)
	{
		CA2T strLog(log.c_str(), CP_UTF8);
		CT2A strLog2(strLog.m_psz);
		g_lpLogCallbackFunc(m_connId, strLog2.m_psz, LOG_TRACE);
	}
}
