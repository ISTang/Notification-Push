
// tuixinDlg.cpp : 实现文件
//

#include "stdafx.h"
#include "tuixin.h"
#include "tuixinDlg.h"
#include "afxdialogex.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#endif


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
	, m_strServer(_T("210.209.93.127"))
	, m_nPort(1234)
	, m_strUsername(_T("13808188051"))
	, m_strPassword(_T("gzdx342"))
	, m_bTrackPacket(false)
	, m_nThreads(100)
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

	// TODO: 在此添加额外的初始化代码

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
	UpdateData();

	TCHAR szBuffer[MAX_LINE + 1];
	CListBox *plstStatus = (CListBox *)GetDlgItem(IDC_STATUS);

	for (int i = 0; i < m_nThreads; i++)
	{
		wsprintf(szBuffer, _T("#%d: %s"), i + 1, _T("初始化线程..."));
		plstStatus->InsertString(i, szBuffer);
	}
	
	for (int i = 0; i < m_nThreads; i++)
	{
		// 创建客户端UI线程
		CClientThread *pClientThread = (CClientThread *)AfxBeginThread(RUNTIME_CLASS(CClientThread),
			THREAD_PRIORITY_NORMAL, CREATE_SUSPENDED);

		// 设置线程参数
		pClientThread->m_nClientId = i;
		pClientThread->m_strServer = m_strServer;
		pClientThread->m_nPort = m_nPort;
		pClientThread->m_strUsername = m_strUsername;
		pClientThread->m_strPassword = m_strPassword;
		pClientThread->m_bTrackPacket = m_bTrackPacket;
		pClientThread->m_hwndOutput = GetDlgItem(IDC_OUTPUT)->GetSafeHwnd();
		pClientThread->m_hwndStatus = GetDlgItem(IDC_STATUS)->GetSafeHwnd();;

		// 启动线程
		pClientThread->ResumeThread();

		// 记录新的线程
		m_clientThreads.push_back(pClientThread);
	}
}
