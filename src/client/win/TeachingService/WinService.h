#pragma once

#include "WinSvc.h"
#include "WinServiceMsg.h" // Event message ids

#define SERVICE_CONTROL_USER 128

class CNTService
{
public:
	CNTService(const TCHAR * szServiceName);
	virtual ~CNTService();
	BOOL ParseStandardArgs(int argc, TCHAR * argv[]);
	BOOL IsInstalled();
	BOOL Install();
	BOOL Uninstall();
	BOOL StartService();
	void SetStatus(DWORD dwState);
	BOOL Initialize();
	virtual void Run();
	virtual BOOL OnInit();
	virtual void OnStop();
	virtual void OnInterrogate();
	virtual void OnPause();
	virtual void OnContinue();
	virtual void OnShutdown();
	virtual BOOL OnUserControl(DWORD dwOpcode);

	// static member functions
	static void WINAPI ServiceMain(DWORD dwArgc, LPTSTR* lpszArgv);
	static void WINAPI Handler(DWORD dwOpcode);

	// data members
	TCHAR m_szServiceName[64];
	SERVICE_STATUS_HANDLE m_hServiceStatus;
	SERVICE_STATUS m_Status;
	BOOL m_bIsRunning;

	// static data
	static CNTService* m_pThis; // nasty hack to get object ptr
};

class CMyService : public CNTService
{
public:
	CMyService();
	virtual BOOL OnInit();
	virtual void Run();
	virtual BOOL OnUserControl(DWORD dwOpcode);
	virtual void OnStop();

protected:
	bool DoConnect();

	static void CALLBACK onTextReceived(long connId, LPCSTR lpszText);
	static void CALLBACK onTextSent(long connId, LPCSTR lpszText);

	static void CALLBACK onLoginStatus(long connId, int nStatus);
	static void CALLBACK onLoginFailed(long connId, LPCSTR lpszReason);
	static void CALLBACK onLog(long connId, LPCSTR lpszLogText, int nLogLevel);
	static void CALLBACK onMsgKeyReceived(long connId, LPCSTR lpszMsgKey);
	static void CALLBACK onMaxInactiveTimeReceived(long connId, int nMaxInactiveTime);
	static void CALLBACK onMsgReceived(long connId, LPCSTR lpszMsg);
	static void CALLBACK onMsgReplied(long connId, LPCSTR lpszMsgId, bool bSuccess, LPCSTR lpszError);

	int CheckUser(LPCTSTR szUserId, LPCTSTR szRealName, LPCTSTR szUserPassword); // 0-failed, 1-student, 2-teacher
	long Signin(LPCTSTR szUserId, LPCTSTR szRealName, LPCTSTR szComputerName); // -1-error
	bool Signout(long nSignId);

	void SaveStatus();
	bool DoQueryTest(); 

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
	// 是否跟踪分组
	BOOL m_bTrackPacket;
	int m_nLoginStatus;

	CString m_strDatabaseName;
	CString m_strDatabaseServer;
	CString m_strDatabaseAccount;
	CString m_strDatabasePassword;
	_ConnectionPtr m_pConnection;

	CString m_strAppId;
	CString m_strAppPassword;
	CString m_strProtectKey;

	bool m_bRun;
};
