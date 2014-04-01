
// teachingClientDlg.h : 头文件
//

#pragma once
#include "afxwin.h"
#include "SystemTray.h"
#include "cMD5.h"

#define WM_ICON_NOTIFY  WM_APP+10

//const int AUTO_RECONNECT_INTERVAL = 1000 * 30;
const int MAX_STATUS_LINES = 100;
const int MAX_MSG_COUNT = 20;

// CTeachingClientDlg 对话框
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

// 构造
public:
	CTeachingClientDlg(CWnd* pParent = NULL);	// 标准构造函数

// 对话框数据
	enum { IDD = IDD_TEACHINGCLIENT_DIALOG };

	protected:
	virtual void DoDataExchange(CDataExchange* pDX);	// DDX/DDV 支持
	virtual void OnOK();
	virtual void OnCancel();

// 实现
protected:
	HICON m_hIcon;

	// 生成的消息映射函数
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

	// 服务器地址
	CString m_strServer;
	// 服务器端口
	int m_nPort;
	// 用户名
	CString m_strUsername;
	// 密码
	CString m_strPassword;
	// 重连间隔时间(ms)
	int m_nReconnectDelay;
	// 输出
	CEdit m_edtOutput;
	// 接收者(多个用逗号隔开)
	CString m_strReceivers;
	// 要推送的消息
	CString m_strMsgToPush;
	// 发送后是否自动清除
	BOOL m_bClearWhenSent;
	// 是否跟踪分组
	BOOL m_bTrackPacket;

	int m_nLoginStatus;
	int m_nMsgCount;
};
