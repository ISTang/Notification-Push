package com.tpsoft.notifyclient.utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.tpsoft.notifyclient.R;

public class MessageDialog extends Activity implements OnTouchListener {

	private static final int TOAST_INFO_TIME = 1000*60*5;
	private static final int TOAST_ALERT_TIME = 1000*60*10;

	private HttpDownloader httpDownloader = new HttpDownloader();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		// Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Remove notification bar
		//this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		//		WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// 设置窗口背景透明
		setTheme(R.style.Transparent);
		setContentView(R.layout.transparent);

		showMessage();

		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		// this is very important here ;)
		super.onDestroy();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		final int actionPerformed = event.getAction();

		// reset idle timer
		// put this here so that the touching of empty space is captured too
		// it seems that LinearLayout doesn't trigger a MotionEvent.ACTION_UP or
		// MotionEvent.ACTION_MOVE
		if (actionPerformed == MotionEvent.ACTION_DOWN) {
			super.onTouchEvent(event);
		}

		return false;// do not consume event!
	}

	@SuppressLint("HandlerLeak")
	public void showMessage() {
		final Context mContext = MessageDialog.this;
		final Bundle msgBundle = getIntent().getExtras();

		// 准备弹出界面
		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		final View notifyView = inflater.inflate(R.layout.notification_popup,
				(ViewGroup) findViewById(R.id.message));
		if (msgBundle.getBoolean("alert"))
			notifyView.setBackgroundResource(R.drawable.alert_bg);
		else
			notifyView.setBackgroundResource(R.drawable.info_bg);
		// 消息重要级别
		ImageView msgLevel = (ImageView) notifyView.findViewById(R.id.msgLevel);
		if (msgBundle.getBoolean("alert"))
			msgLevel.setImageResource(R.drawable.alert_icon);
		else
			msgLevel.setImageResource(R.drawable.info_icon);
		// 消息标题
		TextView msgTitle = (TextView) notifyView.findViewById(R.id.msgTitle);
		if (msgBundle.containsKey("title"))
			msgTitle.setText(msgBundle.getString("title"));
		else
			msgTitle.setText("");
		// 消息正文
		TextView msgBody = (TextView) notifyView.findViewById(R.id.msgBody);
		msgBody.setText(msgBundle.getString("body"));
		// 详情URL
		TextView msgUrl = (TextView) notifyView.findViewById(R.id.msgUrl);
		if (msgBundle.containsKey("url"))
			msgUrl.setText(msgBundle.getString("url"));
		else
			msgUrl.setText("");
		// 图片
		final ImageView msgAttachment = (ImageView) notifyView
				.findViewById(R.id.msgAttachment);
		msgAttachment.setImageBitmap(null);
		if (msgBundle.getBoolean("showPic")) {
			final Handler handler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					switch (msg.what) {
					case 0:
						if (msg.arg1 == 0) {
							msgAttachment.setImageBitmap((Bitmap) msg.obj);
						}
						showAlertDialog(mContext, msgBundle, notifyView);
						break;
					default:
						super.handleMessage(msg);
					}
				}
			};

			new Thread() {
				public void run() {
					String sdcardPath = Environment
							.getExternalStorageDirectory().getPath();
					File imageFile = new File(sdcardPath + "/tmp/"
							+ msgBundle.getString("picFilename"));
					int errCode = httpDownloader.downFile(
							msgBundle.getString("picUrl"), "tmp",
							msgBundle.getString("picFilename"), true);
					Bitmap bitmap = null;
					if (errCode == 0) {
						try {
							FileInputStream fis = new FileInputStream(imageFile);
							bitmap = BitmapFactory.decodeStream(fis);
							fis.close();
							imageFile.delete();
						} catch (Exception e) {
							e.printStackTrace();
							errCode = -1;
						}
					}
					//
					Message msg = new Message();
					msg.what = 0;
					msg.arg1 = errCode;
					if (bitmap != null) {
						msg.obj = bitmap;
					}
					handler.sendMessage(msg);
				}
			}.start();
		} else {
			showAlertDialog(mContext, msgBundle, notifyView);
		}
	}

	private void showAlertDialog(Context mContext, Bundle msgBundle,
			View notifyView) {
		AlertDialog.Builder builder;
		final AlertDialog alertDialog;
		builder = new AlertDialog.Builder(mContext);
		builder.setView(notifyView);

		alertDialog = builder.create();
		alertDialog.setCanceledOnTouchOutside(false);
		alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		WindowManager.LayoutParams alertDialogLayoutParams = alertDialog
				.getWindow().getAttributes();
		alertDialog.getWindow().setBackgroundDrawable(
				new ColorDrawable(android.graphics.Color.TRANSPARENT));
		if (msgBundle.getBoolean("alert")) {
			alertDialogLayoutParams.gravity = Gravity.CENTER_VERTICAL;
		} else {
			alertDialogLayoutParams.gravity = Gravity.BOTTOM;
		}

		final Timer t = new Timer();
		t.schedule(new TimerTask() {
			public void run() {
				closeAlertDialog(alertDialog, t);
			}

		}, msgBundle.getBoolean("alert") ? TOAST_ALERT_TIME : TOAST_INFO_TIME);

		ImageButton closeButton = (ImageButton) notifyView
				.findViewById(R.id.closeButton);
		closeButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				closeAlertDialog(alertDialog, t);
			}
		});

		alertDialog.show();
	}

	private void closeAlertDialog(final AlertDialog alertDialog, final Timer t) {
		alertDialog.dismiss(); // when the task is active then close the dialog
		t.cancel(); // also just top the timer thread, otherwise, you may
					// receive a crash report
		finish();
	}
}