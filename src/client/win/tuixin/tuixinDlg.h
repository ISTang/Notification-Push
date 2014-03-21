
// tuixinDlg.h : 头文件
//

#pragma once
#include "afxwin.h"

const int AUTO_RECONNECT_INTERVAL = 1000 * 30;

// CTuixinDlg 对话框
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

// 构造
public:
	CTuixinDlg(CWnd* pParent = NULL);	// 标准构造函数

// 对话框数据
	enum { IDD = IDD_TUIXIN_DIALOG };

	protected:
	virtual void DoDataExchange(CDataExchange* pDX);	// DDX/DDV 支持


// 实现
protected:
	HICON m_hIcon;

	// 生成的消息映射函数
	virtual BOOL OnInitDialog();
	afx_msg void OnSysCommand(UINT nID, LPARAM lParam);
	afx_msg void OnPaint();
	afx_msg HCURSOR OnQueryDragIcon();
	afx_msg void OnConnect();
	afx_msg void OnDisconnect();
	DECLARE_MESSAGE_MAP()

private:
	// 服务器地址
	CString m_strServer;
	// 服务器端口
	int m_nPort;
	// 用户名
	CString m_strUsername;
	// 密码
	CString m_strPassword;
	// 输出
	CEdit m_edtOutput;
	// 线程数
	int m_nThreads;

	bool m_bTrackPacket;
};
