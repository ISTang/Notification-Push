package com.tpsoft.pushnotification.manage.intf;

import java.util.Collection;

import com.tpsoft.pushnotification.manage.exception.PushMessageException;
import com.tpsoft.pushnotification.manage.model.Message;

/**
 * ��Ϣ���ͽӿ�
 * 
 * @author joebin.don@gmail.com
 * @since 2013-04-10
 */
public interface IMessagePush {

	/**
	 * ��Ӧ�ù㲥��Ϣ
	 * 
	 * @param appId
	 *            Ӧ��ID
	 * @param message
	 *            ��Ϣ
	 * @throws PushMessageException
	 */
	public void broadcast(String appId, Message message)
			throws PushMessageException;

	/**
	 * ���Ӧ�������ʺ�Ⱥ����Ϣ
	 * 
	 * @param appId
	 *            Ӧ��ID
	 * @param accounts
	 *            �˺�(����/�绰����/�����ַ)��
	 * @param message
	 *            ��Ϣ
	 * @throws PushMessageException
	 */
	public void multicast(String appId, Collection<String> accounts,
			Message message) throws PushMessageException;

	/**
	 * ���Ӧ����ָ���ʺŷ�����Ϣ
	 * 
	 * @param appId
	 *            Ӧ��ID
	 * @param account
	 *            �˺�(����/�绰����/�����ַ)
	 * @param message
	 *            ��Ϣ
	 * @throws PushMessageException
	 */
	public void send(String appId, String account, Message message)
			throws PushMessageException;
}
