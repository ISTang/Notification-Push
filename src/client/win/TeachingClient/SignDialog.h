#pragma once

// CSignDialog 对话框

class CSignDialog : public CDialogEx
{
	DECLARE_DYNAMIC(CSignDialog)

public:
	CSignDialog(CWnd* pParent = NULL);   // 标准构造函数
	virtual ~CSignDialog();

// 对话框数据
	enum { IDD = IDD_SIGNIN_DLG };

protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV 支持

	DECLARE_MESSAGE_MAP()
public:
	CString m_strUserId;
	CString m_strRealName;
	CString m_strUserPassword;
	afx_msg void OnBnClickedOk();
};
