#pragma once
#include "SocketComm.h"
#include "cMD5.h"

const int INVALID_ACTION_LINE = 10;
const int INVALID_FIELD_LINE = 20;
const int INVALID_BODY_LENGTH = 30;
const int INVALID_APP_INFO = 100;
const int INVALID_USER_INFO = 200;
const int SERVER_KICKOFF_ME = 300;
const int FEATURE_NOT_SUPPORTED = 1000;
const int SOCKET_WRITE_TIMEOUT = 3000;
const int SOCKET_WRITE_ERROR = 10000;

// 参数定义
#define MAX_INACTIVE_TIME (1000 * 60 * 5 * 1) // 心跳检测最长时间(ms)
#define SHOW_PACKET true // 显示接收到的每个报文
#define TRACK_SOCKET true // 显示发送出的每个报文
//#define BODY_BYTE_LENGTH // 发送报文的体部长度字段是否采用字节单位(未定义时采用字符单位)

// 客户状态定义
const auto CLIENT_STATUS_JUST_CONNECTED = 0; // 刚连接
const auto CLIENT_STATUS_APPID_GOT = 1; // 已通过应用认证
const auto CLIENT_STATUS_USERNAME_GOT = 2; // 已通过用户认证
const auto CLIENT_STATUS_MSGKEY_ACK = 3; // 已确认消息密钥(如果需要的话)
const auto CLIENT_STATUS_ALIVEINT_ACK = 4; // 已确认心跳周期(可以接收心跳信号和推送消息了)


// 行结束符定义
const auto INPUT_RETURN = "\r\n";

// 头部字段名定义
#define FIELD_BODY_BYTE_LENGTH "ByteLength"
//
const auto FIELD_BODY_LENGTH = "Length";
const auto FIELD_ACTION_SUCCESS = "Success";
const auto FIELD_LOGIN_SECURE = "Secure";
const auto FIELD_LOGIN_PASSWORD = "Password";
const auto FIELD_MSG_SECURE = "Secure";
const auto FIELD_MSG_RECEIPT = "Receipt";
const auto FIELD_ACTION_ID = "Id";
const auto FIELD_ACTION_ACCOUNT = "Account";
const auto FIELD_ACTION_ACCOUNTS = "Accounts";


// 错误消息
//const char* INVALID_PROTOCOL_FLAG_MSG = "Invalid protocol flag or not exists";
const auto INVALID_ACTION_LINE_MSG = "Invalid action line";
const auto INVALID_FIELD_LINE_MSG = "Invalid field line";
const auto INVALID_LENGTH_VALUE_MSG = "Invalid length value";
const auto EXTRA_BODY_MSG = "Extra body found";
const auto TOO_BIG_BODY_MSG = "Too big body";
//
const auto WRONG_RESPONSE_MSG = "Wrong response";
const auto WRONG_REQUEST_MSG = "Wrong request";
//
const auto INVALID_SET_APPID_RES_BODY_MSG = "Invalid set appid response body";
const auto INVALID_APPID_MSG = "Check appid failed";
const auto INVALID_SET_USERNAME_RES_BODY_MSG = "Invalid set username response body";
const auto INVALID_USERNAME_MSG = "Check username failed";
//
const auto SERVER_ERROR_MSG = "Server error";
const auto LOGIN_TIMEOUT_MSG = "Login timeout";
const auto INACTIVE_TIMEOUT_MSG = "Inactive timeout";


// 心跳周期
const auto KEEP_ALIVE_INTERVAL = MAX_INACTIVE_TIME;


// 平台(P)同客户端(C)通信内容
//
// 协议头部标志
//const auto PNTP_FLAG = "P\0N\0T\0P\0";

// 关闭连接
#ifdef BODY_BYTE_LENGTH
#define CLOSE_CONN_RES "CLOSE CONN\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s" // 体部: 错误内容(已包含)
#else
#define CLOSE_CONN_RES "CLOSE CONN\r\nLength: %d\r\n\r\n%s" // 体部: 错误内容(已包含)
#endif


//
// : 应用认证
//
// (1)请求(P-->C)
const auto GET_APPID_REQ = "GET APPID\r\n\r\n"; // 无体部
// (2)响应(C-->P): group(1)--体部长度
#ifdef BODY_BYTE_LENGTH
#define GET_APPID_RES "SET APPID\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s,%s" // {0}-体部长度, {1}-应用ID, {2}-应用密码
#else
#define GET_APPID_RES "SET APPID\r\nLength: %d\r\n\r\n%s,%s" // {0}-体部长度, {1}-应用ID, {2}-应用密码
#endif
// (3)成功回复(P-->C)
const auto GET_APPID_SUCCESS_REP = "SET APPID\r\nSuccess: true\r\n\r\n"; // 无体部
// (4)失败回复({0}-{1}和{2}的长度和加1,{1}-错误代码,{2}-错误原因)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define GET_APPID_FAILED_REP "SET APPID\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // 体部: 错误代码及解释(已包含)
#else
#define GET_APPID_FAILED_REP "SET APPID\r\nSuccess: false\r\nLength: %d\r\n\r\n%d,%s" // 体部: 错误代码及解释(已包含)
#endif
//
// :用户认证
//
// (1)请求 ({0}-是否安全登录, {1}-是否需要密码)(P-->C)
const auto GET_USERNAME_REQ = "GET USERNAME\r\nSecure: %s\r\nPassword: %d\r\n\r\n"; // 无体部
// (2)响应 (C-->P): group(1)--是否需要解密, group(2)--是否包含密码, group(3)--体部长度
#ifdef BODY_BYTE_LENGTH
#define GET_USERNAME_RES "SET USERNAME\r\nSecure: %s\r\nPassword: %s\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s" // {0}-体部是否加密, {1}-体部是否包含密码, {2}-体部长度, {3}-用户名(和密码)
#else
#define GET_USERNAME_RES "SET USERNAME\r\nSecure: %s\r\nPassword: %s\r\nLength: %d\r\n\r\n%s" // {0}-体部是否加密, {1}-体部是否包含密码, {2}-体部长度, {3}-用户名(和密码)
#endif
// (3)成功回复 (P-->C)
const auto GET_USERNAME_SUCCESS_REP = "SET USERNAME\r\nSuccess: true\r\n\r\n"; // 无体部
// (4)失败回复 ({0}-{1}和{2}的长度和加1,{1}-错误代码,{2}-错误原因)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define GET_USERNAME_FAILED_REP "SET USERNAME\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // 体部: 错误代码及原因(已包含)
#else
#define GET_USERNAME_FAILED_REP "SET USERNAME\r\nSuccess: false\r\nLength: %d\r\n\r\n%d,%s" // 体部: 错误代码及原因(已包含)
#endif
//
// :消息密钥
//
// (1)命令 ({0}-是否安全消息, {1}-{2}的长度, {2}-密钥内容)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define SET_MSGKEY_CMD "SET MSGKEY\r\nSecure: %s\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s" // 体部: 消息密钥(已包含)
#else
#define SET_MSGKEY_CMD = "SET MSGKEY\r\nSecure: %s\r\nLength: %d\r\n\r\n%s" // 体部: 消息密钥(已包含)
#endif
// (2)确认 (C-->P)
const auto SET_MSGKEY_ACK = "SET MSGKEY\r\n\r\n"; // 不需要体部
//
// :设置心跳周期
//
// (1)命令 ({0}-{1}的长度, {1}-心跳周期(s)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define SET_ALIVEINT_CMD "SET ALIVEINT\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d" // 体部: 心跳周期(已包含)
#else
#define SET_ALIVEINT_CMD "SET ALIVEINT\r\nLength: %d\r\n\r\n%d" // 体部: 心跳周期(已包含)
#endif
// (2)确认 (C-->P)
const auto SET_ALIVEINT_ACK = "SET ALIVEINT\r\n\r\n"; // 不需要体部
//
// :确认心跳信号
//
// (1)请求 (C-->P)
const auto SET_ALIVE_REQ_HEAD = "SET ALIVE\r\n\r\n"; // 不需要体部
const auto SET_ALIVE_REQ = "SET ALIVE\r\n\r\n"; // 不需要体部
// (2)确认 (P-->C)
const auto SET_ALIVE_ACK = "SET ALIVE\r\n\r\n"; // 不需要体部
//
//
// :消息推送(平台发起)
//
// (1)命令 ({0}-消息是否加密, {1}-消息是否需要确认, {2}-{3}的长度, {3}-消息对象)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define PUSH_MSG_CMD "PUSH MSG\r\nSecure: %s\r\nReceipt: %s\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s" // 体部: 消息对象(已包含)
#else
#define PUSH_MSG_CMD "PUSH MSG\r\nSecure: %s\r\nReceipt: %s\r\nLength: %d\r\n\r\n%s" // 体部: 消息对象(已包含)
#endif
// (2)确认 (C-->P)
const auto PUSH_MSG_ACK = "PUSH MSG\r\n\r\n"; // 不需要体部
//
// :广播消息(客户端发起)
//
// (1)请求 ({0}-发送标识[回传用] {1}-消息是否加密, {2}-{3}的长度, {3}-消息对象)(C-->P)
#ifdef BODY_BYTE_LENGTH
#define BROADCAST_MSG_REQ "BROADCAST MSG\r\nId: %s\r\nSecure: %s\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s" // 体部: 消息对象(已包含)
#else
#define BROADCAST_MSG_REQ "BROADCAST MSG\r\nId: %s\r\nSecure: %s\r\nLength: %d\r\n\r\n%s" // 体部: 消息对象(已包含)
#endif
// (2)成功响应 ({0}-发送标识)(P-->C)
const auto BROADCAST_MSG_SUCCESS_RES = "BROADCAST MSG\r\nId: %s\r\nSuccess: true\r\n\r\n"; // 无体部
// (3)失败响应 ({0}-发送标识 {1}-{2}和{3}的长度和加1,{2}-错误代码,{3}-错误原因)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define BROADCAST_MSG_FAILED_RES "BROADCAST MSG\r\nId: %s\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // 体部: 错误代码及解释(已包含)
#else
#define BROADCAST_MSG_FAILED_RES "BROADCAST MSG\r\nId: %s\r\nSuccess: false\r\nLength: %d\r\n\r\n%d,%s" // 体部: 错误代码及解释(已包含)
#endif
//
// :群发消息(客户端发起)
//
// (1)请求 ({0}-接收者账号[逗号分隔] {1}-发送标识[回传用] {2}-消息是否加密, {3}-{4}的长度, {4}-消息对象)(C-->P)
#ifdef BODY_BYTE_LENGTH
#define MULTICAST_MSG_REQ "MULTICAST MSG\r\nAccounts: %s\r\nId: %s\r\nSecure: %s\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s" // 体部: 消息对象(已包含)
#else
#define MULTICAST_MSG_REQ "MULTICAST MSG\r\nAccounts: %s\r\nId: %s\r\nSecure: %s\r\nLength: %d\r\n\r\n%s" // 体部: 消息对象(已包含)
#endif
// (2)成功响应 ({0}-发送标识)(P-->C)
const auto MULTICAST_MSG_SUCCESS_RES = "MULTICAST MSG\r\nId: %s\r\nSuccess: true\r\n\r\n"; // 无体部
// (3)失败响应 ({0}-发送标识 {1}-{2}和{3}的长度和加1,{2}-错误代码,{3}-错误原因)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define MULTICAST_MSG_FAILED_RES "MULTICAST MSG\r\nId: %s\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // 体部: 错误代码及解释(已包含)
#else
#define MULTICAST_MSG_FAILED_RES "MULTICAST MSG\r\nId: %s\r\nSuccess: false\r\nLength: %d\r\n\r\n%d,%s" // 体部: 错误代码及解释(已包含)
#endif
//
// :发消息(客户端发起)
//
// (1)请求 ({0}-接收者账号 {1}-发送标识[回传用] {2}-消息是否加密, {3}-{4}的长度, {4}-消息对象)(C-->P)
#ifdef BODY_BYTE_LENGTH
#define SEND_MSG_REQ "SEND MSG\r\nAccount: %s\r\nId: %s\r\nSecure: %s\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s}" // 体部: 消息对象(已包含)
#else
#define SEND_MSG_REQ "SEND MSG\r\nAccount: %s\r\nId: %s\r\nSecure: %s\r\nLength: %d\r\n\r\n%s" // 体部: 消息对象(已包含)
#endif
// (2)成功响应 ({0}-发送标识)(P-->C)
const auto SEND_MSG_SUCCESS_RES = "SEND MSG\r\nId: %s\r\nSuccess: true\r\n\r\n"; // 无体部
// (3)失败响应 ({0}-发送标识 {1}-{2}和{3}的长度和加1,{2}-错误代码,{3}-错误原因)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define SEND_MSG_FAILED_RES "SEND MSG\r\nId: %s\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // 体部: 错误代码及解释(已包含)
#else
#define SEND_MSG_FAILED_RES "SEND MSG\r\nId: %s\r\nSuccess: false\r\nLength: {1}\r\n\r\n%d,%s" // 体部: 错误代码及解释(已包含)
#endif
//
//
// :查询公众号
//
// (1)请求 ({0}-查询标识[回传用], {1}-{2}的长度, {2}查询条件)(C-->P)
#ifdef BODY_BYTE_LENGTH
#define QUERY_PUBLIC_REQ "QUERY PUBLIC\r\nId: %s\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s" // 体部:公众号(支持模糊匹配)
#else
#define QUERY_PUBLIC_REQ "QUERY PUBLIC\r\nId: %s\r\nLength: %d\r\n\r\n%s" // 体部:公众号(支持模糊匹配)
#endif
// (2)成功响应 ({0}-查询标识, {1}-{2}的长度,{2}-公众号列表)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define QUERY_PUBLIC_SUCCESS_RES "QUERY PUBLIC\r\nId: %d\r\nSuccess: true\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s" // 体部:公众号列表[{\"name\":'<name>',\"avatar\":\"<avatar>\",\"desc\":'<desc>',\"type\":<type>},...]
#else
#define QUERY_PUBLIC_SUCCESS_RES "QUERY PUBLIC\r\nId: %d\r\nSuccess: true\r\nLength: %d\r\n\r\n%s" // 体部:公众号列表[{\"name\":'<name>',\"avatar\":\"<avatar>\",\"desc\":'<desc>',\"type\":<type>},...]
#endif
// (3)失败响应 ({0}-查询标识, {1}-{2}和{3}的长度和加1,{2}-错误代码,{3}-错误原因)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define QUERY_PUBLIC_FAILED_RES "QUERY PUBLIC\r\nId: %s\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // 体部:错误代码及解释(已包含)
#else
#define QUERY_PUBLIC_FAILED_RES "QUERY PUBLIC\r\nId: %s\r\nSuccess: false\r\nLength: %d\r\n\r\n%d,%s" // 体部:错误代码及解释(已包含)
#endif
//
// :关注公众号
//
// (1)请求 ({0}-公众号)(C-->P)
const auto FOLLOW_PUBLIC_REQ = "FOLLOW PUBLIC\r\nAccount: %s\r\n\r\n"; // 无体部
// (2)成功响应 ({0}-公众号)(P-->C)
const auto FOLLOW_PUBLIC_SUCCESS_RES = "FOLLOW PUBLIC\r\nAccount: %s\r\nSuccess: true\r\n\r\n"; // 无体部
// (3)失败响应 ({0}-公众号 {1}-{2}和{3}的长度和加1,{2}-错误代码,{3}-错误原因)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define FOLLOW_PUBLIC_FAILED_RES "FOLLCOW PUBLIC\r\nAccount: %s\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // 体部:错误代码及解释(已包含)
#else
#define FOLLOW_PUBLIC_FAILED_RES "FOLLCOW PUBLIC\r\nAccount: %s\r\nSuccess: false\r\nLength: %d\r\n\r\n%d,%s" // 体部:错误代码及解释(已包含)
#endif
//
// :取消关注公众号
//
// (1)请求 ({0}-公众号)(C-->P)
const auto UNFOLLOW_PUBLIC_REQ = "UNFOLLOW PUBLIC\r\nAccount: %s\r\n\r\n"; // 无体部
// (2)成功响应 ({0}-公众号)(P-->C)
const auto UNFOLLOW_PUBLIC_SUCCESS_RES = "UNFOLLOW PUBLIC\r\nAccount: %s\r\nSuccess: true\r\n\r\n"; // 无体部
// (3)失败响应 ({0}-公众号 {1}-{2}和{3}的长度和加1,{2}-错误代码,{3}-错误原因)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define UNFOLLOW_PUBLIC_FAILED_RES "UNFOLLCOW PUBLIC\r\nAccount: %s\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // 体部:错误代码及解释(已包含)
#else
#define UNFOLLOW_PUBLIC_FAILED_RES "UNFOLLCOW PUBLIC\r\nAccount: %s\r\nSuccess: false\r\nLength: %d\r\n\r\n%d,%s"; // 体部:错误代码及解释(已包含)
#endif
//
// :获取已关注的公众号
//
// (1)请求 (C-->P)
const auto GET_FOLLOWED_REQ = "GET FOLLOWED\r\n\r\n"; // 无体部
// (2)成功响应 ({0}-{1}的长度,{1}-公众号列表)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define GET_FOLLOWED_SUCCESS_RES = "GET FOLLOWED\r\nSuccess: true\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s"; // 体部:公众号列表(逗号分隔)
#else
#define GET_FOLLOWED_SUCCESS_RES = "GET FOLLOWED\r\nSuccess: true\r\nLength: %d\r\n\r\n%s"; // 体部:公众号列表(逗号分隔)
#endif
// (3)失败响应 ({0}-{1}和{2}的长度和加1,{1}-错误代码,{2}-错误原因)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define GET_FOLLOWED_FAILED_RES "GET FOLLOWED\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // 体部:错误代码及解释(已包含)
#else
#define GET_FOLLOWED_FAILED_RES "GET FOLLOWED\r\nSuccess: false\r\nLength: %d\r\n\r\n%d,%s" // 体部:错误代码及解释(已包含)
#endif

typedef void (*LogFunc)(int level, const char *log);

typedef void (*HandleSocketErrorFunc)();
typedef void (*HandleSocketCloseFunc)();

typedef void (*CallbackFunc)(const char *err);

typedef void (*HandlePacketFunc)(SOCKET socket, const char *action, const char *target, std::map<const char *, const char *>fields, const char *body, LogFunc logger, CallbackFunc callback);

// 应用信息
struct AppInfo
{
	AppInfo() {}
	AppInfo(const std::string &id, const std::string &password, const std::string &protectKey)
	{
		this->id = id;
		this->password = password;
		this->protectKey = protectKey;
	}

	std::string id; // 应用ID
	std::string password; // 应用密码
	std::string protectKey; // 应用认证信息加密秘钥(如果需要加密)
};

// 登录信息
struct LoginInfo
{
	LoginInfo() {}
	LoginInfo(const std::string &username, const std::string &password)
	{
		this->username = username;
		this->password = password;
	}

	std::string username; // 用户名
	std::string password; // 密码
};

// 连接
class Connection : public CSocketComm  
{
public:
	Connection(void);
	virtual ~Connection(void);

protected:
	virtual void onConnected(void) = 0; // 连接成功事件
	virtual void onConnectFailed(void) = 0; // 连接失败事件
	virtual void onDisconnected(bool passive = true) = 0; // 连接断开事件

	virtual void onTextReceived(const std::string& text) = 0; // 接收到文本事件(utf8)
	virtual void onTextSent(const std::string& text) = 0; // 输出文本事件

	virtual void handlePacket(const std::string &action, const std::string &target, 
		std::map<std::string, std::string> &fields, const std::string &body) = 0; // 处理报文

	virtual void error(const std::string& log) = 0; // 输出错误(utf8)
	virtual void warn(const std::string& log) = 0; // 输出警告(utf8)
	virtual void info(const std::string& log) = 0; // 输出信息(utf8)
	virtual void debug(const std::string& log) = 0; // 输出调试(utf8)
	virtual void trace(const std::string& log) = 0; // 输出跟踪(utf8)

	bool write(const std::string& msg, bool end=false); // utf8

	std::string peerAddress; // 对方地址(IP[端口])
	bool canReconnect;

protected:
	virtual void OnDataReceived(const LPBYTE lpBuffer, DWORD dwCount);
	virtual void OnEvent(UINT uEvent, LPVOID lpvData);

	void initPacket(void); // 初始化报文

	char *unhandleInput;
	bool waitForHead; // 等待头部(false表示等待体部或不需要再等待)

	std::string  headInput; // 头部输入
	bool  actionLineFound; // 是否已找到动作行

	std::string  head; // 头部内容
	std::string  body; // 体部内容

	std::string  action; // 动作
	std::string  target; // 目标
	std::map<std::string, std::string> fields; // 字段表
	unsigned bodyLength; // 体部长度
};

// 客户端连接
class ClientConnection : public Connection
{
public:
	enum { LOGOUT=0, LOGINING=1, LOGON=2 };

public:
	ClientConnection(void);
	~ClientConnection(void);

	void setAppInfo(const AppInfo &appInfo);
	void setLoginInfo(const LoginInfo &loginInfo);
	void setServerInfo(const std::string &serverHost, int serverPort);
	void SetAutoReconnect(bool autoReconnect, int reconnectDelay);

	bool connect();
	void disconnect();

	bool send(const std::string &receiver, const std::string &msgId, const std::string &msg, bool secure=false); // utf8
	bool multicast(const std::vector<std::string> &receivers, const std::string &msgId, const std::string &msg, bool secure=false); // utf8
	bool broadcast(const std::string &msgId, const std::string &msg, bool secure=false); // utf8

private:
	static UINT WINAPI doConnect(LPVOID pParam);
	HANDLE connectHandle;
	bool disconnectNow;

protected:
	virtual void onLoginStatus(int nStatus) = 0;
	virtual void onAppCheckFailed(const std::string& reason) = 0; // utf8
	virtual void onUserCheckFailed(const std::string& reason) = 0; // utf8
	virtual void onMsgKeyReceived(const std::string& msgKey) = 0; // utf8
	virtual void onMaxInactiveTimeReceived(int nMaxInactiveTime) = 0;
	virtual void onMsgReceived(const std::string& msg) = 0; // utf8
	virtual void onMsgReplied(const std::string& msgId, bool success, const std::string& error) = 0; // utf8

	std::string msgKey;
	int maxInactiveTime; // client(self)
	time_t lastActiveTime; // server

protected:
	virtual void onConnected(void);
	virtual void onConnectFailed(void);
	virtual void onDisconnected(bool passive = true);

	virtual void handlePacket(const std::string &action, const std::string &target,
		std::map<std::string, std::string> &fields, const std::string &body);

	void startKeepAlive(void);
	void stopKeepAlive(void);
	
	static unsigned __stdcall keepAlive(void * pThis);

	AppInfo appInfo;
	LoginInfo loginInfo;
	std::string serverHost;
	int serverPort;
	bool autoReconnect;
	int reconnectDelay;

    bool clientLogon;
    bool clientLogining;

	HANDLE keepAliveHandle;

	cMD5 md5;
};
