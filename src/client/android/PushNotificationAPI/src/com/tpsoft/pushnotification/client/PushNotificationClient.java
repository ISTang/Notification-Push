package com.tpsoft.pushnotification.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.tpsoft.pushnotification.model.AppParams;
import com.tpsoft.pushnotification.model.LoginParams;
import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.pushnotification.model.NetworkParams;
import com.tpsoft.pushnotification.service.NotifyPushService;

public class PushNotificationClient {

	public static final String TAG_APILOG = "PushNotification-API";

	private Context context;
	private MessageTransceiverListener listener;

	private boolean clientStarted = false;
	private boolean clientLogon = false;

	public PushNotificationClient(Context context,
			MessageTransceiverListener listener) {
		this.context = context;
		this.listener = listener;

		// ע��㲥������
		MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.tpsoft.pushnotification.NotifyPushService");
		context.registerReceiver(myBroadcastReceiver, filter);
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

	private class MyBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					"com.tpsoft.pushnotification.NotifyPushService")) {
				// ��Ҫ��ע...
				String action = intent.getStringExtra("action");
				if (action.equals("notify")) {
					// ����Ϣ֪ͨ
					String msgText = intent.getStringExtra("msgText");
					MyMessage msg;
					try {
						msg = MyMessage.extractMessage(msgText);
					} catch (Exception e) {
						Log.w(TAG_APILOG, msgText);
						return;
					}
					listener.onMessageReceived(msg);
				} else if (action.equals("log")) {
					// ��־֪ͨ
					int type = intent.getIntExtra("type", 0);
					int code = intent.getIntExtra("code", 0);
					String params = intent.getStringExtra("params");
					switch (type) {
					case NotifyPushService.LOG_CONNECT: // ����:
						switch (code) {
						case NotifyPushService.STATUS_CONNECT_CONNECTING: // ���ӷ�����...
							listener.onLoginStatus(true, 1, "���ӷ����� " + params
									+ "...");
							break;
						case NotifyPushService.STATUS_CONNECT_CONNECTED: // �Ѿ����ӵ�������
							listener.onLoginStatus(true, 2, "�����ӵ���������");
							break;
						case NotifyPushService.STATUS_CONNECT_APP_CERTIFICATING: // Ӧ����֤...
							listener.onLoginStatus(true, 3, "У��Ӧ��ID�ͽ�������...");
							break;
						case NotifyPushService.STATUS_CONNECT_APP_CERTIFICATED: // Ӧ����֤ͨ��
							listener.onLoginStatus(true, 4, "Ӧ����֤ͨ����");
							break;
						case NotifyPushService.STATUS_CONNECT_USER_CERTIFICATING: // �û���֤...
							listener.onLoginStatus(true, 5, "У���û���������...");
							break;
						case NotifyPushService.STATUS_CONNECT_USER_CERTIFICATED: // �û���֤ͨ��
							listener.onLoginStatus(true, 6, "�û���֤ͨ����");
							break;
						case NotifyPushService.STATUS_CONNECT_MSGKEY_RECEIVED: // �յ���Ϣ��Կ
							listener.onLoginStatus(true, 7, "�յ���Ϣ��Կ��");
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVEINTERVAL_RECEIVED: // �յ���������
							listener.onLoginStatus(true, 8, "�յ���������: "
									+ Integer.parseInt(params) / 1000 + "�롣");
							break;
						case NotifyPushService.STATUS_CONNECT_LOGON: // ��¼�ɹ�
							listener.onLoginStatus(false, 0, "��¼�ɹ���");
							clientLogon = true;
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVE: // ���������ź�
							Log.d(TAG_APILOG, "���������ź�...");
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVE_REPLIED: // �յ������ظ��ź�
							Log.d(TAG_APILOG, "�յ������ظ��ź�");
							break;
						case NotifyPushService.ERROR_CONNECT_NETWORK_UNAVAILABLE: // ���粻����
							listener.onLoginStatus(false, -1, "���粻���ã�");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_BROKEN: // �������ж�
							listener.onLoginStatus(false, -2, "�������жϣ�");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_SERVER_UNAVAILABLE: // ������������
							listener.onLoginStatus(false, -3, "�����������ã�");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_LOGIN_TIMEOUT: // ��¼��ʱ
							listener.onLoginStatus(false, -4, "��¼��ʱ��");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_IO_FAULT: // ����IO����
							listener.onLoginStatus(false, -5, "����IO���ϣ�");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_APP_CERTIFICATE: // Ӧ����֤ʧ��
							listener.onLoginStatus(false, -6, "Ӧ����֤ʧ�ܣ�");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_USER_CERTIFICATE: // �û���֤ʧ��
							listener.onLoginStatus(false, -7, "�û���֤ʧ�ܣ�");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_SERVER: // ����������
							listener.onLoginStatus(false, -8, "����������");
							clientLogon = false;
							break;
						default:
							break;
						}
						break;
					case NotifyPushService.LOG_SENDMSG: // ������Ϣ:
						switch (code) {
						case NotifyPushService.STATUS_SENDMSG_SUBMIT: // �ύ��Ϣ
							listener.onMessageStatus(Integer.parseInt(params),
									1, "�ύ...");
							break;
						case NotifyPushService.STATUS_SENDMSG_SUBMITTED: // ���ύ��Ϣ
							listener.onMessageStatus(Integer.parseInt(params),
									2, "���ύ");
							break;
						case NotifyPushService.STATUS_SENDMSG_OK: // �յ���Ϣȷ��
							listener.onMessageStatus(Integer.parseInt(params),
									3, "��ȷ��");
							break;
						case NotifyPushService.ERROR_SENDMSG_NOT_LOGON: // ��δ��¼�ɹ�
							listener.onMessageStatus(Integer.parseInt(params),
									-1, "δ��¼");
							break;
						case NotifyPushService.ERROR_SENDMSG_DATA: // ��Ϣ���ݴ���
							listener.onMessageStatus(Integer.parseInt(params),
									-2, "���ݴ���");
							break;
						case NotifyPushService.ERROR_SENDMSG_SUBMIT: // �ύ��Ϣʧ��
							listener.onMessageStatus(Integer.parseInt(params),
									-3, "�ύʧ�ܣ�");
							break;
						case NotifyPushService.ERROR_SENDMSG_FAILED: // ������Ϣʧ��
							int pos = params.indexOf(":");
							int msgId = Integer.parseInt(params.substring(0,
									pos));
							String err = params.substring(pos + 1);
							String errcode = err.substring(0, err.indexOf(","));
							String errmsg = err.substring(err.indexOf(",") + 1);
							listener.onMessageStatus(msgId, -4, "����ʧ��(#"
									+ errcode + ":" + errmsg + ")��");
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
					listener.onTransceiverStatus(clientStarted);
				} else if (action.equals("logining")) {
					// ��¼״̬֪ͨ
					boolean logining = intent
							.getBooleanExtra("logining", false);
					listener.onLoginStatus(logining, 0, logining ? "���ڵ�¼..."
							: "��¼������");
				} else {
					// δ֪����
				}
			}
		}
	}
}
