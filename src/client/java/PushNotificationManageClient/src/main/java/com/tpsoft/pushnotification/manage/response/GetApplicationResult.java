package com.tpsoft.pushnotification.manage.response;


public class GetApplicationResult {
	private boolean success;
	private String name;
	private boolean need_login = false;
	private boolean need_login_password = false;
	private boolean auto_create_account = true;
	private boolean protect_login = false;
	private boolean encrypt_message = false;
	private String password;
	private String public_key;
	private String private_key;
	private int errcode;
	private String errmsg;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isNeed_login() {
		return need_login;
	}

	public void setNeed_login(boolean needLogin) {
		this.need_login = needLogin;
	}

	public boolean isNeed_login_password() {
		return need_login_password;
	}

	public void setNeed_login_password(boolean needLoginPassword) {
		this.need_login_password = needLoginPassword;
	}

	public boolean isAuto_create_account() {
		return auto_create_account;
	}

	public void setAuto_create_account(boolean autoCreateAccount) {
		this.auto_create_account = autoCreateAccount;
	}

	public boolean isProtect_login() {
		return protect_login;
	}

	public void setProtect_login(boolean protectLogin) {
		this.protect_login = protectLogin;
	}

	public boolean isEncrypt_message() {
		return encrypt_message;
	}

	public void setEncrypt_message(boolean encryptMessage) {
		this.encrypt_message = encryptMessage;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPublic_key() {
		return public_key;
	}

	public void setPublic_key(String publicKey) {
		this.public_key = publicKey;
	}

	public String getPrivate_key() {
		return private_key;
	}

	public void setPrivate_key(String privateKey) {
		this.private_key = privateKey;
	}

	public int getErrcode() {
		return errcode;
	}

	public void setErrcode(int errcode) {
		this.errcode = errcode;
	}

	public String getErrmsg() {
		return errmsg;
	}

	public void setErrmsg(String errmsg) {
		this.errmsg = errmsg;
	}

}
