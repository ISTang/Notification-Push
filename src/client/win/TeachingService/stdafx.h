// stdafx.h : include file for standard system include files,
// or project specific include files that are used frequently, but
// are changed infrequently
//

#pragma once

#include "targetver.h"

#include <stdio.h>
#include <tchar.h>
#define _ATL_CSTRING_EXPLICIT_CONSTRUCTORS      // some CString constructors will be explicit

#ifndef VC_EXTRALEAN
#define VC_EXTRALEAN            // Exclude rarely-used stuff from Windows headers
#endif

#include <afx.h>
#include <afxwin.h>         // MFC core and standard components
#include <afxext.h>         // MFC extensions
#ifndef _AFX_NO_OLE_SUPPORT
#include <afxdtctl.h>           // MFC support for Internet Explorer 4 Common Controls
#endif
#ifndef _AFX_NO_AFXCMN_SUPPORT
#include <afxcmn.h>                     // MFC support for Windows Common Controls
#endif // _AFX_NO_AFXCMN_SUPPORT

#include <afxsock.h>            // MFC Ì×½Ó×ÖÀ©Õ¹

#include <string>
#include <vector>
#include <list>
#include <map>
#include <iostream>
#include <stdio.h>

void DebugMsg(const TCHAR * pwszFormat,...);
void DebugLastError();

#include <comdef.h>
#include <icrsint.h>
#import "C:\program files\common files\system\ado\msado15.dll" \
	no_namespace rename("EOF", "ADOEOF")
