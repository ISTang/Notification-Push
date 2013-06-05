package com.tpsoft.notifyclient.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

/*
 * Toast�Զ�����ʾʱ��
 * ʹ�÷���
 * 1.�ȳ�ʼ���� MyToast myToast = new MyToast(this);
 * 2.��ʾ��Ϣ   myToast.setText("Ҫ��ʾ������");//����Ҫ��ʾ������
 *            myToast.show(6000);  //������Ϣ��ʾʱ�䣬��λ���룬����3000���룬����ֻ����3000�ı�����
 *                                   ����0ʱ��һֱ��ʾ��ֻ�е���myToast.cancel();ʱ�Ż�ȡ����
 * 3.ȡ����Ϣ��ʾ   myToast.cancel();
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
			mHandler.postDelayed(mToastThread, DEFAULT);// ÿ��3����ʾһ��
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
		mHandler.removeCallbacks(mToastThread);// �Ȱ���ʾ�߳�ɾ��
		mToast.cancel();// �����һ���̵߳���ʾЧ��cancel������һ�˰�����
		currDuration = DEFAULT;
	}
}
