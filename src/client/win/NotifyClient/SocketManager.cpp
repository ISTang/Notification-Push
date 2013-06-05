// SocketManager.cpp: implementation of the CSocketManager class.
//
//////////////////////////////////////////////////////////////////////

#include "stdafx.h"
#include "SocketManager.h"

using namespace std;

#ifdef _DEBUG
#undef THIS_FILE
static char THIS_FILE[]=__FILE__;
#define new DEBUG_NEW
#endif

/*
const UINT EVT_CONSUCCESS = 0x0000;	// Connection established
const UINT EVT_CONFAILURE = 0x0001;	// General failure - Wait Connection failed
const UINT EVT_CONDROP	  = 0x0002;	// Connection dropped
const UINT EVT_ZEROLENGTH = 0x0003;	// Zero length message
*/

void RequestLocker()
{
	WaitForSingleObject(g_oLocker, INFINITE);
}
void ReleaseLocker()
{
	ReleaseMutex(g_oLocker);
}

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

CSocketManager::CSocketManager(ClientInfo* pClientInfo)
{
	this->m_pClientInfo = pClientInfo;
}

CSocketManager::~CSocketManager()
{

}

void CSocketManager::OnDataReceived(const LPBYTE lpBuffer, DWORD dwCount)
{
	string sInput = m_pClientInfo->oldInput + string((LPCSTR)lpBuffer, dwCount);

	DWORD dwBytesWritten = 0L;

	//RequestLocker();
	//cout<<sInput;
	//ReleasetLocker();

	if (sInput.compare(0, strlen(ID_PROMPT), ID_PROMPT)==0) {
		// 接收到输入ID提示符
		m_pClientInfo->oldInput = sInput.substr(strlen(ID_PROMPT));
		sInput = ID_PROMPT;
	}
	else if (sInput.compare(0, strlen(PASSWORD_PROMPT), PASSWORD_PROMPT)==0) {
		// 接收到输入密码提示符
		m_pClientInfo->oldInput = sInput.substr(strlen(PASSWORD_PROMPT));
		sInput = PASSWORD_PROMPT;
	} else {
		// 其它输入
		int pos = sInput.find_first_of(NEWLINE);
		if (pos!=sInput.npos) {
			m_pClientInfo->oldInput = sInput.substr(pos+strlen(NEWLINE));
			sInput = sInput.substr(0, pos);
		} else {
			m_pClientInfo->oldInput = sInput;
			sInput = "";
		}
	}

	if (sInput=="") return;

	if (SERVER_BUSY_MSG==sInput) {

		RequestLocker();
		cout<<"["<<m_pClientInfo->clientId<<"] Server busy"<<endl;
		ReleaseLocker();

		m_pClientInfo->serverBusy = true;
	}
	else if (ID_PROMPT==sInput) {

		RequestLocker();
		cout<<"["<<m_pClientInfo->clientId<<"] Sending client id..."<<endl;
		ReleaseLocker();

		string sTemp = m_pClientInfo->clientId + NEWLINE;
		dwBytesWritten = WriteComm((LPBYTE)(sTemp.c_str()), sTemp.length(), SOCKET_WRITE_TIMEOUT);
		if (dwBytesWritten!=sTemp.length()) {
			// TODO 发送失败
		}
	}
	else if (INVALID_ID_MSG==sInput) {

		RequestLocker();
		cout<<"["<<m_pClientInfo->clientId<<"] Invalid client id"<<endl;
		ReleaseLocker();

		m_pClientInfo->invalidClientInfo = true;
	}
	else if (PASSWORD_PROMPT==sInput) {

		RequestLocker();
		cout<<"["<<m_pClientInfo->clientId<<"] Sending client password..."<<endl;
		ReleaseLocker();

		string sTemp = m_pClientInfo->clientId + NEWLINE;
		dwBytesWritten = WriteComm((LPBYTE)(sTemp.c_str()), sTemp.length(), SOCKET_WRITE_TIMEOUT);
		if (dwBytesWritten!=sTemp.length()) {
			// TODO 发送失败
		}
	}
	else if (INVALID_PASSWORD_MSG==sInput) {

		RequestLocker();
		cout<<"["<<m_pClientInfo->clientId<<"] Invalid client password"<<endl;
		ReleaseLocker();

		m_pClientInfo->invalidClientInfo = true;
	}
	else if (WAIT_NOTIFY_MSG==sInput) {

		RequestLocker();
		cout<<"["<<m_pClientInfo->clientId<<"] Logon."<<endl;
		ReleaseLocker();

		m_pClientInfo->clientLogon = true;
		m_pClientInfo->clientLogining = false;
	}
	else if (CLIENT_OFFLINE_MSG==sInput || string(ID_PROMPT)+CLIENT_OFFLINE_MSG==sInput
		|| string(PASSWORD_PROMPT)+CLIENT_OFFLINE_MSG==sInput) {

		RequestLocker();
		cout<<"["<<m_pClientInfo->clientId<<"] Server kickoff me."<<endl;
		ReleaseLocker();

		m_pClientInfo->clientLogon = false;
	}
	else {

		RequestLocker();
		cout<<"["<<m_pClientInfo->clientId<<"] >>>"<<sInput<<"<<<"<<endl;
		ReleaseLocker();
	}
}

///////////////////////////////////////////////////////////////////////////////
// OnEvent
// Send message to parent window to indicate connection status
void CSocketManager::OnEvent(UINT uEvent, LPVOID lpvData)
{
	switch( uEvent )
	{
		case EVT_CONSUCCESS:
			//RequestLocker();
			//cout<<"["<<m_pClientInfo->clientId<<"] Connected."<<endl;
			//ReleaseLocker();
			break;
		case EVT_CONFAILURE:
			//RequestLocker();
			//cout<<"["<<m_pClientInfo->clientId<<"] Failed to connect."<<endl;
			//ReleaseLocker();
			break;
		case EVT_CONDROP:
			RequestLocker();
			cout<<"["<<m_pClientInfo->clientId<<"] Disconnected."<<endl;
			ReleaseLocker();

			m_pClientInfo->disconnected = true;
			break;
		case EVT_ZEROLENGTH:
			RequestLocker();
			cout<<"["<<m_pClientInfo->clientId<<"] Zero length data."<<endl;
			ReleaseLocker();
			break;
		default:
			RequestLocker();
			cout<<"["<<m_pClientInfo->clientId<<"] Unknown Socket event."<<endl;
			ReleaseLocker();
			break;
	}
}
