package com.tpsoft.notifyclient.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

/*
 * Toast自定义显示时间
 * 使用方法
 * 1.先初始化类 MyToast myToast = new MyToast(this);
 * 2.显示消息   myToast.setText("要显示的内容");//设置要显示的内容
 *            myToast.show(6000);  //传入消息显示时间，单位毫秒，最少3000毫秒，并且只能是3000的倍数。
 *                                   传入0时会一直显示，只有调用myToast.cancel();时才会取消。
 * 3.取消消息显示   myToast.cancel();
 * */

public class MyToast {

	private final int DEFAULT = 3000;

	private Context mContext = null;
	private Toast mToast = null;
	private Handler mHandler = null;
	private int duration = 0;
	private int currDuration = 0;

	private Runnable mToastThread = new Runnable() {

		public void run() {
			mToast.show();
			mHandler.postDelayed(mToastThread, DEFAULT);// 每隔3秒显示一次
			if (duration != 0) {
				if (currDuration <= duration) {
					currDuration += DEFAULT;
				} else {
					cancel();
				}
			}

		}
	};

	@SuppressLint("ShowToast")
	public MyToast(Context context) {
		mContext = context;
		currDuration = DEFAULT;
		mHandler = new Handler(mContext.getMainLooper());
		mToast = Toast.makeText(mContext, "", Toast.LENGTH_LONG);
	}

	public void setText(String text) {
		mToast.setText(text);
	}

	public void setText(int resId) {
		mToast.setText(resId);
	}

	public void setView(View view) {
		mToast.setView(view);
	}

	public void setGravity(int gravity, int xOffset, int yOffset) {
		mToast.setGravity(gravity, xOffset, yOffset);
	}

	public void setMargin(float horizontalMargin, float verticalMargin) {
		mToast.setMargin(horizontalMargin, verticalMargin);
	}

	public void show(int duration) {
		this.duration = duration;
		mHandler.post(mToastThread);
	}

	public void cancel() {
		mHandler.removeCallbacks(mToastThread);// 先把显示线程删除
		mToast.cancel();// 把最后一个线程的显示效果cancel掉，就一了百了了
		currDuration = DEFAULT;
	}
}
