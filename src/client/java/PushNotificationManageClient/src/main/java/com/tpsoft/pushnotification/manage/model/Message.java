package com.tpsoft.pushnotification.manage.model;

import java.util.List;

/**
 * 消息
 * 
 * @author joebin.don@gmail.com
 * @since 2013-06-10
 */
public class Message {
	/**
	 * 消息附件
	 */
	public static class Attachment {
		private String title; // 附件标题: 1-256个字符
		private String type; // 附件类型: MIME类型，如text/plain, text/html, image/jpeg,
								// audio/mpeg, video/mpeg,
								// application/oct-stream等
		private String filename; // 附件文件名(filename) 1-256个字符
		private String url; // 附件下载URL: 仅支持HTTP协议，1-1024个字符

		public Attachment() {
		}

		public Attachment(String title, String type, String filename, String url) {
			this.title = title;
			this.type = type;
			this.filename = filename;
			this.url = url;
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

	}

	private String title; // 消息标题: 可选，1-20个字符
	private String body; // 消息正文: 1-4096字节
	private String type; // 正文类型: text、html或xml，可选，默认为text
	private List<Attachment> attachments; // 附件: 可选，最多4个
	private String url; // 消息链接: 可选，1-1024个字符，仅支持HTTP协议
	private String send_time; // 发送时间: 延迟发送时间，格式为yyyyMMddhhmmss可选，默认为立即发送
	private String expiration; // 过期时间: 可选，格式为yyyyMMddhhmmss，默认为永不过期
	private String callback; // 用于在消息发送成功或失败时通知发送者，可选，仅支持HTTP协议，1-1024个字符
	private boolean need_receipt; // 是否需要回执(true/false)，可选，默认为不需要回执(false)

	public Message() {
	}

	public Message(String title, String body, String type, String url) {
		this.title = title;
		this.body = body;
		this.type = type;
		this.url = url;
	}

	public Message(String title, String body, String type,
			List<Attachment> attachments, String url) {
		this.title = title;
		this.body = body;
		this.type = type;
		this.attachments = attachments;
		this.url = url;
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

	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getSendTime() {
		return send_time;
	}

	public void setSendTime(String sendTime) {
		this.send_time = sendTime;
	}

	public String getExpiration() {
		return expiration;
	}

	public void setExpiration(String expiration) {
		this.expiration = expiration;
	}

	public String getCallback() {
		return callback;
	}

	public void setCallback(String callback) {
		this.callback = callback;
	}

	public boolean isNeedReceipt() {
		return need_receipt;
	}

	public void setNeedReceipt(boolean needReceipt) {
		this.need_receipt = needReceipt;
	}
}
