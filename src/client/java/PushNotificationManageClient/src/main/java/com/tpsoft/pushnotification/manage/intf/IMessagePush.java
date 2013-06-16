package com.tpsoft.pushnotification.manage.intf;

import java.util.Collection;

import com.tpsoft.pushnotification.manage.exception.PushMessageException;
import com.tpsoft.pushnotification.manage.model.Message;

/**
 * 消息推送接口
 * 
 * @author joebin.don@gmail.com
 * @since 2013-04-10
 */
public interface IMessagePush {

	/**
	 * 向应用广播消息
	 * 
	 * @param appId
	 *            应用ID
	 * @param message
	 *            消息
	 * @throws PushMessageException
	 */
	public void broadcast(String appId, Message message)
			throws PushMessageException;

	/**
	 * 针对应用向多个帐号群发消息
	 * 
	 * @param appId
	 *            应用ID
	 * @param accounts
	 *            账号(名称/电话号码/邮箱地址)表
	 * @param message
	 *            消息
	 * @throws PushMessageException
	 */
	public void multicast(String appId, Collection<String> accounts,
			Message message) throws PushMessageException;

	/**
	 * 针对应用向指定帐号发送消息
	 * 
	 * @param appId
	 *            应用ID
	 * @param account
	 *            账号(名称/电话号码/邮箱地址)
	 * @param message
	 *            消息
	 * @throws PushMessageException
	 */
	public void send(String appId, String account, Message message)
			throws PushMessageException;
}
