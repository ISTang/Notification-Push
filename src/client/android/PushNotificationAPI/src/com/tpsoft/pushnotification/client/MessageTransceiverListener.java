package com.tpsoft.pushnotification.client;

import com.tpsoft.pushnotification.model.MyMessage;
import com.tpsoft.pushnotification.model.PublicAccount;

/**
 * ��Ϣ�շ���������
 * 
 * @author �̱�
 * @since 2013-10-14
 */
public interface MessageTransceiverListener {

	/**
	 * �շ���״̬֪ͨ
	 * 
	 * @param started
	 *            �շ����Ƿ�����
	 */
	public void onTransceiverStatus(boolean started);

	/**
	 * ��¼֪ͨ
	 * 
	 * @param logining
	 *            �Ƿ����ڵ�¼
	 */
	public void onLogining(boolean logining);

	/**
	 * ��¼״̬֪ͨ
	 * 
	 * @param code
	 *            ��¼�׶λ�δ��¼ԭ�����
	 * @param text
	 *            ��¼�׶λ�δ��¼ԭ���ı�
	 */
	public void onLoginStatus(int code, String text);

	/**
	 * ��Ϣ����״̬֪ͨ
	 * 
	 * @param msgId
	 *            ��ϢID
	 * @param code
	 *            ����״̬����
	 * @param text
	 *            ����״̬�ı�
	 */
	public void onMessageSendStatus(int msgId, int code, String text);

	/**
	 * ����Ϣ֪ͨ
	 * 
	 * @param msg
	 *            ��Ϣ
	 */
	public void onNewMessageReceived(MyMessage msg);

	/**
	 * ���ں���Ϣ�б�֪ͨ
	 * 
	 * @param accounts
	 *            ���ں���Ϣ�б�
	 */
	public void onPublicAccountsReceived(PublicAccount[] accounts);

	/**
	 * ���ں��ѹ�ע֪ͨ
	 * 
	 * @param account
	 *            ���ں�
	 */
	public void onPublicAccountFollowed(String accountName);

	/**
	 * ���ں�����ȡ����ע֪ͨ
	 * 
	 * @param accountName
	 *            ���ں�
	 */
	public void onPublicAccountUnfollowed(String accountName);

	/**
	 * �ѹ�ע���ں���Ϣ�б�֪ͨ
	 * 
	 * @param accounts
	 *            ���ں���Ϣ�б�
	 */
	public void onFollowedAccountsReceived(PublicAccount[] accounts);
}
