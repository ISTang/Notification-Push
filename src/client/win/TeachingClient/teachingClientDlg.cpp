
// teachingClientDlg.cpp : 实现文件
//

#include "stdafx.h"
#include "teachingClient.h"
#include "teachingClientDlg.h"
#include "afxdialogex.h"
#include "NotificationPushAPI_proto.h"
#include "MyMessage.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#endif

static CTeachingClientDlg *g_pTeachingClientDlg;
static LPTSTR LoginStatusText[] = {
	_T("已注销"),
	_T("正在登录..."),
	_T("已登录")
};
static LPTSTR LogLevelText[] = {
	_T("TRACE"),
	_T("DEBUG"),
	_T("INFO"),
	_T("WARN"),
	_T("ERROR")
};

// 用于应用程序“关于”菜单项的 CAboutDlg 对话框

class CAboutDlg : public CDialogEx
{
public:
	CAboutDlg();

// 对话框数据
	enum { IDD = IDD_ABOUTBOX };

	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV 支持

// 实现
protected:
	DECLARE_MESSAGE_MAP()
};

CAboutDlg::CAboutDlg() : CDialogEx(CAboutDlg::IDD)
{
}

void CAboutDlg::DoDataExchange(CDataExchange* pDX)
{
	CDialogEx::DoDataExchange(pDX);
}

BEGIN_MESSAGE_MAP(CAboutDlg, CDialogEx)
END_MESSAGE_MAP()


// CTeachingClientDlg 对话框




CTeachingClientDlg::CTeachingClientDlg(CWnd* pParent /*=NULL*/)
	: CDialogEx(CTeachingClientDlg::IDD, pParent)
	, m_strServer(_T("isajia.com"))
	, m_nPort(3457)
	, m_bTrackPacket(false)
	, m_nReconnectDelay(1000)
	, m_bClearWhenSent(TRUE)
	, m_nLoginStatus(0)
	, m_nMsgCount(0)
{
	m_hIcon = AfxGetApp()->LoadIcon(IDR_MAINFRAME);
}

void CTeachingClientDlg::DoDataExchange(CDataExchange* pDX)
{
	CDialogEx::DoDataExchange(pDX);
	DDX_Text(pDX, IDC_RECEIVERS, m_strReceivers);
	DDX_Text(pDX, IDC_MSG, m_strMsgToPush);
	DDX_Check(pDX, IDC_CLEARWHENSENT, m_bClearWhenSent);
	DDX_Control(pDX, IDC_OUTPUT, m_edtOutput);
}
void CTeachingClientDlg::OnOK()
{
	if (GetFocus() == GetDlgItem(IDC_SEND))
	{
		CDialogEx::OnOK();
	}
}

void CTeachingClientDlg::OnCancel()
{
	if (MessageBox(_T("是否真的要退出程序?"), _T("退出"), MB_ICONWARNING|MB_YESNO)==IDYES)
	{
		CDialogEx::OnCancel();
	}
}

BEGIN_MESSAGE_MAP(CTeachingClientDlg, CDialogEx)
	ON_MESSAGE(WM_ICON_NOTIFY, OnTrayNotification)
	ON_WM_SYSCOMMAND()
	ON_WM_PAINT()
	ON_WM_QUERYDRAGICON()
	ON_WM_CLOSE()
	ON_COMMAND(ID_APP_SHOW, &CTeachingClientDlg::OnAppShow)
	ON_BN_CLICKED(IDC_SEND, CTeachingClientDlg::OnSendMsg)
	ON_BN_CLICKED(IDC_BROADCAST, CTeachingClientDlg::OnBroadcastMsg)
END_MESSAGE_MAP()


// CTeachingClientDlg 消息处理程序

BOOL CTeachingClientDlg::OnInitDialog()
{
	CDialogEx::OnInitDialog();

	// 将“关于...”菜单项添加到系统菜单中。

	// IDM_ABOUTBOX 必须在系统命令范围内。
	ASSERT((IDM_ABOUTBOX & 0xFFF0) == IDM_ABOUTBOX);
	ASSERT(IDM_ABOUTBOX < 0xF000);

	CMenu* pSysMenu = GetSystemMenu(FALSE);
	if (pSysMenu != NULL)
	{
		BOOL bNameValid;
		CString strAboutMenu;
		bNameValid = strAboutMenu.LoadString(IDS_ABOUTBOX);
		ASSERT(bNameValid);
		if (!strAboutMenu.IsEmpty())
		{
			pSysMenu->AppendMenu(MF_SEPARATOR);
			pSysMenu->AppendMenu(MF_STRING, IDM_ABOUTBOX, strAboutMenu);
		}
	}

	// 设置此对话框的图标。当应用程序主窗口不是对话框时，框架将自动
	//  执行此操作
	SetIcon(m_hIcon, TRUE);			// 设置大图标
	SetIcon(m_hIcon, FALSE);		// 设置小图标

	CString strToolTip = _T("教学客户端");
	if (!m_TrayIcon.Create(this, WM_ICON_NOTIFY, strToolTip, 
                       m_hIcon, IDR_POPUP_MENU))
		/*return FALSE*/;

	// 动态生成用户名(机器名)和密码(机器名的MD5)
	TCHAR computerName[MAX_COMPUTERNAME_LENGTH + 1];
	DWORD size = sizeof(computerName) / sizeof(computerName[0]);
	GetComputerName(computerName, &size);
	CT2A computerName2(computerName);
	//
	m_strUsername = computerName;
	m_strPassword = md5.CalcMD5FromString(computerName2.m_psz);
	//	
	g_pTeachingClientDlg = this;
	GetDlgItem(IDC_RECEIVERS)->SetFocus();
	// 自动登录
	DoConnect();
	// 最小化
	CSystemTray::MinimiseToTray(this);

	return FALSE;  // 除非将焦点设置到控件，否则返回 TRUE
}

LRESULT CTeachingClientDlg::OnTrayNotification(WPARAM wParam, LPARAM lParam)
{
    // Delegate all the work back to the default 
        // implementation in CSystemTray.
    return m_TrayIcon.OnTrayNotification(wParam, lParam);
}

void CTeachingClientDlg::OnSysCommand(UINT nID, LPARAM lParam)
{
	if ((nID & 0xFFF0) == IDM_ABOUTBOX)
	{
		CAboutDlg dlgAbout;
		dlgAbout.DoModal();
	}
	else if ((nID & 0xFFF0) == SC_MINIMIZE)
	{
		CSystemTray::MinimiseToTray(this);
	}
	else
	{
		CDialogEx::OnSysCommand(nID, lParam);
	}
}

// 如果向对话框添加最小化按钮，则需要下面的代码
//  来绘制该图标。对于使用文档/视图模型的 MFC 应用程序，
//  这将由框架自动完成。

void CTeachingClientDlg::OnPaint()
{
	if (IsIconic())
	{
		CPaintDC dc(this); // 用于绘制的设备上下文

		SendMessage(WM_ICONERASEBKGND, reinterpret_cast<WPARAM>(dc.GetSafeHdc()), 0);

		// 使图标在工作区矩形中居中
		int cxIcon = GetSystemMetrics(SM_CXICON);
		int cyIcon = GetSystemMetrics(SM_CYICON);
		CRect rect;
		GetClientRect(&rect);
		int x = (rect.Width() - cxIcon + 1) / 2;
		int y = (rect.Height() - cyIcon + 1) / 2;

		// 绘制图标

		dc.DrawIcon(x, y, m_hIcon);
	}
	else
	{
		CDialogEx::OnPaint();
	}
}

//当用户拖动最小化窗口时系统调用此函数取得光标
//显示。
HCURSOR CTeachingClientDlg::OnQueryDragIcon()
{
	return static_cast<HCURSOR>(m_hIcon);
}

void CTeachingClientDlg::OnAppShow()
{
	CSystemTray::MaximiseFromTray(this);
}

void CTeachingClientDlg::OnClose()
{
	Disconnect(0);

	m_TrayIcon.HideIcon();
	CDialogEx::OnClose();
}


void CTeachingClientDlg::DoConnect()
{
	char szPort[10];
	_itoa_s(m_nPort, szPort, 10);
	CT2A server(m_strServer);

	CT2A username(m_strUsername);
	CT2A password(m_strPassword);

	CEdit *pEdit = (CEdit *)GetDlgItem(IDC_OUTPUT);
	pEdit->SetWindowText(_T(""));

	CListBox *plstStatus = (CListBox *)GetDlgItem(IDC_STATUS);
	plstStatus->ResetContent();

	SetAppInfo("5A6D032A-DB6C-43BF-98EE-A699FBCAA628", "THHgux8k", "");
	SetServerInfo(server.m_psz, m_nPort);

	SetTextReceivedCallbackFunc(onTextReceived);
	SetTextSentCallbackFunc(onTextSent);
	SetLoginStatusCallbackFunc(onLoginStatus);
	SetLoginFailedCallbackFunc(onLoginFailed);
	SetLogCallbackFunc(onLog);
	SetMsgKeyReceivedCallbackFunc(onMsgKeyReceived);
	SetMaxInactiveTimeReceivedCallbackFunc(onMaxInactiveTimeReceived);
	SetMsgReceivedCallbackFunc(onMsgReceived);
	SetMsgRepliedCallbackFunc(onMsgReplied);

	if (!LoginAsUser(0, username, password, true, m_nReconnectDelay, m_bTrackPacket==TRUE)) {
		CString str;
		str.Format(_T("登录失败!"));
		::MessageBox(m_hWnd, str.GetBuffer(), _T("错误"), MB_OK|MB_ICONERROR);
	}
}

void CALLBACK CTeachingClientDlg::onTextReceived(long connId, LPCSTR lpszText)
{
}

void CALLBACK CTeachingClientDlg::onTextSent(long connId, LPCSTR lpszText)
{
}

void CALLBACK CTeachingClientDlg::onLoginStatus(long connId, int nStatus)
{
	if (nStatus == g_pTeachingClientDlg->m_nLoginStatus) return;
	g_pTeachingClientDlg->m_nLoginStatus = nStatus;

	CTime time = CTime::GetCurrentTime();

	CString str;
	str.Format(_T("[%s]%s"), time.Format("%H:%M:%S"), LoginStatusText[nStatus]);

	CListBox *plstStatus = (CListBox *)g_pTeachingClientDlg->GetDlgItem(IDC_STATUS);
	plstStatus->InsertString(plstStatus->GetCount(), str);
	if (plstStatus->GetCount()>MAX_STATUS_LINES)
	{
		for (int i=0; i<plstStatus->GetCount()-MAX_STATUS_LINES; i++)
		{
			plstStatus->DeleteString(0);
		}
	}
	plstStatus->SetCurSel(plstStatus->GetCount() - 1);

	switch (nStatus)
	{
	case LOGOUT:
		g_pTeachingClientDlg->GetDlgItem(IDC_SEND)->EnableWindow(FALSE);
		g_pTeachingClientDlg->GetDlgItem(IDC_BROADCAST)->EnableWindow(FALSE);
		break;
	case LOGINING:
		g_pTeachingClientDlg->GetDlgItem(IDC_SEND)->EnableWindow(FALSE);
		g_pTeachingClientDlg->GetDlgItem(IDC_BROADCAST)->EnableWindow(FALSE);
		break;
	case LOGON:
		g_pTeachingClientDlg->GetDlgItem(IDC_SEND)->EnableWindow(TRUE);
		g_pTeachingClientDlg->GetDlgItem(IDC_BROADCAST)->EnableWindow(TRUE);
		break;
	}
}

void CALLBACK CTeachingClientDlg::onLoginFailed(long connId, LPCSTR lpszReason)
{
	g_pTeachingClientDlg->GetDlgItem(IDC_CONNECT)->EnableWindow(TRUE);
	g_pTeachingClientDlg->GetDlgItem(IDC_DISCONNECT)->EnableWindow(FALSE);

	CA2T text(lpszReason);

	CTime time = CTime::GetCurrentTime();

	CString str;
	str.Format(_T("[%s]登录失败: %s"), time.Format("%H:%M:%S"), text.m_psz);

	CListBox *plstStatus = (CListBox *)g_pTeachingClientDlg->GetDlgItem(IDC_STATUS);
	plstStatus->InsertString(plstStatus->GetCount(), str);
	if (plstStatus->GetCount()>MAX_STATUS_LINES)
	{
		for (int i=0; i<plstStatus->GetCount()-MAX_STATUS_LINES; i++)
		{
			plstStatus->DeleteString(0);
		}
	}
	plstStatus->SetCurSel(plstStatus->GetCount() - 1);

	::MessageBox(g_pTeachingClientDlg->GetSafeHwnd(), _T("登录失败！"), _T("错误"), MB_OK|MB_ICONERROR);
}

void CALLBACK CTeachingClientDlg::onLog(long connId, LPCSTR lpszLogText, int nLogLevel)
{
	CA2T text(lpszLogText);

	CTime time = CTime::GetCurrentTime();

	CString str;
	str.Format(_T("[%s]%s: %s"), time.Format("%H:%M:%S"), LogLevelText[nLogLevel], text.m_psz);

	CListBox *plstStatus = (CListBox *)g_pTeachingClientDlg->GetDlgItem(IDC_STATUS);
	plstStatus->InsertString(plstStatus->GetCount(), str);
	if (plstStatus->GetCount()>MAX_STATUS_LINES)
	{
		for (int i=0; i<plstStatus->GetCount()-MAX_STATUS_LINES; i++)
		{
			plstStatus->DeleteString(0);
		}
	}
	plstStatus->SetCurSel(plstStatus->GetCount() - 1);
}

void CALLBACK CTeachingClientDlg::onMsgKeyReceived(long connId, LPCSTR lpszMsgKey)
{
}

void CALLBACK CTeachingClientDlg::onMaxInactiveTimeReceived(long connId, int nMaxInactiveTime)
{
}

void CALLBACK CTeachingClientDlg::onMsgReceived(long connId, LPCSTR lpszMsg)
{
	MyMessage msg = MyMessage::parse(lpszMsg);
	CA2T msgSender(msg.getSender().c_str());
	CA2T msgBody(msg.getBody().c_str());

	CString str;
	str.Format(_T("%s说: %s"), msgSender.m_psz, msgBody.m_psz);

	g_pTeachingClientDlg->AppendChatMsg(str);
}

void CALLBACK CTeachingClientDlg::onMsgReplied(long connId, LPCSTR lpszMsgId, bool bSuccess, LPCSTR lpszError)
{
	CA2T msgId(lpszMsgId);

	if (bSuccess) {
		//str.Format(_T("消息发送成功"));
	} else {
		CA2T error(lpszError);
		CString str;
		str.Format(_T("消息发送失败: %s"), error.m_psz);
		::MessageBox(g_pTeachingClientDlg->GetSafeHwnd(), str.GetBuffer(), _T("错误"), MB_OK|MB_ICONERROR);
	}
}

void CTeachingClientDlg::OnSendMsg()
{
	UpdateData();

	m_strMsgToPush = m_strMsgToPush.Trim();
	if (m_strMsgToPush == "")
	{
		GotoDlgCtrl(g_pTeachingClientDlg->GetDlgItem(IDC_MSG));
		return;
	}

	m_strReceivers = m_strReceivers.Trim();
	
	int i = 0;
	CStringArray saItems;
	for (CString sItem = m_strReceivers.Tokenize(_T(","), i); i >= 0; sItem = m_strReceivers.Tokenize(_T(","), i))
	{
		saItems.Add(sItem);
	}
	if (saItems.GetSize() == 0)
	{
		MessageBox(_T("没有接收者被指定！"), _T("提示"), MB_ICONINFORMATION | MB_OK);
		return;
	}

	CT2A strMsgToPush(m_strMsgToPush, CP_UTF8);
	CT2A strReceivers(m_strReceivers, CP_UTF8);
	CT2A strSendId(_T("me"), CP_UTF8);
	if (saItems.GetSize() == 1)
	{
		MyMessage msg;
		msg.setBody(strMsgToPush.m_psz);
		if (!SendMessageTo(0, strReceivers.m_psz, strSendId.m_psz, msg.toJson().c_str(), false))
		{
			MessageBox(_T("消息发送失败！"), _T("错误"), MB_ICONERROR | MB_OK);
			return;
		}

		CString str;
		str.Format(_T("我对 %s 说: %s"), m_strReceivers, m_strMsgToPush);
		g_pTeachingClientDlg->AppendChatMsg(str);

		if (m_bClearWhenSent)
		{
			g_pTeachingClientDlg->GetDlgItem(IDC_MSG)->SetWindowText(_T(""));
			GotoDlgCtrl(g_pTeachingClientDlg->GetDlgItem(IDC_MSG));
		}
	}
	else
	{
		LPCSTR lpszReceiver = strReceivers.m_psz;
		LPCSTR *lpszReceivers = new LPCSTR[saItems.GetSize()+1];
		for (int i = 0; i < saItems.GetSize(); i++)
		{
			lpszReceivers[i] = lpszReceiver;
			char *p = (char *)strchr(lpszReceiver, ',');
			if (p != NULL)
			{
				*p = NULL;
				lpszReceiver = p + 1;
			}

		}
		lpszReceivers[saItems.GetSize()] = NULL;
		if (!MulticastMessage(0, lpszReceivers, "me", strMsgToPush.m_psz, false))
		{
			MessageBox(_T("消息群发失败！"), _T("错误"), MB_ICONERROR | MB_OK);
			return;
		}

		CString str;
		str.Format(_T("我对 %s 说: %s"), m_strReceivers, m_strMsgToPush);
		g_pTeachingClientDlg->AppendChatMsg(str);

		if (m_bClearWhenSent)
		{
			g_pTeachingClientDlg->GetDlgItem(IDC_MSG)->SetWindowText(_T(""));
			GotoDlgCtrl(g_pTeachingClientDlg->GetDlgItem(IDC_MSG));
		}
	}
}


void CTeachingClientDlg::OnBroadcastMsg()
{
	UpdateData();

	m_strMsgToPush = m_strMsgToPush.Trim();
	if (m_strMsgToPush == "")
	{
		GotoDlgCtrl(g_pTeachingClientDlg->GetDlgItem(IDC_MSG));
		return;
	}

	CT2A strMsgToPush(m_strMsgToPush, CP_UTF8);
	MyMessage msg;
	msg.setBody(strMsgToPush.m_psz);
	if (!BroadcastMessage(0, "me", msg.toJson().c_str(), false))
	{
		MessageBox(_T("消息广播失败！"), _T("错误"), MB_ICONERROR | MB_OK);
		return;
	}

	CString str;
	str.Format(_T("我对大家说: %s"), m_strMsgToPush);
	g_pTeachingClientDlg->AppendChatMsg(str);

	if (m_bClearWhenSent)
	{
		g_pTeachingClientDlg->GetDlgItem(IDC_MSG)->SetWindowText(_T(""));
		GotoDlgCtrl(g_pTeachingClientDlg->GetDlgItem(IDC_MSG));
	}
}

void CTeachingClientDlg::AppendChatMsg(const CString& strChatMsg)
{
	CTime time = CTime::GetCurrentTime();

	CEdit *pEdit = (CEdit *)g_pTeachingClientDlg->GetDlgItem(IDC_OUTPUT);

	if (pEdit->GetLineCount()==MAX_MSG_COUNT)
	{
		pEdit->SetWindowText(_T(""));
	}

	CString strOld, strNew;
	pEdit->GetWindowText(strOld);
	strNew.Format(_T("%s[%s]%s"), (strOld=="")?_T(""):strOld+_T("\r\n"), time.Format("%H:%M:%S"), strChatMsg);

	pEdit->SetWindowText(strNew);
	pEdit->LineScroll(pEdit->GetLineCount());
}
