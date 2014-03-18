#include "stdafx.h"
#include "ClientThread.h"

static CMutex g_clsMutex(FALSE, NULL);

IMPLEMENT_DYNCREATE(CClientThread, CWinThread)

CClientThread::CClientThread()
{
}


CClientThread::~CClientThread()
{
}

BOOL CClientThread::InitInstance()
{
	CT2A strUsername(m_strUsername);
	CT2A strPassword(m_strPassword);
	setAppInfo(AppInfo("5A6D032A-DB6C-43BF-98EE-A699FBCAA628"/*4083AD3D-0F41-B78E-4F5D-F41A515F2667"*/, "THHgux8k"/*@0Vd*4Ak"*/,"cpuHCK9V"/*"n9SfmcRs"*/));
	setLoginInfo(LoginInfo(strUsername.m_psz, strPassword.m_psz));

	char szPort[10];
	_itoa_s(m_nPort, szPort, 10);
	CW2A server(m_strServer);
	outputText("正在连接...");
	if (!connect(server.m_psz, m_nPort))
	{
		outputText("连接失败！");
	}

	return TRUE;
}

int CClientThread::ExitInstance()
{
	return 0;
}

void CClientThread::onTextReceived(std::string text)
{
	if (m_bTrackPacket)
	{
		CA2T strText(text.c_str(), CP_UTF8);
		CT2A strText2(strText.m_psz);
		outputText("收到文本: " + std::string(strText2.m_psz));
	}
}

void CClientThread::onTextSent(std::string text)
{
	if (m_bTrackPacket) outputText("发送文本: " + text);
}

void CClientThread::onLoginStatus(int nStatus)
{
	ClientConnection::onLoginStatus(nStatus);

	switch (nStatus)
	{
	case LOGOUT:
		outputText("已断开连接");
		break;
	case LOGINING:
		outputText("正在登录...");
		break;
	case LOGON:
		outputText("登录成功");
		break;
	}
}

void CClientThread::onMsgKeyReceived(void)
{
	outputText("收到消息密钥: " + msgKey);
}

void CClientThread::onMaxInactiveTimeReceived(void)
{
	char szTemp[10];
	_itoa_s(maxInactiveTime, szTemp, 10);
	outputText("收到允许不活跃时长(ms): " + std::string(szTemp));
}

void CClientThread::onMsgReceived(std::string msg)
{
	CA2T strText(msg.c_str(), CP_UTF8);
	CT2A strText2(strText.m_psz);
	outputText("收到消息: " + std::string(strText2.m_psz));
}

void CClientThread::outputText(std::string text)
{
	//CWnd *pwndOutput = CWnd::FromHandle(m_hwndOutput);
	CListBox *plstStatus = (CListBox *)CWnd::FromHandle(m_hwndStatus);

	//CString strOld;
	//pwndOutput->GetWindowText(strOld);

	char szClientId[10]; sprintf_s(szClientId, "%05d", m_nClientId+1);
	CA2T strText((std::string("#") + szClientId + ": " + text + "\r\n").c_str());

	//CString strNew;
	//strNew = strText.m_psz + strOld;
	//pwndOutput->SetWindowText(strNew);

	g_clsMutex.Lock();
	plstStatus->DeleteString(m_nClientId);
	plstStatus->InsertString(m_nClientId, strText);
	g_clsMutex.Unlock();
}

BEGIN_MESSAGE_MAP(CClientThread, CWinThread)
END_MESSAGE_MAP()
