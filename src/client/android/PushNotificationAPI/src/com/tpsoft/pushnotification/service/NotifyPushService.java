package com.tpsoft.pushnotification.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;

import com.tpsoft.pushnotification.R;
import com.tpsoft.pushnotification.model.AppParams;
import com.tpsoft.pushnotification.model.LoginParams;
import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.pushnotification.model.NetworkParams;
import com.tpsoft.pushnotification.utils.Crypt;

@SuppressLint("DefaultLocale")
public class NotifyPushService extends Service {

	// ��־����
	public static final int LOG_CONNECT = 1; // ����
	public static final int LOG_SENDMSG = 2; // ������Ϣ
	public static final int LOG_QUERYPUBLIC = 3; // ��ѯ���ں�
	public static final int LOG_FOLLOWPUBLIC = 4; // ��ע���ں�
	public static final int LOG_UNFOLLOWPUBLIC = 5; // ȡ����ע���ں�
	public static final int LOG_GETFOLLOWED = 6; // ��ȡ�ѹ�ע�Ĺ��ں�
	// ״̬��
	public static final int STATUS_CONNECT_CONNECTING = 1; // ���ӷ�����...
	public static final int STATUS_CONNECT_CONNECTED = 2; // �Ѿ����ӵ�������
	public static final int STATUS_CONNECT_APP_CERTIFICATING = 3; // Ӧ����֤...
	public static final int STATUS_CONNECT_APP_CERTIFICATED = 4; // Ӧ����֤ͨ��
	public static final int STATUS_CONNECT_USER_CERTIFICATING = 5; // �û���֤...
	public static final int STATUS_CONNECT_USER_CERTIFICATED = 6; // �û���֤ͨ��
	public static final int STATUS_CONNECT_MSGKEY_RECEIVED = 7; // �յ���Ϣ��Կ
	public static final int STATUS_CONNECT_KEEPALIVEINTERVAL_RECEIVED = 8; // �յ���������
	public static final int STATUS_CONNECT_LOGON = 9; // ��¼�ɹ�
	public static final int STATUS_CONNECT_KEEPALIVE = 10; // ���������ź�
	public static final int STATUS_CONNECT_KEEPALIVE_REPLIED = 11; // �յ������ظ��ź�
	//
	public static final int STATUS_SENDMSG_SUBMIT = 51; // �ύ��Ϣ
	public static final int STATUS_SENDMSG_SUBMITTED = 52; // ��Ϣ���ύ
	public static final int STATUS_SENDMSG_OK = 53; // ��Ϣ�ѷ���
	//
	public static final int STATUS_QUERYPUBLIC_SUBMIT = 61; // �ύ���ںŲ�ѯ����
	public static final int STATUS_QUERYPUBLIC_SUBMITTED = 62; // ���ںŲ�ѯ�������ύ
	public static final int STATUS_QUERYPUBLIC_OK = 63; // ���ںŲ�ѯ�ɹ�
	//
	public static final int STATUS_FOLLOWPUBLIC_SUBMIT = 71; // �ύ���ںŹ�ע����
	public static final int STATUS_FOLLOWPUBLIC_SUBMITTED = 72; // ���ںŹ�ע�������ύ
	public static final int STATUS_FOLLOWPUBLIC_OK = 73; // ���ںŹ�ע�ɹ�
	//
	public static final int STATUS_UNFOLLOWPUBLIC_SUBMIT = 81; // �ύ���ں�ȡ����ע����
	public static final int STATUS_UNFOLLOWPUBLIC_SUBMITTED = 82; // ���ں�ȡ����ע�������ύ
	public static final int STATUS_UNFOLLOWPUBLIC_OK = 83; // ���ں�ȡ����ע�ɹ�
	//
	public static final int STATUS_GETFOLLOWED_SUBMIT = 91; // �ύ��ȡ�ѹ�ע���ں�����
	public static final int STATUS_GETFOLLOWED_SUBMITTED = 92; // ��ȡ�ѹ�ע���ں��������ύ
	public static final int STATUS_GETFOLLOWED_OK = 93; // ��ȡ�ѹ�ע���ںųɹ�
	// ������
	public static final int ERROR_CONNECT_NETWORK_UNAVAILABLE = 101; // ���粻����
	public static final int ERROR_CONNECT_BROKEN = 102; // �������ж�
	public static final int ERROR_CONNECT_SERVER_UNAVAILABLE = 103; // ������������
	public static final int ERROR_CONNECT_LOGIN_TIMEOUT = 104; // ��¼��ʱ
	public static final int ERROR_CONNECT_IO_FAULT = 105; // ����IO����
	public static final int ERROR_CONNECT_APP_CERTIFICATE = 106; // Ӧ����֤ʧ��
	public static final int ERROR_CONNECT_USER_CERTIFICATE = 107; // �û���֤ʧ��
	public static final int ERROR_CONNECT_SERVER = 108; // ����������
	//
	public static final int ERROR_SENDMSG_NOT_LOGON = 151; // ��δ��¼�ɹ�
	public static final int ERROR_SENDMSG_DATA = 152; // ��Ϣ���ݳ���
	public static final int ERROR_SENDMSG_SUBMIT = 153; // �ύ��Ϣʧ��
	public static final int ERROR_SENDMSG_FAILED = 154; // ������Ϣʧ��
	//
	public static final int ERROR_QUERYPUBLIC_NOT_LOGON = 161; // ��δ��¼�ɹ�
	public static final int ERROR_QUERYPUBLIC_SUBMIT = 162; // �ύ���ںŲ�ѯ����ʧ��
	public static final int ERROR_QUERYPUBLIC_FAILED = 163; // ���ںŲ�ѯʧ��
	//
	public static final int ERROR_FOLLOWPUBLIC_NOT_LOGON = 171; // ��δ��¼�ɹ�
	public static final int ERROR_FOLLOWPUBLIC_SUBMIT = 172; // �ύ���ںŹ�ע����ʧ��
	public static final int ERROR_FOLLOWPUBLIC_FAILED = 173; // ���ںŹ�עʧ��
	//
	public static final int ERROR_UNFOLLOWPUBLIC_NOT_LOGON = 181; // ��δ��¼�ɹ�
	public static final int ERROR_UNFOLLOWPUBLIC_SUBMIT = 182; // �ύ���ں�ȡ����ע����ʧ��
	public static final int ERROR_UNFOLLOWPUBLIC_FAILED = 183; // ���ں�ȡ����עʧ��
	//
	public static final int ERROR_GETFOLLOWED_NOT_LOGON = 191; // ��δ��¼�ɹ�
	public static final int ERROR_GETFOLLOWED_SUBMIT = 192; // �ύ��ȡ�ѹ�ע���ں�����ʧ��
	public static final int ERROR_GETFOLLOWED_FAILED = 193; // ��ȡ�ѹ�ע���ں�ʧ��

	// �н�����־
	private static final String INPUT_RETURN = "\r\n";

	// ͷ���ֶ�������
	private static final String FIELD_BODY_BYTE_LENGTH = "ByteLength";
	private static final String FIELD_BODY_LENGTH = "Length";
	private static final String FIELD_ACTION_SUCCESS = "Success";
	private static final String FIELD_LOGIN_SECURE = "Secure";
	private static final String FIELD_LOGIN_PASSWORD = "Password";
	private static final String FIELD_MSG_RECEIPT = "Receipt";
	private static final String FIELD_ACTION_ID = "Id";
	private static final String FIELD_ACTION_ACCOUNT = "Account";

	private static final String CLOSE_CONN_RES = "CLOSE CONN\r\nLength: %d\r\n\r\n%s"; // �岿:
																						// ��������(�Ѱ���)
	private static final String GET_APPID_RES = "SET APPID\r\nLength: %d\r\n\r\n%s,%s"; // 0-�岿����,
																						// 1-Ӧ��ID,
																						// 2-Ӧ������
	private static final String GET_USERNAME_RES = "SET USERNAME\r\nSecure: %s\r\nPassword: %s\r\nLength: %d\r\n\r\n%s"; // 0-�岿�Ƿ����,
																															// 1-�岿�Ƿ��������,
																															// 2-�岿����,
																															// 3-�û���(������)
	private static final String SET_MSGKEY_ACK = "SET MSGKEY\r\n\r\n"; // ����Ҫ�岿
	private static final String SET_ALIVEINT_ACK = "SET ALIVEINT\r\n\r\n"; // ����Ҫ�岿
	private static final String PUSH_MSG_ACK = "PUSH MSG\r\n\r\n"; // ����Ҫ�岿
	private static final String SET_ALIVE_REQ = "SET ALIVE\r\n\r\n"; // ����Ҫ�岿

	private static final String SEND_MSG_REQ = "SEND MSG\r\nAccount: %s\r\nId: %d\r\nSecure: %s\r\nLength:%d\r\n\r\n%s"; // 0-�������˺�,
																															// 1-���ͱ�ʶ[�ش���],
																															// 2-��Ϣ�Ƿ����,
																															// 3-�岿����,
																															// 4-��ϢJSON����
	private static final String QUERY_PUBLIC_REQ = "QUERY PUBLIC\r\nId: %d\r\nLength:%d\r\n\r\n%s"; // 0-��ѯ��ʶ[�ش���],1-�岿����,2-���ں�(֧��ģ��ƥ��)
	private static final String FOLLOW_PUBLIC_REQ = "FOLLOW PUBLIC\r\nAccount: %s\r\n\r\n"; // 0-���ں�
	private static final String UNFOLLOW_PUBLIC_REQ = "UNFOLLOW PUBLIC\r\nAccount: %s\r\n\r\n"; // 0-���ں�
	private static final String GET_FOLLOWED_REQ = "GET FOLLOWED\r\n\r\n";

	// ������Ϣ
	private static final String INVALID_ACTION_LINE = "Invalid aciton line";
	private static final String INVALID_FIELD_LINE = "Invalid field line";
	private static final String INVALID_LENGTH_VALUE_MSG = "Invalid length value";

	// �׽��ֻ�������С
	private static final int MAX_SOCKET_BUF = 8192;

	// ֪ͨ��ID
	public static final int ONGOING_NOTIFICATION = 10086;

	public static final String NOTIFICATION_MESSAGE = "message";
	public static final String NOTIFICATION_PUBLIC_ACCOUNTS = "public_accounts";
	public static final String NOTIFICATION_FOLLOWED_ACCOUNTS = "followed_accounts";

	private MyBroadcastReceiver myBroadcastReceiver;
	private Thread mServiceThread = null;
	private boolean exitNow = false;

	private AppParams appParams;
	private LoginParams loginParams;
	private NetworkParams networkParams;

	private boolean receiverStarted = false;
	private boolean clientLogon = false;
	private Socket socket = null;

	@Override
	public void onCreate() {
		super.onCreate();

		// ��ʼ���չ㲥
		myBroadcastReceiver = new MyBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.tpsoft.pushnotification.ServiceController");
		registerReceiver(myBroadcastReceiver, filter);
	}

	@Override
	public void onDestroy() {
		// ֹͣ���չ㲥
		unregisterReceiver(myBroadcastReceiver);

		// ȡ��ǰ̨����
		stopForeground(true);

		// ���������߳�
		if (receiverStarted) {
			exitNow = true;
			try {
				mServiceThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// �÷���ǰ̨����
		Notification notification = new Notification(intent.getIntExtra(
				"notification_logo", R.drawable.ic_launcher),
				intent.getStringExtra("ticker_text"),
				System.currentTimeMillis());
		Intent notificationIntent;
		try {
			notificationIntent = new Intent(this, Class.forName(intent
					.getStringExtra("ActivityClassName")));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return 0;
		}
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this,
				intent.getStringExtra("notification_title"),
				intent.getStringExtra("notification_message"), pendingIntent);
		startForeground(ONGOING_NOTIFICATION, notification);

		return Service.START_STICKY;
	}

	private void showStatus(boolean started) {
		// �㲥������״̬
		Intent activityIntent = new Intent();
		activityIntent
				.setAction("com.tpsoft.pushnotification.NotifyPushService");
		activityIntent.putExtra("action", "status");
		activityIntent.putExtra("started", started);
		sendBroadcast(activityIntent);
	}

	private void showLogining(boolean logining) {
		// �㲥��¼״̬
		Intent activityIntent = new Intent();
		activityIntent
				.setAction("com.tpsoft.pushnotification.NotifyPushService");
		activityIntent.putExtra("action", "logining");
		activityIntent.putExtra("logining", logining);
		sendBroadcast(activityIntent);
	}

	private void showLog(int type, int code, String params) {
		// �㲥��־
		Intent activityIntent = new Intent();
		activityIntent
				.setAction("com.tpsoft.pushnotification.NotifyPushService");
		activityIntent.putExtra("action", "log");
		activityIntent.putExtra("type", type);
		activityIntent.putExtra("code", code);
		if (params != null) {
			activityIntent.putExtra("params", params);
		}
		sendBroadcast(activityIntent);
	}

	private void showNotification(String type, String content) {
		// �㲥����Ϣ��֪ͨ
		Intent activityIntent = new Intent();
		activityIntent
				.setAction("com.tpsoft.pushnotification.NotifyPushService");
		activityIntent.putExtra("action", "notify");
		activityIntent.putExtra("type", type);
		activityIntent.putExtra("content", content);
		sendBroadcast(activityIntent);
	}

	private class MyBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String command = intent.getStringExtra("command");
			if (command.equals("start")) {
				// ������Ϣ��������
				// �����ظ�����
				if (receiverStarted) {
					showStatus(true);
					return;
				}

				// ��ȡ��¼����
				appParams = new AppParams(
						intent.getBundleExtra("com.tpsoft.pushnotification.AppParams"));
				loginParams = new LoginParams(
						intent.getBundleExtra("com.tpsoft.pushnotification.LoginParams"));
				networkParams = new NetworkParams(
						intent.getBundleExtra("com.tpsoft.pushnotification.NetworkParams"));

				// ���������߳�
				exitNow = false;
				mServiceThread = new SocketClientThread();
				mServiceThread.start();

				// ������������־
				receiverStarted = true;
				showStatus(true);
			} else if (command.equals("stop")) {
				// ֹͣ��Ϣ��������
				// ��ֹͣ��ֱ�ӷ���
				if (!receiverStarted) {
					showStatus(false);
					return;
				}

				// ֹͣ�����߳�
				exitNow = true;
				try {
					mServiceThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// ������ֹͣ��־
				receiverStarted = false;
			} else if (command.equals("send")) {
				// ������Ϣ
				int msgId = intent.getIntExtra("msgId", 0); // ���ͱ�ʶ[�ش���]
				boolean secure = intent.getBooleanExtra("secure", false); // ��Ϣ�Ƿ����
				String receiver, msgText;
				try {
					MyMessage message = new MyMessage(
							intent.getBundleExtra("com.tpsoft.pushnotification.MyMessage"));
					receiver = message.getReceiver();
					msgText = MyMessage.makeText(message);
				} catch (Exception e) {
					showLog(LOG_SENDMSG, ERROR_SENDMSG_DATA,
							Integer.toString(msgId));
					return;
				}
				if (!clientLogon) {
					showLog(LOG_SENDMSG, ERROR_SENDMSG_NOT_LOGON,
							Integer.toString(msgId));
					return;
				}
				showLog(LOG_SENDMSG, STATUS_SENDMSG_SUBMIT,
						Integer.toString(msgId));
				try {
					socket.getOutputStream().write(
							(String.format(SEND_MSG_REQ, receiver, msgId,
									Boolean.toString(secure), msgText.length(),
									msgText)).getBytes("UTF-8")); // TODO ֧����Ϣ����
					showLog(LOG_SENDMSG, STATUS_SENDMSG_SUBMITTED,
							Integer.toString(msgId));
				} catch (UnsupportedEncodingException ee) {
					// impossible!
					ee.printStackTrace();
				} catch (IOException ee) {
					showLog(LOG_SENDMSG, ERROR_SENDMSG_SUBMIT,
							Integer.toString(msgId));
				}
			} else if (command.equals("query_public")) {
				// ��ѯ���ں�
				int queryId = intent.getIntExtra("queryId", 0); // ��ѯ��ʶ[�ش���]
				String condition = intent.getStringExtra("condition"); // ��ѯ����:���ں�(֧��ģ��ƥ��)
				if (!clientLogon) {
					showLog(LOG_QUERYPUBLIC, ERROR_QUERYPUBLIC_NOT_LOGON,
							Integer.toString(queryId));
					return;
				}
				showLog(LOG_QUERYPUBLIC, STATUS_QUERYPUBLIC_SUBMIT,
						Integer.toString(queryId));
				try {
					socket.getOutputStream().write(
							(String.format(QUERY_PUBLIC_REQ, queryId,
									condition.length(), condition))
									.getBytes("UTF-8"));
					showLog(LOG_QUERYPUBLIC, STATUS_QUERYPUBLIC_SUBMITTED,
							Integer.toString(queryId));
				} catch (UnsupportedEncodingException ee) {
					// impossible!
					ee.printStackTrace();
				} catch (IOException ee) {
					showLog(LOG_QUERYPUBLIC, ERROR_QUERYPUBLIC_SUBMIT,
							Integer.toString(queryId));
				}
			} else if (command.equals("follow_public")) {
				// ��ע���ں�
				String account = intent.getStringExtra("account"); // ���ں�
				if (!clientLogon) {
					showLog(LOG_FOLLOWPUBLIC, ERROR_FOLLOWPUBLIC_NOT_LOGON,
							account);
					return;
				}
				showLog(LOG_FOLLOWPUBLIC, STATUS_FOLLOWPUBLIC_SUBMIT, account);
				try {
					socket.getOutputStream().write(
							(String.format(FOLLOW_PUBLIC_REQ, account))
									.getBytes("UTF-8"));
					showLog(LOG_FOLLOWPUBLIC, STATUS_FOLLOWPUBLIC_SUBMITTED,
							account);
				} catch (UnsupportedEncodingException ee) {
					// impossible!
					ee.printStackTrace();
				} catch (IOException ee) {
					showLog(LOG_FOLLOWPUBLIC, ERROR_FOLLOWPUBLIC_SUBMIT,
							account);
				}
			} else if (command.equals("unfollow_public")) {
				// ȡ����ע���ں�
				String account = intent.getStringExtra("account"); // ���ں�
				if (!clientLogon) {
					showLog(LOG_UNFOLLOWPUBLIC, ERROR_UNFOLLOWPUBLIC_NOT_LOGON,
							account);
					return;
				}
				showLog(LOG_UNFOLLOWPUBLIC, STATUS_UNFOLLOWPUBLIC_SUBMIT,
						account);
				try {
					socket.getOutputStream().write(
							(String.format(UNFOLLOW_PUBLIC_REQ, account))
									.getBytes("UTF-8"));
					showLog(LOG_UNFOLLOWPUBLIC,
							STATUS_UNFOLLOWPUBLIC_SUBMITTED, account);
				} catch (UnsupportedEncodingException ee) {
					// impossible!
					ee.printStackTrace();
				} catch (IOException ee) {
					showLog(LOG_UNFOLLOWPUBLIC, ERROR_UNFOLLOWPUBLIC_SUBMIT,
							account);
				}
			} else if (command.equals("get_followed")) {
				// ��ȡ�ѹ�ע���ں�
				if (!clientLogon) {
					showLog(LOG_GETFOLLOWED, ERROR_GETFOLLOWED_NOT_LOGON, null);
					return;
				}
				showLog(LOG_GETFOLLOWED, STATUS_GETFOLLOWED_SUBMIT, null);
				try {
					socket.getOutputStream()
							.write((String.format(GET_FOLLOWED_REQ))
									.getBytes("UTF-8"));
					showLog(LOG_GETFOLLOWED, STATUS_GETFOLLOWED_SUBMITTED, null);
				} catch (UnsupportedEncodingException ee) {
					// impossible!
					ee.printStackTrace();
				} catch (IOException ee) {
					showLog(LOG_GETFOLLOWED, ERROR_GETFOLLOWED_SUBMIT, null);
				}
			}
		}
	}

	private class SocketClientThread extends Thread {

		private boolean waitForHead = true; // �ȴ�ͷ��(false��ʾ�ȴ��岿����Ҫ�ٵȴ�)
		private String headInput = ""; // ͷ������
		private boolean actionLineFound = false; // �Է����ҵ�������

		private String action = ""; // ����
		private String target = ""; // Ŀ��
		private Map<String, String> fields = new HashMap<String, String>(); // �ֶα�
		private boolean bodyByteLength = false; // �岿���ȵ�λ�Ƿ�Ϊ�ֽ�(Ĭ��Ϊ��--���ַ�Ϊ��λ)
		private int bodyLength = 0; // �岿����(Ĭ��Ϊ����Ҫ�岿)
		private String body = ""; // �岿����
		private byte[] unhandledInput = new byte[0]; // ��δ���������

		private String msgKey;
		private int keepAliveInterval;

		private boolean invalidAppInfo = false;
		private boolean invalidClientInfo = false;

		private Calendar connectedTime;
		private Calendar lastActiveTime;
		private Calendar serverActiveTime;

		private SocketClientThread() {

		}

		@Override
		public void run() {
			byte buffer[] = new byte[MAX_SOCKET_BUF];

			boolean networkOk = true;
			socket = null;
			InputStream in = null;
			OutputStream out = null;
			reconnect: while (!exitNow) {
				headInput = "";
				resetPacketStaus();
				clientLogon = false;
				// �ر����е��׽���
				try {
					if (socket != null) {
						socket.close();
					}
				} catch (IOException ee) {
					ee.printStackTrace();
				}
				socket = null;
				if (!isNetworkAvailable()) {
					if (networkOk) {
						showLog(LOG_CONNECT, ERROR_CONNECT_NETWORK_UNAVAILABLE,
								null);
						networkOk = false;
					}
					showLogining(false);
					waitForReconnect();
					continue reconnect;
				}
				networkOk = true;
				showLog(LOG_CONNECT,
						STATUS_CONNECT_CONNECTING,
						loginParams.getServerHost() + "["
								+ loginParams.getServerPort() + "]");
				while (!exitNow) {
					// �����µ��׽���
					socket = new Socket();
					// ��������
					showLogining(true);
					try {
						socket.connect(
								new InetSocketAddress(loginParams
										.getServerHost(), loginParams
										.getServerPort()), networkParams
										.getConnectTimeout());
						socket.setSoTimeout(networkParams.getReadTimeout()); // ���ö���ʱ(ms)
						// socket.setKeepAlive(true);
						//
						in = socket.getInputStream();
						out = socket.getOutputStream();
						break;
					} catch (IOException e) {
						Log.w("Network",
								String.format("����ʧ��: %s", e.getMessage()));
						if (socket.isConnected()) {
							try {
								socket.close();
							} catch (IOException ee) {
								ee.printStackTrace();
							}
							socket = null;
						}
						showLogining(false);
						waitForReconnect();
					}
				}
				if (exitNow) {
					showLogining(false);
					break;
				}
				connectedTime = Calendar.getInstance();
				showLog(LOG_CONNECT, STATUS_CONNECT_CONNECTED, null);

				waitData: while (!exitNow) {
					// �ȴ����Է���������Ϣ
					int byteCount;
					try {
						byteCount = in.read(buffer);
						if (byteCount == -1) {
							// ����EOF
							showLog(LOG_CONNECT, ERROR_CONNECT_BROKEN, null);
							showLogining(false);
							waitForReconnect();
							continue reconnect;
						} else if (byteCount == 0) {
							throw new SocketTimeoutException("������");
						} else {
							// ��ȡ������
							serverActiveTime = Calendar.getInstance();
						}
					} catch (SocketTimeoutException e) {
						// ��ȡ��ʱ
						Calendar now = Calendar.getInstance();
						if (clientLogon) {
							// �ѵ�¼����Ҫʱ���������ź�
							long diff1 = now.getTimeInMillis()
									- lastActiveTime.getTimeInMillis();
							if (diff1 >= keepAliveInterval / 2) {
								showLog(LOG_CONNECT, STATUS_CONNECT_KEEPALIVE,
										null);
								try {
									socket.getOutputStream().write(
											SET_ALIVE_REQ.getBytes("UTF-8"));
								} catch (UnsupportedEncodingException ee) {
									// impossible!
									ee.printStackTrace();
								} catch (IOException ee) {
									showLog(LOG_CONNECT,
											ERROR_CONNECT_IO_FAULT,
											"���������źŲ��ɹ�: " + ee.getMessage());
									waitForReconnect();
									continue reconnect;
								}
								lastActiveTime = Calendar.getInstance();
							}

							//
							long diff2 = now.getTimeInMillis()
									- serverActiveTime.getTimeInMillis();
							if (diff2 >= keepAliveInterval) {
								showLog(LOG_CONNECT,
										ERROR_CONNECT_SERVER_UNAVAILABLE,
										"������������");
								waitForReconnect();
								continue reconnect;
							}
						} else {
							// ���ڵ�¼������¼��ʱ
							long diff = now.getTimeInMillis()
									- connectedTime.getTimeInMillis();
							if (diff >= networkParams.getLoginTimeout()) {
								showLog(LOG_CONNECT,
										ERROR_CONNECT_LOGIN_TIMEOUT, null);
								showLogining(false);
								waitForReconnect();
								continue reconnect;
							}
						}
						continue waitData;
					} catch (IOException e) {
						// �������
						showLog(LOG_CONNECT, ERROR_CONNECT_IO_FAULT,
								e.getMessage());
						showLogining(false);
						waitForReconnect();
						continue reconnect;
					}

					// �������Է�����������
					byte[] data = new byte[byteCount];
					for (int i = 0; i < byteCount; i++) {
						data[i] = buffer[i];
					}
					try {
						handleServerData(data, out);
					} catch (Exception e) {
						showLog(LOG_CONNECT, ERROR_CONNECT_SERVER,
								e.getMessage());
						showLogining(false);
						waitForReconnect();
						continue reconnect;
					}
					if (clientLogon) {
						// �ѵ�¼
						lastActiveTime = Calendar.getInstance();
						showLogining(false);
					} else {
						// ����¼
						if (invalidAppInfo || invalidClientInfo) {
							// Ӧ�û�ͻ���Ϣ��Ч
							showLogining(false);
							break reconnect;
						}
					}
				}
			}

			// �ر����е��׽���
			try {
				if (socket != null) {
					socket.close();
					socket = null;
				}
			} catch (IOException ee) {
				ee.printStackTrace();
			}

			receiverStarted = false;
			showLogining(false);
			showStatus(false);
		}

		private void handleServerData(byte data[], OutputStream out)
				throws Exception {
			// ���µ������ֽ�׷�ӵ�ԭ�ֽڻ�������
			if (unhandledInput.length != 0) {
				byte[] tmpUnhandledInput = new byte[unhandledInput.length
						+ data.length];
				for (int i = 0; i < unhandledInput.length; i++)
					tmpUnhandledInput[i] = unhandledInput[i];
				for (int i = 0; i < data.length; i++)
					tmpUnhandledInput[unhandledInput.length + i] = data[i];
				unhandledInput = tmpUnhandledInput;
			} else {
				unhandledInput = data;
			}
			// ���Խ��ֽڻ�����ת�����ַ���
			String newInput = new String(unhandledInput, "UTF-8");
			if (newInput.getBytes("UTF-8").length != unhandledInput.length) {
				// δ���յ��������ַ���
				return;
			}
			unhandledInput = new byte[0];
			// �����½��յ����ַ���
			if (waitForHead) {
				// ��ȡͷ��
				headInput += newInput;

				// ����ͷ��
				boolean emptyLineFound = false;
				do {
					// ��ʼ��
					String line = "";

					// Ѱ���н�����
					int pos = headInput.indexOf(INPUT_RETURN);
					if (pos == -1) {
						// δ�ҵ��н�����
						return;
					}

					// �ҵ��н�����
					// ��¼������(�������н�����)
					line = headInput.substring(0, pos);

					// ����ͷ����
					if (!actionLineFound) {
						// ������
						String[] starr = line.split("\\s+");
						if (starr.length != 2) {
							// ��ʽ����
							String ss = INVALID_ACTION_LINE + ":" + line;
							out.write(String.format(CLOSE_CONN_RES,
									ss.length(), ss).getBytes("UTF-8"));
							throw new Exception("�����и�ʽ����: " + line);
						}

						action = starr[0].trim().toUpperCase(); // ����
						target = starr[1].trim(); // Ŀ��

						actionLineFound = true;
					} else if (!line.equals("")) {
						// ������
						String[] starr = line.split("\\s*:\\s*");
						if (starr.length != 2) {
							// ��ʽ����
							String ss = INVALID_FIELD_LINE + ":" + line;
							out.write(String.format(CLOSE_CONN_RES,
									ss.length(), ss).getBytes("UTF-8"));
							throw new Exception("�����и�ʽ����: " + line);
						}

						String name = starr[0].trim().toUpperCase(); // ����
						String value = starr[1].trim(); // ֵ
						fields.put(name, value);
					} else {
						// �ҵ�����
						emptyLineFound = true;
					}

					// ΪѰ����һ���н�����׼��
					headInput = headInput
							.substring(pos + INPUT_RETURN.length());
				} while (!emptyLineFound); // ֱ����������

				// ͷ����ȡ���
				waitForHead = false;

				// �ж��岿���������Ƿ����ֽ�(Ĭ��Ϊ�ַ�)
				String bodyByteLengthFieldName = FIELD_BODY_BYTE_LENGTH
						.toUpperCase();
				if (fields.containsKey(bodyByteLengthFieldName)
						&& fields.get(bodyByteLengthFieldName.toUpperCase())
								.toUpperCase().equals("TRUE")) {
					bodyByteLength = true;
				}

				// ȷ���岿����
				String bodyLengthFieldName = FIELD_BODY_LENGTH.toUpperCase();
				if (fields.containsKey(bodyLengthFieldName)) {
					int tmpBodyLength = -1;
					try {
						tmpBodyLength = Integer.parseInt(fields
								.get(bodyLengthFieldName));
					} catch (NumberFormatException e) {
						throw new Exception("�岿���ȸ�ʽ����: "
								+ fields.get(bodyLengthFieldName));
					}
					if (tmpBodyLength < 0) {
						out.write(String.format(CLOSE_CONN_RES,
								INVALID_LENGTH_VALUE_MSG.length(),
								INVALID_LENGTH_VALUE_MSG).getBytes("UTF-8"));
						throw new Exception("�岿����ֵ��Ч: "
								+ fields.get(bodyLengthFieldName));
					}
					bodyLength = tmpBodyLength;
				}

				// ����������������Ϊ�岿
				body = headInput;
			} else {
				// ��ȡ�岿
				body += newInput;
			}

			// ����岿����
			int bodyBytes = body.getBytes("UTF-8").length;
			if ((!bodyByteLength && body.length() < bodyLength)
					|| (bodyByteLength && bodyBytes < bodyLength)) {
				// ��δ��ȡ���
				return;
			} else if ((!bodyByteLength && body.length() > bodyLength)
					|| (bodyByteLength && bodyBytes > bodyLength)) {
				// �������������Ϊ��һ�����ĵ�ͷ������
				headInput = body.substring(bodyLength);
				body = body.substring(0, bodyLength);
			} else {
				// û�ж��������
				headInput = "";
			}

			// �����µı���
			try {
				handlePacket(out, action, target, fields, body);
			} catch (Exception e) {
				throw e;
			} finally {
				resetPacketStaus();
			}
		}

		private void handlePacket(OutputStream out, String aciton,
				String target, Map<String, String> field, String body)
				throws Exception {
			if (action.equals("GET") && target.equals("APPID")) {
				// Ӧ����֤���󣬷��͵�ǰӦ��ID������
				showLog(LOG_CONNECT, STATUS_CONNECT_APP_CERTIFICATING, null);
				String password = Crypt.md5(appParams.getAppPassword());
				out.write(String.format(GET_APPID_RES,
						appParams.getAppId().length() + 1 + password.length(),
						appParams.getAppId(), password).getBytes("UTF-8"));
			} else if (action.equals("SET") && target.equals("APPID")) {
				// Ӧ����֤�ظ�
				boolean appOk = (fields.get(FIELD_ACTION_SUCCESS.toUpperCase())
						.toUpperCase().equals("TRUE"));
				if (appOk) {
					showLog(LOG_CONNECT, STATUS_CONNECT_APP_CERTIFICATED, null);
				} else {
					showLog(LOG_CONNECT, ERROR_CONNECT_APP_CERTIFICATE, body);
					invalidAppInfo = true;
				}
			} else if (action.equals("GET") && target.equals("USERNAME")) {
				// �û���֤���󣬷����û���������
				showLog(LOG_CONNECT, STATUS_CONNECT_USER_CERTIFICATING, null);
				if (fields.get(FIELD_LOGIN_SECURE.toUpperCase()).toUpperCase()
						.equals("TRUE")) {
					// ��ȫ��¼
					if (fields.get(FIELD_LOGIN_PASSWORD.toUpperCase())
							.toUpperCase().equals("TRUE")) {
						// ��Ҫ��¼����
						String password = Crypt.md5(loginParams
								.getClientPassword());
						String ss = Crypt.rsaEncrypt(
								appParams.getLoginProtectKey(),
								(loginParams.getClientId() + "," + password));
						out.write(String.format(GET_USERNAME_RES, "true",
								"true", ss.length(), ss).getBytes("UTF-8"));
					} else {
						// ����Ҫ��¼����
						String ss = Crypt.rsaEncrypt(
								appParams.getLoginProtectKey(),
								loginParams.getClientId());
						out.write(String.format(GET_USERNAME_RES, "true",
								"false", ss.length(), ss).getBytes("UTF-8"));
					}
				} else {
					// �ǰ�ȫ��¼
					if (fields.get(FIELD_LOGIN_PASSWORD.toUpperCase())
							.toUpperCase().equals("TRUE")) {
						// ��Ҫ��¼����
						String password = Crypt.md5(loginParams
								.getClientPassword());
						String ss = loginParams.getClientId() + "," + password;
						out.write(String.format(GET_USERNAME_RES, "false",
								"true", ss.length(), ss).getBytes("UTF-8"));
					} else {
						// ����Ҫ��¼����
						String ss = loginParams.getClientId();
						out.write(String.format(GET_USERNAME_RES, "false",
								"false", ss.length(), ss).getBytes("UTF-8"));
					}
				}
			} else if (action.equals("SET") && target.equals("USERNAME")) {
				// �û���֤�ظ�
				boolean usernameOk = (fields.get(
						FIELD_ACTION_SUCCESS.toUpperCase()).toUpperCase()
						.equals("TRUE"));
				if (usernameOk) {
					showLog(LOG_CONNECT, STATUS_CONNECT_USER_CERTIFICATED, null);
				} else {
					showLog(LOG_CONNECT, ERROR_CONNECT_USER_CERTIFICATE, body);
					invalidClientInfo = true;
				}
			} else if (action.equals("SET") && target.equals("MSGKEY")) {
				// �յ���Ϣ��Կ
				msgKey = body;
				if (fields.get(FIELD_LOGIN_SECURE.toUpperCase()).toUpperCase()
						.equals("TRUE")) {
					// ������Ϣ��Կ
					msgKey = Crypt.rsaDecrypt(appParams.getLoginProtectKey(),
							body);
				}
				out.write(String.format(SET_MSGKEY_ACK).getBytes("UTF-8"));
				showLog(LOG_CONNECT, STATUS_CONNECT_MSGKEY_RECEIVED, null);
			} else if (action.equals("SET") && target.equals("ALIVEINT")) {
				// ������������
				keepAliveInterval = Integer.parseInt(body);
				out.write(String.format(SET_ALIVEINT_ACK).getBytes("UTF-8"));
				showLog(LOG_CONNECT, STATUS_CONNECT_KEEPALIVEINTERVAL_RECEIVED,
						Integer.toString(keepAliveInterval));

				clientLogon = true;
				showLog(LOG_CONNECT, STATUS_CONNECT_LOGON, null);
			} else if (action.equals("SET") && target.equals("ALIVE")) {
				// �յ������ظ��ź�
				showLog(LOG_CONNECT, STATUS_CONNECT_KEEPALIVE_REPLIED, null);
			} else if (action.equals("PUSH") && target.equals("MSG")) {
				// �յ���Ϣ
				boolean secure = fields.get(FIELD_LOGIN_SECURE.toUpperCase())
						.toUpperCase().equals("TRUE");
				boolean receipt = fields.get(FIELD_MSG_RECEIPT.toUpperCase())
						.toUpperCase().equals("TRUE");
				String msg = body;
				if (receipt) {
					// ȷ�����յ���Ϣ
					out.write(PUSH_MSG_ACK.getBytes("UTF-8"));
				}
				if (secure) {
					// ��Ϣ����
					msg = Crypt.desDecrypt(msgKey, msg);
				}
				showNotification(NOTIFICATION_MESSAGE, msg);
			} else if (action.equals("SEND") && target.equals("MSG")) {
				// �յ���Ϣ�ظ�
				String msgId = fields.get(FIELD_ACTION_ID.toUpperCase());
				boolean success = fields
						.get(FIELD_ACTION_SUCCESS.toUpperCase()).toUpperCase()
						.equals("TRUE");
				if (success) {
					showLog(LOG_SENDMSG, STATUS_SENDMSG_OK, msgId);
				} else {
					String err = body;
					showLog(LOG_SENDMSG, ERROR_SENDMSG_FAILED, msgId + ":"
							+ err);
				}
			} else if (action.equals("QUERY") && target.equals("PUBLIC")) {
				// �յ����ںŲ�ѯ�ظ�
				String queryId = fields.get(FIELD_ACTION_ID.toUpperCase());
				boolean success = fields
						.get(FIELD_ACTION_SUCCESS.toUpperCase()).toUpperCase()
						.equals("TRUE");
				if (success) {
					String accountsText = body;
					showNotification(NOTIFICATION_PUBLIC_ACCOUNTS, accountsText);
					showLog(LOG_QUERYPUBLIC, STATUS_QUERYPUBLIC_OK, queryId);
				} else {
					String err = body;
					showLog(LOG_QUERYPUBLIC, ERROR_QUERYPUBLIC_FAILED, queryId
							+ ":" + err);
				}
			} else if (action.equals("FOLLOW") && target.equals("PUBLIC")) {
				// �յ����ںŹ�ע�ظ�
				String publicAccount = fields.get(FIELD_ACTION_ACCOUNT
						.toUpperCase());
				boolean success = fields
						.get(FIELD_ACTION_SUCCESS.toUpperCase()).toUpperCase()
						.equals("TRUE");
				if (success) {
					showLog(LOG_FOLLOWPUBLIC, STATUS_FOLLOWPUBLIC_OK,
							publicAccount);
				} else {
					String err = body;
					showLog(LOG_FOLLOWPUBLIC, ERROR_FOLLOWPUBLIC_FAILED,
							publicAccount + ":" + err);
				}
			} else if (action.equals("UNFOLLOW") && target.equals("PUBLIC")) {
				// �յ����ں�ȡ����ע�ظ�
				String publicAccount = fields.get(FIELD_ACTION_ACCOUNT
						.toUpperCase());
				boolean success = fields
						.get(FIELD_ACTION_SUCCESS.toUpperCase()).toUpperCase()
						.equals("TRUE");
				if (success) {
					showLog(LOG_UNFOLLOWPUBLIC, STATUS_UNFOLLOWPUBLIC_OK,
							publicAccount);
				} else {
					String err = body;
					showLog(LOG_UNFOLLOWPUBLIC, ERROR_UNFOLLOWPUBLIC_FAILED,
							publicAccount + ":" + err);
				}
			} else if (action.equals("GET") && target.equals("FOLLOWED")) {
				// �յ���ȡ�ѹ�ע���ںŻظ�
				boolean success = fields
						.get(FIELD_ACTION_SUCCESS.toUpperCase()).toUpperCase()
						.equals("TRUE");
				if (success) {
					String accountsText = body;
					showNotification(NOTIFICATION_FOLLOWED_ACCOUNTS,
							accountsText);
					showLog(LOG_GETFOLLOWED, STATUS_GETFOLLOWED_OK, null);
				} else {
					String err = body;
					showLog(LOG_GETFOLLOWED, ERROR_GETFOLLOWED_FAILED, err);
				}
			} else if (action.equals("CLOSE") && target.equals("CONN")) {
				// �����������Ͽ�����
				throw new Exception("�����������Ͽ�����: " + body);
			} else {
				// �޷����Ķ���
				throw new Exception("δ֪����: " + action + " " + target);
			}
		}

		private void resetPacketStaus() {
			waitForHead = true;
			actionLineFound = false;
			//
			action = target = "";
			fields.clear();
			bodyByteLength = false;
			bodyLength = 0;
			body = "";
			unhandledInput = new byte[0];
		}

		private void waitForReconnect() {
			try {
				Thread.sleep(networkParams.getReconnectDelay());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
}
