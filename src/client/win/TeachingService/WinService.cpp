#include "stdafx.h"
#include "WinService.h"
#include "NotificationPushAPI_proto.h"
#include "MyMessage.h"
#include "Markup.h"

static CMyService *g_pMyService;

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

// Global variable
TCHAR * g_szServiceName = NULL;
HANDLE g_hEventSource;

// static variables
CNTService* CNTService::m_pThis = NULL;

CNTService::CNTService(const TCHAR * szServiceName)
{
	// copy the address of the current object so we can access it from
	// the static member callback functions. 
	// WARNING: This limits the application to only one CNTService object. 
	m_pThis = this;

	// Set the default service name and version
	_tcsncpy_s(m_szServiceName,64,szServiceName,sizeof(m_szServiceName) - 1);
	g_szServiceName = &m_szServiceName[0];
	g_hEventSource = NULL;

	// set up the initial service status 
	m_hServiceStatus = NULL;
	m_Status.dwServiceType = SERVICE_WIN32_OWN_PROCESS;
	m_Status.dwCurrentState = SERVICE_STOPPED;
	m_Status.dwControlsAccepted = SERVICE_ACCEPT_STOP;
	m_Status.dwWin32ExitCode = 0;
	m_Status.dwServiceSpecificExitCode = 0;
	m_Status.dwCheckPoint = 0;
	m_Status.dwWaitHint = 0;
	m_bIsRunning = FALSE;
}

CNTService::~CNTService()
{
	if (g_hEventSource)
	{
		::DeregisterEventSource(g_hEventSource);
	}
}

////////////////////////////////////////////////////////////////////////////////////////
// Default command line argument parsing

// Returns TRUE if it found an arg it recognised, FALSE if not
// Note: processing some arguments causes output to stdout to be generated.
BOOL CNTService::ParseStandardArgs(int argc, TCHAR * argv[])
{
	// See if we have any command line args we recognise
	if (argc == 2)
	{
		if (_tcsicmp(argv[1], L"-install") == 0)
		{
			// Request to install.
			if (IsInstalled())
			{
				DebugMsg(L"The service %s is already installed\n",m_szServiceName);
			}
			else
			{
				// Try and install the copy that's running
				if (Install())
				{
					DebugMsg(L"The service %s has been installed\n",m_szServiceName);
				}
				else
				{
					DebugMsg(L"The service %s failed to be installed\n",m_szServiceName);
					DebugLastError();
				}
			}
			return TRUE; // say we processed the argument
		}
		else if (_tcsicmp(argv[1], L"-uninstall") == 0)
		{
			// Request to uninstall.
			if (!IsInstalled())
			{
				DebugMsg(L"The service %s is not installed\n",m_szServiceName);
			}
			else
			{
				// Try and remove the copy that's installed
				if (Uninstall())
				{
					// Get the executable file path
					TCHAR szFilePath[_MAX_PATH];
					::GetModuleFileName(NULL,szFilePath,sizeof(szFilePath));
					DebugMsg(L"The service %s (%s) has been uninstalled\n",m_szServiceName,szFilePath);
				}
				else
				{
					DebugMsg(L"The service %s failed to be uninstalled\n",m_szServiceName);
					DebugLastError();
				}
			}
			return TRUE; // say we processed the argument
		}
	}
	else
	{
		DebugMsg(L"Usage:\n");
		DebugMsg(L"To install the service use the command:\n%s.exe -install\n",m_szServiceName);
		DebugMsg(L"To uninstall the service use the command:\n%s.exe -uninstall\n",m_szServiceName);
	}

	// Don't recognise the args
	return FALSE;
}

////////////////////////////////////////////////////////////////////////////////////////
// Install/uninstall routines

// Test if the service is currently installed
BOOL CNTService::IsInstalled()
{
	BOOL bResult = FALSE;

	// Open the Service Control Manager with full access
	SC_HANDLE hSCM = ::OpenSCManager(NULL,NULL,SC_MANAGER_ALL_ACCESS);

	if (hSCM)
	{
		// Try to open the service
		SC_HANDLE hService = ::OpenService(hSCM,m_szServiceName,SERVICE_QUERY_CONFIG);
		if (hService)
		{
			bResult = TRUE;
			::CloseServiceHandle(hService);
		}
		::CloseServiceHandle(hSCM);
	}

	return bResult;
}

// Install the service
BOOL CNTService::Install()
{
	// Open the Service Control Manager with full access
	SC_HANDLE hSCM = ::OpenSCManager(NULL,NULL,SC_MANAGER_ALL_ACCESS);

	if (!hSCM)
		return FALSE;

	// Get the executable file path
	TCHAR szFilePath[_MAX_PATH];
	::GetModuleFileName(NULL, szFilePath, sizeof(szFilePath));

	// Create the service
	SC_HANDLE hService = ::CreateService(hSCM,m_szServiceName,m_szServiceName,SERVICE_ALL_ACCESS,SERVICE_WIN32_OWN_PROCESS,SERVICE_AUTO_START,SERVICE_ERROR_NORMAL,szFilePath,NULL,NULL,NULL,NULL,NULL);
	if (!hService)
	{
		::CloseServiceHandle(hSCM);
		return FALSE;
	}

	// Set the description of the service
	SERVICE_DESCRIPTION Desc;
	Desc.lpDescription = L"Command Line Service description goes here...";
	ChangeServiceConfig2(hService,SERVICE_CONFIG_DESCRIPTION,&Desc);

	// Tidy up
	::CloseServiceHandle(hService);
	::CloseServiceHandle(hSCM);
	return TRUE;
}

// Uninstall the service
BOOL CNTService::Uninstall()
{
	// Open the Service Control Manager with full access
	SC_HANDLE hSCM = ::OpenSCManager(NULL,NULL,SC_MANAGER_ALL_ACCESS);
	
	if (!hSCM)
		return FALSE;

	BOOL bResult = FALSE;
	SC_HANDLE hService = ::OpenService(hSCM,m_szServiceName,DELETE);
	if (hService)
	{
		if (::DeleteService(hService))
			bResult = TRUE;
		::CloseServiceHandle(hService);
	}

	::CloseServiceHandle(hSCM);
	return bResult;
}

//////////////////////////////////////////////////////////////////////////////////////////////
// Service startup and registration

BOOL CNTService::StartService()
{
	SERVICE_TABLE_ENTRY st[] = 
	{
		{m_szServiceName, ServiceMain},
		{NULL, NULL}
	};

	BOOL b = ::StartServiceCtrlDispatcher(st);
	DebugMsg(L"Returned from StartServiceCtrlDispatcher()\n");
	return b;
}

// static member function (callback)
void CNTService::ServiceMain(DWORD dwArgc, LPTSTR* lpszArgv)
{
	// Get a pointer to the C++ object
	CNTService* pService = m_pThis;

	DebugMsg(L"Entering CNTService::ServiceMain()\n");

	// Register the control request handler
	pService->m_Status.dwCurrentState = SERVICE_START_PENDING;
	pService->m_hServiceStatus = RegisterServiceCtrlHandler(pService->m_szServiceName,Handler);
	if (pService->m_hServiceStatus == NULL)
	{
		return;
	}

	// Start the initialisation
	if (pService->Initialize()) {

		// Do the real work. 
		// When the Run function returns, the service has stopped.
		pService->m_bIsRunning = TRUE;
		pService->m_Status.dwWin32ExitCode = 0;
		pService->m_Status.dwCheckPoint = 0;
		pService->m_Status.dwWaitHint = 0;
		pService->Run();
	}

	// Tell the service manager we are stopped
	pService->SetStatus(SERVICE_STOPPED);
	DebugMsg(L"Leaving CNTService::ServiceMain()\n");
}

///////////////////////////////////////////////////////////////////////////////////////////
// status functions

void CNTService::SetStatus(DWORD dwState)
{
	DebugMsg(L"CNTService::SetStatus(%lu,%lu)\n",m_hServiceStatus,dwState);
	m_Status.dwCurrentState = dwState;
	::SetServiceStatus(m_hServiceStatus,&m_Status);
}

///////////////////////////////////////////////////////////////////////////////////////////
// Service initialization

BOOL CNTService::Initialize()
{
	DebugMsg(L"Entering CNTService::Initialize()\n");

	// Start the initialization
	SetStatus(SERVICE_START_PENDING);

	// Perform the actual initialization
	BOOL bResult = OnInit(); 

	// Set final state
	m_Status.dwWin32ExitCode = GetLastError();
	m_Status.dwCheckPoint = 0;
	m_Status.dwWaitHint = 0;
	if (!bResult) {
		SetStatus(SERVICE_STOPPED);
		return FALSE;    
	}

	SetStatus(SERVICE_RUNNING);

	DebugMsg(L"Leaving CNTService::Initialize()\n");
	return TRUE;
}

///////////////////////////////////////////////////////////////////////////////////////////////
// main function to do the real work of the service

// This function performs the main work of the service. 
// When this function returns the service has stopped.
void CNTService::Run()
{
	DebugMsg(L"Entering CNTService::Run()");

	while (m_bIsRunning)
	{
		DebugMsg(L"Sleeping for 5 seconds...\n");
		Sleep(5000);
	}

	// nothing more to do
	DebugMsg(L"Leaving CNTService::Run()");
}

//////////////////////////////////////////////////////////////////////////////////////
// Control request handlers

// static member function (callback) to handle commands from the
// service control manager
void CNTService::Handler(DWORD dwOpcode)
{
	// Get a pointer to the object
	CNTService* pService = m_pThis;

	DebugMsg(L"CNTService::Handler(%lu)\n",dwOpcode);
	switch (dwOpcode)
	{
	case SERVICE_CONTROL_STOP: // 1
		pService->SetStatus(SERVICE_STOP_PENDING);
		pService->OnStop();
		pService->m_bIsRunning = FALSE;
		break;

	case SERVICE_CONTROL_PAUSE: // 2
		pService->OnPause();
		break;

	case SERVICE_CONTROL_CONTINUE: // 3
		pService->OnContinue();
		break;

	case SERVICE_CONTROL_INTERROGATE: // 4
		pService->OnInterrogate();
		break;

	case SERVICE_CONTROL_SHUTDOWN: // 5
		pService->OnShutdown();
		break;

	default:
		if (dwOpcode >= SERVICE_CONTROL_USER)
		{
			if (!pService->OnUserControl(dwOpcode))
			{
			}
		}
		else
		{
		}
		break;
	}

	// Report current status
	DebugMsg(L"Updating status (%lu,%lu)\n",pService->m_hServiceStatus,pService->m_Status.dwCurrentState);
	::SetServiceStatus(pService->m_hServiceStatus,&pService->m_Status);
}

// Called when the service is first initialized
BOOL CNTService::OnInit()
{
	DebugMsg(L"CNTService::OnInit()\n");
	return TRUE;
}

// Called when the service control manager wants to stop the service
void CNTService::OnStop()
{
	DebugMsg(L"CNTService::OnStop()\n");
}

// called when the service is interrogated
void CNTService::OnInterrogate()
{
	DebugMsg(L"CNTService::OnInterrogate()\n");
}

// called when the service is paused
void CNTService::OnPause()
{
	DebugMsg(L"CNTService::OnPause()\n");
}

// called when the service is continued
void CNTService::OnContinue()
{
	DebugMsg(L"CNTService::OnContinue()\n");
}

// called wh'e''''''''']]''n the service is shut down
void CNTService::OnShutdown()
{
	DebugMsg(L"CNTService::OnShutdown()\n");
}

// called when the service gets a user control message
BOOL CNTService::OnUserControl(DWORD dwOpcode)
{
	DebugMsg(L"CNTService::OnUserControl(%8.8lXH)\n",dwOpcode);
	return FALSE; // say not handled
}

CMyService::CMyService() : 
CNTService(L"TeachingService")
, m_strServer(_T("127.0.0.1"))
, m_nPort(3457)
, m_strUsername(_T("TeachingService"))
, m_strPassword(_T("666666"))
, m_bTrackPacket(false)
, m_nReconnectDelay(1000)
, m_nLoginStatus(0)
, m_bRun(true)
, m_strDatabaseName(_T("labinfo"))
, m_strDatabaseServer(_T("127.0.0.1"))
, m_strDatabaseAccount(_T("sa"))
, m_strDatabasePassword(_T(""))
, m_strAppId(_T("5A6D032A-DB6C-43BF-98EE-A699FBCAA628"))
, m_strAppPassword(_T("THHgux8k"))
, m_strProtectKey(_T(""))
{
	g_pMyService = this;
}

BOOL CMyService::OnInit()
{
	CFile file;
	CString tmp, text;
	CFileException ex;

	TCHAR szPath[MAX_PATH];
	if (!GetModuleFileName(NULL, szPath, MAX_PATH))
	{
		DebugMsg(_T("无法获取当前模块文件名"));
		return FALSE;
	}
	TCHAR szDrv[_MAX_DRIVE], szDir[_MAX_DIR];
	_tsplitpath(szPath, szDrv, szDir, NULL, NULL);
	CString strTargetPath;
	strTargetPath.Format(_T("%s%sTeachingService.xml"), szDrv, szDir);
	DebugMsg(_T("打开配置文件 %s... "), strTargetPath);
	if (!file.Open(strTargetPath, CFile::modeRead | CFile::shareDenyWrite))
	{
		DebugMsg(_T("无法打开配置文件"));
	}
	else
	{
		CArchive ar(&file, CArchive::load);
		while (ar.ReadString(tmp))//循环读取文件，直到文件结束
		{
			text += tmp + _T("\r\n");
		}
		DebugMsg(text);

		CMarkup xml;
		xml.SetDoc(text);
		//
		if (xml.FindChildElem(_T("msgpush")))
		{
			xml.IntoElem();
			if (xml.FindChildElem(_T("server")))
			{
				m_strServer = xml.GetChildData();
				DebugMsg(_T("msgpush server: %s"), m_strServer);
			}
			if (xml.FindChildElem(_T("port")))
			{
				m_nPort = StrToInt(xml.GetChildData());
				DebugMsg(_T("msgpush port: %d"), m_nPort);
			}
			if (xml.FindChildElem(_T("username")))
			{
				m_strUsername = xml.GetChildData();
				DebugMsg(_T("msgpush username: %s"), m_strUsername);
			}
			if (xml.FindChildElem(_T("password")))
			{
				m_strPassword = xml.GetChildData();
				DebugMsg(_T("msgpush password: %s"), m_strPassword);
			}
			xml.OutOfElem();
		}
		//
		if (xml.FindChildElem(_T("database")))
		{
			xml.IntoElem();
			if (xml.FindChildElem(_T("name")))
			{
				m_strDatabaseName = xml.GetChildData();
				DebugMsg(_T("database name: %s"), m_strDatabaseName);
			}
			if (xml.FindChildElem(_T("server")))
			{
				m_strDatabaseServer = xml.GetChildData();
				DebugMsg(_T("database server: %s"), m_strDatabaseServer);
			}
			if (xml.FindChildElem(_T("username")))
			{
				m_strDatabaseAccount = xml.GetChildData();
				DebugMsg(_T("database account: %s"), m_strDatabaseAccount);
			}
			if (xml.FindChildElem(_T("password")))
			{
				m_strDatabasePassword = xml.GetChildData();
				DebugMsg(_T("database password: %s"), m_strDatabasePassword);
			}
			xml.OutOfElem();
		}
		//
		if (xml.FindChildElem(_T("app")))
		{
			xml.IntoElem();
			if (xml.FindChildElem(_T("id")))
			{
				m_strAppId = xml.GetChildData();
				DebugMsg(_T("app id: %s"), m_strAppPassword);
			}
			if (xml.FindChildElem(_T("password")))
			{
				m_strAppPassword = xml.GetChildData();
				DebugMsg(_T("app password: %s"), m_strAppPassword);
			}
			if (xml.FindChildElem(_T("protectkey")))
			{
				m_strProtectKey = xml.GetChildData();
				DebugMsg(_T("app protectkey: %s"), m_strProtectKey);
			}
			xml.OutOfElem();
		}
	}

	return TRUE;
}

// The main service body
void CMyService::Run()
{
	DebugMsg(L"Entering CMyService::Run()\n");

	HRESULT hr;
	try
	{
		DebugMsg(_T("初始化COM..."));
		CoInitialize(NULL);
		DebugMsg(_T("创建 ADODB.Connection 实例..."));
		hr = m_pConnection.CreateInstance("ADODB.Connection");
		if (SUCCEEDED(hr))
		{
			//connect database
			DebugMsg(_T("连接数据库..."));
			_bstr_t bstrDatabaseAccount = m_strDatabaseAccount;
			_bstr_t bstrDatabasePassword = m_strDatabasePassword.AllocSysString();
			_bstr_t bstrConnect = _T("Provider=SQLOLEDB.1;Initial Catalog=\"") + m_strDatabaseName + _T("\";Data Source=") + m_strDatabaseServer;
			hr = m_pConnection->Open(bstrConnect, bstrDatabaseAccount, bstrDatabasePassword, adModeUnknown);
			DebugMsg(_T("连接成功"));
		}
		else
		{
			DebugLastError();
			m_bRun = false;
			return;
		}
	}
	catch (_com_error e)
	{
		DebugMsg(_T("连接数据库失败！\r\n错误信息:%s"), e.ErrorMessage());
		m_bRun = false;
		return;
	}

	DebugMsg(_T("进行数据库查询测试..."));
	DoQueryTest();

	DebugMsg(_T("连接消息推送平台..."));
	if (!DoConnect())
	{
		m_bRun = false;
		return;
	}

	DebugMsg(_T("等待服务被终止..."));
	do
	{
		Sleep(1000);
	}
	while (m_bRun);

	DebugMsg(L"Leaving CMyService::Run()\n");
}

// Stop the service
void CMyService::OnStop()
{
	DoQueryTest();
	m_pConnection->Close();

	Disconnect(0);
	m_bRun = false;
}

// Process user control requests
BOOL CMyService::OnUserControl(DWORD dwOpcode)
{
	switch (dwOpcode)
	{
	case SERVICE_CONTROL_USER + 0:
		// Save the current status in the registry
		SaveStatus();
		return TRUE;

	default:
		break;
	}
	return FALSE; // say not handled
}

// Save the current status in the registry
void CMyService::SaveStatus()
{
	DebugMsg(L"CMyService::SaveStatus()\n");
}

bool CMyService::DoConnect()
{
	char szPort[10];
	_itoa_s(m_nPort, szPort, 10);
	CT2A server(m_strServer);

	CT2A appId(m_strAppId);
	CT2A appPassword(m_strAppPassword);
	CT2A protectKey(m_strProtectKey);

	SetAppInfo(appId, appPassword, protectKey);
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

	CT2A username(m_strUsername);
	CT2A password(m_strPassword);
	if (!LoginAsUser(0, username, password, true, m_nReconnectDelay, m_bTrackPacket == TRUE)) {
		DebugMsg(_T("登录失败!"));
		return false;
	}

	return true;
}

void CALLBACK CMyService::onTextReceived(long connId, LPCSTR lpszText)
{
}

void CALLBACK CMyService::onTextSent(long connId, LPCSTR lpszText)
{
}

void CALLBACK CMyService::onLoginStatus(long connId, int nStatus)
{
	if (nStatus == g_pMyService->m_nLoginStatus) return;
	g_pMyService->m_nLoginStatus = nStatus;

	CTime time = CTime::GetCurrentTime();

	DebugMsg(_T("[%s]%s"), time.Format("%H:%M:%S"), LoginStatusText[nStatus]);

	switch (nStatus)
	{
	case LOGOUT:
		break;
	case LOGINING:
		break;
	case LOGON:
		break;
	}
}

void CALLBACK CMyService::onLoginFailed(long connId, LPCSTR lpszReason)
{
	CA2T text(lpszReason);

	CTime time = CTime::GetCurrentTime();

	DebugMsg(_T("[%s]登录失败: %s"), time.Format("%H:%M:%S"), text.m_psz);
}

void CALLBACK CMyService::onLog(long connId, LPCSTR lpszLogText, int nLogLevel)
{
	CA2T text(lpszLogText);

	CTime time = CTime::GetCurrentTime();
	DebugMsg(_T("[%s]%s: %s"), time.Format("%H:%M:%S"), LogLevelText[nLogLevel], text.m_psz);
}

void CALLBACK CMyService::onMsgKeyReceived(long connId, LPCSTR lpszMsgKey)
{
}

void CALLBACK CMyService::onMaxInactiveTimeReceived(long connId, int nMaxInactiveTime)
{
}

void CALLBACK CMyService::onMsgReceived(long connId, LPCSTR lpszMsg)
{
	MyMessage msg = MyMessage::parse(lpszMsg);
	CA2T msgSender(msg.getSender().c_str());
	CA2T msgType(msg.getType().c_str());
	CA2T msgTitle(msg.getTitle().c_str());
	CA2T msgBody(msg.getBody().c_str());

	DebugMsg(_T("%s说: [%s]%s(%s)"), msgSender.m_psz, msgTitle.m_psz, msgBody.m_psz, msgType.m_psz);
	if (_tcscmp(msgType.m_psz, _T("xml")) != 0) return;

	CMarkup xml;
	xml.SetDoc(msgBody);
	if (_tcscmp(msgTitle.m_psz, _T("signin")) == 0)
	{
		MyMessage msg2;
		msg2.setType("xml");
		msg2.setTitle("signin_result");
		CString xmlResp;

		if (!xml.FindChildElem(_T("userid"))) return;
		CString userId = xml.GetChildData();
		if (!xml.FindChildElem(_T("realname"))) return;
		CString realName = xml.GetChildData();
		if (!xml.FindChildElem(_T("userpassword"))) return;
		CString userPassword = xml.GetChildData();
		DebugMsg(_T("Sign info: userId=%s, realName=%s, userPassword=%s"), userId, realName, userPassword);
		int userType = g_pMyService->CheckUser(userId, realName, userPassword);
		if (userType)
		{
			// 身份检查成功
			long signId = g_pMyService->Signin(userId, realName, msgSender.m_psz);

			xmlResp.Format(
				_T("<root>")
				_T("  <success>true</success>")
				_T("  <role>%s</role>")
				_T("  <signid>%ld</signid>")
				_T("</root>"),
				userType==1?_T("sudent"):_T("teacher"),
				signId);
		}
		else
		{
			// 身份检查失败
			xmlResp.Format(
				_T("<root>")
				_T("  <success>false</success>")
				_T("  <reason>%s</reason>")
				_T("</root>"),
				_T("签到信息有误！"));
		}
		CT2A resp(xmlResp, CP_UTF8);
		CA2T receiver(msg.getSender().c_str());
		CT2A receiver2(receiver, CP_UTF8);
		msg2.setBody(resp.m_psz);
		if (!SendMessageTo(0, receiver2.m_psz, "signin_result", msg2.toJson().c_str()))
		{
			DebugMsg(_T("Failed to send message."));
		}
	}
	else if (_tcscmp(msgTitle.m_psz, _T("signout")) == 0)
	{
		MyMessage msg2;
		msg2.setType("xml");
		msg2.setTitle("signout_result");
		CString xmlResp;

		if (!xml.FindChildElem(_T("signid"))) return;
		long signId = StrToLong(xml.GetChildData());
		DebugMsg(_T("Signout: %ld"), signId);
		bool ok = g_pMyService->Signout(signId);
		if (ok)
		{
			// 签出成功
			xmlResp.Format(
				_T("<root>")
				_T("  <success>true</success>")
				_T("</root>"));
		}
		else
		{
			// 签出失败
			xmlResp.Format(
				_T("<root>")
				_T("  <success>false</success>")
				_T("  <reason>%s</reason>")
				_T("</root>"),
				_T("服务器出错！"));
		}
		CT2A resp(xmlResp, CP_UTF8);
		CA2T receiver(msg.getSender().c_str());
		CT2A receiver2(receiver, CP_UTF8);
		msg2.setBody(resp.m_psz);
		if (!SendMessageTo(0, receiver2.m_psz, "signout_result", msg2.toJson().c_str()))
		{
			DebugMsg(_T("Failed to send message."));
		}
	}
}

void CALLBACK CMyService::onMsgReplied(long connId, LPCSTR lpszMsgId, bool bSuccess, LPCSTR lpszError)
{
	CA2T msgId(lpszMsgId);

	if (bSuccess) {
		//str.Format(_T("消息发送成功"));
	}
	else {
		CA2T error(lpszError);
		DebugMsg(_T("消息发送失败: %s"), error.m_psz);
	}
}

int CMyService::CheckUser(LPCTSTR szUserId, LPCTSTR szRealName, LPCTSTR szUserPassword)
{
	try
	{
		_variant_t RecordsAffected;
		_RecordsetPtr pRecordset;
		CString cmdText;
		int len = lstrlen(szUserId);
		int ret = 0;
		if (len==10)
		{
			// 学生
			cmdText.Format(_T("SELECT count(*) as matchedCount from student where student_id='%s' and student_name='%s' and login_password='%s'"),
				szUserId, szRealName, szUserPassword);
			DebugMsg(_T("Execute %s..."), cmdText);
			pRecordset = m_pConnection->Execute((_bstr_t)cmdText, &RecordsAffected, adCmdText);
			ret = 1;
		}
		else if (len==5 && szUserId[0]==_T('T'))
		{
			// 教师
			cmdText.Format(_T("SELECT count(*) AS matchedCount FROM teacher WHERE teacher_id='%s' AND teacher_name='%s' AND login_password='%s'"),
				szUserId, szRealName, szUserPassword);
			DebugMsg(_T("Execute %s..."), cmdText);
			pRecordset = m_pConnection->Execute((_bstr_t)cmdText, &RecordsAffected, adCmdText);
			ret = 2;
		}
		else
		{
			// 错误!!!
			DebugMsg(_T("Wrong user id: %s"), szUserId);
			return 0;
		}
		_variant_t matchedCount;
		matchedCount = pRecordset->GetCollect("matchedCount");
		if ((int)matchedCount==0) ret = 0; 
		pRecordset->Close();
		return ret;
	}
	catch (_com_error e)
	{
		DebugMsg(_T("查询失败！\r\n错误信息:%s"), e.ErrorMessage());
		return 0;

	}
}

long CMyService::Signin(LPCTSTR szUserId, LPCTSTR szRealName, LPCTSTR szComputerName)
{
	try
	{
		_variant_t RecordsAffected;
		CString cmdText;
		cmdText.Format(_T("INSERT INTO sign (userId, realName, signinTime, computerName) values ('%s','%s',getdate(),'%s')"),
			szUserId, szRealName, szComputerName);
		DebugMsg(_T("Execute %s..."), cmdText);
		m_pConnection->Execute((_bstr_t)cmdText, &RecordsAffected, adCmdText);
		//
		cmdText.Format(_T("SELECT TOP 1 id AS signid FROM sign ORDER BY signinTime desc"));
		DebugMsg(_T("Execute %s..."), cmdText);
		_RecordsetPtr pRecordset;
		pRecordset = m_pConnection->Execute((_bstr_t)cmdText, &RecordsAffected, adCmdText);
		_variant_t signId;
		signId = pRecordset->GetCollect("signid");
		pRecordset->Close();
		return signId;
	}
	catch (_com_error e)
	{
		DebugMsg(_T("插入失败！\r\n错误信息:%s"), e.ErrorMessage());
	}
	return -1;
}

bool CMyService::Signout(long nSignId)
{
	try
	{
		_variant_t RecordsAffected;
		CString cmdText;
		cmdText.Format(_T("UPDATE sign SET signoutTime=getdate() WHERE id=%ld"), nSignId);
		DebugMsg(_T("Execute %s..."), cmdText);
		m_pConnection->Execute((_bstr_t)cmdText, &RecordsAffected, adCmdText);
		return true;
	}
	catch (_com_error e)
	{
		DebugMsg(_T("更新失败！\r\n错误信息:%s"), e.ErrorMessage());
		return false;
	}
}

bool CMyService::DoQueryTest()
{
	try
	{
		_variant_t RecordsAffected;
		_RecordsetPtr pRecordset;
		pRecordset = m_pConnection->Execute("SELECT getdate() as now", &RecordsAffected, adCmdText);
		while (!pRecordset->ADOEOF)
		{
			_variant_t now;
			now = pRecordset->GetCollect("now");
			DebugMsg(_T("Server datetime: %s"), (TCHAR*)(_bstr_t)now);

			pRecordset->MoveNext();
		}
		pRecordset->Close();
		return true;
	}
	catch (_com_error e)
	{
		DebugMsg(_T("查询失败！\r\n错误信息:%s"), e.ErrorMessage());
		return false;

	}
}
