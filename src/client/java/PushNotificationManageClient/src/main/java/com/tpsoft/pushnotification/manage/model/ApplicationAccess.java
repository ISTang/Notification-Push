package com.tpsoft.pushnotification.manage.model;

/**
 * Ӧ�ý�����Ϣ
 * 
 * @author joebin.don@gmail.com
 * @since 2013-06-09
 */
public class ApplicationAccess {
	/**
	 * �������
	 */
	public static class AccessParams {
		private boolean needLogin = false; // ��Ҫ��¼: �ͻ����Ƿ�����ȵ�¼(true/false[Ĭ��])
		private boolean needLoginPassword = false; // ��Ҫ��¼����:
													// �ͻ��˵�¼ʱ�Ƿ�����ṩ����(true/false[Ĭ��])
		private boolean autoCreateAccount = true; // �Զ��½��ʺ�:
													// ���ʺŲ����ڣ�ϵͳ�Ƿ��Զ��½�(true[Ĭ��]/false)
		private boolean protectLogin = false; // ��¼����:
		// ��¼�����д������Ϣ�Ƿ���Ҫ����(true/false[Ĭ��])--���ʺ���Ҫ��¼�����
		private boolean encryptMessage = false; // ��Ϣ����:
												// �Ƿ���Ҫ����Ϣ���м���(true/false[Ĭ��])

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
	 * �ͻ��˲���
	 */
	public static class ClientParams {
		private String id; // Ӧ��ID: ϵͳ���ɵ�GUID
		private String password; // Ӧ������: ϵͳ���ɵ�8λ�������(ԭ��)
		private String protectKey; // ��¼������Կ: ϵͳ���ɵ�8λ����ַ�(��ʹ�޵�¼����Ҳ���ṩ)

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

	private String name; // Ӧ������: 1-40���ַ�
	private AccessParams accessParams; // �������
	private ClientParams clientParams; // �ͻ��˲���

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
