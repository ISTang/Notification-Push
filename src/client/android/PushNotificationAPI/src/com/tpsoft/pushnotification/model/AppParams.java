package com.tpsoft.pushnotification.model;

import android.os.Bundle;

public class AppParams {

	private String appId;
	private String appPassword;
	private String loginProtectKey;

	public AppParams(String appId, String appPassword, String loginProtectKey) {
		this.appId = appId;
		this.appPassword = appPassword;
		this.loginProtectKey = loginProtectKey;
	}

	public AppParams(Bundle bundle) {
		appId = bundle.getString("appId");
		appPassword = bundle.getString("appPassword");
		loginProtectKey = bundle.getString("loginProtectKey");
	}

	public Bundle getBundle() {
		Bundle bundle = new Bundle();
		bundle.putString("appId", appId);
		bundle.putString("appPassword", appPassword);
		bundle.putString("loginProtectKey", loginProtectKey);
		return bundle;
	}

	public AppParams() {
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getAppPassword() {
		return appPassword;
	}

	public void setAppPassword(String appPassword) {
		this.appPassword = appPassword;
	}

	public String getLoginProtectKey() {
		return loginProtectKey;
	}

	public void setLoginProtectKey(String loginProtectKey) {
		this.loginProtectKey = loginProtectKey;
	}

}
