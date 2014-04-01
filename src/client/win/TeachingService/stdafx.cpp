// stdafx.cpp : source file that includes just the standard includes
// CLS.pch will be the pre-compiled header
// stdafx.obj will contain the pre-compiled type information

#include "stdafx.h"
#include <vector>

// TODO: reference any additional headers you need in STDAFX.H
// and not in this file
// Output to the debugger window
void DebugMsg(const TCHAR * pwszFormat,...)
{
	va_list arglist;
	va_start(arglist,pwszFormat);
	int nBufSize = _vsctprintf(pwszFormat,arglist);
	if (nBufSize++)
	{
		std::vector<TCHAR> Buffer(nBufSize,0);
		TCHAR * pBuffer = &Buffer[0];
		if (pBuffer)
		{
			_vstprintf_s(pBuffer,nBufSize,pwszFormat,arglist);
			va_end(arglist);
			OutputDebugString(pBuffer);
		}
	}
	else
		OutputDebugString(pwszFormat);
}

void DebugLastError()
{
	LPVOID lpMsgBuf;
	DWORD dwLastError = GetLastError(); 
	FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,NULL,dwLastError,MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),(LPTSTR)&lpMsgBuf,0,NULL);
	DebugMsg(L"%s\n",lpMsgBuf);
	LocalFree(lpMsgBuf);
}
