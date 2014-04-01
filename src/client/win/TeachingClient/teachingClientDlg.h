
// teachingClientDlg.h : ͷ�ļ�
//

#pragma once
#include "afxwin.h"
#include "SystemTray.h"
#include "cMD5.h"

#define WM_ICON_NOTIFY  WM_APP+10

//const int AUTO_RECONNECT_INTERVAL = 1000 * 30;
const int MAX_STATUS_LINES = 100;
const int MAX_MSG_COUNT = 20;

// CTeachingClientDlg �Ի���
class CTeachingClientDlg : public CDialogEx
{
private:
	CSystemTray m_TrayIcon;
	cMD5 md5;

protected:
	static void CALLBACK onTextReceived(long connId, LPCSTR lpszText);
	static void CALLBACK onTextSent(long connId, LPCSTR lpszText);

	static void CALLBACK onLoginStatus(long connId, int nStatus);
	static void CALLBACK onLoginFailed(long connId, LPCSTR lpszReason);
	static void CALLBACK onLog(long connId, LPCSTR lpszLogText, int nLogLevel);
	static void CALLBACK onMsgKeyReceived(long connId, LPCSTR lpszMsgKey);
	static void CALLBACK onMaxInactiveTimeReceived(long connId, int nMaxInactiveTime);
	static void CALLBACK onMsgReceived(long connId, LPCSTR lpszMsg);
	static void CALLBACK onMsgReplied(long connId, LPCSTR lpszMsgId, bool bSuccess, LPCSTR lpszError);

// ����
public:
	CTeachingClientDlg(CWnd* pParent = NULL);	// ��׼���캯��

// �Ի�������
	enum { IDD = IDD_TEACHINGCLIENT_DIALOG };

	protected:
	virtual void DoDataExchange(CDataExchange* pDX);	// DDX/DDV ֧��
	virtual void OnOK();
	virtual void OnCancel();

// ʵ��
protected:
	HICON m_hIcon;

	// ���ɵ���Ϣӳ�亯��
	virtual BOOL OnInitDialog();
	LRESULT OnTrayNotification(WPARAM wParam, LPARAM lParam);
	afx_msg void OnSysCommand(UINT nID, LPARAM lParam);
	afx_msg void OnPaint();
	afx_msg HCURSOR OnQueryDragIcon();
	afx_msg void OnAppShow();
	afx_msg void OnClose();
	afx_msg void OnSendMsg();
	afx_msg void OnBroadcastMsg();
	DECLARE_MESSAGE_MAP()

private:
	void DoConnect();
	void AppendChatMsg(const CString& strChatMsg);

	// ��������ַ
	CString m_strServer;
	// �������˿�
	int m_nPort;
	// �û���
	CString m_strUsername;
	// ����
	CString m_strPassword;
	// �������ʱ��(ms)
	int m_nReconnectDelay;
	// ���
	CEdit m_edtOutput;
	// ������(����ö��Ÿ���)
	CString m_strReceivers;
	// Ҫ���͵���Ϣ
	CString m_strMsgToPush;
	// ���ͺ��Ƿ��Զ����
	BOOL m_bClearWhenSent;
	// �Ƿ���ٷ���
	BOOL m_bTrackPacket;

	int m_nLoginStatus;
	int m_nMsgCount;
};
