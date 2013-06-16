package com.tpsoft.pushnotification.manage.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tpsoft.pushnotification.manage.config.ServiceConfig;
import com.tpsoft.pushnotification.manage.exception.PushMessageException;
import com.tpsoft.pushnotification.manage.intf.IMessagePush;
import com.tpsoft.pushnotification.manage.model.Message;
import com.tpsoft.pushnotification.manage.response.BroadcastMessageResult;
import com.tpsoft.pushnotification.manage.response.MulticastMessageResult;
import com.tpsoft.pushnotification.manage.response.SendMessageResult;

public class MessagePushImpl implements IMessagePush {

	private ServiceConfig serviceConfig;

	private Gson gson = new GsonBuilder().create();

	public MessagePushImpl() {

	}

	public MessagePushImpl(ServiceConfig serviceConfig) {
		this.serviceConfig = serviceConfig;
	}

	public ServiceConfig getServiceConfig() {
		return serviceConfig;
	}

	public void setServiceConfig(ServiceConfig serviceConfig) {
		this.serviceConfig = serviceConfig;
	}

	public void broadcast(String appId, Message message)
			throws PushMessageException {
		ClientRequest req = new ClientRequest(
				serviceConfig.getBroadcastMessageEndpoint());
		req.pathParameter("id", appId).body(ServiceConfig.CONTENT_TYPE,
				gson.toJson(message));

		ClientResponse<BroadcastMessageResult> res;
		try {
			res = req.post(BroadcastMessageResult.class);
		} catch (Exception e) {
			throw new PushMessageException(String.format("#-1:%s",
					e.getMessage()));
		}
		BroadcastMessageResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new PushMessageException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
	}

	public void multicast(String appId, Collection<String> accounts,
			Message message) throws PushMessageException {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		for (String account : accounts) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("name", account);
			list.add(map);
		}

		String reqBody = String.format("{\"accounts\":%s,\"message\":%s}",
				gson.toJson(list), gson.toJson(message));

		ClientRequest req = new ClientRequest(
				serviceConfig.getMulticastMessageEndpoint());
		req.pathParameter("id", appId)
				.body(ServiceConfig.CONTENT_TYPE, reqBody);

		ClientResponse<MulticastMessageResult> res;
		try {
			res = req.post(MulticastMessageResult.class);
		} catch (Exception e) {
			throw new PushMessageException(String.format("#-1:%s",
					e.getMessage()));
		}
		MulticastMessageResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new PushMessageException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
	}

	public void send(String appId, String account, Message message)
			throws PushMessageException {
		ClientRequest req = new ClientRequest(
				serviceConfig.getSendMessageEndpoint());
		req.pathParameter("id", appId).pathParameter("name", account)
				.body(ServiceConfig.CONTENT_TYPE, gson.toJson(message));

		ClientResponse<SendMessageResult> res;
		try {
			res = req.post(SendMessageResult.class);
		} catch (Exception e) {
			throw new PushMessageException(String.format("#-1:%s",
					e.getMessage()));
		}
		SendMessageResult result = res.getEntity();
		if (!result.isSuccess()) {
			throw new PushMessageException(String.format("#%d:%s",
					result.getErrcode(), result.getErrmsg()));
		}
	}
}
