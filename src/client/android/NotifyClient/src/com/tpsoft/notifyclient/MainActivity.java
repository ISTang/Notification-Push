package com.tpsoft.notifyclient;

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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.tpsoft.notifyclient.utils.MessageDialog;
import com.tpsoft.pushnotification.model.AppParams;
import com.tpsoft.pushnotification.model.LoginParams;
import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.pushnotification.model.NetworkParams;
import com.tpsoft.pushnotification.service.NotifyPushService;

@SuppressWarnings("deprecation")
@SuppressLint("SimpleDateFormat")
public class MainActivity extends TabActivity {

	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"HH:mm:ss", Locale.CHINESE);

	private static final int MAX_MSG_COUNT = 20;
	private static final int MAX_LOG_COUNT = 100;
	private LinearLayout msg;
	private TextView logger;
	private int msgCount = 0;
	private boolean useMsgColor1 = true;
	private int logCount = 0;

	private NotificationManager mNM;
	private MyBroadcastReceiver myBroadcastReceiver = null;

	private TabHost tabHost;

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

		if (myBroadcastReceiver == null) {
			// 准备与后台服务通信
			myBroadcastReceiver = new MyBroadcastReceiver();
			IntentFilter filter = new IntentFilter();
			filter.addAction("com.tpsoft.pushnotification.NotifyPushService");
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
		} else {
			menu.findItem(R.id.menu_start).setEnabled(true);
			menu.findItem(R.id.menu_stop).setEnabled(false);
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

	private class MyBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getStringExtra("action");
			if (action.equals("notify")) {
				showNotification(intent.getStringExtra("msgText"));
			} else if (action.equals("log")) {
				showLog(intent.getStringExtra("logText"));
			} else if (action.equals("status")) {
				boolean receiverStarted = intent.getBooleanExtra("started",
						false);
				MyApplicationClass.clientStarted = receiverStarted;
				showLog(getText(
						receiverStarted ? R.string.receiver_started
								: R.string.receiver_stopped).toString());
			} else {
				;
			}
		}
	}

	private void showNotification(String msgText) {
		// 声音提醒
		if (MyApplicationClass.userSettings.isPlaySound()) {
			MyApplicationClass.playSoundPool
					.play(MyApplicationClass.ALERT_MSG ? MyApplicationClass.ALERT_SOUND
							: MyApplicationClass.INFO_SOUND, 0);
		}

		// 解析消息文本
		MyMessage message;
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
		} else {
			showMsg(message);
		}

		// 为消息对话框准备数据
		Bundle msgParams = new Bundle();
		msgParams.putBoolean("alert", MyApplicationClass.ALERT_MSG);
		if (message.getTitle() != null && !message.getTitle().equals(""))
			msgParams.putString("title", message.getTitle());
		msgParams.putString("body", message.getBody());
		if (message.getUrl() != null && !message.getUrl().equals(""))
			msgParams.putString("url", message.getUrl());
		if (attachmentUrl != null) {
			msgParams.putString("picUrl", attachmentUrl);
			msgParams.putString("picFilename", attachmentFilename);
		}
		msgParams
				.putBoolean(
						"showPic",
						(attachmentUrl != null
								&& MyApplicationClass.mExternalStorageAvailable && MyApplicationClass.mExternalStorageWriteable));

		// 显示消息对话框
		Intent i = new Intent(MainActivity.this, MessageDialog.class);
		i.putExtras(msgParams);
		startActivity(i);
	}

	private void showMsg(MyMessage message) {
		Resources res = getResources();

		TextView tv = new TextView(this);
		tv.setAutoLinkMask(Linkify.WEB_URLS);
		//
		tv.setTextColor(res.getColor(useMsgColor1 ? R.color.message_color_1
				: R.color.message_color_2));
		//
		String msgText = sdf.format(message.getGenerateTime())
				+ (message.getTitle() == null || message.getTitle().equals("") ? ""
						: " [" + message.getTitle() + "]")
				+ " "
				+ message.getBody()
				+ (message.getUrl() == null || message.getUrl().equals("") ? ""
						: " " + message.getUrl());
		tv.setText(msgText + "\r\n");
		if (msgCount < MAX_MSG_COUNT) {
			msg.addView(tv, 0);
			msgCount++;
		} else {
			msg.removeViewAt(msg.getChildCount() - 1);
			msg.addView(tv, 0);
		}

		useMsgColor1 = !useMsgColor1;
	}

	private void showLog(String logText) {
		String log = "[" + sdf.format(new Date()) + "] " + logText;
		if (logCount < MAX_LOG_COUNT) {
			logger.setText(log + "\r\n" + logger.getText());
			logCount++;
		} else {
			logger.setText(log + "\r\n");
			logCount = 1;
		}
	}

	private void writeSms(String message) {
		ContentValues values = new ContentValues();
		values.put("address", MyApplicationClass.SMS_SENDER_NUMBER);
		values.put("body", message);
		getContentResolver().insert(Uri.parse("content://sms/inbox"), values);
	}

}
