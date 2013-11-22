package com.tpsoft.pushnotification.client;

import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.pushnotification.model.PublicAccount;

/**
 * 消息收发器侦听器
 * 
 * @author 教兵
 * @since 2013-10-14
 */
public interface MessageTransceiverListener {

	/**
	 * 收发器状态通知
	 * 
	 * @param started
	 *            收发器是否启动
	 */
	public void onTransceiverStatus(boolean started);

	/**
	 * 登录通知
	 * 
	 * @param logining
	 *            是否正在登录
	 */
	public void onLogining(boolean logining);

	/**
	 * 登录状态通知
	 * 
	 * @param code
	 *            登录阶段或未登录原因代码
	 * @param text
	 *            登录阶段或未登录原因文本
	 */
	public void onLoginStatus(int code, String text);

	/**
	 * 消息发送状态通知
	 * 
	 * @param msgId
	 *            消息ID
	 * @param code
	 *            发送状态代码
	 * @param text
	 *            发送状态文本
	 */
	public void onMessageSendStatus(int msgId, int code, String text);

	/**
	 * 新消息通知
	 * 
	 * @param msg
	 *            消息
	 */
	public void onNewMessageReceived(MyMessage msg);

	/**
	 * 公众号信息列表通知
	 * 
	 * @param accounts
	 *            公众号信息列表
	 */
	public void onPublicAccountsReceived(PublicAccount[] accounts);

	/**
	 * 公众号已关注通知
	 * 
	 * @param account
	 *            公众号
	 */
	public void onPublicAccountFollowed(String accountName);

	/**
	 * 公众号已已取消关注通知
	 * 
	 * @param accountName
	 *            公众号
	 */
	public void onPublicAccountUnfollowed(String accountName);

	/**
	 * 已关注公众号信息列表通知
	 * 
	 * @param accounts
	 *            公众号信息列表
	 */
	public void onFollowedAccountsReceived(PublicAccount[] accounts);
}
