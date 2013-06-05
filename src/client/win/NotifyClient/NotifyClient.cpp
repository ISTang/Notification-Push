// NotifyClient.cpp : 定义控制台应用程序的入口点。
//

#include "stdafx.h"
#include "SocketManager.h"

using namespace std;

string g_sServerAddr = "127.0.0.1";
int g_nServerPort = 8088;
int g_nClientIdStart = 10000;
int g_nClientCount = 1;

HANDLE g_oLocker;

void createWorker(int i, ClientInfo **pClientInfos, HANDLE*pThreadHandles);

void CheckKey( void *dummy );

unsigned testSingleClient(ClientInfo *pClientInfo);
unsigned __stdcall testMultipleClient(void *param);

static bool repeat = true;

int main(int argc, char* argv[]) {

	if (argc < 5) {
		cerr<<"Usage: NotifyClient <server_addr> <server_port> <client_id_start> <client_count>"<<endl;
		return 0;
	}

	g_sServerAddr = argv[1];
	g_nServerPort = atoi(argv[2]);
	g_nClientIdStart = atoi(argv[3]);
	g_nClientCount = atoi(argv[4]);

	if (g_nClientIdStart<10000 || g_nClientIdStart+g_nClientCount>100000) {
		cerr<<"client_id_start must be less than or equals to 10000, and the max value of client id must not be greater than 100000"<<endl;
		return 1;
	}

	WSADATA	WSAData = { 0 };
	if ( 0 != WSAStartup( WSA_VERSION, &WSAData ) )
	{
		// Tell the user that we could not find a usable
		// WinSock DLL.
		if ( LOBYTE( WSAData.wVersion ) != LOBYTE(WSA_VERSION) ||
			 HIBYTE( WSAData.wVersion ) != HIBYTE(WSA_VERSION) )
			 ::MessageBox(NULL, _T("Incorrect version of WS2_32.dll found"), _T("Error"), MB_OK);

		WSACleanup( );
		return 0;
	}

	g_oLocker = CreateMutex(NULL, FALSE, "log");

	HANDLE *pThreadHandles = new HANDLE[g_nClientCount];
	ClientInfo **pClientInfos = new ClientInfo *[g_nClientCount];

	cout<<"Connecting..."<<endl;
	for (int i=0; i<g_nClientCount; i++) {

		createWorker(i, pClientInfos, pThreadHandles);

		//// 延迟1-2ms
		//Sleep(1+rand()%2);
	}

	/* Launch CheckKey thread to check for terminating keystroke. */
	_beginthread(CheckKey, 0, NULL);

	while(repeat) {

		for (int i=0; i<g_nClientCount; i++) {

			HANDLE hThread = pThreadHandles[i];
			if (WAIT_OBJECT_0==WaitForSingleObject( hThread, 0 )) {
				// worker exited
				ClientInfo *pClientInfo = pClientInfos[i];

				if (!pClientInfo->invalidClientInfo) {
					// recreate it
					createWorker(i, pClientInfos, pThreadHandles);

					RequestLocker();
					cout<<"["<<pClientInfo->clientId<<"] "<<"worker thread recreated"<<endl;
					ReleaseLocker();
				}
				delete pClientInfo;
				CloseHandle(hThread);
			}
		}

		Sleep( 1000L );
	}

	for (int i=0; i<g_nClientCount; i++) {

		ClientInfo *pClientInfo = pClientInfos[i];
		HANDLE hThread = pThreadHandles[i];

		delete pClientInfo;
		CloseHandle(hThread);
	}

	delete[] pThreadHandles;
	delete[] pClientInfos;

	CloseHandle(g_oLocker);

	// Terminate use of the WS2_32.DLL
	WSACleanup();

	return 0;
}

void createWorker(int i, ClientInfo **pClientInfos, HANDLE*pThreadHandles)
{
	int id = g_nClientIdStart + i;
	char sClientId[7];
	sprintf_s(sClientId, "%06d", id);

	ClientInfo *pClientInfo = new ClientInfo(sClientId);
	pClientInfos[i] = pClientInfo;

	//RequestLocker();
	//cout<<"Starting thread for client ID "<<sClientId<<endl;
	//ReleaseLocker()

	HANDLE hThread;
	unsigned threadId;
	hThread = (HANDLE)_beginthreadex(NULL, 0, &testMultipleClient, pClientInfo, 0, &threadId);
	pThreadHandles[i] = hThread;
}

/* CheckKey - Thread to wait for a keystroke, then clear repeat flag. */
void CheckKey( void *dummy )
{
	_getch();
	repeat = 0;    /* _endthread implied */

}

unsigned testSingleClient(ClientInfo *pClientInfo) {

	//RequestLocker();
	//cout<<"client_id:"<<pClientInfo->clientId<<endl;
	//RleaseLocker();

	CSocketManager socketManager(pClientInfo);
	bool bSuccess;
    DWORD dwBytesWritten = 0L;

	char sServerPort[10];
	itoa(g_nServerPort, sServerPort, 10);
	string sKeepAliveMsg = string(KEEP_ALIVE_MSG) + NEWLINE;
	
	int keepAliveCount = 0;

	int errCode = 0;

	while(repeat) {

		if (pClientInfo->clientLogon) {
			// 已登录，发送心跳包

			if (pClientInfo->disconnected) {
				// 连接被断开
				socketManager.StopComm();
				pClientInfo->clientLogon = false;
				errCode = ERROR_DISCONNECTED;
				break;
			}

			Sleep(KEEP_ALIVE_INTERVAL);

			dwBytesWritten = socketManager.WriteComm((LPBYTE)sKeepAliveMsg.c_str(), sKeepAliveMsg.length(), SOCKET_WRITE_TIMEOUT);
			if (dwBytesWritten != sKeepAliveMsg.length()) {
				// 发送失败
				RequestLocker();
				cout<<"["<<pClientInfo->clientId<<"] "<<"Failed to keep alive"<<endl;
				ReleaseLocker();

				keepAliveCount++;

				if (keepAliveCount>KEEP_ALIVE_COUNT) {
					
					socketManager.StopComm();
					pClientInfo->clientLogon = false;
					errCode = ERROR_CANNOT_SEND;
					break;
				}
			}
		}
		else if (pClientInfo->clientLogining) {
			// 正登录

			if (pClientInfo->disconnected) {
				// 连接被断开
				socketManager.StopComm();
				pClientInfo->clientLogining = false;
				errCode = ERROR_DISCONNECTED;
				break;
			}

			Sleep(100L);

		} else {
			// 未登录

			if (pClientInfo->invalidClientInfo) {
				// 客户信息无效(不再登录)
				errCode = ERROR_INVALID_CLIENTINFO;
				break;
			}

			if (pClientInfo->serverBusy) {
				// 服务器忙(稍后再试)

				Sleep((1+rand()%10)*1L);
				pClientInfo->serverBusy = false;
			}

			// 开始连接
			bSuccess = socketManager.ConnectTo(g_sServerAddr.c_str(), sServerPort, AF_INET, SOCK_STREAM);
			if (bSuccess && socketManager.WatchComm()) {

				RequestLocker();
				cout<<"["<<pClientInfo->clientId<<"] "<<"Connected"<<endl;
				ReleaseLocker();

				// 设置正在登录标志
				pClientInfo->clientLogining = true;
			} else {
				RequestLocker();
				cout<<"["<<pClientInfo->clientId<<"] "<<"Failed to connect"<<endl;
				ReleaseLocker();

				errCode = ERROR_CANNOT_CONNECT;
				break;
			}
		}
	}

	return errCode;
}

unsigned __stdcall testMultipleClient(void *param) {
	ClientInfo *pClientInfo = static_cast<ClientInfo*>(param);

	unsigned errCode = testSingleClient(pClientInfo);

	_endthreadex( errCode );
	return errCode;
}
