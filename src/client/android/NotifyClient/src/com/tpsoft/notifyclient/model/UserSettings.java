package com.tpsoft.notifyclient.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

/**
 * 用户设置
 * 
 * @author joebin.don@gmail.com
 * @since 2013-5-28
 */
public class UserSettings {

	public static final String DEFAULT_SERVER_HOST = "118.244.9.191"; // 默认服务地址
	public static final int DEFAULT_SERVER_PORT = 3456; // 默认服务端口
	private static final String DEFAULT_CLIENT_ID = "111111"; // 默认客户ID
	// private static final String DEFAULT_CLIENT_PASSWORD = DEFAULT_CLIENT_ID;
	// // 默认客户密码

	public static final int PHONE_NUMBER_LENGTH = 11; // 电话号码长度
	private static final String DEFAULT_PHONE_NUMBER = "+8613888888888";

	private static final boolean DEFAULT_EMULATE_SMS = false; // 默认短信模拟状态
	private static final boolean DEFAULT_PLAY_SOUND = true; // 默认声音提醒状态

	private String serverHost; // 服务器地址
	private int serverPort; // 服务器端口
	private String clientId; // 客户ID
	private String clientPassword; // 客户密码
	private boolean emulateSms; // 模拟短信
	private boolean playSound; // 声音提醒

	public UserSettings(Context context) {
		readFromPreferences(context);
	}

	public String getServerHost() {
		return serverHost;
	}

	public int getServerPort() {
		return serverPort;
	}

	public String getClientId() {
		return clientId;
	}

	public String getClientPassword() {
		return clientPassword;
	}

	public boolean isEmulateSms() {
		return emulateSms;
	}

	public boolean isPlaySound() {
		return playSound;
	}

	private void readFromPreferences(Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		serverHost = prefs.getString("serverHost", DEFAULT_SERVER_HOST);
		serverPort = Integer.parseInt(prefs.getString("serverPort",
				Integer.toString(DEFAULT_SERVER_PORT)));
		clientId = prefs.getString("clientId", DEFAULT_CLIENT_ID);
		if (clientId.equals(DEFAULT_CLIENT_ID)) {
			clientId = readPhoneNumber(context);
		}
		//if (clientId.length() > PHONE_NUMBER_LENGTH)
		//	clientId = clientId.substring(clientId.length()
		//			- PHONE_NUMBER_LENGTH, clientId.length());
		clientPassword = clientId;

		emulateSms = prefs.getBoolean("emulateSms", DEFAULT_EMULATE_SMS);
		playSound = prefs.getBoolean("playSound", DEFAULT_PLAY_SOUND);
	}

	public static String readPhoneNumber(Context context) {
		TelephonyManager telMgr = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String phoneNumber = telMgr.getLine1Number();
		if (phoneNumber == null) {
			phoneNumber = DEFAULT_PHONE_NUMBER;
		}
		return phoneNumber;
	}
}
