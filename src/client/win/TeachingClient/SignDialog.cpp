// SignDialog.cpp : ʵ���ļ�
//

#include "stdafx.h"
#include "TeachingClient.h"
#include "SignDialog.h"
#include "afxdialogex.h"
#include "NotificationPushAPI_proto.h"
#include "MyMessage.h"

// CSignDialog �Ի���

IMPLEMENT_DYNAMIC(CSignDialog, CDialogEx)

CSignDialog::CSignDialog(CWnd* pParent /*=NULL*/)
	: CDialogEx(CSignDialog::IDD, pParent)
	, m_strUserId(_T(""))
	, m_strRealName(_T(""))
	, m_strUserPassword(_T(""))
{

}

CSignDialog::~CSignDialog()
{
}

void CSignDialog::DoDataExchange(CDataExchange* pDX)
{
	CDialogEx::DoDataExchange(pDX);
	DDX_Text(pDX, IDC_USERID, m_strUserId);
	DDX_Text(pDX, IDC_REALNAME, m_strRealName);
	DDX_Text(pDX, IDC_USERPASSWORD, m_strUserPassword);
}


BEGIN_MESSAGE_MAP(CSignDialog, CDialogEx)
	ON_BN_CLICKED(IDOK, &CSignDialog::OnBnClickedOk)
END_MESSAGE_MAP()


// CSignDialog ��Ϣ�������


void CSignDialog::OnBnClickedOk()
{
	UpdateData();

	if (m_strUserId.IsEmpty()
		|| m_strRealName.IsEmpty()
		|| m_strUserPassword.IsEmpty() )
	{
		::MessageBox(m_hWnd, _T("�뽫ǩ����Ϣ��д������"), _T("����"), MB_OK|MB_ICONWARNING);
		return;
	}

	CString xmlReq;
	xmlReq.Format(
		_T("<root>")
		_T("  <userid>%s</userid>")
		_T("  <realname>%s</realname>")
		_T("  <userpassword>%s</userpassword>")
		_T("</root>"),
		m_strUserId,
		m_strRealName,
		m_strUserPassword);

	CT2A req(xmlReq, CP_UTF8);
	MyMessage msg;
	msg.setType("xml");
	msg.setTitle("signin");
	msg.setBody(req.m_psz);
	if (SendMessageTo(0, "TeachingService", "signin_request", msg.toJson().c_str()))
	{
		CDialogEx::OnOK();
	}
	else
	{
		::MessageBox(m_hWnd, _T("����ǩ����Ϣʧ�ܣ�"), _T("����"), MB_OK|MB_ICONERROR);
	}
}
