package com.tpsoft.pushnotification.manage.config;

public class ServiceConfig {

	public static final String CONTENT_TYPE = "application/json;charset=utf-8";

	private String server = "218.200.212.120"; // 服务器地址
	private int port = 4567; // 服务端口

	public ServiceConfig() {

	}

	public ServiceConfig(String server, int port) {
		this.server = server;
		this.port = port;
	}

	public String getRegisterApplicationEndpoint() {
		return makeEndpointBase() + "/application/{name}";
	}

	public String getUpdateApplicationEndpoint() {
		return makeEndpointBase() + "/application/{id}";
	}

	public String getUnregisterApplicationEndpoint() {
		return makeEndpointBase() + "/application/{id}";
	}

	public String getListAllApplicationsEndpoint() {
		return makeEndpointBase() + "/applications";
	}

	public String getGetApplicationEndpoint() {
		return makeEndpointBase() + "/application/{id}";
	}

	public String getCheckApplicationNameEndpoint() {
		return makeEndpointBase() + "/application/name/{name}";
	}

	public String getCreateAccountEndpoint() {
		return makeEndpointBase() + "/account/{name}";
	}

	public String getUpdateAccountEndpoint() {
		return makeEndpointBase() + "/account/{id}";
	}

	public String getDisableAccountEndpoint() {
		return makeEndpointBase() + "/account/{id}/disable";
	}

	public String getEnableAccountEndpoint() {
		return makeEndpointBase() + "/account/{id}/enable";
	}

	public String getDeleteAccountEndpoint() {
		return makeEndpointBase() + "/account/{id}";
	}

	public String getCountAccountsEndpoint() {
		return makeEndpointBase() + "/accounts/count";
	}

	public String getListAccountsEndpoint() {
		return makeEndpointBase() + "/accounts";
	}

	public String getGetAccountEndpoint() {
		return makeEndpointBase() + "/account/{id}";
	}

	public String getCheckAccountNameEndpoint() {
		return makeEndpointBase() + "/account/name/{name}";
	}

	public String getCheckAccountPhoneEndpoint() {
		return makeEndpointBase() + "/account/phone/{phone}";
	}

	public String geCheckAccountEmailEndpoint() {
		return makeEndpointBase() + "/account/email/{email}";
	}

	public String getBroadcastMessageEndpoint() {
		return makeEndpointBase() + "/application/{id}/message";
	}

	public String getMulticastMessageEndpoint() {
		return makeEndpointBase() + "/application/{id}/accounts/message";
	}

	public String getSendMessageEndpoint() {
		return makeEndpointBase() + "/application/{id}/account/{name}/message";
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	private String makeEndpointBase() {
		return String.format("http://%s:%d", server, port);
	}
}
