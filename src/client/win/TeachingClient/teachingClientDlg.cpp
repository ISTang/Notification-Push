
// teachingClientDlg.cpp : ʵ���ļ�
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
	_T("��ע��"),
	_T("���ڵ�¼..."),
	_T("�ѵ�¼")
};
static LPTSTR LogLevelText[] = {
	_T("TRACE"),
	_T("DEBUG"),
	_T("INFO"),
	_T("WARN"),
	_T("ERROR")
};

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


// CTeachingClientDlg �Ի���




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
	if (MessageBox(_T("�Ƿ����Ҫ�˳�����?"), _T("�˳�"), MB_ICONWARNING|MB_YESNO)==IDYES)
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


// CTeachingClientDlg ��Ϣ�������

BOOL CTeachingClientDlg::OnInitDialog()
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

	CString strToolTip = _T("��ѧ�ͻ���");
	if (!m_TrayIcon.Create(this, WM_ICON_NOTIFY, strToolTip, 
                       m_hIcon, IDR_POPUP_MENU))
		/*return FALSE*/;

	// ��̬�����û���(������)������(��������MD5)
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
	// �Զ���¼
	DoConnect();
	// ��С��
	CSystemTray::MinimiseToTray(this);

	return FALSE;  // ���ǽ��������õ��ؼ������򷵻� TRUE
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

// �����Ի��������С����ť������Ҫ����Ĵ���
//  �����Ƹ�ͼ�ꡣ����ʹ���ĵ�/��ͼģ�͵� MFC Ӧ�ó���
//  �⽫�ɿ���Զ���ɡ�

void CTeachingClientDlg::OnPaint()
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
		str.Format(_T("��¼ʧ��!"));
		::MessageBox(m_hWnd, str.GetBuffer(), _T("����"), MB_OK|MB_ICONERROR);
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
	str.Format(_T("[%s]��¼ʧ��: %s"), time.Format("%H:%M:%S"), text.m_psz);

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

	::MessageBox(g_pTeachingClientDlg->GetSafeHwnd(), _T("��¼ʧ�ܣ�"), _T("����"), MB_OK|MB_ICONERROR);
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
	str.Format(_T("%s˵: %s"), msgSender.m_psz, msgBody.m_psz);

	g_pTeachingClientDlg->AppendChatMsg(str);
}

void CALLBACK CTeachingClientDlg::onMsgReplied(long connId, LPCSTR lpszMsgId, bool bSuccess, LPCSTR lpszError)
{
	CA2T msgId(lpszMsgId);

	if (bSuccess) {
		//str.Format(_T("��Ϣ���ͳɹ�"));
	} else {
		CA2T error(lpszError);
		CString str;
		str.Format(_T("��Ϣ����ʧ��: %s"), error.m_psz);
		::MessageBox(g_pTeachingClientDlg->GetSafeHwnd(), str.GetBuffer(), _T("����"), MB_OK|MB_ICONERROR);
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
		MessageBox(_T("û�н����߱�ָ����"), _T("��ʾ"), MB_ICONINFORMATION | MB_OK);
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
			MessageBox(_T("��Ϣ����ʧ�ܣ�"), _T("����"), MB_ICONERROR | MB_OK);
			return;
		}

		CString str;
		str.Format(_T("�Ҷ� %s ˵: %s"), m_strReceivers, m_strMsgToPush);
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
			MessageBox(_T("��ϢȺ��ʧ�ܣ�"), _T("����"), MB_ICONERROR | MB_OK);
			return;
		}

		CString str;
		str.Format(_T("�Ҷ� %s ˵: %s"), m_strReceivers, m_strMsgToPush);
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
		MessageBox(_T("��Ϣ�㲥ʧ�ܣ�"), _T("����"), MB_ICONERROR | MB_OK);
		return;
	}

	CString str;
	str.Format(_T("�ҶԴ��˵: %s"), m_strMsgToPush);
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
