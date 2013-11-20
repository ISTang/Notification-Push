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

		// 注册广播接收器
		MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.tpsoft.pushnotification.NotifyPushService");
		context.registerReceiver(myBroadcastReceiver, filter);
	}

	/**
	 * 添加侦听器
	 * 
	 * @param listener
	 *            侦听器
	 */
	public void addListener(MessageTransceiverListener listener) {
		listeners.add(listener);
	}

	/**
	 * 删除侦听器
	 * 
	 * @param listener
	 *            侦听器
	 */
	public void removeListener(MessageTransceiverListener listener) {
		listeners.remove(listener);
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
			for (MessageTransceiverListener listener : listeners)
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
			for (MessageTransceiverListener listener : listeners)
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

	/**
	 * 查询公众号
	 * 
	 * @param queryId
	 *            查询标识[回传用]
	 * @param condition
	 *            查询条件:公众号(支持模糊匹配)
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
	 * 关注公众号
	 * 
	 * @param account
	 *            公众号
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
	 * 取消关注公众号
	 * 
	 * @param account
	 *            公众号
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
	 * 获取已关注公众号
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
				// 需要关注...
				String action = intent.getStringExtra("action");
				if (action.equals("notify")) {
					// 新消息等通知
					String type = intent.getStringExtra("type");
					if (NotifyPushService.NOTIFICATION_MESSAGE.equals(type)) {
						// 新消息
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
						// 公众号信息列表通知
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
						// 已关注公众号信息列表通知
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
					// 日志通知
					int type = intent.getIntExtra("type", 0);
					int code = intent.getIntExtra("code", 0);
					String params = intent.getStringExtra("params");
					switch (type) {
					case NotifyPushService.LOG_CONNECT: // 连接:
						switch (code) {
						case NotifyPushService.STATUS_CONNECT_CONNECTING: // 连接服务器...
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(1, "连接服务器 "
										+ params + "...");
							break;
						case NotifyPushService.STATUS_CONNECT_CONNECTED: // 已经连接到服务器
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(2, "已连接到服务器。");
							break;
						case NotifyPushService.STATUS_CONNECT_APP_CERTIFICATING: // 应用认证...
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(3,
										"校验应用ID和接入密码...");
							break;
						case NotifyPushService.STATUS_CONNECT_APP_CERTIFICATED: // 应用认证通过
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(4, "应用认证通过。");
							break;
						case NotifyPushService.STATUS_CONNECT_USER_CERTIFICATING: // 用户认证...
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(5, "校验用户名和密码...");
							break;
						case NotifyPushService.STATUS_CONNECT_USER_CERTIFICATED: // 用户认证通过
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(6, "用户认证通过。");
							break;
						case NotifyPushService.STATUS_CONNECT_MSGKEY_RECEIVED: // 收到消息密钥
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(7, "收到消息密钥。");
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVEINTERVAL_RECEIVED: // 收到心跳周期
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(8, "收到心跳周期: "
										+ Integer.parseInt(params) / 1000
										+ "秒。");
							break;
						case NotifyPushService.STATUS_CONNECT_LOGON: // 登录成功
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(0, "登录成功。");
							clientLogon = true;
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVE: // 发送心跳信号
							Log.d(TAG_APILOG, "发送心跳信号...");
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVE_REPLIED: // 收到心跳回复信号
							Log.d(TAG_APILOG, "收到心跳回复信号");
							break;
						case NotifyPushService.ERROR_CONNECT_NETWORK_UNAVAILABLE: // 网络不可用
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(-1, "网络不可用！");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_BROKEN: // 连接已中断
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(-2, "连接已中断！");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_SERVER_UNAVAILABLE: // 服务器不可用
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(-3, "服务器不可用！");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_LOGIN_TIMEOUT: // 登录超时
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(-4, "登录超时！");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_IO_FAULT: // 网络IO故障
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(-5, "网络IO故障！");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_APP_CERTIFICATE: // 应用认证失败
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(-6, "应用认证失败！");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_USER_CERTIFICATE: // 用户认证失败
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(-7, "用户认证失败！");
							clientLogon = false;
							break;
						case NotifyPushService.ERROR_CONNECT_SERVER: // 服务器错误
							for (MessageTransceiverListener listener : listeners)
								listener.onLoginStatus(-8, "服务器错误！");
							clientLogon = false;
							break;
						default:
							break;
						}
						break;
					case NotifyPushService.LOG_SENDMSG: // 发送消息:
						switch (code) {
						case NotifyPushService.STATUS_SENDMSG_SUBMIT: // 提交消息
							for (MessageTransceiverListener listener : listeners)
								listener.onMessageSendStatus(
										Integer.parseInt(params), 1, "提交...");
							break;
						case NotifyPushService.STATUS_SENDMSG_SUBMITTED: // 已提交消息
							for (MessageTransceiverListener listener : listeners)
								listener.onMessageSendStatus(
										Integer.parseInt(params), 2, "已提交");
							break;
						case NotifyPushService.STATUS_SENDMSG_OK: // 收到消息确认
							for (MessageTransceiverListener listener : listeners)
								listener.onMessageSendStatus(
										Integer.parseInt(params), 3, "已确认");
							break;
						case NotifyPushService.ERROR_SENDMSG_NOT_LOGON: // 尚未登录成功
							for (MessageTransceiverListener listener : listeners)
								listener.onMessageSendStatus(
										Integer.parseInt(params), -1, "未登录");
							break;
						case NotifyPushService.ERROR_SENDMSG_DATA: // 消息数据错误
							for (MessageTransceiverListener listener : listeners)
								listener.onMessageSendStatus(
										Integer.parseInt(params), -2, "数据错误！");
							break;
						case NotifyPushService.ERROR_SENDMSG_SUBMIT: // 提交消息失败
							for (MessageTransceiverListener listener : listeners)
								listener.onMessageSendStatus(
										Integer.parseInt(params), -3, "提交失败！");
							break;
						case NotifyPushService.ERROR_SENDMSG_FAILED: // 发送消息失败
							int pos = params.indexOf(":");
							int msgId = Integer.parseInt(params.substring(0,
									pos));
							String err = params.substring(pos + 1);
							String errcode = err.substring(0, err.indexOf(","));
							String errmsg = err.substring(err.indexOf(",") + 1);
							for (MessageTransceiverListener listener : listeners)
								listener.onMessageSendStatus(msgId, -4,
										"发送失败(#" + errcode + ":" + errmsg
												+ ")！");
							break;
						default:
							break;
						}
						break;
					case NotifyPushService.LOG_QUERYPUBLIC: // 查询公众号:
						switch (code) {
						case NotifyPushService.STATUS_QUERYPUBLIC_SUBMIT: // 提交请求
							break;
						case NotifyPushService.STATUS_QUERYPUBLIC_SUBMITTED: // 请求已提交
							break;
						case NotifyPushService.STATUS_QUERYPUBLIC_OK: // 操作成功
							break;
						case NotifyPushService.ERROR_QUERYPUBLIC_NOT_LOGON: // 尚未登录成功
							break;
						case NotifyPushService.ERROR_QUERYPUBLIC_SUBMIT: // 提交请求失败
							break;
						case NotifyPushService.ERROR_QUERYPUBLIC_FAILED: // 操作失败
							break;
						default:
							break;
						}
						break;
					case NotifyPushService.LOG_FOLLOWPUBLIC: // 关注公众号:
						switch (code) {
						case NotifyPushService.STATUS_FOLLOWPUBLIC_SUBMIT: // 提交请求
							break;
						case NotifyPushService.STATUS_FOLLOWPUBLIC_SUBMITTED: // 请求已提交
							break;
						case NotifyPushService.STATUS_FOLLOWPUBLIC_OK: // 操作成功
							for (MessageTransceiverListener listener : listeners)
								listener.onPublicAccountFollowed(params);
							break;
						case NotifyPushService.ERROR_FOLLOWPUBLIC_NOT_LOGON: // 尚未登录成功
							break;
						case NotifyPushService.ERROR_FOLLOWPUBLIC_SUBMIT: // 提交请求失败
							break;
						case NotifyPushService.ERROR_FOLLOWPUBLIC_FAILED: // 操作失败
							break;
						default:
							break;
						}
						break;
					case NotifyPushService.LOG_UNFOLLOWPUBLIC: // 取消关注公众号:
						switch (code) {
						case NotifyPushService.STATUS_UNFOLLOWPUBLIC_SUBMIT: // 提交请求
							break;
						case NotifyPushService.STATUS_UNFOLLOWPUBLIC_SUBMITTED: // 请求已提交
							break;
						case NotifyPushService.STATUS_UNFOLLOWPUBLIC_OK: // 操作成功
							for (MessageTransceiverListener listener : listeners)
								listener.onPublicAccountUnfollowed(params);
							break;
						case NotifyPushService.ERROR_UNFOLLOWPUBLIC_NOT_LOGON: // 尚未登录成功
							break;
						case NotifyPushService.ERROR_UNFOLLOWPUBLIC_SUBMIT: // 提交请求失败
							break;
						case NotifyPushService.ERROR_UNFOLLOWPUBLIC_FAILED: // 操作失败
							break;
						default:
							break;
						}
						break;
					case NotifyPushService.LOG_GETFOLLOWED: // 获取已关注的公众号:
						switch (code) {
						case NotifyPushService.STATUS_GETFOLLOWED_SUBMIT: // 提交请求
							break;
						case NotifyPushService.STATUS_GETFOLLOWED_SUBMITTED: // 请求已提交
							break;
						case NotifyPushService.STATUS_GETFOLLOWED_OK: // 操作成功
							break;
						case NotifyPushService.ERROR_GETFOLLOWED_NOT_LOGON: // 尚未登录成功
							break;
						case NotifyPushService.ERROR_GETFOLLOWED_SUBMIT: // 提交请求失败
							break;
						case NotifyPushService.ERROR_GETFOLLOWED_FAILED: // 操作失败
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
					for (MessageTransceiverListener listener : listeners)
						listener.onTransceiverStatus(clientStarted);
				} else if (action.equals("logining")) {
					// 登录状态通知
					boolean logining = intent
							.getBooleanExtra("logining", false);
					for (MessageTransceiverListener listener : listeners)
						listener.onLogining(logining);
				} else {
					// 未知动作
				}
			}
		}
	}
}
