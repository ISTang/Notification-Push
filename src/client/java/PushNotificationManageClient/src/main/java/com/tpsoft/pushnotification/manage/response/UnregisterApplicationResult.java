package com.tpsoft.pushnotification.manage.response;

import com.tpsoft.pushnotification.manage.model.Application;

public class UnregisterApplicationResult extends Application.ClientParams {
	private boolean success;
	private int errcode;
	private String errmsg;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
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