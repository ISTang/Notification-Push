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

		// 注册广播接收器
		MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.tpsoft.pushnotification.NotifyPushService");
		context.registerReceiver(myBroadcastReceiver, filter);
	}

	/**
	 * 启动消息收发器
	 * 
	 * @param appParams
	 *            应用参数
	 * @param loginParams
	 *            登录参数
	 * @param networkParams
	 *            网络参数
	 */
	public void startMessageTransceiver(AppParams appParams,
			LoginParams loginParams, NetworkParams networkParams) {
		if (clientStarted) {
			listener.onTransceiverStatus(true);
			return; // 已经启动
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
		context.sendBroadcast(serviceIntent); // 发送广播
	}

	/**
	 * 停止消息收发器
	 */
	public void stopMessageTransceiver() {
		if (!clientStarted) {
			listener.onTransceiverStatus(false);
			return; // 已经停止
		}

		Intent serviceIntent = new Intent();
		serviceIntent
				.setAction("com.tpsoft.pushnotification.ServiceController");
		serviceIntent.putExtra("command", "stop");
		context.sendBroadcast(serviceIntent); // 发送广播
	}

	/**
	 * 发送消息
	 * 
	 * @param msgId
	 *            消息ID
	 * @param msg
	 *            消息
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
				// 需要关注...
				String action = intent.getStringExtra("action");
				if (action.equals("notify")) {
					// 新消息通知
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
					// 日志通知
					int type = intent.getIntExtra("type", 0);
					int code = intent.getIntExtra("code", 0);
					String params = intent.getStringExtra("params");
					switch (type) {
					case NotifyPushService.LOG_CONNECT: // 连接:
						switch (code) {
						case NotifyPushService.STATUS_CONNECT_CONNECTING: // 连接服务器...
							listener.onLoginStatus(true, 1, "连接服务器 " + params
									+ "...");
							break;
						case NotifyPushService.STATUS_CONNECT_CONNECTED: // 已经连接到服务器
							listener.onLoginStatus(true, 2, "已连接到服务器。");
							break;
						case NotifyPushService.STATUS_CONNECT_APP_CERTIFICATING: // 应用认证...
							listener.onLoginStatus(true, 3, "校验应用ID和接入密码...");
							break;
						case NotifyPushService.STATUS_CONNECT_APP_CERTIFICATED: // 应用认证通过
							listener.onLoginStatus(true, 4, "应用认证通过。");
							break;
						case NotifyPushService.STATUS_CONNECT_USER_CERTIFICATING: // 用户认证...
							listener.onLoginStatus(true, 5, "校验用户名和密码...");
							break;
						case NotifyPushService.STATUS_CONNECT_USER_CERTIFICATED: // 用户认证通过
							listener.onLoginStatus(true, 6, "用户认证通过。");
							break;
						case NotifyPushService.STATUS_CONNECT_MSGKEY_RECEIVED: // 收到消息密钥
							listener.onLoginStatus(true, 7, "收到消息密钥。");
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVEINTERVAL_RECEIVED: // 收到心跳周期
							listener.onLoginStatus(true, 8, "收到心跳周期: "
									+ Integer.parseInt(params) / 1000 + "秒。");
							break;
						case NotifyPushService.STATUS_CONNECT_LOGON: // 登录成功
							listener.onLoginStatus(false, 0, "登录成功。");
							clientLogon = true;
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVE: // 发送心跳信号
							Log.d(TAG_APILOG, "发送心跳信号...");
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVE_REPLIED: // 收到心跳回复信号
							Log.d(TAG_APILOG, "收到心跳回复信号");
							break;
						case NotifyPushService.ERROR_CONNECT_NETWORK_UNAVAILABLE: // 网络不可用
							listener.onLoginStatus(false, -1, "网络不可用！");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_BROKEN: // 连接已中断
							listener.onLoginStatus(false, -2, "连接已中断！");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_SERVER_UNAVAILABLE: // 服务器不可用
							listener.onLoginStatus(false, -3, "服务器不可用！");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_LOGIN_TIMEOUT: // 登录超时
							listener.onLoginStatus(false, -4, "登录超时！");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_IO_FAULT: // 网络IO故障
							listener.onLoginStatus(false, -5, "网络IO故障！");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_APP_CERTIFICATE: // 应用认证失败
							listener.onLoginStatus(false, -6, "应用认证失败！");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_USER_CERTIFICATE: // 用户认证失败
							listener.onLoginStatus(false, -7, "用户认证失败！");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_SERVER: // 服务器错误
							listener.onLoginStatus(false, -8, "服务器错误！");
							clientLogon = false;
							break;
						default:
							break;
						}
						break;
					case NotifyPushService.LOG_SENDMSG: // 发送消息:
						switch (code) {
						case NotifyPushService.STATUS_SENDMSG_SUBMIT: // 提交消息
							listener.onMessageStatus(Integer.parseInt(params),
									1, "提交...");
							break;
						case NotifyPushService.STATUS_SENDMSG_SUBMITTED: // 已提交消息
							listener.onMessageStatus(Integer.parseInt(params),
									2, "已提交");
							break;
						case NotifyPushService.STATUS_SENDMSG_OK: // 收到消息确认
							listener.onMessageStatus(Integer.parseInt(params),
									3, "已确认");
							break;
						case NotifyPushService.ERROR_SENDMSG_NOT_LOGON: // 尚未登录成功
							listener.onMessageStatus(Integer.parseInt(params),
									-1, "未登录");
							break;
						case NotifyPushService.ERROR_SENDMSG_DATA: // 消息数据错误
							listener.onMessageStatus(Integer.parseInt(params),
									-2, "数据错误！");
							break;
						case NotifyPushService.ERROR_SENDMSG_SUBMIT: // 提交消息失败
							listener.onMessageStatus(Integer.parseInt(params),
									-3, "提交失败！");
							break;
						case NotifyPushService.ERROR_SENDMSG_FAILED: // 发送消息失败
							int pos = params.indexOf(":");
							int msgId = Integer.parseInt(params.substring(0,
									pos));
							String err = params.substring(pos + 1);
							String errcode = err.substring(0, err.indexOf(","));
							String errmsg = err.substring(err.indexOf(",") + 1);
							listener.onMessageStatus(msgId, -4, "发送失败(#"
									+ errcode + ":" + errmsg + ")！");
							break;
						default:
							break;
						}
						break;
					default:
						break;
					}
				} else if (action.equals("status")) {
					// 消息接收器状态通知
					clientStarted = intent.getBooleanExtra("started", false);
					if (!clientStarted)
						clientLogon = false;
					listener.onTransceiverStatus(clientStarted);
				} else if (action.equals("logining")) {
					// 登录状态通知
					boolean logining = intent
							.getBooleanExtra("logining", false);
					listener.onLoginStatus(logining, 0, logining ? "正在登录..."
							: "登录结束。");
				} else {
					// 未知动作
				}
			}
		}
	}
}
