package com.tpsoft.pushnotification.manage.model;

/**
 * 应用接入信息
 * 
 * @author joebin.don@gmail.com
 * @since 2013-06-09
 */
public class Application {
	/**
	 * 接入参数
	 */
	public static class AccessParams {
		private boolean need_login = false; // 需要登录: 客户端是否必须先登录(true/false[默认])
		private boolean need_login_password = false; // 需要登录密码:
														// 客户端登录时是否必须提供密码(true/false[默认])
		private boolean auto_create_account = true; // 自动新建帐号:
													// 若帐号不存在，系统是否自动新建(true[默认]/false)
		private boolean protect_login = false; // 登录保护:
		// 登录过程中传输的信息是否需要加密(true/false[默认])--仅适合需要登录的情况
		private boolean encrypt_message = false; // 消息加密:
													// 是否需要对消息进行加密(true/false[默认])

		public AccessParams() {
		}

		public AccessParams(boolean needLogin, boolean needLoginPassword,
				boolean autoCreateAccount, boolean protectLogin,
				boolean encryptMessage) {
			this.need_login = needLogin;
			this.need_login_password = needLoginPassword;
			this.auto_create_account = autoCreateAccount;
			this.protect_login = protectLogin;
			this.encrypt_message = encryptMessage;
		}

		public boolean isNeedLogin() {
			return need_login;
		}

		public void setNeedLogin(boolean needLogin) {
			this.need_login = needLogin;
		}

		public boolean isNeedLoginPassword() {
			return need_login_password;
		}

		public void setNeedLoginPassword(boolean needLoginPassword) {
			this.need_login_password = needLoginPassword;
		}

		public boolean isAutoCreateAccount() {
			return auto_create_account;
		}

		public void setAutoCreateAccount(boolean autoCreateAccount) {
			this.auto_create_account = autoCreateAccount;
		}

		public boolean isProtectLogin() {
			return protect_login;
		}

		public void setProtectLogin(boolean protectLogin) {
			this.protect_login = protectLogin;
		}

		public boolean isEncryptMessage() {
			return encrypt_message;
		}

		public void setEncryptMessage(boolean encryptMessage) {
			this.encrypt_message = encryptMessage;
		}

	}

	/**
	 * 客户端参数
	 */
	public static class ClientParams {
		private String id; // 应用ID: 系统生成的GUID
		private String password; // 应用密码: 系统生成的8位随机密码(原文)
		private String public_key; // 登录保护秘钥: 系统生成的8位随机字符(即使无登录保护也会提供)
		private String private_key; // 登录保护秘钥: 系统生成的8位随机字符(即使无登录保护也会提供)

		public ClientParams() {
		}

		public ClientParams(String id, String password, String publicKey,
				String privateKey) {
			this.id = id;
			this.password = password;
			this.public_key = publicKey;
			this.private_key = privateKey;
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

		public String getPublicKey() {
			return public_key;
		}

		public void setPublicKey(String publicKey) {
			this.public_key = publicKey;
		}

		public String getPrivateKey() {
			return private_key;
		}

		public void setPrivateKey(String privateKey) {
			this.private_key = privateKey;
		}
	}

	private String name; // 应用名称: 1-40个字符
	private AccessParams accessParams; // 接入参数
	private ClientParams clientParams; // 客户端参数

	public Application() {
	}

	public Application(String name) {
		this.name = name;
	}

	public Application(String name, AccessParams accessParams,
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
