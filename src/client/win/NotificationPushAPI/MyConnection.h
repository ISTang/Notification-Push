#pragma once
#include "protocol.h"
class MyConnection : public ClientConnection
{
public:
	MyConnection();
	~MyConnection();

	void SetTrackPacket(bool bTrackPacket) { m_bTrackPacket = bTrackPacket; }
	void SetConnId(long connId) { m_connId = connId; }

protected:
	virtual void onTextReceived(const std::string& text); // utf8
	virtual void onTextSent(const std::string& text);

	virtual void onLoginStatus(int nStatus);
	virtual void onMsgKeyReceived(const std::string& msgKey); // utf8
	virtual void onMaxInactiveTimeReceived(int nMaxInactiveTime);
	virtual void onMsgReceived(const std::string& msg); // utf8
	virtual void onMsgReplied(const std::string& msgId, bool success, const std::string& error); // utf8

	virtual void error(const std::string& log);
	virtual void warn(const std::string& log);
	virtual void info(const std::string& log);
	virtual void debug(const std::string& log);
	virtual void trace(const std::string& log);

	bool m_bTrackPacket;
	long m_connId;
};

