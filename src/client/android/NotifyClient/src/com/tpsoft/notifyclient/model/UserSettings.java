package com.tpsoft.notifyclient.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

/**
 * �û�����
 * 
 * @author joebin.don@gmail.com
 * @since 2013-5-28
 */
public class UserSettings {

	public static final String DEFAULT_SERVER_HOST = "118.244.9.191"; // Ĭ�Ϸ����ַ
	public static final int DEFAULT_SERVER_PORT = 3456; // Ĭ�Ϸ���˿�
	private static final String DEFAULT_CLIENT_ID = "111111"; // Ĭ�Ͽͻ�ID
	// private static final String DEFAULT_CLIENT_PASSWORD = DEFAULT_CLIENT_ID;
	// // Ĭ�Ͽͻ�����

	public static final int PHONE_NUMBER_LENGTH = 11; // �绰���볤��
	private static final String DEFAULT_PHONE_NUMBER = "+8613888888888";

	private static final boolean DEFAULT_EMULATE_SMS = false; // Ĭ�϶���ģ��״̬
	private static final boolean DEFAULT_PLAY_SOUND = true; // Ĭ����������״̬

	private String serverHost; // ��������ַ
	private int serverPort; // �������˿�
	private String clientId; // �ͻ�ID
	private String clientPassword; // �ͻ�����
	private boolean emulateSms; // ģ�����
	private boolean playSound; // ��������

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
