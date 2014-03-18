package com.tpsoft.pushnotification.manage.model;

import java.util.List;

/**
 * ÏûÏ¢
 * 
 * @author joebin.don@gmail.com
 * @since 2013-06-10
 */
public class MessageWithLoginInfo extends Message {
	private LoginInfo user;

	public MessageWithLoginInfo() {
		super();
	}

	public MessageWithLoginInfo(String title, String body, String type,
			String url) {
		super(title, body, type, url);
	}

	public MessageWithLoginInfo(String title, String body, String type,
			List<Attachment> attachments, String url) {
		super(title, body, type, attachments, url);
	}
	
	public MessageWithLoginInfo(LoginInfo user, Message msg) {
		this.user = user;
		setTitle(msg.getTitle());
		setBody(msg.getBody());
		setType(msg.getType());
		setAttachments(msg.getAttachments());
		setUrl(msg.getUrl());
		setSendTime(msg.getSendTime());
		setExpiration(msg.getExpiration());
		setCallback(msg.getCallback());
		setNeedReceipt(msg.isNeedReceipt());
	}

	public LoginInfo getUser() {
		return user;
	}

	public void setUser(LoginInfo user) {
		this.user = user;
	}

}
