// CLS.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include "WinService.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#endif


// The one and only application object

CWinApp theApp;

using namespace std;

int _tmain(int argc, TCHAR* argv[], TCHAR* envp[])
{
	int nRetCode = 0;

	// initialize MFC and print and error on failure
	if (!AfxWinInit(::GetModuleHandle(NULL), NULL, ::GetCommandLine(), 0))
	{
		// TODO: change error code to suit your needs
		_tprintf(_T("Fatal Error: MFC initialization failed\n"));
		nRetCode = 1;
	}
	else if (!AfxSocketInit())
	{
		_tprintf(_T("Fatal Error: Socket initialization failed\n"));
		nRetCode = 2;
	}
	else if (!AfxOleInit())//初始化OLE/COM库环境        
	{
		_tprintf(_T("Fatal Error: OLE initialization failed\n"));
		nRetCode = 3;
	}
	else
	{
		// Parse for standard arguments (install, uninstall, usage)
		CMyService MyService;
		if (!MyService.ParseStandardArgs(argc, argv))
			MyService.StartService();

		// When we get here, the service has been stopped
		return MyService.m_Status.dwWin32ExitCode;
	}

	return nRetCode;
}
