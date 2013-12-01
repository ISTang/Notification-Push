消息推送客户端API用法简单说明

1.初始化客户端

private PushNotificationClient mClient;
mClient = new PushNotificationClient(context); // context为Context实例，比如Activity
mClient.addListener(listener); // listener为MessageTransceiverListener接口实例


2.启动/停止消息收发器

A.启动消息收发器

AppParams appParams = new AppParams(APP_ID/*应用ID*/,
		APP_PASSWORD/*应用密码*/,
		LOGIN_PROTECT_KEY/*登录保护密钥*/);
LoginParams loginParams = new LoginParams(
		SERVER_HOST/*服务器地址*/,
		SERVER_PORT/*服务器端口*/,
 		CLIENT_ID/*客户端ID(可自动创建)*/,
		CLIENT_PASSWORD/*客户端密码*/);
NetworkParams networkParams = new NetworkParams(); // 网络参数：可设置连接超时、重试连接延迟、登录超时和读超时，默认值分别为3秒、3秒、1分钟和100毫秒

mClient.startMessageTransceiver(appParams, loginParams, networkParams);

参数:
  appParams 应用参数
  loginParams 登录参数
  networkParams 网络参数

注：消息收发器启动成功后，会自动登录并发送心跳信号，除非登录时遇到应用或用户认证失败的情况（此时会自动停止收发器并产生事件--onTransceiverStatus）。
 
B.停止消息收发器

mClient.stopMessageTransceiver();

C.响应消息收发器状态改变

改写 onTransceiverStatus(started) 方法

参数:
 started 是否已启动(true/false)

D.响应登录状态改变

改写 onLogining(logining) 方法和 onLoginStatus(code, text) 方法

参数:
 logining 是否正在登录(true/false)
 code 登录阶段或未登录原因代码(见下表)
 text 登录阶段或未登录原因文本

阶段代码及含义:

  code    text
=====================================
   0     登录成功。
   1     连接服务器 <服务器地址>
   2     已连接到服务器。
   3     校验应用ID和接入密码...
   4     应用认证通过。
   5     校验用户名和密码...
   6     用户认证通过。
   7     收到消息密钥。
   8     收到心跳周期: <n> 秒。
  -1     网络不可用！
  -2     连接已中断！
  -3     服务器不可用！
  -4     登录超时！
  -5     网络IO故障！
  -6     应用认证失败！
  -7     用户认证失败！
  -8     服务器错误！

   
3. 发送/接收消息

A.发送消息

MyMessage msg = new MyMessage();
msg.setReceiver(RECEIVER_ID/*接收者ID*/);
msg.setBody(CONTENT/*消息内容*/);
//设置其它字段(可选)
mClient.sendMessage(msgId/*消息ID，用于跟踪消息发送状态*/, msg);

B.响应消息发送状态改变

改写 onMessageSendStatus(msgId, code, text) 方法

参数:
 msgId 消息ID，发送时指定
 code 状态代码(见下表)
 text 状态描述
 
状态代码及含义:
  code    text
=====================================
   0     已确认--发送成功
   1     提交...
   2     已提交
  -1     未登录
  -2     数据错误！
  -3     提交失败！
  -4     发送失败(#<errcode>:<errmsg>)

C.响应接收到的新消息

改写 onNewMessageReceived(msg) 方法

参数:
  msg MyMessage实例

4.释放客户端

mClient.release();

注：在不再需要接收消息时，最好调用此方法，否则会导致资源浪费。


====附录1====

/**
 * 消息收发器侦听器接口
 * 
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

	// 公众号部分略...
}


====附录2====

// 消息结构
class MyMessage {
	public static class Attachment {
		private String title; // 附件标题
		private String type; // 附件类型
		private String filename; // 附件文件名
		private String url; // 附件下载URL
	}
	
	private String sender; // 消息发送者账号名称(me代表是自己发送的消息)
	private String receiver; // 消息接收者(账号名称、电话号码或Email地址皆可，仅用于发送)
	private String title; // 消息标题(可选)
	private String body; // 消息内容
	private String type; // 消息类型(text[默认]/xml/html)
	private String url; // 消息详情URL(可选)
	private Date generateTime; // 消息生成时间(yyyyMMddHHmmss)
	private Date expiration; // 消息过期时间(可选，格式同上)
	private Attachment[] attachments; // 消息附件
}
