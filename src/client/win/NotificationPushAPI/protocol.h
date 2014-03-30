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

// ��������
#define MAX_INACTIVE_TIME (1000 * 60 * 5 * 1) // ��������ʱ��(ms)
#define SHOW_PACKET true // ��ʾ���յ���ÿ������
#define TRACK_SOCKET true // ��ʾ���ͳ���ÿ������
//#define BODY_BYTE_LENGTH // ���ͱ��ĵ��岿�����ֶ��Ƿ�����ֽڵ�λ(δ����ʱ�����ַ���λ)

// �ͻ�״̬����
const auto CLIENT_STATUS_JUST_CONNECTED = 0; // ������
const auto CLIENT_STATUS_APPID_GOT = 1; // ��ͨ��Ӧ����֤
const auto CLIENT_STATUS_USERNAME_GOT = 2; // ��ͨ���û���֤
const auto CLIENT_STATUS_MSGKEY_ACK = 3; // ��ȷ����Ϣ��Կ(�����Ҫ�Ļ�)
const auto CLIENT_STATUS_ALIVEINT_ACK = 4; // ��ȷ����������(���Խ��������źź�������Ϣ��)


// �н���������
const auto INPUT_RETURN = "\r\n";

// ͷ���ֶ�������
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


// ������Ϣ
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


// ��������
const auto KEEP_ALIVE_INTERVAL = MAX_INACTIVE_TIME;


// ƽ̨(P)ͬ�ͻ���(C)ͨ������
//
// Э��ͷ����־
//const auto PNTP_FLAG = "P\0N\0T\0P\0";

// �ر�����
#ifdef BODY_BYTE_LENGTH
#define CLOSE_CONN_RES "CLOSE CONN\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s" // �岿: ��������(�Ѱ���)
#else
#define CLOSE_CONN_RES "CLOSE CONN\r\nLength: %d\r\n\r\n%s" // �岿: ��������(�Ѱ���)
#endif


//
// : Ӧ����֤
//
// (1)����(P-->C)
const auto GET_APPID_REQ = "GET APPID\r\n\r\n"; // ���岿
// (2)��Ӧ(C-->P): group(1)--�岿����
#ifdef BODY_BYTE_LENGTH
#define GET_APPID_RES "SET APPID\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s,%s" // {0}-�岿����, {1}-Ӧ��ID, {2}-Ӧ������
#else
#define GET_APPID_RES "SET APPID\r\nLength: %d\r\n\r\n%s,%s" // {0}-�岿����, {1}-Ӧ��ID, {2}-Ӧ������
#endif
// (3)�ɹ��ظ�(P-->C)
const auto GET_APPID_SUCCESS_REP = "SET APPID\r\nSuccess: true\r\n\r\n"; // ���岿
// (4)ʧ�ܻظ�({0}-{1}��{2}�ĳ��Ⱥͼ�1,{1}-�������,{2}-����ԭ��)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define GET_APPID_FAILED_REP "SET APPID\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // �岿: ������뼰����(�Ѱ���)
#else
#define GET_APPID_FAILED_REP "SET APPID\r\nSuccess: false\r\nLength: %d\r\n\r\n%d,%s" // �岿: ������뼰����(�Ѱ���)
#endif
//
// :�û���֤
//
// (1)���� ({0}-�Ƿ�ȫ��¼, {1}-�Ƿ���Ҫ����)(P-->C)
const auto GET_USERNAME_REQ = "GET USERNAME\r\nSecure: %s\r\nPassword: %d\r\n\r\n"; // ���岿
// (2)��Ӧ (C-->P): group(1)--�Ƿ���Ҫ����, group(2)--�Ƿ��������, group(3)--�岿����
#ifdef BODY_BYTE_LENGTH
#define GET_USERNAME_RES "SET USERNAME\r\nSecure: %s\r\nPassword: %s\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s" // {0}-�岿�Ƿ����, {1}-�岿�Ƿ��������, {2}-�岿����, {3}-�û���(������)
#else
#define GET_USERNAME_RES "SET USERNAME\r\nSecure: %s\r\nPassword: %s\r\nLength: %d\r\n\r\n%s" // {0}-�岿�Ƿ����, {1}-�岿�Ƿ��������, {2}-�岿����, {3}-�û���(������)
#endif
// (3)�ɹ��ظ� (P-->C)
const auto GET_USERNAME_SUCCESS_REP = "SET USERNAME\r\nSuccess: true\r\n\r\n"; // ���岿
// (4)ʧ�ܻظ� ({0}-{1}��{2}�ĳ��Ⱥͼ�1,{1}-�������,{2}-����ԭ��)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define GET_USERNAME_FAILED_REP "SET USERNAME\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // �岿: ������뼰ԭ��(�Ѱ���)
#else
#define GET_USERNAME_FAILED_REP "SET USERNAME\r\nSuccess: false\r\nLength: %d\r\n\r\n%d,%s" // �岿: ������뼰ԭ��(�Ѱ���)
#endif
//
// :��Ϣ��Կ
//
// (1)���� ({0}-�Ƿ�ȫ��Ϣ, {1}-{2}�ĳ���, {2}-��Կ����)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define SET_MSGKEY_CMD "SET MSGKEY\r\nSecure: %s\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s" // �岿: ��Ϣ��Կ(�Ѱ���)
#else
#define SET_MSGKEY_CMD = "SET MSGKEY\r\nSecure: %s\r\nLength: %d\r\n\r\n%s" // �岿: ��Ϣ��Կ(�Ѱ���)
#endif
// (2)ȷ�� (C-->P)
const auto SET_MSGKEY_ACK = "SET MSGKEY\r\n\r\n"; // ����Ҫ�岿
//
// :������������
//
// (1)���� ({0}-{1}�ĳ���, {1}-��������(s)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define SET_ALIVEINT_CMD "SET ALIVEINT\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d" // �岿: ��������(�Ѱ���)
#else
#define SET_ALIVEINT_CMD "SET ALIVEINT\r\nLength: %d\r\n\r\n%d" // �岿: ��������(�Ѱ���)
#endif
// (2)ȷ�� (C-->P)
const auto SET_ALIVEINT_ACK = "SET ALIVEINT\r\n\r\n"; // ����Ҫ�岿
//
// :ȷ�������ź�
//
// (1)���� (C-->P)
const auto SET_ALIVE_REQ_HEAD = "SET ALIVE\r\n\r\n"; // ����Ҫ�岿
const auto SET_ALIVE_REQ = "SET ALIVE\r\n\r\n"; // ����Ҫ�岿
// (2)ȷ�� (P-->C)
const auto SET_ALIVE_ACK = "SET ALIVE\r\n\r\n"; // ����Ҫ�岿
//
//
// :��Ϣ����(ƽ̨����)
//
// (1)���� ({0}-��Ϣ�Ƿ����, {1}-��Ϣ�Ƿ���Ҫȷ��, {2}-{3}�ĳ���, {3}-��Ϣ����)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define PUSH_MSG_CMD "PUSH MSG\r\nSecure: %s\r\nReceipt: %s\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s" // �岿: ��Ϣ����(�Ѱ���)
#else
#define PUSH_MSG_CMD "PUSH MSG\r\nSecure: %s\r\nReceipt: %s\r\nLength: %d\r\n\r\n%s" // �岿: ��Ϣ����(�Ѱ���)
#endif
// (2)ȷ�� (C-->P)
const auto PUSH_MSG_ACK = "PUSH MSG\r\n\r\n"; // ����Ҫ�岿
//
// :�㲥��Ϣ(�ͻ��˷���)
//
// (1)���� ({0}-���ͱ�ʶ[�ش���] {1}-��Ϣ�Ƿ����, {2}-{3}�ĳ���, {3}-��Ϣ����)(C-->P)
#ifdef BODY_BYTE_LENGTH
#define BROADCAST_MSG_REQ "BROADCAST MSG\r\nId: %s\r\nSecure: %s\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s" // �岿: ��Ϣ����(�Ѱ���)
#else
#define BROADCAST_MSG_REQ "BROADCAST MSG\r\nId: %s\r\nSecure: %s\r\nLength: %d\r\n\r\n%s" // �岿: ��Ϣ����(�Ѱ���)
#endif
// (2)�ɹ���Ӧ ({0}-���ͱ�ʶ)(P-->C)
const auto BROADCAST_MSG_SUCCESS_RES = "BROADCAST MSG\r\nId: %s\r\nSuccess: true\r\n\r\n"; // ���岿
// (3)ʧ����Ӧ ({0}-���ͱ�ʶ {1}-{2}��{3}�ĳ��Ⱥͼ�1,{2}-�������,{3}-����ԭ��)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define BROADCAST_MSG_FAILED_RES "BROADCAST MSG\r\nId: %s\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // �岿: ������뼰����(�Ѱ���)
#else
#define BROADCAST_MSG_FAILED_RES "BROADCAST MSG\r\nId: %s\r\nSuccess: false\r\nLength: %d\r\n\r\n%d,%s" // �岿: ������뼰����(�Ѱ���)
#endif
//
// :Ⱥ����Ϣ(�ͻ��˷���)
//
// (1)���� ({0}-�������˺�[���ŷָ�] {1}-���ͱ�ʶ[�ش���] {2}-��Ϣ�Ƿ����, {3}-{4}�ĳ���, {4}-��Ϣ����)(C-->P)
#ifdef BODY_BYTE_LENGTH
#define MULTICAST_MSG_REQ "MULTICAST MSG\r\nAccounts: %s\r\nId: %s\r\nSecure: %s\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s" // �岿: ��Ϣ����(�Ѱ���)
#else
#define MULTICAST_MSG_REQ "MULTICAST MSG\r\nAccounts: %s\r\nId: %s\r\nSecure: %s\r\nLength: %d\r\n\r\n%s" // �岿: ��Ϣ����(�Ѱ���)
#endif
// (2)�ɹ���Ӧ ({0}-���ͱ�ʶ)(P-->C)
const auto MULTICAST_MSG_SUCCESS_RES = "MULTICAST MSG\r\nId: %s\r\nSuccess: true\r\n\r\n"; // ���岿
// (3)ʧ����Ӧ ({0}-���ͱ�ʶ {1}-{2}��{3}�ĳ��Ⱥͼ�1,{2}-�������,{3}-����ԭ��)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define MULTICAST_MSG_FAILED_RES "MULTICAST MSG\r\nId: %s\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // �岿: ������뼰����(�Ѱ���)
#else
#define MULTICAST_MSG_FAILED_RES "MULTICAST MSG\r\nId: %s\r\nSuccess: false\r\nLength: %d\r\n\r\n%d,%s" // �岿: ������뼰����(�Ѱ���)
#endif
//
// :����Ϣ(�ͻ��˷���)
//
// (1)���� ({0}-�������˺� {1}-���ͱ�ʶ[�ش���] {2}-��Ϣ�Ƿ����, {3}-{4}�ĳ���, {4}-��Ϣ����)(C-->P)
#ifdef BODY_BYTE_LENGTH
#define SEND_MSG_REQ "SEND MSG\r\nAccount: %s\r\nId: %s\r\nSecure: %s\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s}" // �岿: ��Ϣ����(�Ѱ���)
#else
#define SEND_MSG_REQ "SEND MSG\r\nAccount: %s\r\nId: %s\r\nSecure: %s\r\nLength: %d\r\n\r\n%s" // �岿: ��Ϣ����(�Ѱ���)
#endif
// (2)�ɹ���Ӧ ({0}-���ͱ�ʶ)(P-->C)
const auto SEND_MSG_SUCCESS_RES = "SEND MSG\r\nId: %s\r\nSuccess: true\r\n\r\n"; // ���岿
// (3)ʧ����Ӧ ({0}-���ͱ�ʶ {1}-{2}��{3}�ĳ��Ⱥͼ�1,{2}-�������,{3}-����ԭ��)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define SEND_MSG_FAILED_RES "SEND MSG\r\nId: %s\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // �岿: ������뼰����(�Ѱ���)
#else
#define SEND_MSG_FAILED_RES "SEND MSG\r\nId: %s\r\nSuccess: false\r\nLength: {1}\r\n\r\n%d,%s" // �岿: ������뼰����(�Ѱ���)
#endif
//
//
// :��ѯ���ں�
//
// (1)���� ({0}-��ѯ��ʶ[�ش���], {1}-{2}�ĳ���, {2}��ѯ����)(C-->P)
#ifdef BODY_BYTE_LENGTH
#define QUERY_PUBLIC_REQ "QUERY PUBLIC\r\nId: %s\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s" // �岿:���ں�(֧��ģ��ƥ��)
#else
#define QUERY_PUBLIC_REQ "QUERY PUBLIC\r\nId: %s\r\nLength: %d\r\n\r\n%s" // �岿:���ں�(֧��ģ��ƥ��)
#endif
// (2)�ɹ���Ӧ ({0}-��ѯ��ʶ, {1}-{2}�ĳ���,{2}-���ں��б�)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define QUERY_PUBLIC_SUCCESS_RES "QUERY PUBLIC\r\nId: %d\r\nSuccess: true\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s" // �岿:���ں��б�[{\"name\":'<name>',\"avatar\":\"<avatar>\",\"desc\":'<desc>',\"type\":<type>},...]
#else
#define QUERY_PUBLIC_SUCCESS_RES "QUERY PUBLIC\r\nId: %d\r\nSuccess: true\r\nLength: %d\r\n\r\n%s" // �岿:���ں��б�[{\"name\":'<name>',\"avatar\":\"<avatar>\",\"desc\":'<desc>',\"type\":<type>},...]
#endif
// (3)ʧ����Ӧ ({0}-��ѯ��ʶ, {1}-{2}��{3}�ĳ��Ⱥͼ�1,{2}-�������,{3}-����ԭ��)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define QUERY_PUBLIC_FAILED_RES "QUERY PUBLIC\r\nId: %s\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // �岿:������뼰����(�Ѱ���)
#else
#define QUERY_PUBLIC_FAILED_RES "QUERY PUBLIC\r\nId: %s\r\nSuccess: false\r\nLength: %d\r\n\r\n%d,%s" // �岿:������뼰����(�Ѱ���)
#endif
//
// :��ע���ں�
//
// (1)���� ({0}-���ں�)(C-->P)
const auto FOLLOW_PUBLIC_REQ = "FOLLOW PUBLIC\r\nAccount: %s\r\n\r\n"; // ���岿
// (2)�ɹ���Ӧ ({0}-���ں�)(P-->C)
const auto FOLLOW_PUBLIC_SUCCESS_RES = "FOLLOW PUBLIC\r\nAccount: %s\r\nSuccess: true\r\n\r\n"; // ���岿
// (3)ʧ����Ӧ ({0}-���ں� {1}-{2}��{3}�ĳ��Ⱥͼ�1,{2}-�������,{3}-����ԭ��)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define FOLLOW_PUBLIC_FAILED_RES "FOLLCOW PUBLIC\r\nAccount: %s\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // �岿:������뼰����(�Ѱ���)
#else
#define FOLLOW_PUBLIC_FAILED_RES "FOLLCOW PUBLIC\r\nAccount: %s\r\nSuccess: false\r\nLength: %d\r\n\r\n%d,%s" // �岿:������뼰����(�Ѱ���)
#endif
//
// :ȡ����ע���ں�
//
// (1)���� ({0}-���ں�)(C-->P)
const auto UNFOLLOW_PUBLIC_REQ = "UNFOLLOW PUBLIC\r\nAccount: %s\r\n\r\n"; // ���岿
// (2)�ɹ���Ӧ ({0}-���ں�)(P-->C)
const auto UNFOLLOW_PUBLIC_SUCCESS_RES = "UNFOLLOW PUBLIC\r\nAccount: %s\r\nSuccess: true\r\n\r\n"; // ���岿
// (3)ʧ����Ӧ ({0}-���ں� {1}-{2}��{3}�ĳ��Ⱥͼ�1,{2}-�������,{3}-����ԭ��)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define UNFOLLOW_PUBLIC_FAILED_RES "UNFOLLCOW PUBLIC\r\nAccount: %s\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // �岿:������뼰����(�Ѱ���)
#else
#define UNFOLLOW_PUBLIC_FAILED_RES "UNFOLLCOW PUBLIC\r\nAccount: %s\r\nSuccess: false\r\nLength: %d\r\n\r\n%d,%s"; // �岿:������뼰����(�Ѱ���)
#endif
//
// :��ȡ�ѹ�ע�Ĺ��ں�
//
// (1)���� (C-->P)
const auto GET_FOLLOWED_REQ = "GET FOLLOWED\r\n\r\n"; // ���岿
// (2)�ɹ���Ӧ ({0}-{1}�ĳ���,{1}-���ں��б�)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define GET_FOLLOWED_SUCCESS_RES = "GET FOLLOWED\r\nSuccess: true\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%s"; // �岿:���ں��б�(���ŷָ�)
#else
#define GET_FOLLOWED_SUCCESS_RES = "GET FOLLOWED\r\nSuccess: true\r\nLength: %d\r\n\r\n%s"; // �岿:���ں��б�(���ŷָ�)
#endif
// (3)ʧ����Ӧ ({0}-{1}��{2}�ĳ��Ⱥͼ�1,{1}-�������,{2}-����ԭ��)(P-->C)
#ifdef BODY_BYTE_LENGTH
#define GET_FOLLOWED_FAILED_RES "GET FOLLOWED\r\nSuccess: false\r\n"##FIELD_BODY_BYTE_LENGTH##":true\r\nLength: %d\r\n\r\n%d,%s" // �岿:������뼰����(�Ѱ���)
#else
#define GET_FOLLOWED_FAILED_RES "GET FOLLOWED\r\nSuccess: false\r\nLength: %d\r\n\r\n%d,%s" // �岿:������뼰����(�Ѱ���)
#endif

typedef void (*LogFunc)(int level, const char *log);

typedef void (*HandleSocketErrorFunc)();
typedef void (*HandleSocketCloseFunc)();

typedef void (*CallbackFunc)(const char *err);

typedef void (*HandlePacketFunc)(SOCKET socket, const char *action, const char *target, std::map<const char *, const char *>fields, const char *body, LogFunc logger, CallbackFunc callback);

// Ӧ����Ϣ
struct AppInfo
{
	AppInfo() {}
	AppInfo(const std::string &id, const std::string &password, const std::string &protectKey)
	{
		this->id = id;
		this->password = password;
		this->protectKey = protectKey;
	}

	std::string id; // Ӧ��ID
	std::string password; // Ӧ������
	std::string protectKey; // Ӧ����֤��Ϣ������Կ(�����Ҫ����)
};

// ��¼��Ϣ
struct LoginInfo
{
	LoginInfo() {}
	LoginInfo(const std::string &username, const std::string &password)
	{
		this->username = username;
		this->password = password;
	}

	std::string username; // �û���
	std::string password; // ����
};

// ����
class Connection : public CSocketComm  
{
public:
	Connection(void);
	virtual ~Connection(void);

protected:
	virtual void onConnected(void) = 0; // ���ӳɹ��¼�
	virtual void onConnectFailed(void) = 0; // ����ʧ���¼�
	virtual void onDisconnected(bool passive = true) = 0; // ���ӶϿ��¼�

	virtual void onTextReceived(const std::string& text) = 0; // ���յ��ı��¼�(utf8)
	virtual void onTextSent(const std::string& text) = 0; // ����ı��¼�

	virtual void handlePacket(const std::string &action, const std::string &target, 
		std::map<std::string, std::string> &fields, const std::string &body) = 0; // ������

	virtual void error(const std::string& log) = 0; // �������(utf8)
	virtual void warn(const std::string& log) = 0; // �������(utf8)
	virtual void info(const std::string& log) = 0; // �����Ϣ(utf8)
	virtual void debug(const std::string& log) = 0; // �������(utf8)
	virtual void trace(const std::string& log) = 0; // �������(utf8)

	bool write(const std::string& msg, bool end=false); // utf8

	std::string peerAddress; // �Է���ַ(IP[�˿�])
	bool canReconnect;

protected:
	virtual void OnDataReceived(const LPBYTE lpBuffer, DWORD dwCount);
	virtual void OnEvent(UINT uEvent, LPVOID lpvData);

	void initPacket(void); // ��ʼ������

	char *unhandleInput;
	bool waitForHead; // �ȴ�ͷ��(false��ʾ�ȴ��岿����Ҫ�ٵȴ�)

	std::string  headInput; // ͷ������
	bool  actionLineFound; // �Ƿ����ҵ�������

	std::string  head; // ͷ������
	std::string  body; // �岿����

	std::string  action; // ����
	std::string  target; // Ŀ��
	std::map<std::string, std::string> fields; // �ֶα�
	unsigned bodyLength; // �岿����
};

// �ͻ�������
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
