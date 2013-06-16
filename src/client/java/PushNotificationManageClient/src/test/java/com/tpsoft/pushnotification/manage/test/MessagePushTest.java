package com.tpsoft.pushnotification.manage.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.tpsoft.pushnotification.manage.config.ServiceConfig;
import com.tpsoft.pushnotification.manage.exception.PushMessageException;
import com.tpsoft.pushnotification.manage.impl.MessagePushImpl;
import com.tpsoft.pushnotification.manage.intf.IMessagePush;
import com.tpsoft.pushnotification.manage.model.Message;
import com.tpsoft.pushnotification.manage.model.Message.Attachment;

public class MessagePushTest {

	private ServiceConfig serviceConfig = new ServiceConfig();
	private IMessagePush messagePush = new MessagePushImpl(serviceConfig);

	@Before
	public void setUp() throws Exception {
		serviceConfig.setServer("118.244.9.191");
		serviceConfig.setPort(4567);
	}

	@Test
	public void testBroadcast() {
		String appId = "4083AD3D-0F41-B78E-4F5D-F41A515F2667";
		Message message = new Message("测试消息", "这是一条广播消息。", "text",
				"http://www.baidu.com/");
		message.setNeedReceipt(true);

		Attachment attachment = new Message.Attachment(
				"image",
				"image/jpg",
				"U474P4T19D19F12274DT20130613153015.jpg",
				"http://www.chinanews.com/edu_other/include09/U474P4T19D19F12274DT20130613153015.jpg");
		List<Attachment> attachments = new ArrayList<Attachment>();
		attachments.add(attachment);
		message.setAttachments(attachments);

		try {
			messagePush.broadcast(appId, message);
			System.out.println("One message broadcasted.");
		} catch (PushMessageException e) {
			System.err.println(e.getMessage());
		}
	}

	@SuppressWarnings("serial")
	@Test
	public void testMulticast() {
		String appId = "4083AD3D-0F41-B78E-4F5D-F41A515F2667";
		List<String> accounts = new ArrayList<String>() {
			{
				add("+8613808188051");
				add("13342884875");
			}
		};
		Message message = new Message("测试消息", "这是一条群发消息。", "text",
				"http://www.baidu.com/");
		message.setNeedReceipt(true);

		Attachment attachment = new Message.Attachment(
				"图片",
				"image/png",
				"b03533fa828ba61eed6096974034970a314e59ff.png",
				"http://f.hiphotos.baidu.com/album/w%3D2048/sign=a06e8c9991ef76c6d0d2fc2ba92efcfa/b03533fa828ba61eed6096974034970a314e59ff.jpg");
		List<Attachment> attachments = new ArrayList<Attachment>();
		attachments.add(attachment);
		message.setAttachments(attachments);

		try {
			messagePush.multicast(appId, accounts, message);
			System.out.println("One message multicasted.");
		} catch (PushMessageException e) {
			System.err.println(e.getMessage());
		}
	}

	@Test
	public void testSend() {
		String appId = "4083AD3D-0F41-B78E-4F5D-F41A515F2667";
		String addcount = "+8613808188051";
		Message message = new Message("测试消息", "这是一条私密消息。", "text",
				"http://www.baidu.com/");
		message.setNeedReceipt(true);

		Attachment attachment = new Message.Attachment(
				"图片",
				"image/png",
				"b03533fa828ba61eed6096974034970a314e59ff.png",
				"http://f.hiphotos.baidu.com/album/w%3D2048/sign=a06e8c9991ef76c6d0d2fc2ba92efcfa/b03533fa828ba61eed6096974034970a314e59ff.jpg");
		List<Attachment> attachments = new ArrayList<Attachment>();
		attachments.add(attachment);
		message.setAttachments(attachments);

		try {
			messagePush.send(appId, addcount, message);
			System.out.println("One message sent.");
		} catch (PushMessageException e) {
			System.err.println(e.getMessage());
		}
	}

}
