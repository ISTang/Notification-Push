package com.tpsoft.notifyclient;

import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
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
import com.tpsoft.pushnotification.client.MessageTransceiverListener;
import com.tpsoft.pushnotification.client.PushNotificationClient;
import com.tpsoft.pushnotification.model.AppParams;
import com.tpsoft.pushnotification.model.LoginParams;
import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.pushnotification.model.NetworkParams;
import com.tpsoft.pushnotification.model.PublicAccount;
import com.tpsoft.pushnotification.service.NotifyPushService;

@SuppressWarnings("deprecation")
@SuppressLint("SimpleDateFormat")
public class MainActivity extends TabActivity implements
		MessageTransceiverListener {

	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"HH:mm:ss", Locale.CHINESE);

	private static final int MAX_MSG_COUNT = 20;
	private static final int MAX_LOG_COUNT = 200;

	private static final int MESSAGE_START_RECEIVER = 1;
	private static final int MESSAGE_SHOW_NOTIFICATION = 2;

	private LinearLayout msg;
	private TextView logger;
	private int msgCount = 0;
	private boolean useMsgColor1 = true;
	private int logCount = 0;

	private MessageHandler msgHandler;

	private NotificationManager mNM;
	private boolean messagePopupClosed = true;

	private TabHost tabHost;

	private HttpDownloader httpDownloader = new HttpDownloader();

	private PushNotificationClient mClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 设置外观
		setTabs();

		// 实例化客户端
		mClient = new PushNotificationClient(this);
		mClient.addListener(this);

		// 准备通知
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// 初始化消息和日志控件
		msg = (LinearLayout) findViewById(R.id.msg);
		logger = (TextView) findViewById(R.id.log);

		// 创建 Handler 对象
		msgHandler = new MessageHandler(this);

		// 启动消息收发器
		Message msg = new Message();
		msg.what = MESSAGE_START_RECEIVER;
		msgHandler.sendMessageDelayed(msg, 1000);
	}

	@Override
	protected void onDestroy() {
		mClient.release(true); // 停止后台服务
		mNM.cancel(R.id.app_notification_id);

		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
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
		super.onConfigurationChanged(newConfig);
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

	@Override
	public void onTransceiverStatus(boolean started) {
		MyApplicationClass.clientStarted = started;
		showLog(getText(
				started ? R.string.receiver_started : R.string.receiver_stopped)
				.toString());
		Toast.makeText(
				MainActivity.this,
				started ? R.string.receiver_started : R.string.receiver_stopped,
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onLogining(boolean logining) {
		if (logining) {
			Toast.makeText(MainActivity.this, "正在登录...", Toast.LENGTH_SHORT)
					.show();
		}
	}

	@Override
	public void onLoginStatus(int code, String text) {
		if (code != 0)
			showLog(String.format("%s", text));
		else
			showLog("登录成功。");
	}

	@Override
	public void onMessageSendStatus(int msgId, int code, final String text) {
	}

	@Override
	public void onNewMessageReceived(MyMessage msg) {
		showNotification(msg);
	}

	@Override
	public void onPublicAccountsReceived(PublicAccount[] accounts) {
	}

	@Override
	public void onPublicAccountFollowed(String accountName) {

	}

	@Override
	public void onPublicAccountUnfollowed(String accountName) {

	}

	@Override
	public void onFollowedAccountsReceived(PublicAccount[] accounts) {
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

		mClient.startMessageTransceiver(appParams, loginParams, networkParams);
	}

	private void stopMessageReceiver() {
		if (!MyApplicationClass.clientStarted)
			return;

		showLog(getText(R.string.receiver_stopping).toString());
		Toast.makeText(this, getText(R.string.receiver_stopping),
				Toast.LENGTH_SHORT).show();

		mClient.stopMessageTransceiver();
	}

	private void sendMessage() {
		if (!MyApplicationClass.clientStarted)
			return;
		startActivity(new Intent(this, SendMessageActivity.class));
	}

	private void showNotification(final MyMessage msg) {
		showLog(getText(R.string.msg_received).toString());

		// 获取图片URL及文件名
		String attachmentFilename = null;
		String attachmentUrl = null;
		if (msg.getAttachments() != null) {
			for (MyMessage.Attachment attachment : msg.getAttachments()) {
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
					MyMessage.dateFormat.format(msg.getGenerateTime()),
					(msg.getTitle() == null ? "---" : msg.getTitle()),
					msg.getBody(), attachmentUrl != null ? "[" + attachmentUrl
							+ "]" : "",
					!msg.getUrl().equals("") ? "[" + msg.getUrl() + "]" : "");

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
		if (msg.getTitle() != null && !msg.getTitle().equals(""))
			msgParams.putString("title", msg.getTitle());
		msgParams.putString("body", msg.getBody());
		if (msg.getUrl() != null && !msg.getUrl().equals(""))
			msgParams.putString("url", msg.getUrl());
		//
		final Intent messageDialogIntent = (messagePopupClosed ? new Intent(
				MainActivity.this, MessageDialog.class) : new Intent());
		if (attachmentUrl != null
				&& (MyApplicationClass.mExternalStorageAvailable && MyApplicationClass.mExternalStorageWriteable)) {
			final String imageFilename = new Date().getTime() + "_"
					+ attachmentFilename;
			final String imageUrl = attachmentUrl;
			//
			final String sdcardPath = Environment.getExternalStorageDirectory()
					.getPath();

			new Thread() {
				public void run() {
					int errCode = 0;
					if (!MyApplicationClass.savedImages.containsKey(imageUrl)) {
						errCode = httpDownloader.downFile(imageUrl, sdcardPath
								+ "/tmp/", imageFilename, true);
						if (errCode != 0) {
							msgParams.putString("attachmentUrl", imageUrl);
							MyApplicationClass.savedImages.put(imageUrl,
									sdcardPath + "/tmp/" + imageFilename);
						}
					}
					Message message = new Message();
					message.what = MESSAGE_SHOW_NOTIFICATION;
					message.arg1 = errCode;
					message.obj = msg;
					message.setData(msgParams);
					msgHandler.sendMessage(message);
				}
			}.start();
		} else {
			// 添加到消息列表
			showMsg(msg, null);
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

	private static class MessageHandler extends Handler {
		private WeakReference<MainActivity> mActivity;

		public MessageHandler(MainActivity activity) {
			mActivity = new WeakReference<MainActivity>(activity);
		}

		@Override
		public void handleMessage(Message message) {
			switch (message.what) {
			case MESSAGE_START_RECEIVER:
				mActivity.get().startMessageReceiver();
				break;
			case MESSAGE_SHOW_NOTIFICATION:
				Bundle msgParams = message.getData();
				MyMessage msg = (MyMessage) message.obj;
				String imageFilepath = message.arg1 == 0 ? MyApplicationClass.savedImages
						.get(msgParams.get("attachmentUrl")) : null;
				// 添加到消息列表
				mActivity.get().showMsg(msg,
						message.arg1 == 0 ? imageFilepath : null);
				// 显示/更新消息对话框
				Intent messageDialogIntent = (mActivity.get().messagePopupClosed ? new Intent(
						mActivity.get(), MessageDialog.class) : new Intent());
				messageDialogIntent.putExtras(msgParams);
				if (mActivity.get().messagePopupClosed) {
					// 声音提醒
					if (MyApplicationClass.userSettings.isPlaySound()) {
						MyApplicationClass.playSoundPool
								.play(MyApplicationClass.ALERT_MSG ? MyApplicationClass.ALERT_SOUND
										: MyApplicationClass.INFO_SOUND, 0);
					}
					// 显示消息对话框
					mActivity.get().startActivity(messageDialogIntent);
					mActivity.get().messagePopupClosed = false;
				} else {
					// 更新消息对话框
					messageDialogIntent
							.setAction("com.tpsoft.notifyclient.MainActivity");
					messageDialogIntent.putExtra("action", "update");
					mActivity.get().sendBroadcast(messageDialogIntent);
				}
				break;
			default:
				super.handleMessage(message);
			}
		}
	};
}
