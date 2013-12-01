��Ϣ���Ϳͻ���API�÷���˵��

1.��ʼ���ͻ���

private PushNotificationClient mClient;
mClient = new PushNotificationClient(context); // contextΪContextʵ��������Activity
mClient.addListener(listener); // listenerΪMessageTransceiverListener�ӿ�ʵ��


2.����/ֹͣ��Ϣ�շ���

A.������Ϣ�շ���

AppParams appParams = new AppParams(APP_ID/*Ӧ��ID*/,
		APP_PASSWORD/*Ӧ������*/,
		LOGIN_PROTECT_KEY/*��¼������Կ*/);
LoginParams loginParams = new LoginParams(
		SERVER_HOST/*��������ַ*/,
		SERVER_PORT/*�������˿�*/,
 		CLIENT_ID/*�ͻ���ID(���Զ�����)*/,
		CLIENT_PASSWORD/*�ͻ�������*/);
NetworkParams networkParams = new NetworkParams(); // ������������������ӳ�ʱ�����������ӳ١���¼��ʱ�Ͷ���ʱ��Ĭ��ֵ�ֱ�Ϊ3�롢3�롢1���Ӻ�100����

mClient.startMessageTransceiver(appParams, loginParams, networkParams);

����:
  appParams Ӧ�ò���
  loginParams ��¼����
  networkParams �������

ע����Ϣ�շ��������ɹ��󣬻��Զ���¼�����������źţ����ǵ�¼ʱ����Ӧ�û��û���֤ʧ�ܵ��������ʱ���Զ�ֹͣ�շ����������¼�--onTransceiverStatus����
 
B.ֹͣ��Ϣ�շ���

mClient.stopMessageTransceiver();

C.��Ӧ��Ϣ�շ���״̬�ı�

��д onTransceiverStatus(started) ����

����:
 started �Ƿ�������(true/false)

D.��Ӧ��¼״̬�ı�

��д onLogining(logining) ������ onLoginStatus(code, text) ����

����:
 logining �Ƿ����ڵ�¼(true/false)
 code ��¼�׶λ�δ��¼ԭ�����(���±�)
 text ��¼�׶λ�δ��¼ԭ���ı�

�׶δ��뼰����:

  code    text
=====================================
   0     ��¼�ɹ���
   1     ���ӷ����� <��������ַ>
   2     �����ӵ���������
   3     У��Ӧ��ID�ͽ�������...
   4     Ӧ����֤ͨ����
   5     У���û���������...
   6     �û���֤ͨ����
   7     �յ���Ϣ��Կ��
   8     �յ���������: <n> �롣
  -1     ���粻���ã�
  -2     �������жϣ�
  -3     �����������ã�
  -4     ��¼��ʱ��
  -5     ����IO���ϣ�
  -6     Ӧ����֤ʧ�ܣ�
  -7     �û���֤ʧ�ܣ�
  -8     ����������

   
3. ����/������Ϣ

A.������Ϣ

MyMessage msg = new MyMessage();
msg.setReceiver(RECEIVER_ID/*������ID*/);
msg.setBody(CONTENT/*��Ϣ����*/);
//���������ֶ�(��ѡ)
mClient.sendMessage(msgId/*��ϢID�����ڸ�����Ϣ����״̬*/, msg);

B.��Ӧ��Ϣ����״̬�ı�

��д onMessageSendStatus(msgId, code, text) ����

����:
 msgId ��ϢID������ʱָ��
 code ״̬����(���±�)
 text ״̬����
 
״̬���뼰����:
  code    text
=====================================
   0     ��ȷ��--���ͳɹ�
   1     �ύ...
   2     ���ύ
  -1     δ��¼
  -2     ���ݴ���
  -3     �ύʧ�ܣ�
  -4     ����ʧ��(#<errcode>:<errmsg>)

C.��Ӧ���յ�������Ϣ

��д onNewMessageReceived(msg) ����

����:
  msg MyMessageʵ��

4.�ͷſͻ���

mClient.release();

ע���ڲ�����Ҫ������Ϣʱ����õ��ô˷���������ᵼ����Դ�˷ѡ�


====��¼1====

/**
 * ��Ϣ�շ����������ӿ�
 * 
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

	// ���ںŲ�����...
}


====��¼2====

// ��Ϣ�ṹ
class MyMessage {
	public static class Attachment {
		private String title; // ��������
		private String type; // ��������
		private String filename; // �����ļ���
		private String url; // ��������URL
	}
	
	private String sender; // ��Ϣ�������˺�����(me�������Լ����͵���Ϣ)
	private String receiver; // ��Ϣ������(�˺����ơ��绰�����Email��ַ�Կɣ������ڷ���)
	private String title; // ��Ϣ����(��ѡ)
	private String body; // ��Ϣ����
	private String type; // ��Ϣ����(text[Ĭ��]/xml/html)
	private String url; // ��Ϣ����URL(��ѡ)
	private Date generateTime; // ��Ϣ����ʱ��(yyyyMMddHHmmss)
	private Date expiration; // ��Ϣ����ʱ��(��ѡ����ʽͬ��)
	private Attachment[] attachments; // ��Ϣ����
}
