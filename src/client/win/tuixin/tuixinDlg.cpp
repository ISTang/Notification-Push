
// tuixinDlg.cpp : 实现文件
//

#include "stdafx.h"
#include "tuixin.h"
#include "tuixinDlg.h"
#include "afxdialogex.h"
#include "NotificationPushAPI_proto.h"
#include "MyMessage.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#endif

static CTuixinDlg *g_pTuixinDlg;
static LPTSTR LoginStatusText[] = {
	L"已注销",
	L"正在登录...",
	L"已登录"
};
static LPTSTR LogLevelText[] = {
	L"TRACE",
	L"DEBUG",
	L"INFO",
	L"WARN",
	L"ERROR"
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


// CTuixinDlg 对话框




CTuixinDlg::CTuixinDlg(CWnd* pParent /*=NULL*/)
	: CDialogEx(CTuixinDlg::IDD, pParent)
	, m_strServer(_T("isajia.com"))
	, m_nPort(3457)
	, m_strUsername(_T(""))
	, m_strPassword(_T(""))
	, m_bTrackPacket(false)
	, m_nReconnectDelay(1000)
	, m_strReceivers(_T(""))
	, m_strMsgToPush(_T(""))
	, m_bClearWhenSent(TRUE)
	, m_nLoginStatus(0)
	, m_nMsgCount(0)
{
	m_hIcon = AfxGetApp()->LoadIcon(IDR_MAINFRAME);
}

void CTuixinDlg::DoDataExchange(CDataExchange* pDX)
{
	CDialogEx::DoDataExchange(pDX);
	DDX_Text(pDX, IDC_SERVER, m_strServer);
	DDX_Text(pDX, IDC_PORT, m_nPort);
	DDX_Text(pDX, IDC_USERNAME, m_strUsername);
	DDX_Text(pDX, IDC_PASSWORD, m_strPassword);
	DDV_MinMaxInt(pDX, m_nPort, 1024, 32767);
	DDX_Control(pDX, IDC_OUTPUT, m_edtOutput);
	DDX_Text(pDX, IDC_RECEIVERS, m_strReceivers);
	DDX_Text(pDX, IDC_MSG, m_strMsgToPush);
	DDX_Check(pDX, IDC_CLEARWHENSENT, m_bClearWhenSent);
}

void CTuixinDlg::OnOK()
{
	if (GetFocus() == GetDlgItem(IDC_SEND))
	{
		CDialogEx::OnOK();
	}
}

void CTuixinDlg::OnCancel()
{
	if (MessageBox(L"是否真的要退出程序?", L"退出", MB_ICONWARNING|MB_YESNO)==IDYES)
	{
		CDialogEx::OnCancel();
	}
}

BEGIN_MESSAGE_MAP(CTuixinDlg, CDialogEx)
	ON_WM_SYSCOMMAND()
	ON_WM_PAINT()
	ON_WM_QUERYDRAGICON()
	ON_BN_CLICKED(IDC_CONNECT, &CTuixinDlg::OnConnect)
	ON_BN_CLICKED(IDC_DISCONNECT, &CTuixinDlg::OnDisconnect)
	ON_BN_CLICKED(IDC_SEND, &CTuixinDlg::OnSendMsg)
	ON_BN_CLICKED(IDC_BROADCAST, &CTuixinDlg::OnBroadcastMsg)
	ON_WM_CLOSE()
	ON_WM_ACTIVATE()
END_MESSAGE_MAP()


// CTuixinDlg 消息处理程序

BOOL CTuixinDlg::OnInitDialog()
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

	g_pTuixinDlg = this;
	GetDlgItem(IDC_USERNAME)->SetFocus();

	return FALSE;  // 除非将焦点设置到控件，否则返回 TRUE
}

void CTuixinDlg::OnSysCommand(UINT nID, LPARAM lParam)
{
	if ((nID & 0xFFF0) == IDM_ABOUTBOX)
	{
		CAboutDlg dlgAbout;
		dlgAbout.DoModal();
	}
	else
	{
		CDialogEx::OnSysCommand(nID, lParam);
	}
}

// 如果向对话框添加最小化按钮，则需要下面的代码
//  来绘制该图标。对于使用文档/视图模型的 MFC 应用程序，
//  这将由框架自动完成。

void CTuixinDlg::OnPaint()
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
HCURSOR CTuixinDlg::OnQueryDragIcon()
{
	return static_cast<HCURSOR>(m_hIcon);
}

void CTuixinDlg::OnConnect()
{
	GetDlgItem(IDC_CONNECT)->EnableWindow(FALSE);
	GetDlgItem(IDC_DISCONNECT)->EnableWindow(TRUE);

	UpdateData();

	char szPort[10];
	_itoa_s(m_nPort, szPort, 10);
	CT2A server(m_strServer);

	CT2A username(m_strUsername);
	CT2A password(m_strPassword);

	CEdit *pEdit = (CEdit *)GetDlgItem(IDC_OUTPUT);
	pEdit->SetWindowText(L"");

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
		::MessageBox(m_hWnd, str.GetBuffer(), L"错误", MB_OK|MB_ICONERROR);
	}
}

void CTuixinDlg::OnDisconnect()
{
	Disconnect(0);

	GetDlgItem(IDC_CONNECT)->EnableWindow(TRUE);
	GetDlgItem(IDC_DISCONNECT)->EnableWindow(FALSE);

	GetDlgItem(IDC_SEND)->EnableWindow(FALSE);
	GetDlgItem(IDC_BROADCAST)->EnableWindow(FALSE);
}

void CALLBACK CTuixinDlg::onTextReceived(long connId, LPCSTR lpszText)
{
}

void CALLBACK CTuixinDlg::onTextSent(long connId, LPCSTR lpszText)
{
}

void CALLBACK CTuixinDlg::onLoginStatus(long connId, int nStatus)
{
	if (nStatus == g_pTuixinDlg->m_nLoginStatus) return;
	g_pTuixinDlg->m_nLoginStatus = nStatus;

	CTime time = CTime::GetCurrentTime();

	CString str;
	str.Format(_T("[%s]%s"), time.Format("%H:%M:%S"), LoginStatusText[nStatus]);

	CListBox *plstStatus = (CListBox *)g_pTuixinDlg->GetDlgItem(IDC_STATUS);
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
		g_pTuixinDlg->GetDlgItem(IDC_SEND)->EnableWindow(FALSE);
		g_pTuixinDlg->GetDlgItem(IDC_BROADCAST)->EnableWindow(FALSE);
		break;
	case LOGINING:
		g_pTuixinDlg->GetDlgItem(IDC_SEND)->EnableWindow(FALSE);
		g_pTuixinDlg->GetDlgItem(IDC_BROADCAST)->EnableWindow(FALSE);
		break;
	case LOGON:
		g_pTuixinDlg->GetDlgItem(IDC_SEND)->EnableWindow(TRUE);
		g_pTuixinDlg->GetDlgItem(IDC_BROADCAST)->EnableWindow(TRUE);
		break;
	}
}

void CALLBACK CTuixinDlg::onLoginFailed(long connId, LPCSTR lpszReason)
{
	g_pTuixinDlg->GetDlgItem(IDC_CONNECT)->EnableWindow(TRUE);
	g_pTuixinDlg->GetDlgItem(IDC_DISCONNECT)->EnableWindow(FALSE);

	CA2T text(lpszReason);

	CTime time = CTime::GetCurrentTime();

	CString str;
	str.Format(_T("[%s]登录失败: %s"), time.Format("%H:%M:%S"), text.m_psz);

	CListBox *plstStatus = (CListBox *)g_pTuixinDlg->GetDlgItem(IDC_STATUS);
	plstStatus->InsertString(plstStatus->GetCount(), str);
	if (plstStatus->GetCount()>MAX_STATUS_LINES)
	{
		for (int i=0; i<plstStatus->GetCount()-MAX_STATUS_LINES; i++)
		{
			plstStatus->DeleteString(0);
		}
	}
	plstStatus->SetCurSel(plstStatus->GetCount() - 1);

	::MessageBox(g_pTuixinDlg->GetSafeHwnd(), L"登录失败！", L"错误", MB_OK|MB_ICONERROR);
}

void CALLBACK CTuixinDlg::onLog(long connId, LPCSTR lpszLogText, int nLogLevel)
{
	CA2T text(lpszLogText);

	CTime time = CTime::GetCurrentTime();

	CString str;
	str.Format(_T("[%s]%s: %s"), time.Format("%H:%M:%S"), LogLevelText[nLogLevel], text.m_psz);

	CListBox *plstStatus = (CListBox *)g_pTuixinDlg->GetDlgItem(IDC_STATUS);
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

void CALLBACK CTuixinDlg::onMsgKeyReceived(long connId, LPCSTR lpszMsgKey)
{
}

void CALLBACK CTuixinDlg::onMaxInactiveTimeReceived(long connId, int nMaxInactiveTime)
{
}

void CALLBACK CTuixinDlg::onMsgReceived(long connId, LPCSTR lpszMsg)
{
	MyMessage msg = MyMessage::parse(lpszMsg);
	CA2T msgSender(msg.getSender().c_str());
	CA2T msgBody(msg.getBody().c_str());

	CString str;
	str.Format(_T("%s说: %s"), msgSender.m_psz, msgBody.m_psz);

	g_pTuixinDlg->AppendChatMsg(str);
}

void CALLBACK CTuixinDlg::onMsgReplied(long connId, LPCSTR lpszMsgId, bool bSuccess, LPCSTR lpszError)
{
	CA2T msgId(lpszMsgId);

	if (bSuccess) {
		//str.Format(_T("消息发送成功"));
	} else {
		CA2T error(lpszError);
		CString str;
		str.Format(_T("消息发送失败: %s"), error.m_psz);
		::MessageBox(g_pTuixinDlg->GetSafeHwnd(), str.GetBuffer(), L"错误", MB_OK|MB_ICONERROR);
	}
}

void CTuixinDlg::OnSendMsg()
{
	UpdateData();

	m_strMsgToPush = m_strMsgToPush.Trim();
	if (m_strMsgToPush == "")
	{
		GotoDlgCtrl(g_pTuixinDlg->GetDlgItem(IDC_MSG));
		return;
	}

	m_strReceivers = m_strReceivers.Trim();
	
	int i = 0;
	CStringArray saItems;
	for (CString sItem = m_strReceivers.Tokenize(L",", i); i >= 0; sItem = m_strReceivers.Tokenize(L",", i))
	{
		saItems.Add(sItem);
	}
	if (saItems.GetSize() == 0)
	{
		MessageBox(L"没有接收者被指定！", L"提示", MB_ICONINFORMATION | MB_OK);
		return;
	}

	CT2A strMsgToPush(m_strMsgToPush, CP_UTF8);
	CT2A strReceivers(m_strReceivers, CP_UTF8);
	CT2A strSendId(L"me", CP_UTF8);
	if (saItems.GetSize() == 1)
	{
		MyMessage msg;
		msg.setBody(strMsgToPush.m_psz);
		if (!SendMessageTo(0, strReceivers.m_psz, strSendId.m_psz, msg.toJson().c_str(), false))
		{
			MessageBox(L"消息发送失败！", L"错误", MB_ICONERROR | MB_OK);
			return;
		}

		CString str;
		str.Format(_T("我对 %s 说: %s"), m_strReceivers, m_strMsgToPush);
		g_pTuixinDlg->AppendChatMsg(str);

		if (m_bClearWhenSent)
		{
			g_pTuixinDlg->GetDlgItem(IDC_MSG)->SetWindowText(L"");
			GotoDlgCtrl(g_pTuixinDlg->GetDlgItem(IDC_MSG));
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
			MessageBox(L"消息群发失败！", L"错误", MB_ICONERROR | MB_OK);
			return;
		}

		CString str;
		str.Format(_T("我对 %s 说: %s"), m_strReceivers, m_strMsgToPush);
		g_pTuixinDlg->AppendChatMsg(str);

		if (m_bClearWhenSent)
		{
			g_pTuixinDlg->GetDlgItem(IDC_MSG)->SetWindowText(L"");
			GotoDlgCtrl(g_pTuixinDlg->GetDlgItem(IDC_MSG));
		}
	}
}


void CTuixinDlg::OnBroadcastMsg()
{
	UpdateData();

	m_strMsgToPush = m_strMsgToPush.Trim();
	if (m_strMsgToPush == "")
	{
		GotoDlgCtrl(g_pTuixinDlg->GetDlgItem(IDC_MSG));
		return;
	}

	CT2A strMsgToPush(m_strMsgToPush, CP_UTF8);
	MyMessage msg;
	msg.setBody(strMsgToPush.m_psz);
	if (!BroadcastMessage(0, "me", msg.toJson().c_str(), false))
	{
		MessageBox(L"消息广播失败！", L"错误", MB_ICONERROR | MB_OK);
		return;
	}

	CString str;
	str.Format(_T("我对大家说: %s"), m_strMsgToPush);
	g_pTuixinDlg->AppendChatMsg(str);

	if (m_bClearWhenSent)
	{
		g_pTuixinDlg->GetDlgItem(IDC_MSG)->SetWindowText(L"");
		GotoDlgCtrl(g_pTuixinDlg->GetDlgItem(IDC_MSG));
	}
}

void CTuixinDlg::AppendChatMsg(const CString& strChatMsg)
{
	CTime time = CTime::GetCurrentTime();

	CEdit *pEdit = (CEdit *)g_pTuixinDlg->GetDlgItem(IDC_OUTPUT);

	if (pEdit->GetLineCount()==MAX_MSG_COUNT)
	{
		pEdit->SetWindowText(L"");
	}

	CString strOld, strNew;
	pEdit->GetWindowText(strOld);
	strNew.Format(_T("%s[%s]%s"), (strOld==L""?L"":strOld+L"\r\n"), time.Format("%H:%M:%S"), strChatMsg);

	pEdit->SetWindowText(strNew);
	pEdit->LineScroll(pEdit->GetLineCount());
}


void CTuixinDlg::OnClose()
{
	OnDisconnect();

	CDialogEx::OnClose();
}
