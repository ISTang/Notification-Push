#pragma once
#include "protocol.h"
#include "afxwin.h"
class CClientThread :
	public CWinThread, public ClientConnection
{
	DECLARE_DYNCREATE(CClientThread)

protected:
	CClientThread();
	virtual ~CClientThread();

public:
	virtual BOOL InitInstance();
	virtual int ExitInstance();

	void SetTrackPacket(bool bTrackPacket) { m_bTrackPacket = bTrackPacket; }

protected:
	virtual void onTextReceived(const std::string& text); // utf8
	virtual void onTextSent(const std::string& text);

	virtual void onLoginStatus(int nStatus);
	virtual void onMsgKeyReceived(void);
	virtual void onMaxInactiveTimeReceived(void);
	virtual void onMsgReceived(const std::string& msg); // utf8

	virtual void error(const std::string& log);
	virtual void warn(const std::string& log);
	virtual void info(const std::string& log);
	virtual void debug(const std::string& log);
	virtual void trace(const std::string& log);

	bool m_bTrackPacket;

protected:
	DECLARE_MESSAGE_MAP()
};

