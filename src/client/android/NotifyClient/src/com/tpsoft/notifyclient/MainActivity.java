package com.tpsoft.notifyclient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.ContactsContract;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.tpsoft.notifyclient.model.UserSettings;
import com.tpsoft.notifyclient.utils.HttpDownloader;
import com.tpsoft.notifyclient.utils.MessageDialog;
import com.tpsoft.notifyclient.utils.PlaySoundPool;
import com.tpsoft.pushnotification.model.AppParams;
import com.tpsoft.pushnotification.model.LoginParams;
import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.pushnotification.model.NetworkParams;
import com.tpsoft.pushnotification.service.NotifyPushService;

@SuppressLint("SimpleDateFormat")
public class MainActivity extends Activity {

	// //////////////////////////////////////
	private BroadcastReceiver mExternalStorageReceiver;
	private boolean mExternalStorageAvailable = false;
	private boolean mExternalStorageWriteable = false;

	// //////////////////////////////////////
	private static final boolean ALERT_MSG = false;

	private static final int INFO_SOUND = 1;
	private static final int ALERT_SOUND = 2;
	// private static final boolean TOAST_WITH_SOUND = true;
	private PlaySoundPool playSoundPool;

	// //////////////////////////////////////
	private static final String SMS_SENDER_NUMBER = "10086";
	private static final String SMS_SENDER_NAME = "10086";
	private String smsSenderName = SMS_SENDER_NAME;

	// //////////////////////////////////////
	public static final String APP_ID = "4083AD3D-0F41-B78E-4F5D-F41A515F2667";
	public static final String APP_PASSWORD = "@0Vd*4Ak";
	public static final String LOGIN_PROTECT_KEY = "n9SfmcRs";
	private UserSettings userSettings;

	// //////////////////////////////////////
	private static final int MAX_LOG_COUNT = 512;
	private TextView logger;
	private int logCount = 0;

	// //////////////////////////////////////
	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"HH:mm:ss", Locale.CHINESE);

	// //////////////////////////////////////
	private NotificationManager mNM;
	private MyBroadcastReceiver myBroadcastReceiver;
	private boolean clientStarted = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 装入用户设置
		loadUserSettings();

		// 准备音效
		playSoundPool = new PlaySoundPool(this);
		playSoundPool.loadSfx(R.raw.info, INFO_SOUND);
		playSoundPool.loadSfx(R.raw.alert, ALERT_SOUND);

		// 准备通知
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// 初始化日志控件
		logger = (TextView) findViewById(R.id.log);

		// 开始监视外部存储器状态
		startWatchingExternalStorage();

		// 启动后台服务
		Intent intent = new Intent(MainActivity.this, NotifyPushService.class);
		intent.putExtra("ActivityClassName", this.getClass().getName());
		startService(intent);

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

	@Override
	protected void onDestroy() {
		mNM.cancel(R.id.app_notification_id);

		stopWatchingExternalStorage();

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (clientStarted) {
			menu.findItem(R.id.menu_start).setEnabled(false);
			menu.findItem(R.id.menu_stop).setEnabled(true);
			menu.findItem(R.id.menu_settings).setEnabled(false);
		} else {
			menu.findItem(R.id.menu_start).setEnabled(true);
			menu.findItem(R.id.menu_stop).setEnabled(false);
			menu.findItem(R.id.menu_settings).setEnabled(true);
		}
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
			showLog("已停止消息接收器");
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

	private void startMessageReceiver() {
		Toast.makeText(this, getText(R.string.receiver_starting),
				Toast.LENGTH_SHORT).show();

		// 获取最新设置
		loadUserSettings();

		AppParams appParams = new AppParams(APP_ID, APP_PASSWORD,
				LOGIN_PROTECT_KEY);
		LoginParams loginParams = new LoginParams(userSettings.getServerHost(),
				userSettings.getServerPort(), userSettings.getClientId(),
				userSettings.getClientPassword());
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

		clientStarted = true;
	}

	private void stopMessageReceiver() {
		showLog(getText(R.string.receiver_stopping).toString());
		Toast.makeText(this, getText(R.string.receiver_stopping),
				Toast.LENGTH_SHORT).show();
		Intent serviceIntent = new Intent();
		serviceIntent
				.setAction("com.tpsoft.pushnotification.ServiceController");
		serviceIntent.putExtra("command", "stop");
		sendBroadcast(serviceIntent);
		clientStarted = false;
	}

	private class MyBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getStringExtra("action");
			if (action.equals("notify")) {
				showNotification(intent.getStringExtra("msgText"));
			} else {
				showLog(intent.getStringExtra("logText"));
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void showNotification(String msgText) {
		// 用声音提示有新消息
		if (userSettings.isPlaySound()) {
			playSoundPool.play(ALERT_MSG ? ALERT_SOUND : INFO_SOUND, 0);
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

		// 格式化消息
		String formattedMessage = String.format("%s %s: %s%s%s",
				MyMessage.dateFormat.format(message.getGenerateTime()),
				(message.getTitle() == null ? "---" : message.getTitle()),
				message.getBody(), attachmentUrl != null ? "[" + attachmentUrl
						+ "]" : "", !message.getUrl().equals("") ? "["
						+ message.getUrl() + "]" : "");

		// 输出消息
		if (userSettings.isEmulateSms()) {

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
			notification.setLatestEventInfo(this, smsSenderName,
					formattedMessage, contentIntent);

			mNM.notify(R.id.app_notification_id, notification);
		} else {
			// 日志
			showLog("[消息] " + formattedMessage);
		}

		// 为消息对话框准备数据
		Bundle msgParams = new Bundle();
		msgParams.putBoolean("alert", ALERT_MSG);
		if (message.getTitle() != null && !message.getTitle().equals(""))
			msgParams.putString("title", message.getTitle());
		msgParams.putString("body", message.getBody());
		if (message.getUrl() != null && !message.getUrl().equals(""))
			msgParams.putString("url", message.getUrl());
		if (attachmentUrl != null) {
			msgParams.putString("picUrl", attachmentUrl);
			msgParams.putString("picFilename", attachmentFilename);
		}
		msgParams.putBoolean("showPic", (attachmentUrl != null
				&& mExternalStorageAvailable && mExternalStorageWriteable));

		// 显示消息对话框
		Intent i = new Intent(MainActivity.this, MessageDialog.class);
		i.putExtras(msgParams);
		startActivity(i);
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
		values.put("address", SMS_SENDER_NUMBER);
		values.put("body", message);
		getContentResolver().insert(Uri.parse("content://sms/inbox"), values);
	}

	private void loadUserSettings() {
		userSettings = new UserSettings(this);
		if (userSettings.isEmulateSms()) {
			String name = queryNameByNum(SMS_SENDER_NUMBER, this);
			if (name != null) {
				smsSenderName = name;
			}
		}
	}

	private static String queryNameByNum(String num, Context context) {
		Cursor cursorOriginal = context
				.getContentResolver()
				.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
						new String[] { ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME },
						ContactsContract.CommonDataKinds.Phone.NUMBER + "='"
								+ num + "'", null, null);
		if (null != cursorOriginal) {
			if (cursorOriginal.getCount() > 1) {
				return null;
			} else {
				if (cursorOriginal.moveToFirst()) {
					return cursorOriginal
							.getString(cursorOriginal
									.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
				} else {
					return null;
				}
			}
		} else {
			return null;
		}
	}

	private void startWatchingExternalStorage() {
		mExternalStorageReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				updateExternalStorageState();
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_REMOVED);
		registerReceiver(mExternalStorageReceiver, filter);
		updateExternalStorageState();
	}

	private void stopWatchingExternalStorage() {
		unregisterReceiver(mExternalStorageReceiver);
	}

	private void updateExternalStorageState() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
	}
}
