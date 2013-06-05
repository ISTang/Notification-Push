package com.tpsoft.pushnotification.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

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

	private String title;
	private String body;
	private String type;
	private String url;
	private Date generateTime;
	private Date expiration;
	private Attachment[] attachments;

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

	public MyMessage(String body, Date generateTime) {
		this.body = body;
		this.generateTime = generateTime;
	}

	public MyMessage() {
	}

	public static MyMessage extractMessage(String msgText) throws Exception {
		MyMessage message = new MyMessage();
		try {
			JSONObject jsonObject = new JSONObject(msgText);
			if (jsonObject.has("title")) {
				message.setTitle(jsonObject.getString("title"));
			} else {
				message.setTitle("");
			}
			message.setBody(jsonObject.getString("body"));
			if (jsonObject.has("type")) {
				message.setType(jsonObject.getString("type"));
			} else {
				message.setType("text");
			}
			if (jsonObject.has("url")
					&& !jsonObject.getString("url").equals("null")) {
				message.setUrl(jsonObject.getString("url"));
			} else {
				message.setUrl("");
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

		} catch (Exception e) {
			throw e;
		}
		return message;
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
