package com.tpsoft.pushnotification.model;

import java.text.ParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 公众号
 * 
 * @author joebin
 * @since 2013-10-19
 */
public class PublicAccount {

	private String name; // 名称
	private String avatar; // 头像URL
	private String desc; // 描述
	private int type; // 类型:1-资讯、2-教育、3-娱乐、4-游戏等

	public PublicAccount(String name, String avatar, String desc, int type) {
		this.name = name;
		this.avatar = avatar;
		this.desc = desc;
		this.type = type;
	}

	public static PublicAccount[] extractPublicAccounts(String accountsText)
			throws JSONException, ParseException {
		// 公众号列表:[{\"name\":'<name>',\"avatar\":\"<avatar>\",\"desc\":'<desc>',\"type\":<type>},...]
		PublicAccount[] accounts;
		try {
			JSONArray jsonArray = new JSONArray(accountsText);
			accounts = new PublicAccount[jsonArray.length()];
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				PublicAccount account = new PublicAccount();
				account.setName(jsonObject.getString("name"));
				account.setAvatar(jsonObject.getString("avatar"));
				account.setDesc(jsonObject.getString("desc"));
				account.setType(jsonObject.getInt("type"));
				//
				accounts[i] = account;
			}
		} catch (JSONException e) {
			throw e;
		}
		return accounts;
	}

	public PublicAccount() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

}
