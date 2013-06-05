package com.tpsoft.pushnotification.model;

import android.os.Bundle;

public class NetworkParams {
	
	public static final int CONNECT_TIMEOUT = 1000 * 3; // 连接超时(ms)
	public static final int RECONNECT_DELAY = 1000 * 3; // 重试连接延迟(ms);
	public static final int LOGIN_TIMEOUT = 1000 * 60; // 登录超时(ms)
	public static final int READ_TIMEOUT = (int) (1000 * 0.1); // 读超时(ms)

	private int connectTimeout = CONNECT_TIMEOUT;
	private int reconnectDelay = RECONNECT_DELAY;
	private int loginTimeout = LOGIN_TIMEOUT;
	private int readTimeout = READ_TIMEOUT;

	public NetworkParams() {
	}

	public NetworkParams(int connectTimeout, int reconnectDelay,
			int loginTimeout, int readTimeout) {
		this.connectTimeout = connectTimeout;
		this.reconnectDelay = reconnectDelay;
		this.loginTimeout = loginTimeout;
		this.readTimeout = readTimeout;
	}

	public NetworkParams(Bundle bundle) {
		connectTimeout = bundle.getInt("connectTimeout");
		reconnectDelay = bundle.getInt("reconnectDelay");
		loginTimeout = bundle.getInt("loginTimeout");
		readTimeout = bundle.getInt("readTimeout");
	}

	public Bundle getBundle() {
		Bundle bundle = new Bundle();
		bundle.putInt("connectTimeout", connectTimeout);
		bundle.putInt("reconnectDelay", reconnectDelay);
		bundle.putInt("loginTimeout", loginTimeout);
		bundle.putInt("readTimeout", readTimeout);
		return bundle;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getReconnectDelay() {
		return reconnectDelay;
	}

	public void setReconnectDelay(int reconnectDelay) {
		this.reconnectDelay = reconnectDelay;
	}

	public int getLoginTimeout() {
		return loginTimeout;
	}

	public void setLoginTimeout(int loginTimeout) {
		this.loginTimeout = loginTimeout;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

}
