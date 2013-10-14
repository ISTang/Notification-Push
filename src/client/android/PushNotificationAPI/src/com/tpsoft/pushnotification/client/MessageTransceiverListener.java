package com.tpsoft.pushnotification.client;

import com.tpsoft.pushnotification.model.MyMessage;

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
	 * ��¼״̬֪ͨ
	 * 
	 * @param logining
	 *            �Ƿ����ڵ�¼
	 * @param code
	 *            ��¼�׶λ�δ��¼ԭ�����
	 * @param text
	 *            ��¼�׶λ�δ��¼ԭ���ı�
	 */
	public void onLoginStatus(boolean logining, int code, String text);

	/**
	 * ��Ϣ(����)״̬֪ͨ
	 * 
	 * @param msgId
	 *            ��ϢID
	 * @param code
	 *            ����״̬����
	 * @param text
	 *            ����״̬�ı�
	 */
	public void onMessageStatus(String msgId, int code, String text);

	/**
	 * ����Ϣ֪ͨ
	 * 
	 * @param msg
	 *            ��Ϣ
	 */
	public void onMessageReceived(MyMessage msg);
}
