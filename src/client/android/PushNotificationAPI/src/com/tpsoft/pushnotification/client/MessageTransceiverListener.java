package com.tpsoft.pushnotification.client;

import com.tpsoft.pushnotification.model.MyMessage;

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
	 * 登录状态通知
	 * 
	 * @param logining
	 *            是否正在登录
	 * @param code
	 *            登录阶段或未登录原因代码
	 * @param text
	 *            登录阶段或未登录原因文本
	 */
	public void onLoginStatus(boolean logining, int code, String text);

	/**
	 * 消息(发送)状态通知
	 * 
	 * @param msgId
	 *            消息ID
	 * @param code
	 *            发送状态代码
	 * @param text
	 *            发送状态文本
	 */
	public void onMessageStatus(String msgId, int code, String text);

	/**
	 * 新消息通知
	 * 
	 * @param msg
	 *            消息
	 */
	public void onMessageReceived(MyMessage msg);
}
