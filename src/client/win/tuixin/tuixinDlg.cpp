
// tuixinDlg.cpp : ʵ���ļ�
//

#include "stdafx.h"
#include "tuixin.h"
#include "tuixinDlg.h"
#include "afxdialogex.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#endif


// ����Ӧ�ó��򡰹��ڡ��˵���� CAboutDlg �Ի���

class CAboutDlg : public CDialogEx
{
public:
	CAboutDlg();

// �Ի�������
	enum { IDD = IDD_ABOUTBOX };

	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV ֧��

// ʵ��
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


// CTuixinDlg �Ի���




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


// CTuixinDlg ��Ϣ�������

BOOL CTuixinDlg::OnInitDialog()
{
	CDialogEx::OnInitDialog();

	// ��������...���˵�����ӵ�ϵͳ�˵��С�

	// IDM_ABOUTBOX ������ϵͳ���Χ�ڡ�
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

	// ���ô˶Ի����ͼ�ꡣ��Ӧ�ó��������ڲ��ǶԻ���ʱ����ܽ��Զ�
	//  ִ�д˲���
	SetIcon(m_hIcon, TRUE);			// ���ô�ͼ��
	SetIcon(m_hIcon, FALSE);		// ����Сͼ��

	// TODO: �ڴ���Ӷ���ĳ�ʼ������

	return TRUE;  // ���ǽ��������õ��ؼ������򷵻� TRUE
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

// �����Ի��������С����ť������Ҫ����Ĵ���
//  �����Ƹ�ͼ�ꡣ����ʹ���ĵ�/��ͼģ�͵� MFC Ӧ�ó���
//  �⽫�ɿ���Զ���ɡ�

void CTuixinDlg::OnPaint()
{
	if (IsIconic())
	{
		CPaintDC dc(this); // ���ڻ��Ƶ��豸������

		SendMessage(WM_ICONERASEBKGND, reinterpret_cast<WPARAM>(dc.GetSafeHdc()), 0);

		// ʹͼ���ڹ����������о���
		int cxIcon = GetSystemMetrics(SM_CXICON);
		int cyIcon = GetSystemMetrics(SM_CYICON);
		CRect rect;
		GetClientRect(&rect);
		int x = (rect.Width() - cxIcon + 1) / 2;
		int y = (rect.Height() - cyIcon + 1) / 2;

		// ����ͼ��

		dc.DrawIcon(x, y, m_hIcon);
	}
	else
	{
		CDialogEx::OnPaint();
	}
}

//���û��϶���С������ʱϵͳ���ô˺���ȡ�ù��
//��ʾ��
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
		wsprintf(szBuffer, _T("#%d: %s"), i + 1, _T("��ʼ���߳�..."));
		plstStatus->InsertString(i, szBuffer);
	}
	
	for (int i = 0; i < m_nThreads; i++)
	{
		// �����ͻ���UI�߳�
		CClientThread *pClientThread = (CClientThread *)AfxBeginThread(RUNTIME_CLASS(CClientThread),
			THREAD_PRIORITY_NORMAL, CREATE_SUSPENDED);

		// �����̲߳���
		pClientThread->m_nClientId = i;
		pClientThread->m_strServer = m_strServer;
		pClientThread->m_nPort = m_nPort;
		pClientThread->m_strUsername = m_strUsername;
		pClientThread->m_strPassword = m_strPassword;
		pClientThread->m_bTrackPacket = m_bTrackPacket;
		pClientThread->m_hwndOutput = GetDlgItem(IDC_OUTPUT)->GetSafeHwnd();
		pClientThread->m_hwndStatus = GetDlgItem(IDC_STATUS)->GetSafeHwnd();;

		// �����߳�
		pClientThread->ResumeThread();

		// ��¼�µ��߳�
		m_clientThreads.push_back(pClientThread);
	}
}
