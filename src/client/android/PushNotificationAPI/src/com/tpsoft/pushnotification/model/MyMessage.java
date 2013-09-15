package com.tpsoft.pushnotification.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

public class MyMessage {
	public static SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyyMMddHHmmss", Locale.CHINA);

	public static class Attachment {
		private String title; // 附件标题
		private String type; // 附件类型
		private String filename; // 附件文件名
		private String url; // 附件下载URL

		public Attachment(String title, String type, String filename, String url) {
			this.title = title;
			this.type = type;
			this.filename = filename;
			this.url = url;
		}

		public Attachment() {
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

	};

	private String sender;
	private String receiver;
	private String title;
	private String body;
	private String type;
	private String url;
	private Date generateTime;
	private Date expiration;
	private Attachment[] attachments;

	public MyMessage(String sender, String receiver, String title, String body,
			String type, String url, Date generateTime, Date expiration,
			Attachment[] attachments) {
		this.sender = sender;
		this.receiver = receiver;
		this.title = title;
		this.body = body;
		this.type = type;
		this.url = url;
		this.generateTime = generateTime;
		this.expiration = expiration;
		this.attachments = attachments;
	}

	public MyMessage(String title, String body, String type, String url,
			Date generateTime, Date expiration, Attachment[] attachments) {
		this.title = title;
		this.body = body;
		this.type = type;
		this.url = url;
		this.generateTime = generateTime;
		this.expiration = expiration;
		this.attachments = attachments;
	}

	public MyMessage() {
	}

	public MyMessage(String body, Date generateTime) {
		this.body = body;
		this.generateTime = generateTime;
	}

	public MyMessage(Bundle bundle) throws ParseException {
		sender = bundle.getString("sender");
		receiver = bundle.getString("receiver");
		title = bundle.getString("title");
		body = bundle.getString("body");
		type = bundle.getString("type");
		if (type == null) {
			type = "text";
		}
		url = bundle.getString("url");
		if (bundle.containsKey("generateTime")) {
			generateTime = dateFormat.parse(bundle.getString("generateTime"));
		}
		if (bundle.containsKey("expiration")) {
			expiration = dateFormat.parse(bundle.getString("expiration"));
		}
		int attachmentCount = bundle.getInt("attachmentCount");
		if (attachmentCount != 0) {
			attachments = new Attachment[attachmentCount];
			for (int i = 0; i < attachmentCount; i++) {
				Attachment attachment = new Attachment();
				attachment.setTitle(bundle.getString("attachment" + i
						+ "_title"));
				attachment
						.setType(bundle.getString("attachment" + i + "_type"));
				attachment.setFilename(bundle.getString("attachment" + i
						+ "_filename"));
				attachment.setUrl(bundle.getString("attachment" + i + "_url"));
				attachments[i] = attachment;
			}
		}
	}

	public static MyMessage extractMessage(String msgText)
			throws JSONException, ParseException {
		MyMessage message = new MyMessage();
		try {
			JSONObject jsonObject = new JSONObject(msgText);
			if (jsonObject.has("sender_name")) {
				message.setSender(jsonObject.getString("sender_name"));
			}
			if (jsonObject.has("title")
					&& jsonObject.getString("title") != null
					&& !jsonObject.getString("title").equals("")) {
				message.setTitle(jsonObject.getString("title"));
			}
			message.setBody(jsonObject.getString("body"));
			if (jsonObject.has("type")) {
				message.setType(jsonObject.getString("type"));
			} else {
				message.setType("text");
			}
			if (jsonObject.has("url") && jsonObject.getString("url") != null
					&& !jsonObject.getString("url").equals("")) {
				message.setUrl(jsonObject.getString("url"));
			}
			if (jsonObject.has("generate_time")) {
				message.setGenerateTime(dateFormat.parse(jsonObject
						.getString("generate_time")));
			}
			if (jsonObject.has("expiration")) {
				message.setExpiration(dateFormat.parse(jsonObject
						.getString("expiration")));
			}
			// 解析附件
			if (jsonObject.has("attachments")) {
				JSONArray jsonArray = jsonObject.getJSONArray("attachments");
				MyMessage.Attachment[] attachments = new MyMessage.Attachment[jsonArray
						.length()];
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject2 = jsonArray.getJSONObject(i);
					MyMessage.Attachment attachment = new MyMessage.Attachment();
					attachment.setTitle(jsonObject2.getString("title"));
					attachment.setType(jsonObject2.getString("type"));
					attachment.setFilename(jsonObject2.getString("filename"));
					attachment.setUrl(jsonObject2.getString("url"));
					//
					attachments[i] = attachment;
				}
				//
				message.setAttachments(attachments);
			}

		} catch (JSONException e) {
			throw e;
		}
		return message;
	}

	public static String makeText(MyMessage message) throws JSONException {
		JSONObject object = new JSONObject();
		try {
			if (message.sender != null)
				object.put("sender_name", message.sender);
			if (message.title != null)
				object.put("title", message.title);
			object.put("body", message.body);
			if (message.type != null)
				object.put("type", message.type);
			else
				object.put("type", "text");
			if (message.url != null)
				object.put("url", message.url);
			if (message.generateTime != null)
				object.put("generate_time",
						dateFormat.format(message.generateTime));
			if (message.expiration != null)
				object.put("expiration", dateFormat.format(message.expiration));
			if (message.getAttachmentCount() != 0) {
				JSONArray array = new JSONArray();
				for (int i = 0; i < message.getAttachmentCount(); i++) {
					MyMessage.Attachment attachment = message.getAttachment(i);
					JSONObject object2 = new JSONObject();
					object2.put("title", attachment.title);
					object2.put("type", attachment.type);
					object2.put("filename", attachment.filename);
					object2.put("url", attachment.url);
					array.put(object2);
				}
				object.put("attachments", array);
			}
		} catch (JSONException e) {
			throw e;
		}
		return object.toString();
	}

	public Bundle getBundle() {
		Bundle bundle = new Bundle();
		if (sender != null)
			bundle.putString("sender", sender);
		if (receiver != null)
			bundle.putString("receiver", receiver);
		if (title != null)
			bundle.putString("title", title);
		bundle.putString("body", body);
		bundle.putString("type", type);
		if (url != null)
			bundle.putString("url", url);
		if (generateTime != null)
			bundle.putString("generateTime", dateFormat.format(generateTime));
		if (expiration != null)
			bundle.putString("expiration", dateFormat.format(expiration));
		if (attachments != null) {
			bundle.putInt("attachmentCount", attachments.length);
			for (int i = 0; i < attachments.length; i++) {
				Attachment attachment = attachments[i];
				bundle.putString("attachment" + i + "_title",
						attachment.getTitle());
				bundle.putString("attachment" + i + "_type",
						attachment.getType());
				bundle.putString("attachment" + i + "_filename",
						attachment.getFilename());
				bundle.putString("attachment" + i + "_url", attachment.getUrl());
			}
		}
		return bundle;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getReceiver() {
		return receiver;
	}

	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Date getGenerateTime() {
		return generateTime;
	}

	public void setGenerateTime(Date generateTime) {
		this.generateTime = generateTime;
	}

	public Date getExpiration() {
		return expiration;
	}

	public void setExpiration(Date expiration) {
		this.expiration = expiration;
	}

	public Attachment[] getAttachments() {
		return attachments;
	}

	public int getAttachmentCount() {
		if (attachments == null)
			return 0;
		return attachments.length;
	}

	public Attachment getAttachment(int index) {
		if (attachments == null || index < 0 || index >= attachments.length)
			return null;
		return attachments[index];
	}

	public void setAttachments(Attachment[] attachments) {
		this.attachments = attachments;
	}

}
