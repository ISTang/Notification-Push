package com.tpsoft.notifyclient;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.provider.Settings;

import com.tpsoft.notifyclient.model.UserSettings;

public class SettingsActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);

		EditTextPreference prefServerHost = (EditTextPreference) findPreference("serverHost");
		EditTextPreference prefServerPort = (EditTextPreference) findPreference("serverPort");
		EditTextPreference prefClientId = (EditTextPreference) findPreference("clientId");
		CheckBoxPreference prefEmulateSms = (CheckBoxPreference) findPreference("emulateSms");
		CheckBoxPreference prefPlaySound = (CheckBoxPreference) findPreference("playSound");

		// 生成客户端标识(电话号码)
		if (prefClientId.getText().equals("")) {
			String phoneNumber = UserSettings.readPhoneNumber(this);
			prefClientId.setText(phoneNumber);
			Settings.System.putString(getContentResolver(), "clientId",
					phoneNumber);
		}

		// 允许显示现有值
		prefServerHost.setSummary(prefServerHost.getText());
		prefServerPort.setSummary(prefServerPort.getText());
		prefClientId.setSummary(prefClientId.getText());
		updateSummary(prefEmulateSms, prefEmulateSms.isChecked());
		updateSummary(prefPlaySound, prefPlaySound.isChecked());

		// 允许监听值改变事件
		prefServerHost.setOnPreferenceChangeListener(this);
		prefServerPort.setOnPreferenceChangeListener(this);
		prefClientId.setOnPreferenceChangeListener(this);
		prefEmulateSms.setOnPreferenceChangeListener(this);
		prefPlaySound.setOnPreferenceChangeListener(this);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		updateSummary(preference, newValue);
		return true;
	}

	private void updateSummary(Preference preference, Object newValue) {
		if (preference.getKey().equals("emulateSms")
				|| preference.getKey().equals("playSound")) {
			preference.setSummary((Boolean) newValue ? "允许" : "禁止");
		} else {
			preference.setSummary(newValue.toString());
		}
	}

}
