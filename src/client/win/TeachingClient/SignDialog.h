#pragma once

// CSignDialog �Ի���

class CSignDialog : public CDialogEx
{
	DECLARE_DYNAMIC(CSignDialog)

public:
	CSignDialog(CWnd* pParent = NULL);   // ��׼���캯��
	virtual ~CSignDialog();

// �Ի�������
	enum { IDD = IDD_SIGNIN_DLG };

protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV ֧��

	DECLARE_MESSAGE_MAP()
public:
	CString m_strUserId;
	CString m_strRealName;
	CString m_strUserPassword;
	afx_msg void OnBnClickedOk();
};
