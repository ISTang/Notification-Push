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

	// 日志类型
	public static final int LOG_CONNECT = 1; // 连接
	public static final int LOG_SENDMSG = 2; // 发送消息
	public static final int LOG_QUERYPUBLIC = 3; // 查询公众号
	public static final int LOG_FOLLOWPUBLIC = 4; // 关注公众号
	public static final int LOG_UNFOLLOWPUBLIC = 5; // 取消关注公众号
	public static final int LOG_GETFOLLOWED = 6; // 获取已关注的公众号
	// 状态码
	public static final int STATUS_CONNECT_CONNECTING = 1; // 连接服务器...
	public static final int STATUS_CONNECT_CONNECTED = 2; // 已经连接到服务器
	public static final int STATUS_CONNECT_APP_CERTIFICATING = 3; // 应用认证...
	public static final int STATUS_CONNECT_APP_CERTIFICATED = 4; // 应用认证通过
	public static final int STATUS_CONNECT_USER_CERTIFICATING = 5; // 用户认证...
	public static final int STATUS_CONNECT_USER_CERTIFICATED = 6; // 用户认证通过
	public static final int STATUS_CONNECT_MSGKEY_RECEIVED = 7; // 收到消息密钥
	public static final int STATUS_CONNECT_KEEPALIVEINTERVAL_RECEIVED = 8; // 收到心跳周期
	public static final int STATUS_CONNECT_LOGON = 9; // 登录成功
	public static final int STATUS_CONNECT_KEEPALIVE = 10; // 发送心跳信号
	public static final int STATUS_CONNECT_KEEPALIVE_REPLIED = 11; // 收到心跳回复信号
	//
	public static final int STATUS_SENDMSG_SUBMIT = 51; // 提交消息
	public static final int STATUS_SENDMSG_SUBMITTED = 52; // 消息已提交
	public static final int STATUS_SENDMSG_OK = 53; // 消息已发送
	//
	public static final int STATUS_QUERYPUBLIC_SUBMIT = 61; // 提交公众号查询请求
	public static final int STATUS_QUERYPUBLIC_SUBMITTED = 62; // 公众号查询请求已提交
	public static final int STATUS_QUERYPUBLIC_OK = 63; // 公众号查询成功
	//
	public static final int STATUS_FOLLOWPUBLIC_SUBMIT = 71; // 提交公众号关注请求
	public static final int STATUS_FOLLOWPUBLIC_SUBMITTED = 72; // 公众号关注请求已提交
	public static final int STATUS_FOLLOWPUBLIC_OK = 73; // 公众号关注成功
	//
	public static final int STATUS_UNFOLLOWPUBLIC_SUBMIT = 81; // 提交公众号取消关注请求
	public static final int STATUS_UNFOLLOWPUBLIC_SUBMITTED = 82; // 公众号取消关注请求已提交
	public static final int STATUS_UNFOLLOWPUBLIC_OK = 83; // 公众号取消关注成功
	//
	public static final int STATUS_GETFOLLOWED_SUBMIT = 91; // 提交获取已关注公众号请求
	public static final int STATUS_GETFOLLOWED_SUBMITTED = 92; // 获取已关注公众号请求已提交
	public static final int STATUS_GETFOLLOWED_OK = 93; // 获取已关注公众号成功
	// 错误码
	public static final int ERROR_CONNECT_NETWORK_UNAVAILABLE = 101; // 网络不可用
	public static final int ERROR_CONNECT_BROKEN = 102; // 连接已中断
	public static final int ERROR_CONNECT_SERVER_UNAVAILABLE = 103; // 服务器不可用
	public static final int ERROR_CONNECT_LOGIN_TIMEOUT = 104; // 登录超时
	public static final int ERROR_CONNECT_IO_FAULT = 105; // 网络IO故障
	public static final int ERROR_CONNECT_APP_CERTIFICATE = 106; // 应用认证失败
	public static final int ERROR_CONNECT_USER_CERTIFICATE = 107; // 用户认证失败
	public static final int ERROR_CONNECT_SERVER = 108; // 服务器错误
	//
	public static final int ERROR_SENDMSG_NOT_LOGON = 151; // 尚未登录成功
	public static final int ERROR_SENDMSG_DATA = 152; // 消息数据出错
	public static final int ERROR_SENDMSG_SUBMIT = 153; // 提交消息失败
	public static final int ERROR_SENDMSG_FAILED = 154; // 发送消息失败
	//
	public static final int ERROR_QUERYPUBLIC_NOT_LOGON = 161; // 尚未登录成功
	public static final int ERROR_QUERYPUBLIC_SUBMIT = 162; // 提交公众号查询请求失败
	public static final int ERROR_QUERYPUBLIC_FAILED = 163; // 公众号查询失败
	//
	public static final int ERROR_FOLLOWPUBLIC_NOT_LOGON = 171; // 尚未登录成功
	public static final int ERROR_FOLLOWPUBLIC_SUBMIT = 172; // 提交公众号关注请求失败
	public static final int ERROR_FOLLOWPUBLIC_FAILED = 173; // 公众号关注失败
	//
	public static final int ERROR_UNFOLLOWPUBLIC_NOT_LOGON = 181; // 尚未登录成功
	public static final int ERROR_UNFOLLOWPUBLIC_SUBMIT = 182; // 提交公众号取消关注请求失败
	public static final int ERROR_UNFOLLOWPUBLIC_FAILED = 183; // 公众号取消关注失败
	//
	public static final int ERROR_GETFOLLOWED_NOT_LOGON = 191; // 尚未登录成功
	public static final int ERROR_GETFOLLOWED_SUBMIT = 192; // 提交获取已关注公众号请求失败
	public static final int ERROR_GETFOLLOWED_FAILED = 193; // 获取已关注公众号失败

	// 行结束标志
	private static final String INPUT_RETURN = "\r\n";

	// 头部字段名定义
	private static final String FIELD_BODY_BYTE_LENGTH = "ByteLength";
	private static final String FIELD_BODY_LENGTH = "Length";
	private static final String FIELD_ACTION_SUCCESS = "Success";
	private static final String FIELD_LOGIN_SECURE = "Secure";
	private static final String FIELD_LOGIN_PASSWORD = "Password";
	private static final String FIELD_MSG_RECEIPT = "Receipt";
	private static final String FIELD_ACTION_ID = "Id";
	private static final String FIELD_ACTION_ACCOUNT = "Account";

	private static final String CLOSE_CONN_RES = "CLOSE CONN\r\nLength: %d\r\n\r\n%s"; // 体部:
																						// 错误内容(已包含)
	private static final String GET_APPID_RES = "SET APPID\r\nLength: %d\r\n\r\n%s,%s"; // 0-体部长度,
																						// 1-应用ID,
																						// 2-应用密码
	private static final String GET_USERNAME_RES = "SET USERNAME\r\nSecure: %s\r\nPassword: %s\r\nLength: %d\r\n\r\n%s"; // 0-体部是否加密,
																															// 1-体部是否包含密码,
																															// 2-体部长度,
																															// 3-用户名(和密码)
	private static final String SET_MSGKEY_ACK = "SET MSGKEY\r\n\r\n"; // 不需要体部
	private static final String SET_ALIVEINT_ACK = "SET ALIVEINT\r\n\r\n"; // 不需要体部
	private static final String PUSH_MSG_ACK = "PUSH MSG\r\n\r\n"; // 不需要体部
	private static final String SET_ALIVE_REQ = "SET ALIVE\r\n\r\n"; // 不需要体部

	private static final String SEND_MSG_REQ = "SEND MSG\r\nAccount: %s\r\nId: %d\r\nSecure: %s\r\nLength:%d\r\n\r\n%s"; // 0-接收者账号,
																															// 1-发送标识[回传用],
																															// 2-消息是否加密,
																															// 3-体部长度,
																															// 4-消息JSON对象
	private static final String QUERY_PUBLIC_REQ = "QUERY PUBLIC\r\nId: %d\r\nLength:%d\r\n\r\n%s"; // 0-查询标识[回传用],1-体部长度,2-公众号(支持模糊匹配)
	private static final String FOLLOW_PUBLIC_REQ = "FOLLOW PUBLIC\r\nAccount: %s\r\n\r\n"; // 0-公众号
	private static final String UNFOLLOW_PUBLIC_REQ = "UNFOLLOW PUBLIC\r\nAccount: %s\r\n\r\n"; // 0-公众号
	private static final String GET_FOLLOWED_REQ = "GET FOLLOWED\r\n\r\n";

	// 错误消息
	private static final String INVALID_ACTION_LINE = "Invalid aciton line";
	private static final String INVALID_FIELD_LINE = "Invalid field line";
	private static final String INVALID_LENGTH_VALUE_MSG = "Invalid length value";

	// 套接字缓冲区大小
	private static final int MAX_SOCKET_BUF = 8192;

	// 通知栏ID
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

		// 开始接收广播
		myBroadcastReceiver = new MyBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.tpsoft.pushnotification.ServiceController");
		registerReceiver(myBroadcastReceiver, filter);
	}

	@Override
	public void onDestroy() {
		// 停止接收广播
		unregisterReceiver(myBroadcastReceiver);

		// 取消前台服务
		stopForeground(true);

		// 结束工作线程
		if (receiverStarted) {
			exitNow = true;
			try {
				mServiceThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// 通知主控方服务已停止
		Intent activityIntent = new Intent();
		activityIntent
				.setAction("com.tpsoft.pushnotification.NotifyPushService");
		activityIntent.putExtra("action", "service");
		activityIntent.putExtra("started", false);
		sendBroadcast(activityIntent);

		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.hasExtra("MainActivityClassName")) {
			// 让服务前台运行
			Notification notification = new Notification(intent.getIntExtra(
					"notification_logo", intent.getIntExtra(
							"notification_logo", R.drawable.ic_launcher)),
					getText(R.string.ticker_text), System.currentTimeMillis());

			Intent notificationIntent;
			try {
				notificationIntent = new Intent(this, Class.forName(intent
						.getStringExtra("MainActivityClassName")));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return 0;
			}

			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
					notificationIntent, 0);

			notification.setLatestEventInfo(this,
					intent.getStringExtra("notification_title"),
					intent.getStringExtra("notification_message"),
					pendingIntent);
			startForeground(ONGOING_NOTIFICATION, notification);
		}

		// 通知主控方服务已启动
		Intent activityIntent = new Intent();
		activityIntent
				.setAction("com.tpsoft.pushnotification.NotifyPushService");
		activityIntent.putExtra("action", "service");
		activityIntent.putExtra("started", true);
		sendBroadcast(activityIntent);

		return Service.START_STICKY;
	}

	private void showStatus(boolean started) {
		// 广播接收器状态
		Intent activityIntent = new Intent();
		activityIntent
				.setAction("com.tpsoft.pushnotification.NotifyPushService");
		activityIntent.putExtra("action", "status");
		activityIntent.putExtra("started", started);
		sendBroadcast(activityIntent);
	}

	private void showLogining(boolean logining) {
		// 广播登录状态
		Intent activityIntent = new Intent();
		activityIntent
				.setAction("com.tpsoft.pushnotification.NotifyPushService");
		activityIntent.putExtra("action", "logining");
		activityIntent.putExtra("logining", logining);
		sendBroadcast(activityIntent);
	}

	private void showLog(int type, int code, String params) {
		// 广播日志
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
		// 广播新消息等通知
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
				// 启动消息接收器：
				// 避免重复启动
				if (receiverStarted) {
					showStatus(true);
					return;
				}

				// 获取登录参数
				appParams = new AppParams(
						intent.getBundleExtra("com.tpsoft.pushnotification.AppParams"));
				loginParams = new LoginParams(
						intent.getBundleExtra("com.tpsoft.pushnotification.LoginParams"));
				networkParams = new NetworkParams(
						intent.getBundleExtra("com.tpsoft.pushnotification.NetworkParams"));

				// 启动工作线程
				exitNow = false;
				mServiceThread = new SocketClientThread();
				mServiceThread.start();

				// 设置已启动标志
				receiverStarted = true;
				showStatus(true);
			} else if (command.equals("stop")) {
				// 停止消息接收器：
				// 已停止则直接返回
				if (!receiverStarted) {
					showStatus(false);
					return;
				}

				// 停止工作线程
				exitNow = true;
				try {
					mServiceThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// 设置已停止标志
				receiverStarted = false;
			} else if (command.equals("send")) {
				// 发送消息
				int msgId = intent.getIntExtra("msgId", 0); // 发送标识[回传用]
				boolean secure = intent.getBooleanExtra("secure", false); // 消息是否加密
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
									msgText)).getBytes("UTF-8")); // TODO 支持消息加密
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
				// 查询公众号
				int queryId = intent.getIntExtra("queryId", 0); // 查询标识[回传用]
				String condition = intent.getStringExtra("condition"); // 查询条件:公众号(支持模糊匹配)
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
				// 关注公众号
				String account = intent.getStringExtra("account"); // 公众号
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
				// 取消关注公众号
				String account = intent.getStringExtra("account"); // 公众号
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
				// 获取已关注公众号
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

		private boolean waitForHead = true; // 等待头部(false表示等待体部或不需要再等待)
		private String headInput = ""; // 头部输入
		private boolean actionLineFound = false; // 以否已找到动作行

		private String action = ""; // 动作
		private String target = ""; // 目标
		private Map<String, String> fields = new HashMap<String, String>(); // 字段表
		private boolean bodyByteLength = false; // 体部长度单位是否为字节(默认为否--以字符为单位)
		private int bodyLength = 0; // 体部长度(默认为不需要体部)
		private String body = ""; // 体部内容
		private byte[] unhandledInput = new byte[0]; // 尚未处理的输入

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
				// 关闭已有的套接字
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
					// 创建新的套接字
					socket = new Socket();
					// 尝试连接
					showLogining(true);
					try {
						socket.connect(
								new InetSocketAddress(loginParams
										.getServerHost(), loginParams
										.getServerPort()), networkParams
										.getConnectTimeout());
						socket.setSoTimeout(networkParams.getReadTimeout()); // 设置读超时(ms)
						// socket.setKeepAlive(true);
						//
						in = socket.getInputStream();
						out = socket.getOutputStream();
						break;
					} catch (IOException e) {
						Log.w("Network",
								String.format("连接失败: %s", e.getMessage()));
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
					// 等待来自服务器的消息
					int byteCount;
					try {
						byteCount = in.read(buffer);
						if (byteCount == -1) {
							// 遇到EOF
							showLog(LOG_CONNECT, ERROR_CONNECT_BROKEN, null);
							showLogining(false);
							waitForReconnect();
							continue reconnect;
						} else if (byteCount == 0) {
							throw new SocketTimeoutException("空数据");
						} else {
							// 读取到数据
							serverActiveTime = Calendar.getInstance();
						}
					} catch (SocketTimeoutException e) {
						// 读取超时
						Calendar now = Calendar.getInstance();
						if (clientLogon) {
							// 已登录，必要时发送心跳信号
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
											"发送心跳信号不成功: " + ee.getMessage());
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
										"服务器不可用");
								waitForReconnect();
								continue reconnect;
							}
						} else {
							// 正在登录，检测登录超时
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
						// 网络错误
						showLog(LOG_CONNECT, ERROR_CONNECT_IO_FAULT,
								e.getMessage());
						showLogining(false);
						waitForReconnect();
						continue reconnect;
					}

					// 处理来自服务器的数据
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
						// 已登录
						lastActiveTime = Calendar.getInstance();
						showLogining(false);
					} else {
						// 正登录
						if (invalidAppInfo || invalidClientInfo) {
							// 应用或客户信息无效
							showLogining(false);
							break reconnect;
						}
					}
				}
			}

			// 关闭已有的套接字
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
			// 将新的输入字节追加到原字节缓冲区中
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
			// 尝试将字节缓冲区转换成字符串
			String newInput = new String(unhandledInput, "UTF-8");
			if (newInput.getBytes("UTF-8").length != unhandledInput.length) {
				// 未接收到完整的字符串
				return;
			}
			unhandledInput = new byte[0];
			// 处理新接收到的字符串
			if (waitForHead) {
				// 读取头部
				headInput += newInput;

				// 解析头部
				boolean emptyLineFound = false;
				do {
					// 初始化
					String line = "";

					// 寻找行结束符
					int pos = headInput.indexOf(INPUT_RETURN);
					if (pos == -1) {
						// 未找到行结束符
						return;
					}

					// 找到行结束符
					// 记录行内容(不包括行结束符)
					line = headInput.substring(0, pos);

					// 解析头部行
					if (!actionLineFound) {
						// 动作行
						String[] starr = line.split("\\s+");
						if (starr.length != 2) {
							// 格式不对
							String ss = INVALID_ACTION_LINE + ":" + line;
							out.write(String.format(CLOSE_CONN_RES,
									ss.length(), ss).getBytes("UTF-8"));
							throw new Exception("动作行格式不对: " + line);
						}

						action = starr[0].trim().toUpperCase(); // 动作
						target = starr[1].trim(); // 目标

						actionLineFound = true;
					} else if (!line.equals("")) {
						// 属性行
						String[] starr = line.split("\\s*:\\s*");
						if (starr.length != 2) {
							// 格式不对
							String ss = INVALID_FIELD_LINE + ":" + line;
							out.write(String.format(CLOSE_CONN_RES,
									ss.length(), ss).getBytes("UTF-8"));
							throw new Exception("属性行格式不对: " + line);
						}

						String name = starr[0].trim().toUpperCase(); // 名字
						String value = starr[1].trim(); // 值
						fields.put(name, value);
					} else {
						// 找到空行
						emptyLineFound = true;
					}

					// 为寻找下一个行结束符准备
					headInput = headInput
							.substring(pos + INPUT_RETURN.length());
				} while (!emptyLineFound); // 直到遇到空行

				// 头部读取完毕
				waitForHead = false;

				// 判断体部长度类型是否是字节(默认为字符)
				String bodyByteLengthFieldName = FIELD_BODY_BYTE_LENGTH
						.toUpperCase();
				if (fields.containsKey(bodyByteLengthFieldName)
						&& fields.get(bodyByteLengthFieldName.toUpperCase())
								.toUpperCase().equals("TRUE")) {
					bodyByteLength = true;
				}

				// 确定体部长度
				String bodyLengthFieldName = FIELD_BODY_LENGTH.toUpperCase();
				if (fields.containsKey(bodyLengthFieldName)) {
					int tmpBodyLength = -1;
					try {
						tmpBodyLength = Integer.parseInt(fields
								.get(bodyLengthFieldName));
					} catch (NumberFormatException e) {
						throw new Exception("体部长度格式不对: "
								+ fields.get(bodyLengthFieldName));
					}
					if (tmpBodyLength < 0) {
						out.write(String.format(CLOSE_CONN_RES,
								INVALID_LENGTH_VALUE_MSG.length(),
								INVALID_LENGTH_VALUE_MSG).getBytes("UTF-8"));
						throw new Exception("体部长度值无效: "
								+ fields.get(bodyLengthFieldName));
					}
					bodyLength = tmpBodyLength;
				}

				// 将余下输入内容作为体部
				body = headInput;
			} else {
				// 读取体部
				body += newInput;
			}

			// 检查体部内容
			int bodyBytes = body.getBytes("UTF-8").length;
			if ((!bodyByteLength && body.length() < bodyLength)
					|| (bodyByteLength && bodyBytes < bodyLength)) {
				// 尚未读取完毕
				return;
			} else if ((!bodyByteLength && body.length() > bodyLength)
					|| (bodyByteLength && bodyBytes > bodyLength)) {
				// 将多余的内容作为下一个报文的头部输入
				headInput = body.substring(bodyLength);
				body = body.substring(0, bodyLength);
			} else {
				// 没有多余的输入
				headInput = "";
			}

			// 处理新的报文
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
				// 应用认证请求，发送当前应用ID及密码
				showLog(LOG_CONNECT, STATUS_CONNECT_APP_CERTIFICATING, null);
				String password = Crypt.md5(appParams.getAppPassword());
				out.write(String.format(GET_APPID_RES,
						appParams.getAppId().length() + 1 + password.length(),
						appParams.getAppId(), password).getBytes("UTF-8"));
			} else if (action.equals("SET") && target.equals("APPID")) {
				// 应用认证回复
				boolean appOk = (fields.get(FIELD_ACTION_SUCCESS.toUpperCase())
						.toUpperCase().equals("TRUE"));
				if (appOk) {
					showLog(LOG_CONNECT, STATUS_CONNECT_APP_CERTIFICATED, null);
				} else {
					showLog(LOG_CONNECT, ERROR_CONNECT_APP_CERTIFICATE, body);
					invalidAppInfo = true;
				}
			} else if (action.equals("GET") && target.equals("USERNAME")) {
				// 用户认证请求，发送用户名及密码
				showLog(LOG_CONNECT, STATUS_CONNECT_USER_CERTIFICATING, null);
				if (fields.get(FIELD_LOGIN_SECURE.toUpperCase()).toUpperCase()
						.equals("TRUE")) {
					// 安全登录
					if (fields.get(FIELD_LOGIN_PASSWORD.toUpperCase())
							.toUpperCase().equals("TRUE")) {
						// 需要登录密码
						String password = Crypt.md5(loginParams
								.getClientPassword());
						String ss = Crypt.rsaEncrypt(
								appParams.getLoginProtectKey(),
								(loginParams.getClientId() + "," + password));
						out.write(String.format(GET_USERNAME_RES, "true",
								"true", ss.length(), ss).getBytes("UTF-8"));
					} else {
						// 不需要登录密码
						String ss = Crypt.rsaEncrypt(
								appParams.getLoginProtectKey(),
								loginParams.getClientId());
						out.write(String.format(GET_USERNAME_RES, "true",
								"false", ss.length(), ss).getBytes("UTF-8"));
					}
				} else {
					// 非安全登录
					if (fields.get(FIELD_LOGIN_PASSWORD.toUpperCase())
							.toUpperCase().equals("TRUE")) {
						// 需要登录密码
						String password = Crypt.md5(loginParams
								.getClientPassword());
						String ss = loginParams.getClientId() + "," + password;
						out.write(String.format(GET_USERNAME_RES, "false",
								"true", ss.length(), ss).getBytes("UTF-8"));
					} else {
						// 不需要登录密码
						String ss = loginParams.getClientId();
						out.write(String.format(GET_USERNAME_RES, "false",
								"false", ss.length(), ss).getBytes("UTF-8"));
					}
				}
			} else if (action.equals("SET") && target.equals("USERNAME")) {
				// 用户认证回复
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
				// 收到消息密钥
				msgKey = body;
				if (fields.get(FIELD_LOGIN_SECURE.toUpperCase()).toUpperCase()
						.equals("TRUE")) {
					// 解密消息密钥
					msgKey = Crypt.rsaDecrypt(appParams.getLoginProtectKey(),
							body);
				}
				out.write(String.format(SET_MSGKEY_ACK).getBytes("UTF-8"));
				showLog(LOG_CONNECT, STATUS_CONNECT_MSGKEY_RECEIVED, null);
			} else if (action.equals("SET") && target.equals("ALIVEINT")) {
				// 设置心跳周期
				keepAliveInterval = Integer.parseInt(body);
				out.write(String.format(SET_ALIVEINT_ACK).getBytes("UTF-8"));
				showLog(LOG_CONNECT, STATUS_CONNECT_KEEPALIVEINTERVAL_RECEIVED,
						Integer.toString(keepAliveInterval));

				clientLogon = true;
				showLog(LOG_CONNECT, STATUS_CONNECT_LOGON, null);
			} else if (action.equals("SET") && target.equals("ALIVE")) {
				// 收到心跳回复信号
				showLog(LOG_CONNECT, STATUS_CONNECT_KEEPALIVE_REPLIED, null);
			} else if (action.equals("PUSH") && target.equals("MSG")) {
				// 收到消息
				boolean secure = fields.get(FIELD_LOGIN_SECURE.toUpperCase())
						.toUpperCase().equals("TRUE");
				boolean receipt = fields.get(FIELD_MSG_RECEIPT.toUpperCase())
						.toUpperCase().equals("TRUE");
				String msg = body;
				if (receipt) {
					// 确认已收到消息
					out.write(PUSH_MSG_ACK.getBytes("UTF-8"));
				}
				if (secure) {
					// 消息解密
					msg = Crypt.desDecrypt(msgKey, msg);
				}
				showNotification(NOTIFICATION_MESSAGE, msg);
			} else if (action.equals("SEND") && target.equals("MSG")) {
				// 收到消息回复
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
				// 收到公众号查询回复
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
				// 收到公众号关注回复
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
				// 收到公众号取消关注回复
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
				// 收到获取已关注公众号回复
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
				// 服务器主动断开连接
				throw new Exception("服务器主动断开连接: " + body);
			} else {
				// 无法理解的动作
				throw new Exception("未知动作: " + action + " " + target);
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
