package com.tpsoft.pushnotification.client;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.tpsoft.pushnotification.model.AppParams;
import com.tpsoft.pushnotification.model.LoginParams;
import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.pushnotification.model.NetworkParams;
import com.tpsoft.pushnotification.model.PublicAccount;
import com.tpsoft.pushnotification.service.NotifyPushService;

public class PushNotificationClient {

	public static final String TAG_APILOG = "PushNotification-API";

	private Context context;
	private List<MessageTransceiverListener> listeners = new ArrayList<MessageTransceiverListener>();

	private static boolean clientStarted = false;
	private static boolean clientLogon = false;

	public PushNotificationClient(Context context) {
		this.context = context;

		// ע��㲥������
		MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.tpsoft.pushnotification.NotifyPushService");
		context.registerReceiver(myBroadcastReceiver, filter);
	}

	/**
	 * ���������
	 * 
	 * @param listener
	 *            ������
	 */
	public void addListener(MessageTransceiverListener listener) {
		listeners.add(listener);
	}

	/**
	 * ɾ��������
	 * 
	 * @param listener
	 *            ������
	 */
	public void removeListener(MessageTransceiverListener listener) {
		listeners.remove(listener);
	}

	/**
	 * ������Ϣ�շ���
	 * 
	 * @param appParams
	 *            Ӧ�ò���
	 * @param loginParams
	 *            ��¼����
	 * @param networkParams
	 *            �������
	 */
	public void startMessageTransceiver(AppParams appParams,
			LoginParams loginParams, NetworkParams networkParams) {
		if (clientStarted) {
			for (MessageTransceiverListener listener : listeners)
				listener.onTransceiverStatus(true);
			return; // �Ѿ�����
		}

		Intent serviceIntent = new Intent();
		serviceIntent
				.setAction("com.tpsoft.pushnotification.ServiceController");
		serviceIntent.putExtra("command", "start");
		serviceIntent.putExtra("com.tpsoft.pushnotification.AppParams",
				appParams.getBundle());
		serviceIntent.putExtra("com.tpsoft.pushnotification.LoginParams",
				loginParams.getBundle());
		serviceIntent.putExtra("com.tpsoft.pushnotification.NetworkParams",
				networkParams.getBundle());
		context.sendBroadcast(serviceIntent); // ���͹㲥
	}

	/**
	 * ֹͣ��Ϣ�շ���
	 */
	public void stopMessageTransceiver() {
		if (!clientStarted) {
			for (MessageTransceiverListener listener : listeners)
				listener.onTransceiverStatus(false);
			return; // �Ѿ�ֹͣ
		}

		Intent serviceIntent = new Intent();
		serviceIntent
				.setAction("com.tpsoft.pushnotification.ServiceController");
		serviceIntent.putExtra("command", "stop");
		context.sendBroadcast(serviceIntent); // ���͹㲥
	}

	/**
	 * ������Ϣ
	 * 
	 * @param msgId
	 *            ��ϢID
	 * @param msg
	 *            ��Ϣ
	 */
	public void sendMessage(int msgId, MyMessage msg) {
		if (!clientLogon)
			return;

		Intent i = new Intent();
		i.setAction("com.tpsoft.pushnotification.ServiceController");
		i.putExtra("command", "send");
		i.putExtra("msgId", msgId);
		// i.putExtra("secure", false);
		i.putExtra("com.tpsoft.pushnotification.MyMessage", msg.getBundle());
		context.sendBroadcast(i);
	}

	/**
	 * ��ѯ���ں�
	 * 
	 * @param queryId
	 *            ��ѯ��ʶ[�ش���]
	 * @param condition
	 *            ��ѯ����:���ں�(֧��ģ��ƥ��)
	 */
	public void queryPublic(int queryId, String condition) {
		if (!clientLogon)
			return;

		Intent i = new Intent();
		i.setAction("com.tpsoft.pushnotification.ServiceController");
		i.putExtra("command", "query_public");
		i.putExtra("queryId", queryId);
		i.putExtra("condition", condition);
		context.sendBroadcast(i);
	}

	/**
	 * ��ע���ں�
	 * 
	 * @param account
	 *            ���ں�
	 */
	public void followPublic(String account) {
		if (!clientLogon)
			return;

		Intent i = new Intent();
		i.setAction("com.tpsoft.pushnotification.ServiceController");
		i.putExtra("command", "follow_public");
		i.putExtra("account", account);
		context.sendBroadcast(i);
	}

	/**
	 * ȡ����ע���ں�
	 * 
	 * @param account
	 *            ���ں�
	 */
	public void unfollowPublic(String account) {
		if (!clientLogon)
			return;

		Intent i = new Intent();
		i.setAction("com.tpsoft.pushnotification.ServiceController");
		i.putExtra("command", "unfollow_public");
		i.putExtra("account", account);
		context.sendBroadcast(i);
	}

	/**
	 * ��ȡ�ѹ�ע���ں�
	 */
	public void getFollowed() {
		if (!clientLogon)
			return;

		Intent i = new Intent();
		i.setAction("com.tpsoft.pushnotification.ServiceController");
		i.putExtra("command", "get_followed");
		context.sendBroadcast(i);
	}

	private class MyBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					"com.tpsoft.pushnotification.NotifyPushService")) {
				// ��Ҫ��ע...
				String action = intent.getStringExtra("action");
				if (action.equals("notify")) {
					// ����Ϣ��֪ͨ
					String type = intent.getStringExtra("type");
					if (NotifyPushService.NOTIFICATION_MESSAGE.equals(type)) {
						// ����Ϣ
						String msgText = intent.getStringExtra("content");
						MyMessage msg;
						try {
							msg = MyMessage.extractMessage(msgText);
						} catch (Exception e) {
							Log.w(TAG_APILOG, msgText);
							return;
						}
						for (MessageTransceiverListener listener : listeners)
							listener.onNewMessageReceived(msg);
					} else if (NotifyPushService.NOTIFICATION_PUBLIC_ACCOUNTS
							.equals(type)) {
						// ���ں���Ϣ�б�֪ͨ
						String accountsText = intent.getStringExtra("content");
						PublicAccount[] accounts;
						try {
							accounts = PublicAccount
									.extractPublicAccounts(accountsText);
						} catch (Exception e) {
							Log.w(TAG_APILOG, accountsText);
							return;
						}
						for (MessageTransceiverListener listener : listeners)
							listener.onPublicAccountsReceived(accounts);
					} else if (NotifyPushService.NOTIFICATION_FOLLOWED_ACCOUNTS
							.equals(type)) {
						// �ѹ�ע���ں���Ϣ�б�֪ͨ
						String accountsText = intent.getStringExtra("content");
						PublicAccount[] accounts;
						try {
							accounts = PublicAccount
									.extractPublicAccounts(accountsText);
						} catch (Exception e) {
							Log.w(TAG_APILOG, accountsText);
							return;
						}
						for (MessageTransceiverListener listener : listeners)
							listener.onFollowedAccountsReceived(accounts);
					}
				} else if (action.equals("log")) {
					// ��־֪ͨ
					int type = intent.getIntExtra("type", 0);
					int code = intent.getIntExtra("code", 0);
					String params = intent.getStringExtra("params");
					switch (type) {
					case NotifyPushService.LOG_CONNECT: // ����:
						switch (code) {
						case NotifyPushService.STATUS_CONNECT_CONNECTING: // ���ӷ�����...
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(1, "���ӷ����� "
										+ params + "...");
							break;
						case NotifyPushService.STATUS_CONNECT_CONNECTED: // �Ѿ����ӵ�������
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(2, "�����ӵ���������");
							break;
						case NotifyPushService.STATUS_CONNECT_APP_CERTIFICATING: // Ӧ����֤...
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(3,
										"У��Ӧ��ID�ͽ�������...");
							break;
						case NotifyPushService.STATUS_CONNECT_APP_CERTIFICATED: // Ӧ����֤ͨ��
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(4, "Ӧ����֤ͨ����");
							break;
						case NotifyPushService.STATUS_CONNECT_USER_CERTIFICATING: // �û���֤...
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(5, "У���û���������...");
							break;
						case NotifyPushService.STATUS_CONNECT_USER_CERTIFICATED: // �û���֤ͨ��
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(6, "�û���֤ͨ����");
							break;
						case NotifyPushService.STATUS_CONNECT_MSGKEY_RECEIVED: // �յ���Ϣ��Կ
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(7, "�յ���Ϣ��Կ��");
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVEINTERVAL_RECEIVED: // �յ���������
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(8, "�յ���������: "
										+ Integer.parseInt(params) / 1000
										+ "�롣");
							break;
						case NotifyPushService.STATUS_CONNECT_LOGON: // ��¼�ɹ�
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(0, "��¼�ɹ���");
							clientLogon = true;
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVE: // ���������ź�
							Log.d(TAG_APILOG, "���������ź�...");
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVE_REPLIED: // �յ������ظ��ź�
							Log.d(TAG_APILOG, "�յ������ظ��ź�");
							break;
						case NotifyPushService.ERROR_CONNECT_NETWORK_UNAVAILABLE: // ���粻����
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(-1, "���粻���ã�");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_BROKEN: // �������ж�
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(-2, "�������жϣ�");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_SERVER_UNAVAILABLE: // ������������
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(-3, "�����������ã�");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_LOGIN_TIMEOUT: // ��¼��ʱ
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(-4, "��¼��ʱ��");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_IO_FAULT: // ����IO����
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(-5, "����IO���ϣ�");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_APP_CERTIFICATE: // Ӧ����֤ʧ��
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(-6, "Ӧ����֤ʧ�ܣ�");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_USER_CERTIFICATE: // �û���֤ʧ��
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(-7, "�û���֤ʧ�ܣ�");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_SERVER: // ����������
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(-8, "����������");
							clientLogon = false;
							break;
						default:
							break;
						}
						break;
					case NotifyPushService.LOG_SENDMSG: // ������Ϣ:
						switch (code) {
						case NotifyPushService.STATUS_SENDMSG_SUBMIT: // �ύ��Ϣ
							for (MessageTransceiverListener listener : listeners)
								listener.onMessageSendStatus(
										Integer.parseInt(params), 1, "�ύ...");
							break;
						case NotifyPushService.STATUS_SENDMSG_SUBMITTED: // ���ύ��Ϣ
							for (MessageTransceiverListener listener : listeners)
								listener.onMessageSendStatus(
										Integer.parseInt(params), 2, "���ύ");
							break;
						case NotifyPushService.STATUS_SENDMSG_OK: // �յ���Ϣȷ��
							for (MessageTransceiverListener listener : listeners)
								listener.onMessageSendStatus(
										Integer.parseInt(params), 3, "��ȷ��");
							break;
						case NotifyPushService.ERROR_SENDMSG_NOT_LOGON: // ��δ��¼�ɹ�
							for (MessageTransceiverListener listener : listeners)
								listener.onMessageSendStatus(
										Integer.parseInt(params), -1, "δ��¼");
							break;
						case NotifyPushService.ERROR_SENDMSG_DATA: // ��Ϣ���ݴ���
							for (MessageTransceiverListener listener : listeners)
								listener.onMessageSendStatus(
										Integer.parseInt(params), -2, "���ݴ���");
							break;
						case NotifyPushService.ERROR_SENDMSG_SUBMIT: // �ύ��Ϣʧ��
							for (MessageTransceiverListener listener : listeners)
								listener.onMessageSendStatus(
										Integer.parseInt(params), -3, "�ύʧ�ܣ�");
							break;
						case NotifyPushService.ERROR_SENDMSG_FAILED: // ������Ϣʧ��
							int pos = params.indexOf(":");
							int msgId = Integer.parseInt(params.substring(0,
									pos));
							String err = params.substring(pos + 1);
							String errcode = err.substring(0, err.indexOf(","));
							String errmsg = err.substring(err.indexOf(",") + 1);
							for (MessageTransceiverListener listener : listeners)
								listener.onMessageSendStatus(msgId, -4,
										"����ʧ��(#" + errcode + ":" + errmsg
												+ ")��");
							break;
						default:
							break;
						}
						break;
					case NotifyPushService.LOG_QUERYPUBLIC: // ��ѯ���ں�:
						switch (code) {
						case NotifyPushService.STATUS_QUERYPUBLIC_SUBMIT: // �ύ����
							break;
						case NotifyPushService.STATUS_QUERYPUBLIC_SUBMITTED: // �������ύ
							break;
						case NotifyPushService.STATUS_QUERYPUBLIC_OK: // �����ɹ�
							break;
						case NotifyPushService.ERROR_QUERYPUBLIC_NOT_LOGON: // ��δ��¼�ɹ�
							break;
						case NotifyPushService.ERROR_QUERYPUBLIC_SUBMIT: // �ύ����ʧ��
							break;
						case NotifyPushService.ERROR_QUERYPUBLIC_FAILED: // ����ʧ��
							break;
						default:
							break;
						}
						break;
					case NotifyPushService.LOG_FOLLOWPUBLIC: // ��ע���ں�:
						switch (code) {
						case NotifyPushService.STATUS_FOLLOWPUBLIC_SUBMIT: // �ύ����
							break;
						case NotifyPushService.STATUS_FOLLOWPUBLIC_SUBMITTED: // �������ύ
							break;
						case NotifyPushService.STATUS_FOLLOWPUBLIC_OK: // �����ɹ�
							for (MessageTransceiverListener listener : listeners)
								listener.onPublicAccountFollowed(params);
							break;
						case NotifyPushService.ERROR_FOLLOWPUBLIC_NOT_LOGON: // ��δ��¼�ɹ�
							break;
						case NotifyPushService.ERROR_FOLLOWPUBLIC_SUBMIT: // �ύ����ʧ��
							break;
						case NotifyPushService.ERROR_FOLLOWPUBLIC_FAILED: // ����ʧ��
							break;
						default:
							break;
						}
						break;
					case NotifyPushService.LOG_UNFOLLOWPUBLIC: // ȡ����ע���ں�:
						switch (code) {
						case NotifyPushService.STATUS_UNFOLLOWPUBLIC_SUBMIT: // �ύ����
							break;
						case NotifyPushService.STATUS_UNFOLLOWPUBLIC_SUBMITTED: // �������ύ
							break;
						case NotifyPushService.STATUS_UNFOLLOWPUBLIC_OK: // �����ɹ�
							for (MessageTransceiverListener listener : listeners)
								listener.onPublicAccountUnfollowed(params);
							break;
						case NotifyPushService.ERROR_UNFOLLOWPUBLIC_NOT_LOGON: // ��δ��¼�ɹ�
							break;
						case NotifyPushService.ERROR_UNFOLLOWPUBLIC_SUBMIT: // �ύ����ʧ��
							break;
						case NotifyPushService.ERROR_UNFOLLOWPUBLIC_FAILED: // ����ʧ��
							break;
						default:
							break;
						}
						break;
					case NotifyPushService.LOG_GETFOLLOWED: // ��ȡ�ѹ�ע�Ĺ��ں�:
						switch (code) {
						case NotifyPushService.STATUS_GETFOLLOWED_SUBMIT: // �ύ����
							break;
						case NotifyPushService.STATUS_GETFOLLOWED_SUBMITTED: // �������ύ
							break;
						case NotifyPushService.STATUS_GETFOLLOWED_OK: // �����ɹ�
							break;
						case NotifyPushService.ERROR_GETFOLLOWED_NOT_LOGON: // ��δ��¼�ɹ�
							break;
						case NotifyPushService.ERROR_GETFOLLOWED_SUBMIT: // �ύ����ʧ��
							break;
						case NotifyPushService.ERROR_GETFOLLOWED_FAILED: // ����ʧ��
							break;
						default:
							break;
						}
						break;
					default:
						break;
					}
				} else if (action.equals("status")) {
					// ��Ϣ������״̬֪ͨ
					clientStarted = intent.getBooleanExtra("started", false);
					if (!clientStarted)
						clientLogon = false;
					for (MessageTransceiverListener listener : listeners)
						listener.onTransceiverStatus(clientStarted);
				} else if (action.equals("logining")) {
					// ��¼״̬֪ͨ
					boolean logining = intent
							.getBooleanExtra("logining", false);
					for (MessageTransceiverListener listener : listeners)
						listener.onLogining(logining);
				} else {
					// δ֪����
				}
			}
		}
	}
}
