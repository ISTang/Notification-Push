
// tuixinDlg.h : ͷ�ļ�
//

#pragma once
#include "afxwin.h"

const int AUTO_RECONNECT_INTERVAL = 1000 * 30;

// CTuixinDlg �Ի���
class CTuixinDlg : public CDialogEx
{
protected:
	static void CALLBACK onTextReceived(long connId, LPCSTR lpszText);
	static void CALLBACK onTextSent(long connId, LPCSTR lpszText);

	static void CALLBACK onLoginStatus(long connId, int nStatus);
	static void CALLBACK onLog(long connId, LPCSTR lpszLogText, int nLogLevel);
	static void CALLBACK onMsgKeyReceived(long connId, LPCSTR lpszMsgKey);
	static void CALLBACK onMaxInactiveTimeReceived(long connId, int nMaxInactiveTime);
	static void CALLBACK onMsgReceived(long connId, LPCSTR lpszMsg);
	static void CALLBACK onMsgReplied(long connId, LPCSTR lpszMsgId, bool bSuccess, LPCSTR lpszError);

// ����
public:
	CTuixinDlg(CWnd* pParent = NULL);	// ��׼���캯��

// �Ի�������
	enum { IDD = IDD_TUIXIN_DIALOG };

	protected:
	virtual void DoDataExchange(CDataExchange* pDX);	// DDX/DDV ֧��


// ʵ��
protected:
	HICON m_hIcon;

	// ���ɵ���Ϣӳ�亯��
	virtual BOOL OnInitDialog();
	afx_msg void OnSysCommand(UINT nID, LPARAM lParam);
	afx_msg void OnPaint();
	afx_msg HCURSOR OnQueryDragIcon();
	afx_msg void OnConnect();
	afx_msg void OnDisconnect();
	DECLARE_MESSAGE_MAP()

private:
	// ��������ַ
	CString m_strServer;
	// �������˿�
	int m_nPort;
	// �û���
	CString m_strUsername;
	// ����
	CString m_strPassword;
	// ���
	CEdit m_edtOutput;
	// �߳���
	int m_nThreads;

	bool m_bTrackPacket;
};
