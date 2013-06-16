package com.tpsoft.pushnotification.manage.response;


public class RegisterApplicationResult {
	private boolean success;
	private String id;
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

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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
