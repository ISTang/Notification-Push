package com.tpsoft.pushnotification.manage.model;

/**
 * �˺���Ϣ
 * 
 * @author joebin.don@gmail.com
 * @since 2013-06-10
 */
public class Account {

	private String name; // �ʺ�����: �������û�������1-20���ַ���Ψһ
	private String phone; // �绰����: �ֻ����룬���Դ����ţ���ѡ���������û�����Ψһ
	private String email; // �����ַ: ��ѡ���������û�����Ψһ
	private String password = "666666"; // ����: ��ѡ��Ĭ��Ϊ6��6
	private boolean disabled = false; // �Ƿ����: �Ƿ��ֹ�ͻ���ʹ�ø��˺�(true/false[Ĭ��])

	public Account() {
	}

	public Account(String name, String phone, String email, String password) {
		this.name = name;
		this.phone = phone;
		this.email = email;
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

}
