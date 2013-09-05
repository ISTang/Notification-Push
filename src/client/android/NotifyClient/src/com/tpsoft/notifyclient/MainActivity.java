package com.tpsoft.notifyclient;

import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.tpsoft.notifyclient.utils.HttpDownloader;
import com.tpsoft.notifyclient.utils.MessageDialog;
import com.tpsoft.pushnotification.model.AppParams;
import com.tpsoft.pushnotification.model.LoginParams;
import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.pushnotification.model.NetworkParams;
import com.tpsoft.pushnotification.service.NotifyPushService;

@SuppressWarnings("deprecation")
@SuppressLint("SimpleDateFormat")
public class MainActivity extends TabActivity {

	private class MyBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					"com.tpsoft.pushnotification.NotifyPushService")) {
				String action = intent.getStringExtra("action");
				if (action.equals("status")) {
					boolean receiverStarted = intent.getBooleanExtra("started",
							false);
					MyApplicationClass.clientStarted = receiverStarted;
					showLog(getText(
							receiverStarted ? R.string.receiver_started
									: R.string.receiver_stopped).toString());
					Toast.makeText(
							MainActivity.this,
							receiverStarted ? R.string.receiver_started
									: R.string.receiver_stopped,
							Toast.LENGTH_SHORT).show();
				} else if (action.equals("logining")) {
					// TODO 响应登录状态变化
					boolean logining = intent
							.getBooleanExtra("logining", false);
					if (logining) {
						Toast.makeText(MainActivity.this, "正在登录...",
								Toast.LENGTH_SHORT).show();
					}
				} else if (action.equals("log")) {
					int type = intent.getIntExtra("type", 0);
					int code = intent.getIntExtra("code", 0);
					String params = intent.getStringExtra("params");
					switch (type) {
					case NotifyPushService.LOG_CONNECT: // 连接:
						switch (code) {
						case NotifyPushService.STATUS_CONNECT_CONNECTING: // 连接服务器...
							showLog("连接服务器 " + params + "...");
							break;
						case NotifyPushService.STATUS_CONNECT_CONNECTED: // 已经连接到服务器
							showLog("已连接到服务器。");
							break;
						case NotifyPushService.STATUS_CONNECT_APP_CERTIFICATING: // 应用认证...
							showLog("校验应用ID和接入密码...");
							break;
						case NotifyPushService.STATUS_CONNECT_APP_CERTIFICATED: // 应用认证通过
							showLog("应用认证通过。");
							break;
						case NotifyPushService.STATUS_CONNECT_USER_CERTIFICATING: // 用户认证...
							showLog("校验用户名和密码...");
							break;
						case NotifyPushService.STATUS_CONNECT_USER_CERTIFICATED: // 用户认证通过
							showLog("用户认证通过。");
							break;
						case NotifyPushService.STATUS_CONNECT_MSGKEY_RECEIVED: // 收到消息密钥
							showLog("收到消息密钥。");
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVEINTERVAL_RECEIVED: // 收到心跳周期
							showLog("收到心跳周期: " + params + "秒。");
							break;
						case NotifyPushService.STATUS_CONNECT_LOGON: // 登录成功
							showLog("登录成功。");
							Toast.makeText(MainActivity.this, "登录成功",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVE: // 发送心跳信号
							showLog("发送心跳信号...");
							break;
						case NotifyPushService.STATUS_CONNECT_KEEPALIVE_REPLIED: // 收到心跳回复信号
							showLog("收到心跳回复信号。");
							break;
						case NotifyPushService.ERROR_CONNECT_NETWORK_UNAVAILABLE: // 网络不可用
							showLog("网络不可用！");
							Toast.makeText(MainActivity.this, "网络不可用",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_BROKEN: // 连接已中断
							showLog("连接已中断！");
							Toast.makeText(MainActivity.this, "连接已中断",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_SERVER_UNAVAILABLE: // 服务器不可用
							showLog("服务器不可用！");
							Toast.makeText(MainActivity.this, "服务器不可用",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_LOGIN_TIMEOUT: // 登录超时
							showLog("登录超时！");
							Toast.makeText(MainActivity.this, "登录超时",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_IO_FAULT: // 网络IO故障
							showLog("网络IO故障！");
							Toast.makeText(MainActivity.this, "网络IO故障",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_APP_CERTIFICATE: // 应用认证失败
							showLog("应用认证失败！");
							Toast.makeText(MainActivity.this, "应用认证失败",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_USER_CERTIFICATE: // 用户认证失败
							showLog("用户认证失败！");
							Toast.makeText(MainActivity.this, "用户认证失败",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_CONNECT_SERVER: // 服务器错误
							showLog("服务器错误！");
							Toast.makeText(MainActivity.this, "服务器错误",
									Toast.LENGTH_SHORT).show();
							break;
						default:
							break;
						}
						break;
					case NotifyPushService.LOG_SENDMSG: // 发送消息:
						switch (code) {
						case NotifyPushService.STATUS_SENDMSG_SUBMIT: // 提交消息
							showLog("提交 #" + params + " 消息...");
							Toast.makeText(MainActivity.this, "提交消息...",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.STATUS_SENDMSG_SUBMITTED: // 已提交消息
							showLog("#" + params + " 消息已提交。");
							Toast.makeText(MainActivity.this, "消息已提交",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.STATUS_SENDMSG_OK: // 收到消息确认
							showLog("#" + params + " 消息已确认。");
							Toast.makeText(MainActivity.this, "消息已确认",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_SENDMSG_NOT_LOGON: // 尚未登录成功
							Toast.makeText(MainActivity.this, "尚未成功登录",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_SENDMSG_DATA: // 消息数据错误
							showLog("#" + params + " 消息数据错误！");
							break;
						case NotifyPushService.ERROR_SENDMSG_SUBMIT: // 提交消息失败
							showLog("#" + params + " 消息提交失败。");
							Toast.makeText(MainActivity.this, "消息提交失败",
									Toast.LENGTH_SHORT).show();
							break;
						case NotifyPushService.ERROR_SENDMSG_FAILED: // 发送消息失败
							int pos = params.indexOf(":");
							String msgId = params.substring(0, pos);
							String err = params.substring(pos + 1);
							String errmsg = err.substring(err.indexOf(",") + 1);
							showLog("#" + msgId + " 消息发送失败(" + errmsg + ")！");
							Toast.makeText(MainActivity.this, "消息发送失败",
									Toast.LENGTH_SHORT).show();
							break;
						default:
							break;
						}
						break;
					default:
						break;
					}
				} else if (action.equals("notify")) {
					showNotification(intent.getStringExtra("msgText"));
				} else {
					;
				}
			} else if (intent.getAction().equals(
					"com.tpsoft.notifyclient.utils.MessageDialog")) {
				String action = intent.getStringExtra("action");
				if (action.equals("popupClosed")) {
					messagePopupClosed = true;
				} else {
					;
				}
			}
		}
	}

	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"HH:mm:ss", Locale.CHINESE);

	private static final int MAX_MSG_COUNT = 20;
	private static final int MAX_LOG_COUNT = 200;
	private LinearLayout msg;
	private TextView logger;
	private int msgCount = 0;
	private boolean useMsgColor1 = true;
	private int logCount = 0;

	private NotificationManager mNM;
	private MyBroadcastReceiver myBroadcastReceiver = null;
	private boolean messagePopupClosed = true;

	private TabHost tabHost;

	private HttpDownloader httpDownloader = new HttpDownloader();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 设置外观
		setTabs();

		// 准备通知
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// 初始化消息和日志控件
		msg = (LinearLayout) findViewById(R.id.msg);
		logger = (TextView) findViewById(R.id.log);

		// 恢复消息显示
		Resources res = getResources();
		for (MyMessage message : MyApplicationClass.savedMsgs) {
			// 获取图片URL
			String imageUrl = null;
			if (message.getAttachments() != null) {
				for (MyMessage.Attachment attachment : message.getAttachments()) {
					if (attachment.getType().matches("image/.*")) {
						imageUrl = attachment.getUrl();
						break;
					}
				}
			}
			msg.addView(
					makeMessageView(
							message,
							(imageUrl != null ? MyApplicationClass.savedImages
									.get(imageUrl) : null), res), 0);
		}
		msgCount = MyApplicationClass.savedMsgs.size();

		if (myBroadcastReceiver == null) {
			// 准备与后台服务通信
			myBroadcastReceiver = new MyBroadcastReceiver();
			try {
				unregisterReceiver(myBroadcastReceiver);
			} catch (Exception e) {
				;
			}
			//
			IntentFilter filter = new IntentFilter();
			filter.addAction("com.tpsoft.pushnotification.NotifyPushService");
			filter.addAction("com.tpsoft.notifyclient.utils.MessageDialog");
			registerReceiver(myBroadcastReceiver, filter);

			// 启动消息接收器
			Handler handler = new Handler();
			Runnable runnable = new Runnable() {
				public void run() {
					startMessageReceiver();
				}
			};
			handler.postDelayed(runnable, 1000); // 启动Timer
		}
	}

	@Override
	protected void onDestroy() {
		mNM.cancel(R.id.app_notification_id);

		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (MyApplicationClass.clientStarted) {
			menu.findItem(R.id.menu_start).setEnabled(false);
			menu.findItem(R.id.menu_stop).setEnabled(true);
			menu.findItem(R.id.menu_sendmsg).setEnabled(true);
		} else {
			menu.findItem(R.id.menu_start).setEnabled(true);
			menu.findItem(R.id.menu_stop).setEnabled(false);
			menu.findItem(R.id.menu_sendmsg).setEnabled(false);
		}
		menu.findItem(R.id.menu_settings).setEnabled(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_start:
			// 启动消息接收器
			showLog(getText(R.string.receiver_starting).toString());
			startMessageReceiver();
			break;
		case R.id.menu_stop:
			// 停止消息接收器
			stopMessageReceiver();
			break;
		case R.id.menu_sendmsg:
			// 发送消息
			sendMessage();
			break;
		case R.id.menu_settings:
			// 打开设置界面
			startActivity(new Intent(this, SettingsActivity.class));
			break;
		}
		return false;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int keyCode = event.getKeyCode();
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK: {
			if (event.isLongPress()) {
				// 退出程序
				mNM.cancel(R.id.app_notification_id);
				//
				stopService(new Intent(MainActivity.this,
						NotifyPushService.class));
				unregisterReceiver(myBroadcastReceiver);
				//
				System.exit(0);
				return true;
			} else {
				// 提示退出
				Toast.makeText(this, getText(R.string.app_exit),
						Toast.LENGTH_SHORT).show();
				boolean flag = false;
				return flag;
			}
		}
		}
		return super.dispatchKeyEvent(event);
	}

	private void setTabs() {
		tabHost = getTabHost();

		addTab(R.string.tab_1, R.drawable.message);
		addTab(R.string.tab_2, R.drawable.log);
	}

	private void addTab(int labelId, int drawableId) {
		TabHost.TabSpec spec = tabHost.newTabSpec("tab" + labelId);

		View tabIndicator = LayoutInflater.from(this).inflate(
				R.layout.tab_indicator, getTabWidget(), false);

		TextView title = (TextView) tabIndicator.findViewById(R.id.title);
		title.setText(labelId);
		ImageView icon = (ImageView) tabIndicator.findViewById(R.id.icon);
		icon.setImageResource(drawableId);

		spec.setIndicator(tabIndicator);
		switch (labelId) {
		case R.string.tab_1:
			spec.setContent(R.id.msgContainer);
			break;
		case R.string.tab_2:
			spec.setContent(R.id.logContainer);
			break;
		}
		tabHost.addTab(spec);

	}

	private void startMessageReceiver() {
		if (MyApplicationClass.clientStarted)
			return;

		showLog(getText(R.string.receiver_starting).toString());
		Toast.makeText(this, getText(R.string.receiver_starting),
				Toast.LENGTH_SHORT).show();

		// 获取最新设置
		MyApplicationClass myApp = (MyApplicationClass) getApplication();
		myApp.loadUserSettings();

		AppParams appParams = new AppParams(MyApplicationClass.APP_ID,
				MyApplicationClass.APP_PASSWORD,
				MyApplicationClass.LOGIN_PROTECT_KEY);
		LoginParams loginParams = new LoginParams(
				MyApplicationClass.userSettings.getServerHost(),
				MyApplicationClass.userSettings.getServerPort(),
				MyApplicationClass.userSettings.getClientId(),
				MyApplicationClass.userSettings.getClientPassword());
		NetworkParams networkParams = new NetworkParams();

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
		sendBroadcast(serviceIntent);
	}

	private void stopMessageReceiver() {
		if (!MyApplicationClass.clientStarted)
			return;

		showLog(getText(R.string.receiver_stopping).toString());
		Toast.makeText(this, getText(R.string.receiver_stopping),
				Toast.LENGTH_SHORT).show();
		Intent serviceIntent = new Intent();
		serviceIntent
				.setAction("com.tpsoft.pushnotification.ServiceController");
		serviceIntent.putExtra("command", "stop");
		sendBroadcast(serviceIntent);
	}

	private void sendMessage() {
		if (!MyApplicationClass.clientStarted)
			return;
		startActivity(new Intent(this, SendMessageActivity.class));
	}

	private void showNotification(String msgText) {
		showLog(getText(R.string.msg_received).toString());

		// 解析消息文本
		final MyMessage message;
		try {
			message = MyMessage.extractMessage(msgText);
		} catch (Exception e) {
			showLog("无法解析消息: " + e.getMessage());
			return;
		}

		// 获取图片URL及文件名
		String attachmentFilename = null;
		String attachmentUrl = null;
		if (message.getAttachments() != null) {
			for (MyMessage.Attachment attachment : message.getAttachments()) {
				if (attachment.getType().matches("image/.*")) {
					attachmentFilename = attachment.getFilename();
					attachmentUrl = attachment.getUrl();
					break;
				}
			}
		}

		// 输出消息
		if (MyApplicationClass.userSettings.isEmulateSms()) {

			// 格式化消息
			String formattedMessage = String.format("%s %s: %s%s%s",
					MyMessage.dateFormat.format(message.getGenerateTime()),
					(message.getTitle() == null ? "---" : message.getTitle()),
					message.getBody(), attachmentUrl != null ? "["
							+ attachmentUrl + "]" : "", !message.getUrl()
							.equals("") ? "[" + message.getUrl() + "]" : "");

			// 模拟短信接收
			writeSms(formattedMessage);

			Notification notification = new Notification(
					android.R.drawable.ic_dialog_email, formattedMessage,
					System.currentTimeMillis());
			Intent intent = new Intent("android.intent.action.MAIN");
			intent.setComponent(new ComponentName("com.android.mms",
					"com.android.mms.ui.ConversationList"));
			PendingIntent contentIntent = PendingIntent.getActivity(
					MainActivity.this, 0, intent,
					PendingIntent.FLAG_CANCEL_CURRENT);
			notification.setLatestEventInfo(this,
					MyApplicationClass.smsSenderName, formattedMessage,
					contentIntent);

			mNM.notify(R.id.app_notification_id, notification);
		}

		// 为消息对话框准备数据
		final Bundle msgParams = new Bundle();
		msgParams.putBoolean("alert", MyApplicationClass.ALERT_MSG);
		if (message.getTitle() != null && !message.getTitle().equals(""))
			msgParams.putString("title", message.getTitle());
		msgParams.putString("body", message.getBody());
		if (message.getUrl() != null && !message.getUrl().equals(""))
			msgParams.putString("url", message.getUrl());
		//
		final Intent messageDialogIntent = (messagePopupClosed ? new Intent(
				MainActivity.this, MessageDialog.class) : new Intent());
		if (attachmentUrl != null
				&& (MyApplicationClass.mExternalStorageAvailable && MyApplicationClass.mExternalStorageWriteable)) {
			final String imageFilename = new Date().getTime() + "_"
					+ attachmentFilename;
			final String imageUrl = attachmentUrl;
			//
			String sdcardPath = Environment.getExternalStorageDirectory()
					.getPath();
			final String imageFilepath = sdcardPath + "/tmp/" + imageFilename;
			final Handler handler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					switch (msg.what) {
					case 0:
						if (msg.arg1 == 0) {
							msgParams.putBoolean("showPic", true);
							msgParams.putString("imageFilepath", imageFilepath);
							MyApplicationClass.savedImages.put(imageUrl,
									imageFilepath);
						} else {
							msgParams.putBoolean("showPic", false);
						}
						// 添加到消息列表
						showMsg(message, msg.arg1 == 0 ? imageFilepath : null);
						// 显示/更新消息对话框
						messageDialogIntent.putExtras(msgParams);
						if (messagePopupClosed) {
							// 声音提醒
							if (MyApplicationClass.userSettings.isPlaySound()) {
								MyApplicationClass.playSoundPool
										.play(MyApplicationClass.ALERT_MSG ? MyApplicationClass.ALERT_SOUND
												: MyApplicationClass.INFO_SOUND,
												0);
							}
							// 显示消息对话框
							startActivity(messageDialogIntent);
							messagePopupClosed = false;
						} else {
							// 更新消息对话框
							messageDialogIntent
									.setAction("com.tpsoft.notifyclient.MainActivity");
							messageDialogIntent.putExtra("action", "update");
							sendBroadcast(messageDialogIntent);
						}
						break;
					default:
						super.handleMessage(msg);
					}
				}
			};

			new Thread() {
				public void run() {
					int errCode = 0;
					if (!MyApplicationClass.savedImages.containsKey(imageUrl)) {
						errCode = httpDownloader.downFile(imageUrl, "tmp",
								imageFilename, true);
					}
					Message msg = new Message();
					msg.what = 0;
					msg.arg1 = errCode;
					handler.sendMessage(msg);
				}
			}.start();
		} else {
			// 添加到消息列表
			showMsg(message, null);
			// 显示/更新消息对话框
			msgParams.putBoolean("showPic", false);
			messageDialogIntent.putExtras(msgParams);
			if (messagePopupClosed) {
				// 声音提醒
				if (MyApplicationClass.userSettings.isPlaySound()) {
					MyApplicationClass.playSoundPool
							.play(MyApplicationClass.ALERT_MSG ? MyApplicationClass.ALERT_SOUND
									: MyApplicationClass.INFO_SOUND, 0);
				}
				// 显示消息对话框
				startActivity(messageDialogIntent);
				messagePopupClosed = false;
			} else {
				// 更新消息对话框
				messageDialogIntent
						.setAction("com.tpsoft.notifyclient.MainActivity");
				messageDialogIntent.putExtra("action", "update");
				sendBroadcast(messageDialogIntent);
			}
		}
	}

	private void showMsg(MyMessage message, String imageFilepath) {
		Resources res = getResources();

		// 生成消息界面
		View view = makeMessageView(message, imageFilepath, res);
		if (msgCount < MAX_MSG_COUNT) {
			msg.addView(view, 0);
			msgCount++;
		} else {
			msg.removeViewAt(msg.getChildCount() - 1);
			msg.addView(view, 0);
		}

		// 保存消息界面
		MyApplicationClass.savedMsgs.add(message);
		if (MyApplicationClass.savedMsgs.size() == MAX_MSG_COUNT) {
			MyApplicationClass.savedMsgs.remove(0);
		}

		useMsgColor1 = !useMsgColor1;
	}

	private void showLog(String logText) {
		String log = "[" + sdf.format(new Date()) + "] " + logText;
		if (logCount < MAX_LOG_COUNT) {
			logger.setText(log + "\r\n" + logger.getText());
			logCount++;
		} else {
			logger.setText(log);
			logCount = 1;
		}
	}

	private void writeSms(String message) {
		ContentValues values = new ContentValues();
		values.put("address", MyApplicationClass.SMS_SENDER_NUMBER);
		values.put("body", message);
		getContentResolver().insert(Uri.parse("content://sms/inbox"), values);
	}

	@SuppressLint("ResourceAsColor")
	private View makeMessageView(MyMessage message, String imageFilepath,
			Resources res) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View listItemView = inflater.inflate(R.layout.message_list_item,
				(ViewGroup) findViewById(R.id.message));
		ImageView msgAttachmentView = (ImageView) listItemView
				.findViewById(R.id.msgAttachment);
		Bitmap bitmap = null;
		if (imageFilepath != null) {
			try {
				FileInputStream fis = new FileInputStream(imageFilepath);
				bitmap = BitmapFactory.decodeStream(fis);
				fis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		msgAttachmentView.setImageBitmap(bitmap);
		//
		TextView msgTitleView = (TextView) listItemView
				.findViewById(R.id.msgTitle);
		msgTitleView.setText(message.getTitle() != null ? message.getTitle()
				: "");
		//
		TextView msgBodyView = (TextView) listItemView
				.findViewById(R.id.msgBody);
		msgBodyView.setText(message.getBody());
		//
		TextView msgTimeView = (TextView) listItemView
				.findViewById(R.id.msgTime);
		msgTimeView.setText(makeTimeString(message.getGenerateTime()));
		return listItemView;
	}

	private String makeTimeString(Date time) {
		return sdf.format(time);
	}
}
