#pragma once
#include "protocol.h"
#include "afxwin.h"
class CTuixinDlg;
class CClientThread :
	public CWinThread, ClientConnection
{
	friend class CTuixinDlg;

	DECLARE_DYNCREATE(CClientThread)

protected:
	CClientThread();           // ��̬������ʹ�õ��ܱ����Ĺ��캯��
	virtual ~CClientThread();

public:
	virtual BOOL InitInstance();
	virtual int ExitInstance();

private:
	virtual void onLoginStatus(int nStatus);
	virtual void onTextReceived(std::string text); // utf8
	virtual void onTextSent(std::string text);

	virtual void onMsgKeyReceived(void);
	virtual void onMaxInactiveTimeReceived(void);
	virtual void onMsgReceived(std::string msg); // utf8

	void outputText(std::string text);

	int m_nClientId;

	// ��������ַ
	CString m_strServer;
	// �������˿�
	int m_nPort;
	// �û���
	CString m_strUsername;
	// ����
	CString m_strPassword;

	bool m_bTrackPacket;

	HWND m_hwndOutput;
	HWND m_hwndStatus;

protected:
	DECLARE_MESSAGE_MAP()
};

