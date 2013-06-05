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
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;

import com.tpsoft.pushnotification.R;
import com.tpsoft.pushnotification.model.AppParams;
import com.tpsoft.pushnotification.model.LoginParams;
import com.tpsoft.pushnotification.model.NetworkParams;
import com.tpsoft.pushnotification.utils.Crypt;

@SuppressLint("DefaultLocale")
public class NotifyPushService extends Service {

	private static final String INPUT_RETURN = "\r\n";

	private static final int ONGOING_NOTIFICATION = 10086;

	private MyBroadcastReceiver myBroadcastReceiver;
	private Thread mServiceThread = null;
	private boolean exitNow = false;

	private AppParams appParams;
	private LoginParams loginParams;
	private NetworkParams networkParams;

	@Override
	public void onCreate() {
		super.onCreate();

		myBroadcastReceiver = new MyBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.tpsoft.pushnotification.ServiceController");
		registerReceiver(myBroadcastReceiver, filter);
	}

	@Override
	public void onDestroy() {
		if (mServiceThread != null) {
			exitNow = true;
			try {
				mServiceThread.join();
			} catch (InterruptedException e) {
				showLog(e.getMessage());
			}
			mServiceThread = null;
		}

		unregisterReceiver(myBroadcastReceiver);

		// 取消前台服务
		stopForeground(true);

		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// 让服务前台运行
		Notification notification = new Notification(R.drawable.ic_launcher,
				getText(R.string.ticker_text), System.currentTimeMillis());
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
				getText(R.string.notification_title),
				getText(R.string.notification_message), pendingIntent);
		startForeground(ONGOING_NOTIFICATION, notification);

		return Service.START_STICKY;
	}

	private void showLog(String logText) {
		Intent activityIntent = new Intent();
		activityIntent
				.setAction("com.tpsoft.pushnotification.NotifyPushService");
		activityIntent.putExtra("action", "log");
		activityIntent.putExtra("logText", logText);
		sendBroadcast(activityIntent);
	}

	private void showNotification(String msgText) {
		Intent activityIntent = new Intent();
		activityIntent
				.setAction("com.tpsoft.pushnotification.NotifyPushService");
		activityIntent.putExtra("action", "notify");
		activityIntent.putExtra("msgText", msgText);
		sendBroadcast(activityIntent);
	}

	private class MyBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String command = intent.getStringExtra("command");
			if (command.equals("start")) {
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
			} else if (command.equals("stop")) {
				// 结束工作线程
				if (mServiceThread != null) {
					// showLog(getText(R.string.receiver_stopping).toString());
					exitNow = true;
					try {
						mServiceThread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					mServiceThread = null;
				}
			}
		}

	}

	private class SocketClientThread extends Thread {

		public static final int MAX_SOCKET_BUF = 8192;

		// 头部字段名定义
		public static final String FIELD_BODY_BYTE_LENGTH = "ByteLength";
		public static final String FIELD_BODY_LENGTH = "Length";
		public static final String FIELD_ACTION_SUCCESS = "Success";
		public static final String FIELD_LOGIN_SECURE = "Secure";
		public static final String FIELD_LOGIN_PASSWORD = "Password";
		public static final String FIELD_MSG_RECEIPT = "Receipt";

		public static final String CLOSE_CONN_RES = "CLOSE CONN\r\nLength: %d\r\n\r\n%s"; // 体部:
																							// 错误内容(已包含)
		public static final String GET_APPID_RES = "SET APPID\r\nLength: %d\r\n\r\n%s,%s"; // 0-体部长度,
																							// 1-应用ID,
																							// 2-应用密码
		public static final String GET_USERNAME_RES = "SET USERNAME\r\nSecure: %s\r\nPassword: %s\r\nLength: %d\r\n\r\n%s"; // 0-体部是否加密,
																															// 1-体部是否包含密码,
																															// 2-体部长度,
																															// 3-用户名(和密码)
		public static final String SET_MSGKEY_ACK = "SET MSGKEY\r\n\r\n"; // 不需要体部
		public static final String SET_ALIVEINT_ACK = "SET ALIVEINT\r\n\r\n"; // 不需要体部
		public static final String PUSH_MSG_ACK = "PUSH MSG\r\n\r\n"; // 不需要体部
		public static final String SET_ALIVE_REQ = "SET ALIVE\r\n\r\n"; // 不需要体部

		// 错误消息
		public static final String INVALID_ACTION_LINE = "Invalid aciton line";
		public static final String INVALID_FIELD_LINE = "Invalid field line";
		public static final String INVALID_LENGTH_VALUE_MSG = "Invalid length value";

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

		private boolean clientLogon = false;
		private boolean invalidAppInfo = false;
		private boolean invalidClientInfo = false;

		private Calendar connectedTime;
		private Calendar lastActiveTime;
		private Calendar serverActiveTime;

		private SocketClientThread() {

		}

		@Override
		public void run() {
			Socket socket = null;
			byte buffer[] = new byte[MAX_SOCKET_BUF];

			boolean networkOk = true;
			socket = null;
			InputStream in = null;
			OutputStream out = null;
			reconnect: while (!exitNow) {
				if (in != null) {
					// 关闭已有的流(同时关闭套接字)
					try {
						if (in != null)
							in.close();
					} catch (IOException ee) {
						ee.printStackTrace();
					}
					in = null;
				}
				if (!isNetworkAvailable()) {
					if (networkOk) {
						showLog("网络不可用");
						networkOk = false;
					}
					waitForReconnect();
					continue reconnect;
				}
				networkOk = true;
				showLog("连接服务器 " + loginParams.getServerHost() + "["
						+ loginParams.getServerPort() + "]...");
				clientLogon = false;
				while (!exitNow) {
					// 创建新的套接字
					socket = new Socket();
					// 尝试连接
					try {
						socket.connect(
								new InetSocketAddress(loginParams
										.getServerHost(), loginParams
										.getServerPort()), networkParams
										.getConnectTimeout());
						socket.setSoTimeout(networkParams.getReadTimeout()); // 设置读超时(ms)
						socket.setKeepAlive(true);
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
						waitForReconnect();
					}
				}
				if (exitNow) {
					break;
				}
				connectedTime = Calendar.getInstance();
				showLog("已连接到服务器");

				waitData: while (!exitNow) {
					// 等待来自服务器的消息
					int byteCount;
					try {
						byteCount = in.read(buffer);
						if (byteCount == -1) {
							// 遇到EOF
							showLog("连接已中断");
							waitForReconnect();
							continue reconnect;
						} else if (byteCount == 0) {
							continue waitData;
						}
					} catch (SocketTimeoutException e) {
						// 读取失败
						Calendar now = Calendar.getInstance();
						if (clientLogon) {
							// 已登录，必要时发送心跳信号
							long diff1 = now.getTimeInMillis()
									- lastActiveTime.getTimeInMillis();
							if (diff1 >= keepAliveInterval / 2) {
								showLog("发送心跳信号...");
								try {
									socket.getOutputStream().write(
											SET_ALIVE_REQ.getBytes("UTF-8"));
								} catch (UnsupportedEncodingException ee) {
									// impossible!
									ee.printStackTrace();
								} catch (IOException ee) {
									showLog("发送心跳信号不成功: " + ee.getMessage());
									waitForReconnect();
									continue reconnect;
								}
								lastActiveTime = Calendar.getInstance();
							}

							//
							long diff2 = now.getTimeInMillis()
									- serverActiveTime.getTimeInMillis();
							if (diff2 >= keepAliveInterval) {
								showLog("服务器不可用");
								waitForReconnect();
								continue reconnect;
							}

						} else /* if (clientLogining) */{
							// 正在登录，检测登录超时
							long diff = now.getTimeInMillis()
									- connectedTime.getTimeInMillis();
							if (diff >= networkParams.getLoginTimeout()) {
								showLog("登录超时");
								waitForReconnect();
								continue reconnect;
							}
						}
						continue waitData;
					} catch (IOException e) {
						// 网络错误
						showLog("接收失败: " + e.getMessage());
						waitForReconnect();
						continue reconnect;
					}
					// 处理来自服务器的数据
					serverActiveTime = Calendar.getInstance();
					byte[] data = new byte[byteCount];
					for (int i = 0; i < byteCount; i++) {
						data[i] = buffer[i];
					}
					try {
						handleServerData(data, out);
					} catch (Exception e) {
						showLog("发生异常: " + e.getMessage());
						waitForReconnect();
						continue reconnect;
					}
					if (clientLogon) {
						// 已登录
						lastActiveTime = Calendar.getInstance();
					} else {
						// 正登录
						if (invalidAppInfo || invalidClientInfo) {
							// 应用或客户信息无效
							break reconnect;
						}
					}
				}
			}

			// 关闭已有的流(同时关闭套接字)
			try {
				if (in != null)
					in.close();
			} catch (IOException ee) {
				ee.printStackTrace();
			}
			in = null;
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
							out.write(String.format(CLOSE_CONN_RES,
									INVALID_ACTION_LINE.length(),
									INVALID_ACTION_LINE).getBytes("UTF-8"));
							return;
						}

						action = starr[0].trim().toUpperCase(); // 动作
						target = starr[1].trim().toUpperCase(); // 目标

						actionLineFound = true;
					} else if (!line.equals("")) {
						// 属性行
						String[] starr = line.split("\\s*:\\s*");
						if (starr.length != 2) {
							// 格式不对
							out.write(String.format(CLOSE_CONN_RES,
									INVALID_FIELD_LINE.length(),
									INVALID_FIELD_LINE).getBytes("UTF-8"));
							return;
						}

						String name = starr[0].trim().toUpperCase(); // 名字
						String value = starr[1].trim().toUpperCase(); // 值
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
						;
					}
					if (tmpBodyLength < 0) {
						out.write(String.format(CLOSE_CONN_RES,
								INVALID_LENGTH_VALUE_MSG.length(),
								INVALID_LENGTH_VALUE_MSG).getBytes("UTF-8"));
						return;
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
				// 准备处理下一个报文
				waitForHead = true;
				actionLineFound = false;
				//
				action = target = "";
				fields.clear();
				bodyByteLength = false;
				bodyLength = 0;
				body = "";
				bodyBytes = 0;
				unhandledInput = new byte[0];
			}
		}

		private void handlePacket(OutputStream out, String aciton,
				String target, Map<String, String> field, String body)
				throws Exception {
			if (action.equals("GET") && target.equals("APPID")) {
				// 应用认证请求，发送当前应用ID及密码
				showLog("接收到应用认证请求，发送应用信息...");
				String password = Crypt.md5(appParams.getAppPassword());
				out.write(String.format(GET_APPID_RES,
						appParams.getAppId().length() + 1 + password.length(),
						appParams.getAppId(), password).getBytes("UTF-8"));
			} else if (action.equals("SET") && target.equals("APPID")) {
				// 应用认证回复
				boolean appOk = (fields.get(FIELD_ACTION_SUCCESS.toUpperCase())
						.toUpperCase().equals("TRUE"));
				if (appOk) {
					showLog("应用认证通过");
				} else {
					showLog("应用认证失败: " + body);
					invalidAppInfo = true;
				}
			} else if (action.equals("GET") && target.equals("USERNAME")) {
				// 用户认证请求，发送用户名及密码
				showLog("接收到用户认证请求，发送用户信息...");
				if (fields.get(FIELD_LOGIN_SECURE.toUpperCase()).toUpperCase()
						.equals("TRUE")) {
					// 安全登录
					if (fields.get(FIELD_LOGIN_PASSWORD.toUpperCase())
							.toUpperCase().equals("TRUE")) {
						// 需要登录密码
						String password = Crypt.md5(loginParams
								.getClientPassword());
						String ss = Crypt
								.rsaEncrypt(
										appParams.getLoginProtectKey(),
										(loginParams.getClientPassword() + "," + password));
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
					showLog("用户认证通过");
				} else {
					showLog("用户认证失败: " + body);
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
				showLog("接收到消息密钥");
			} else if (action.equals("SET") && target.equals("ALIVEINT")) {
				// 设置心跳周期
				keepAliveInterval = Integer.parseInt(body);
				out.write(String.format(SET_ALIVEINT_ACK).getBytes("UTF-8"));
				showLog("接收到心跳周期(ms): " + keepAliveInterval);

				clientLogon = true;
				showLog("登录成功");
			} else if (action.equals("SET") && target.equals("ALIVE")) {
				// 收到心跳回复信号
				showLog("接收到心跳回复包");
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
				showNotification(msg);
			} else if (action.equals("CLOSE") && target.equals("CONN")) {
				// 服务器主动断开连接
				throw new Exception("服务器主动断开连接: " + body);
			} else {
				// 无法理解的动作
				throw new Exception("未知动作: " + action + " " + target);
			}
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
