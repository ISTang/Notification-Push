
// tuixinDlg.cpp : 实现文件
//

#include "stdafx.h"
#include "tuixin.h"
#include "tuixinDlg.h"
#include "afxdialogex.h"
#include "NotificationPushAPI_proto.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#endif

static CTuixinDlg *g_pTuixinDlg;
static LPTSTR LoginStatusText[] = {
	L"已注销",
	L"正在登录",
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
	, m_strUsername(_T("test"))
	, m_strPassword(_T("test"))
	, m_bTrackPacket(false)
	, m_nThreads(1)
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
	DDX_Control(pDX, IDC_OUTPUT, m_edtOutput);
	DDX_Text(pDX, IDC_THREADS, m_nThreads);
	DDV_MinMaxInt(pDX, m_nPort, 1024, 32767);
	DDV_MinMaxInt(pDX, m_nThreads, 1, 10000);
}

BEGIN_MESSAGE_MAP(CTuixinDlg, CDialogEx)
	ON_WM_SYSCOMMAND()
	ON_WM_PAINT()
	ON_WM_QUERYDRAGICON()
	ON_BN_CLICKED(IDC_CONNECT, &CTuixinDlg::OnConnect)
	ON_BN_CLICKED(IDC_DISCONNECT, &CTuixinDlg::OnDisconnect)
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

	return TRUE;  // 除非将焦点设置到控件，否则返回 TRUE
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
	SetLogCallbackFunc(onLog);
	SetMsgKeyReceivedCallbackFunc(onMsgKeyReceived);
	SetMaxInactiveTimeReceivedCallbackFunc(onMaxInactiveTimeReceived);
	SetMsgReceivedCallbackFunc(onMsgReceived);

	CString str;
	str.Format(_T("正在登录...共 %d 个线程"), m_nThreads);
	plstStatus->AddString(str);
	for (int i = 0; i < m_nThreads; i++)
	{
		if (!LoginAsUser(i + 1, username, password, m_bTrackPacket)) {
			str.Format(_T("[%ld] 登录失败!"), long(i+1));
			plstStatus->AddString(str);
		}
	}
}

void CTuixinDlg::OnDisconnect()
{
	for (int i = 0; i < m_nThreads; i++) {
		Disconnect(i+1);
	}

	GetDlgItem(IDC_CONNECT)->EnableWindow(TRUE);
	GetDlgItem(IDC_DISCONNECT)->EnableWindow(FALSE);
}

void CALLBACK CTuixinDlg::onTextReceived(long connId, LPCSTR lpszText)
{
	CA2T text(lpszText);

	CString str;
	str.Format(_T("[%ld] 接收到文本: %s"), connId, text.m_psz);

	CListBox *plstStatus = (CListBox *)g_pTuixinDlg->GetDlgItem(IDC_STATUS);
	plstStatus->AddString(str);
}

void CALLBACK CTuixinDlg::onTextSent(long connId, LPCSTR lpszText)
{
	CA2T text(lpszText);

	CString str;
	str.Format(_T("[%ld] 已发送文本: %s"), connId, text.m_psz);

	CListBox *plstStatus = (CListBox *)g_pTuixinDlg->GetDlgItem(IDC_STATUS);
	plstStatus->AddString(str);
}

void CALLBACK CTuixinDlg::onLoginStatus(long connId, int nStatus)
{
	CString str;
	str.Format(_T("[%ld] 登录状态: %s"), connId, LoginStatusText[nStatus]);

	CListBox *plstStatus = (CListBox *)g_pTuixinDlg->GetDlgItem(IDC_STATUS);
	plstStatus->AddString(str);
}

void CALLBACK CTuixinDlg::onLog(long connId, LPCSTR lpszLogText, int nLogLevel)
{
	CA2T logText(lpszLogText);

	CEdit *pEdit = (CEdit *)g_pTuixinDlg->GetDlgItem(IDC_OUTPUT);

	CString strOld, strNew;
	pEdit->GetWindowText(strOld);
	strNew.Format(_T("%s\r\n[%ld] [%s] %s"), strOld, connId, LogLevelText[nLogLevel], logText.m_psz);

	pEdit->SetWindowText(strNew);
	pEdit->LineScroll(pEdit->GetLineCount());
}

void CALLBACK CTuixinDlg::onMsgKeyReceived(long connId, LPCSTR lpszMsgKey)
{
	CA2T msgkey(lpszMsgKey);

	CString str;
	str.Format(_T("[%ld] 接收到消息密钥: %s"), msgkey.m_psz);

	CListBox *plstStatus = (CListBox *)g_pTuixinDlg->GetDlgItem(IDC_STATUS);
	plstStatus->AddString(str);
}

void CALLBACK CTuixinDlg::onMaxInactiveTimeReceived(long connId, int nMaxInactiveTime)
{
	CString str;
	str.Format(_T("[%ld] 接收到心跳周期(ms): %d"), connId, nMaxInactiveTime);

	CListBox *plstStatus = (CListBox *)g_pTuixinDlg->GetDlgItem(IDC_STATUS);
	plstStatus->AddString(str);
}

void CALLBACK CTuixinDlg::onMsgReceived(long connId, LPCSTR lpszMsg)
{
	CA2T msg(lpszMsg);

	CString str;
	str.Format(_T("[%ld] 接收到消息: %s"), connId, msg.m_psz);

	CListBox *plstStatus = (CListBox *)g_pTuixinDlg->GetDlgItem(IDC_STATUS);
	plstStatus->AddString(str);
}

void CALLBACK CTuixinDlg::onMsgReplied(long connId, LPCSTR lpszMsgId, bool bSuccess, LPCSTR lpszError)
{
	CA2T msgId(lpszMsgId);

	CString str;
	if (bSuccess) {
		str.Format(_T("[%ld] 消息 %s 发送成功"), connId, msgId.m_psz);
	} else {
		CA2T error(lpszError);
		str.Format(_T("[%ld] 消息 %s 发送失败(%s)"), connId, msgId.m_psz, error.m_psz);
	}

	CListBox *plstStatus = (CListBox *)g_pTuixinDlg->GetDlgItem(IDC_STATUS);
	plstStatus->AddString(str);
}
