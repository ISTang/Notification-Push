package com.tpsoft.pushnotification.manage.model;

/**
 * 应用接入信息
 * 
 * @author joebin.don@gmail.com
 * @since 2013-06-09
 */
public class ApplicationAccess {
	/**
	 * 接入参数
	 */
	public static class AccessParams {
		private boolean needLogin = false; // 需要登录: 客户端是否必须先登录(true/false[默认])
		private boolean needLoginPassword = false; // 需要登录密码:
													// 客户端登录时是否必须提供密码(true/false[默认])
		private boolean autoCreateAccount = true; // 自动新建帐号:
													// 若帐号不存在，系统是否自动新建(true[默认]/false)
		private boolean protectLogin = false; // 登录保护:
		// 登录过程中传输的信息是否需要加密(true/false[默认])--仅适合需要登录的情况
		private boolean encryptMessage = false; // 消息加密:
												// 是否需要对消息进行加密(true/false[默认])

		public AccessParams() {
		}

		public AccessParams(String name, boolean needLogin,
				boolean needLoginPassword, boolean autoCreateAccount,
				boolean protectLogin, boolean encryptMessage) {
			this.needLogin = needLogin;
			this.needLoginPassword = needLoginPassword;
			this.autoCreateAccount = autoCreateAccount;
			this.protectLogin = protectLogin;
			this.encryptMessage = encryptMessage;
		}

		public boolean isNeedLogin() {
			return needLogin;
		}

		public void setNeedLogin(boolean needLogin) {
			this.needLogin = needLogin;
		}

		public boolean isNeedLoginPassword() {
			return needLoginPassword;
		}

		public void setNeedLoginPassword(boolean needLoginPassword) {
			this.needLoginPassword = needLoginPassword;
		}

		public boolean isAutoCreateAccount() {
			return autoCreateAccount;
		}

		public void setAutoCreateAccount(boolean autoCreateAccount) {
			this.autoCreateAccount = autoCreateAccount;
		}

		public boolean isProtectLogin() {
			return protectLogin;
		}

		public void setProtectLogin(boolean protectLogin) {
			this.protectLogin = protectLogin;
		}

		public boolean isEncryptMessage() {
			return encryptMessage;
		}

		public void setEncryptMessage(boolean encryptMessage) {
			this.encryptMessage = encryptMessage;
		}

	}

	/**
	 * 客户端参数
	 */
	public static class ClientParams {
		private String id; // 应用ID: 系统生成的GUID
		private String password; // 应用密码: 系统生成的8位随机密码(原文)
		private String protectKey; // 登录保护秘钥: 系统生成的8位随机字符(即使无登录保护也会提供)

		public ClientParams() {
		}

		public ClientParams(String id, String password, String protectKey) {
			this.id = id;
			this.password = password;
			this.protectKey = protectKey;
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

		public String getProtectKey() {
			return protectKey;
		}

		public void setProtectKey(String protectKey) {
			this.protectKey = protectKey;
		}
	}

	private String name; // 应用名称: 1-40个字符
	private AccessParams accessParams; // 接入参数
	private ClientParams clientParams; // 客户端参数

	public ApplicationAccess() {
	}

	public ApplicationAccess(String name) {
		this.name = name;
	}

	public ApplicationAccess(String name, AccessParams accessParams,
			ClientParams clientParams) {
		this.name = name;
		this.accessParams = accessParams;
		this.clientParams = clientParams;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public AccessParams getAccessParams() {
		return accessParams;
	}

	public void setAccessParams(AccessParams accessParams) {
		this.accessParams = accessParams;
	}

	public ClientParams getClientParams() {
		return clientParams;
	}

	public void setClientParams(ClientParams clientParams) {
		this.clientParams = clientParams;
	}
}
