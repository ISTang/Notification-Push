package com.tpsoft.pushnotification.manage.model;

import java.util.List;

/**
 * ��Ϣ
 * 
 * @author joebin.don@gmail.com
 * @since 2013-06-10
 */
public class Message {
	/**
	 * ��Ϣ����
	 */
	public class Attachment {
		private String title; // ��������: 1-256���ַ�
		private String type; // ��������: MIME���ͣ���text/plain, text/html, image/jpeg,
								// audio/mpeg, video/mpeg,
								// application/oct-stream��
		private String filename; // �����ļ���(filename) 1-256���ַ�
		private String url; // ��������URL: ��֧��HTTPЭ�飬1-1024���ַ�

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

	private String title; // ��Ϣ����: ��ѡ��1-20���ַ�
	private String body; // ��Ϣ����: 1-4096�ֽ�
	private String type; // ��������: text��html��xml����ѡ��Ĭ��Ϊtext
	private List<Attachment> attachments; // ����: ��ѡ�����4��
	private String url; // ��Ϣ����: ��ѡ��1-1024���ַ�����֧��HTTPЭ��
	private String sendTime; // ����ʱ��: �ӳٷ���ʱ�䣬��ʽΪyyyyMMddhhmmss��ѡ��Ĭ��Ϊ��������
	private String expiration; // ����ʱ��: ��ѡ����ʽΪyyyyMMddhhmmss��Ĭ��Ϊ��������
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
		return sendTime;
	}

	public void setSendTime(String sendTime) {
		this.sendTime = sendTime;
	}

	public String getExpiration() {
		return expiration;
	}

	public void setExpiration(String expiration) {
		this.expiration = expiration;
	}
}
