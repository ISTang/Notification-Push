
// teachingClient.h : PROJECT_NAME Ӧ�ó������ͷ�ļ�
//

#pragma once

#ifndef __AFXWIN_H__
	#error "�ڰ������ļ�֮ǰ������stdafx.h�������� PCH �ļ�"
#endif

#include "resource.h"		// ������


// CTeachingClientApp:
// �йش����ʵ�֣������ teachingClient.cpp
//

class CTeachingClientApp : public CWinApp
{
public:
	CTeachingClientApp();

// ��д
public:
	virtual BOOL InitInstance();

// ʵ��

	DECLARE_MESSAGE_MAP()
};

extern CTeachingClientApp theApp;