package com.tpsoft.pushnotification.manage.model;

/**
 * Ӧ�ý�����Ϣ
 * 
 * @author joebin.don@gmail.com
 * @since 2013-06-09
 */
public class Application {
	/**
	 * �������
	 */
	public static class AccessParams {
		private boolean need_login = false; // ��Ҫ��¼: �ͻ����Ƿ�����ȵ�¼(true/false[Ĭ��])
		private boolean need_login_password = false; // ��Ҫ��¼����:
														// �ͻ��˵�¼ʱ�Ƿ�����ṩ����(true/false[Ĭ��])
		private boolean auto_create_account = true; // �Զ��½��ʺ�:
													// ���ʺŲ����ڣ�ϵͳ�Ƿ��Զ��½�(true[Ĭ��]/false)
		private boolean protect_login = false; // ��¼����:
		// ��¼�����д������Ϣ�Ƿ���Ҫ����(true/false[Ĭ��])--���ʺ���Ҫ��¼�����
		private boolean encrypt_message = false; // ��Ϣ����:
													// �Ƿ���Ҫ����Ϣ���м���(true/false[Ĭ��])

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
	 * �ͻ��˲���
	 */
	public static class ClientParams {
		private String id; // Ӧ��ID: ϵͳ���ɵ�GUID
		private String password; // Ӧ������: ϵͳ���ɵ�8λ�������(ԭ��)
		private String public_key; // ��¼������Կ: ϵͳ���ɵ�8λ����ַ�(��ʹ�޵�¼����Ҳ���ṩ)
		private String private_key; // ��¼������Կ: ϵͳ���ɵ�8λ����ַ�(��ʹ�޵�¼����Ҳ���ṩ)

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

	private String name; // Ӧ������: 1-40���ַ�
	private AccessParams accessParams; // �������
	private ClientParams clientParams; // �ͻ��˲���

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
