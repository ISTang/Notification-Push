/**
 * Created with JetBrains WebStorm.
 * User: joebin
 * Date: 13-3-17
 * Time: 上午10:05
 */
var config = require(__dirname + '/../config');
var utils = require(__dirname + '/../utils');
var crypt = require(__dirname + '/crypt');
var async = require('async');

String.prototype.trim = utils.StringTrim;
String.prototype.format = utils.StringFormat;

// 客户状态定义
const CLIENT_STATUS_JUST_CONNECTED = 0; // 刚连接
const CLIENT_STATUS_APPID_GOT = 1; // 已通过应用认证
const CLIENT_STATUS_USERNAME_GOT = 2; // 已通过用户认证
const CLIENT_STATUS_MSGKEY_ACK = 3; // 已确认消息密钥(如果需要的话)
const CLIENT_STATUS_ALIVEINT_ACK = 4; // 已确认心跳周期(可以接收心跳信号和推送消息了)

// 行结束符定义
const INPUT_RETURN = "\r\n";

// 头部字段名定义
const FIELD_BODY_BYTE_LENGTH = "ByteLength";
const FIELD_BODY_LENGTH = "Length";
const FIELD_ACTION_SUCCESS = "Success";
const FIELD_LOGIN_SECURE = "Secure";
const FIELD_LOGIN_PASSWORD = "Password";
const FIELD_MSG_ACCOUNT = "Account";
const FIELD_MSG_ID = "Id";
const FIELD_MSG_SECURE = "Secure";
const FIELD_MSG_RECEIPT = "Receipt";
//
const FIELD_REQ_ID = "Id";
const FIELD_PUBLIC_ACCOUNT = "Account";

// 错误消息
const INVALID_PROTOCOL_FLAG_MSG = "Invalid protocol flag or not exists";
const INVALID_ACTION_LINE_MSG = "Invalid action line";
const INVALID_FIELD_LINE_MSG = "Invalid field line";
const INVALID_LENGTH_VALUE_MSG = "Invalid length value";
const EXTRA_BODY_MSG = "Extra body found";
const TOO_BIG_BODY_MSG = "Too big body";
//
const WRONG_RESPONSE_MSG = "Wrong response";
const WRONG_REQUEST_MSG = "Wrong request";
//
const INVALID_SET_APPID_RES_BODY_MSG = "Invalid set appid response body";
const INVALID_APPID_MSG = "Check appid failed";
const INVALID_SET_USERNAME_RES_BODY_MSG = "Invalid set username response body";
const INVALID_USERNAME_MSG = "Check username failed";
//
const SERVER_ERROR_MSG = "Server error";
const LOGIN_TIMEOUT_MSG = "Login timeout";
const INACTIVE_TIMEOUT_MSG = "Inactive timeout";

// 心跳周期
const KEEP_ALIVE_INTERVAL = config.MAX_INACTIVE_TIME;

// 是否显示报文
const SHOW_PACKET = config.SHOW_PACKET;

// 是否跟踪socket
const TRACK_SOCKET = config.TRACK_SOCKET;

// 体部长度是否采用字节单位
const BODY_BYTE_LENGTH = config.BODY_BYTE_LENGTH;

// 平台(P)同客户端(C)通信内容
//
// 协议头部标志
//const PNTP_FLAG = "P\0N\0T\0P\0";
// 关闭连接
const CLOSE_CONN_RES = "CLOSE CONN\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {0}\r\n\r\n{1}"; // 体部: 错误内容(已包含)
//
// : 应用认证
//
// (1)请求(P-->C)
const GET_APPID_REQ = "GET APPID\r\n\r\n"; // 无体部
// (2)响应(C-->P): group(1)--体部长度
const GET_APPID_RES = "SET APPID\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {0}\r\n\r\n{1},{2}"; // {0}-体部长度, {1}-应用ID, {2}-应用密码
// (3)成功回复(P-->C)
const GET_APPID_SUCCESS_REP = "SET APPID\r\nSuccess: true\r\n\r\n"; // 无体部
// (4)失败回复({0}-{1}和{2}的长度和加1,{1}-错误代码,{2}-错误原因)(P-->C)
const GET_APPID_FAILED_REP = "SET APPID\r\nSuccess: false\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {0}\r\n\r\n{1},{2}"; // 体部: 错误代码及解释(已包含)
//
// :用户认证
//
// (1)请求 ({0}-是否安全登录, {1}-是否需要密码)(P-->C)
const GET_USERNAME_REQ = "GET USERNAME\r\nSecure: {0}\r\nPassword: {1}\r\n\r\n"; // 无体部
// (2)响应 (C-->P): group(1)--是否需要解密, group(2)--是否包含密码, group(3)--体部长度
const GET_USERNAME_RES = "SET USERNAME\r\nSecure: {0}\r\nPassword: {1}\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {2}\r\n\r\n{3}"; // {0}-体部是否加密, {1}-体部是否包含密码, {2}-体部长度, {3}-用户名(和密码)
// (3)成功回复 (P-->C)
const GET_USERNAME_SUCCESS_REP = "SET USERNAME\r\nSuccess: true\r\n\r\n"; // 无体部
// (4)失败回复 ({0}-{1}和{2}的长度和加1,{1}-错误代码,{2}-错误原因)(P-->C)
const GET_USERNAME_FAILED_REP = "SET USERNAME\r\nSuccess: false\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {0}\r\n\r\n{1},{2}"; // 体部: 错误代码及原因(已包含)
//
// :消息密钥
//
// (1)命令 ({0}-是否安全消息, {1}-{2}的长度, {2}-密钥内容)(P-->C)
const SET_MSGKEY_CMD = "SET MSGKEY\r\nSecure: {0}\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {1}\r\n\r\n{2}"; // 体部: 消息密钥(已包含)
// (2)确认 (C-->P)
const SET_MSGKEY_ACK = "SET MSGKEY\r\n\r\n"; // 不需要体部
//
// :设置心跳周期
//
// (1)命令 ({0}-{1}的长度, {1}-心跳周期(s)(P-->C)
const SET_ALIVEINT_CMD = "SET ALIVEINT\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {0}\r\n\r\n{1}"; // 体部: 心跳周期(已包含)
// (2)确认 (C-->P)
const SET_ALIVEINT_ACK = "SET ALIVEINT\r\n\r\n"; // 不需要体部
//
// :确认心跳信号
//
// (1)请求 (C-->P)
const SET_ALIVE_REQ_HEAD = /SET ALIVE\r\n\r\n/m; // 不需要体部
const SET_ALIVE_REQ = "SET ALIVE\r\n\r\n"; // 不需要体部
// (2)确认 (P-->C)
const SET_ALIVE_ACK = "SET ALIVE\r\n\r\n"; // 不需要体部
//
//
// :消息推送(平台发起)
//
// (1)命令 ({0}-消息是否加密, {1}-消息是否需要确认, {2}-{3}的长度, {3}-消息JSON对象)(P-->C)
const PUSH_MSG_CMD = "PUSH MSG\r\nSecure: {0}\r\nReceipt: {1}\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {2}\r\n\r\n{3}"; // 体部: 消息JSON对象(已包含)
// (2)确认 (C-->P)
const PUSH_MSG_ACK = "PUSH MSG\r\n\r\n"; // 不需要体部
//
// :发消息(客户端发起)
//
// (1)请求 ({0}-接收者账号 {1}-发送标识[回传用] {2}-消息是否加密, {3}-{4}的长度, {4}-消息JSON对象)(C-->P)
const SEND_MSG_REQ = "SEND MSG\r\nAccount: {0}\r\nId: {1}\r\nSecure: {2}\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {3}\r\n\r\n{4}"; // 体部: 消息JSON对象(已包含)
// (2)成功响应 ({0}-发送标识)(P-->C)
const SEND_MSG_SUCCESS_RES = "SEND MSG\r\nId: {0}\r\nSuccess: true\r\n\r\n"; // 无体部
// (3)失败响应 ({0}-发送标识 {1}-{2}和{3}的长度和加1,{2}-错误代码,{3}-错误原因)(P-->C)
const SEND_MSG_FAILED_RES = "SEND MSG\r\nId: {0}\r\nSuccess: false\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {1}\r\n\r\n{2},{3}"; // 体部: 错误代码及解释(已包含)
//
//
// :查询公众号
//
// (1)请求 ({0}-查询标识[回传用], {1}-{2}的长度, {2}查询条件)(C-->P)
const QUERY_PUBLIC_REQ = "QUERY PUBLIC\r\nId: {0}\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {1}\r\n\r\n{2}"; // 体部:公众号(支持模糊匹配)
// (2)成功响应 ({0}-查询标识, {1}-{2}的长度,{2}-公众号列表)(P-->C)
const QUERY_PUBLIC_SUCCESS_RES = "QUERY PUBLIC\r\nId: {0}\r\nSuccess: true\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {1}\r\n\r\n{2}"; // 体部:公众号列表[{\"name\":'<name>',\"avatar\":\"<avatar>\",\"desc\":'<desc>',\"type\":<type>},...]
// (3)失败响应 ({0}-查询标识, {1}-{2}和{3}的长度和加1,{2}-错误代码,{3}-错误原因)(P-->C)
const QUERY_PUBLIC_FAILED_RES = "QUERY PUBLIC\r\nId: {0}\r\nSuccess: false\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {1}\r\n\r\n{2},{3}"; // 体部:错误代码及解释(已包含)
//
// :关注公众号
//
// (1)请求 ({0}-公众号)(C-->P)
const FOLLOW_PUBLIC_REQ = "FOLLOW PUBLIC\r\nAccount: {0}\r\n\r\n"; // 无体部
// (2)成功响应 ({0}-公众号)(P-->C)
const FOLLOW_PUBLIC_SUCCESS_RES = "FOLLOW PUBLIC\r\nAccount: {0}\r\nSuccess: true\r\n\r\n"; // 无体部
// (3)失败响应 ({0}-公众号 {1}-{2}和{3}的长度和加1,{2}-错误代码,{3}-错误原因)(P-->C)
const FOLLOW_PUBLIC_FAILED_RES = "FOLLCOW PUBLIC\r\nAccount: {0}\r\nSuccess: false\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {1}\r\n\r\n{2},{3}"; // 体部:错误代码及解释(已包含)
//
// :取消关注公众号
//
// (1)请求 ({0}-公众号)(C-->P)
const UNFOLLOW_PUBLIC_REQ = "UNFOLLOW PUBLIC\r\nAccount: {0}\r\n\r\n"; // 无体部
// (2)成功响应 ({0}-公众号)(P-->C)
const UNFOLLOW_PUBLIC_SUCCESS_RES = "UNFOLLOW PUBLIC\r\nAccount: {0}\r\nSuccess: true\r\n\r\n"; // 无体部
// (3)失败响应 ({0}-公众号 {1}-{2}和{3}的长度和加1,{2}-错误代码,{3}-错误原因)(P-->C)
const UNFOLLOW_PUBLIC_FAILED_RES = "UNFOLLCOW PUBLIC\r\nAccount: {0}\r\nSuccess: false\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {1}\r\n\r\n{2},{3}"; // 体部:错误代码及解释(已包含)
//
// :获取已关注的公众号
//
// (1)请求 (C-->P)
const GET_FOLLOWED_REQ = "GET FOLLOWED\r\n\r\n"; // 无体部
// (2)成功响应 ({0}-{1}的长度,{1}-公众号列表)(P-->C)
const GET_FOLLOWED_SUCCESS_RES = "GET FOLLOWED\r\nSuccess: true\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {0}\r\n\r\n{1}"; // 体部:公众号列表(逗号分隔)
// (3)失败响应 ({0}-{1}和{2}的长度和加1,{1}-错误代码,{2}-错误原因)(P-->C)
const GET_FOLLOWED_FAILED_RES = "GET FOLLOWED\r\nSuccess: false\r\n" +
    (BODY_BYTE_LENGTH ? FIELD_BODY_BYTE_LENGTH + ":true\r\n" : "") +
    "Length: {0}\r\n\r\n{1},{2}"; // 体部:错误代码及解释(已包含)


// 处理连接(适合服务器端和客户端)
function handleConnection(socket, handlePacket, handleError, handleClose, logger, isClient) {

    var clientAddress = socket.remoteAddress + "[" + socket.remotePort + "]";

    var waitForHead = true; // 等待头部(false表示等待体部或不需要再等待)
    var headInput = ""; // 头部输入
    var actionLineFound = false; // 是否已找到动作行

    var head = ""; // 头部内容
    var body = ""; // 体部内容

    var action = ""; // 动作
    var target = ""; // 目标
    var fields = []; // 字段表
    var bodyLength = 0; // 体部长度

    socket.on("data", function (data) {

        var newInput = data.toString();
        if (!isClient && TRACK_SOCKET) logger.trace("[SOCKET] read from client " + clientAddress + ": " + newInput);

        var starr = [];

        if (waitForHead) {

            // 读取头部
            headInput += newInput;

            // 从首字符开始处理
            var emptyLineFound = false;
            do {

                // 初始化
                var line = "";

                // 寻找行结束符
                var pos = headInput.indexOf(INPUT_RETURN);
                if (pos == -1) {

                    // 未找到行结束符
                    return;
                }

                // 找到行结束符
                // 记录行内容(不包括行结束符)
                line = headInput.substring(0, pos);

                // 解析头部行
                if (!actionLineFound) {

                    // 检测协议头部标志
                    /*if (line.length<=PNTP_FLAG.length || line.substring(0, PNTP_FLAG.length)!=PNTP_FLAG) {

                     // 标志不对
                     logger.warn(INVALID_PROTOCOL_FLAG_MSG);
                     socket.end("[SOCKET] write to client " + clientAddress + ": " + INVALID_PROTOCOL_FLAG_MSG);
                     return;
                     }

                     line = line.substring(PNTP_FLAG.length);*/

                    // 动作行
                    //logger.debug("[Action line]"+line);
                    starr = line.split(/\s+/);
                    if (starr.length != 2) {

                        // 格式不对
                        if (!isClient && TRACK_SOCKET) logger.trace("[SOCKET] write to client " + clientAddress + ": Invalid action line(" + line + ")");
                        socket.write(/*PNTP_FLAG+*/CLOSE_CONN_RES.format(BODY_BYTE_LENGTH ? Buffer.byteLength(INVALID_ACTION_LINE_MSG) : INVALID_ACTION_LINE_MSG.length, INVALID_ACTION_LINE_MSG));
                        return;
                    }

                    action = starr[0].trim().toLocaleUpperCase(); // 动作
                    target = starr[1].trim().toLocaleUpperCase(); // 目标

                    //logger.debug("Action: " + action + ", target: " + target);
                    actionLineFound = true;
                } else if (line != "") {

                    // 属性行
                    //logger.debug("[Field line]" + line);
                    starr = line.split(/\s*:\s*/);
                    if (starr.length != 2) {

                        // 格式不对
                        if (!isClient && TRACK_SOCKET) logger.trace("[SOCKET] write to client " + clientAddress + ": Invalid field line(" + line + ")");
                        socket.write(/*PNTP_FLAG+*/CLOSE_CONN_RES.format(BODY_BYTE_LENGTH ? Buffer.byteLength(INVALID_FIELD_LINE_MSG) : INVALID_FIELD_LINE_MSG.length, INVALID_FIELD_LINE_MSG));
                        return;
                    }

                    var name = starr[0].trim().toLocaleUpperCase(); // 名字
                    var value = starr[1].trim(); // 值
                    fields[name] = value;

                    //logger.debug("Field: " + name + ", value: " + value);
                } else {

                    // 找到空行
                    //logger.debug("[Empty line]");
                    emptyLineFound = true;
                }

                // 记录头部
                head += line + INPUT_RETURN;

                // 为寻找下一个行结束符准备
                headInput = headInput.substring(pos + INPUT_RETURN.length);
            } while (!emptyLineFound); // 直到遇到空行

            // 头部读取完毕
            waitForHead = false;

            // 确定体部长度
            var bodyLengthFieldName = FIELD_BODY_LENGTH.toUpperCase();
            if (typeof fields[bodyLengthFieldName] != "undefined") {

                bodyLength = parseInt(fields[bodyLengthFieldName]);
                if (isNaN(bodyLength) || bodyLength < 0) {

                    // 体部长度字段值无效
                    if (!isClient && TRACK_SOCKET) logger.trace("[SOCKET] write to client " + clientAddress + ": " + INVALID_LENGTH_VALUE_MSG);
                    socket.write(/*PNTP_FLAG+*/CLOSE_CONN_RES.format(BODY_BYTE_LENGTH ? Buffer.byteLength(INVALID_LENGTH_VALUE_MSG) : INVALID_LENGTH_VALUE_MSG.length, INVALID_LENGTH_VALUE_MSG));
                    return;
                }
            }

            // 将余下输入内容作为体部
            body = headInput;
        } else {

            // 读取体部
            body += newInput;
        }

        // 检查体部内容
        if (body.length < bodyLength) {

            // 尚未读取完体部
            return;
        } else if (body.length > bodyLength) {

            // 将多余的内容作为下一个报文的头部输入
            headInput = body.substring(bodyLength);
            body = body.substring(0, bodyLength);
        } else {

            // 没有多余的输入
            headInput = "";
        }

        //logger.debug("body: "+body);

        if (SHOW_PACKET) logger.trace(head + body);

        // 处理新的报文
        handlePacket(socket, action, target, fields, body, logger, function (err) {
            if (err) logger.error(err);

            // 准备处理下一个报文
            waitForHead = true;
            actionLineFound = false;
            //
            head = "";
            body = "";
            //
            action = "";
            target = "";
            fields = [];
            bodyLength = 0;
        });
    });

    socket.on("error", handleError);
    socket.on("close", handleClose);
}

// 处理同客户端的连接
function handleClientConnection(socket, checkAppId, checkUsername, clientLogon, handleError, handleClose, logger) {

    var clientAddress = socket.remoteAddress + "[" + socket.remotePort + "]";
    var clientStatus = CLIENT_STATUS_JUST_CONNECTED;

    var appId; // 应用ID
    var secureLogin; //安全登录
    var protectKey; // 登录保护密钥
    var needLogin; // 需要登录
    var needPassword; // 需要登录密码
    var autoCreateAccount; // 自动创建账号
    var secureMessage; // 安全消息

    var accountId; // 账号ID
    var accountName; // 账号名称

    var msgKey = null; // 消息密钥(空代表不需要加密消息)

    handleConnection(socket, handlePacket, handleError, handleClose, logger);

    function handlePacket(socket, action, target, fields, body, logger, callback) {

        var checkResult;
        switch (clientStatus) {

            case CLIENT_STATUS_JUST_CONNECTED:

                // 检查动作和目标
                if (action != "SET" || target != "APPID") {

                    if (action == "CLOSE" && target == "CONN") {
                        return callback(clientAddress + " closed connection");
                    }

                    // 错误的响应
                    socket.end(/*PNTP_FLAG+*/CLOSE_CONN_RES.format(BODY_BYTE_LENGTH ? Buffer.byteLength(WRONG_RESPONSE_MSG) : WRONG_RESPONSE_MSG.length, WRONG_RESPONSE_MSG));
                    return callback("[SOCKET] write to client " + clientAddress + ": " + WRONG_RESPONSE_MSG + "(" + action + " " + target + ")");
                }

                logger.debug("Received application certificate response");

                // 解析体部内容
                starr = body.split(",");
                if (starr.length != 2) {

                    // 格式不对
                    socket.write(/*PNTP_FLAG+*/GET_APPID_FAILED_REP.format(1 + 1 + (BODY_BYTE_LENGTH ? Buffer.byteLength(INVALID_SET_APPID_RES_BODY_MSG) : INVALID_SET_APPID_RES_BODY_MSG.length),
                        1, INVALID_SET_APPID_RES_BODY_MSG));
                    return callback("[SOCKET] write to client " + clientAddress + ": " + INVALID_SET_APPID_RES_BODY_MSG + "(" + body + ")");
                }

                // 检查应用 ID和密码
                checkAppId(starr[0], starr[1], function (checkResult) {
                    if (!checkResult.passed) {

                        // ID或密码不对
                        var ss = INVALID_APPID_MSG + ": " + checkResult.reason;
                        socket.write(/*PNTP_FLAG+*/GET_APPID_FAILED_REP.format(1 + 1 + (BODY_BYTE_LENGTH ? Buffer.byteLength(ss) : ss.length), 2, ss));
                        return callback("[SOCKET] write to client " + clientAddress + ": " + INVALID_APPID_MSG + ": " + starr[0] + "," + starr[1]);
                    }

                    // 回复客户端
                    if (TRACK_SOCKET) logger.trace("[SOCKET] write to client " + clientAddress + ": " + GET_APPID_SUCCESS_REP);
                    socket.write(/*PNTP_FLAG+*/GET_APPID_SUCCESS_REP);

                    // 记录应用信息
                    appId = starr[0];
                    secureLogin = checkResult.secureLogin;
                    protectKey = checkResult.protectKey;
                    needLogin = checkResult.needLogin;
                    needPassword = checkResult.needPassword;
                    autoCreateAccount = checkResult.autoCreateAccount;
                    secureMessage = checkResult.secureMessage;

                    if (needLogin) { // 需要登录

                        // 更新客户状态
                        clientStatus = CLIENT_STATUS_APPID_GOT;

                        // 发送用户认证请求
                        if (TRACK_SOCKET) logger.trace("[SOCKET] write to client " + clientAddress + ": Sending user certificate request...");
                        socket.write(/*PNTP_FLAG+*/GET_USERNAME_REQ.format(secureLogin ? "true" : "false",
                            needPassword ? "true" : "false"));
                    } else { // 不需要登录

                        // 设置用户信息
                        accountId = '<nologin>';
                        accountName = '<nologin>';

                        // 更新客户状态
                        clientStatus = CLIENT_STATUS_USERNAME_GOT;

                        // 发送消息密钥或心跳周期
                        nextStep();
                    }

                    callback();
                });

                break;

            case CLIENT_STATUS_APPID_GOT:

                // 检查动作和目标
                if (action != "SET" || target != "USERNAME") {

                    if (action == "CLOSE" && target == "CONN") {
                        return callback(clientAddress + " closed connection");
                    }

                    // 错误的响应
                    socket.end(/*PNTP_FLAG+*/CLOSE_CONN_RES.format(BODY_BYTE_LENGTH ? Buffer.byteLength(WRONG_RESPONSE_MSG) : WRONG_RESPONSE_MSG.length, WRONG_RESPONSE_MSG));
                    return callback("[SOCKET] write to client " + clientAddress + ": " + WRONG_RESPONSE_MSG + "(" + action + " " + target + ")");
                }

                logger.debug(clientAddress + " " + "Received user certificate response");

                // 最好先确认secure和password字段取值正确

                // 解密体部内容(如果需要)
                if (fields[FIELD_LOGIN_SECURE.toUpperCase()].toUpperCase() == "TRUE") {

                    body = crypt.rsaDecrypt(body, protectKey.private);
                }

                // 解析体部内容
                var uname;// 用户名
                var pwd; // 密码
                if (fields[FIELD_LOGIN_PASSWORD.toUpperCase()].toUpperCase() == "TRUE") {

                    var sarr = body.split(",");
                    if (sarr.length != 2) {

                        // 格式不对
                        socket.write(/*PNTP_FLAG+*/GET_USERNAME_FAILED_REP.format(1 + 1 + (BODY_BYTE_LENGTH ? Buffer.byteLength(INVALID_SET_USERNAME_RES_BODY_MSG) : INVALID_SET_USERNAME_RES_BODY_MSG.length),
                            1, INVALID_SET_USERNAME_RES_BODY_MSG));
                        return callback("[SOCKET] write to client " + clientAddress + ": " + INVALID_SET_USERNAME_RES_BODY_MSG + "(" + body + ")");
                    }

                    uname = sarr[0];
                    pwd = sarr[1];
                } else {

                    uname = body;
                    pwd = null;
                }

                // 检查用户名(和密码)
                checkUsername(uname, pwd, autoCreateAccount, function (checkResult) {
                    if (!checkResult.passed) {

                        // 用户名(或密码)不对
                        var ss = INVALID_USERNAME_MSG + ": " + checkResult.reason;
                        socket.write(/*PNTP_FLAG+*/GET_USERNAME_FAILED_REP.format(1 + 1 + (BODY_BYTE_LENGTH ? Buffer.byteLength(ss) : ss.length), 2, ss));
                        return callback("[SOCKET] write to client " + clientAddress + ": " + ss);
                    }

                    // 回复客户端
                    if (TRACK_SOCKET) logger.trace("[SOCKET] write to client " + clientAddress + ": Sending user certificate OK response");
                    socket.write(/*PNTP_FLAG+*/GET_USERNAME_SUCCESS_REP);

                    // 记录用户信息
                    accountId = checkResult.accountId;
                    accountName = checkResult.accountName;
                    //logger.trace("User "+accountId+"("+accountName+") logon");

                    // 发送消息密钥或心跳周期
                    nextStep();

                    callback();
                });

                break;

            case CLIENT_STATUS_USERNAME_GOT:

                // 检查动作和目标
                if (action != "SET" || target != "MSGKEY") {

                    if (action == "CLOSE" && target == "CONN") {
                        return callback(clientAddress + " closed connection");
                    }

                    // 错误的响应
                    socket.end(/*PNTP_FLAG+*/CLOSE_CONN_RES.format(BODY_BYTE_LENGTH ? Buffer.byteLength(WRONG_RESPONSE_MSG) : WRONG_RESPONSE_MSG.length, WRONG_RESPONSE_MSG));
                    return callback("[SOCKET] write to client " + clientAddress + ": " + WRONG_RESPONSE_MSG + "(" + action + " " + target + ")");
                }

                // 更新客户状态
                clientStatus = CLIENT_STATUS_MSGKEY_ACK;

                // 发送心跳周期
                var ss = KEEP_ALIVE_INTERVAL.toString();
                if (TRACK_SOCKET) logger.trace("[SOCKET] write to client " + clientAddress + ": Message key sent, sending keep alive interval...");
                socket.write(/*PNTP_FLAG+*/SET_ALIVEINT_CMD.format(BODY_BYTE_LENGTH ? Buffer.byteLength(ss) : ss.length, ss));
                callback();

                break;

            case CLIENT_STATUS_MSGKEY_ACK:

                // 检查动作和目标
                if (action != "SET" || target != "ALIVEINT") {

                    if (action == "CLOSE" && target == "CONN") {
                        return callback(clientAddress + " closed connection");
                    }

                    // 错误的响应
                    socket.end(/*PNTP_FLAG+*/WRONG_RESPONSE_MSG);
                    return callback("[SOCKET] write to client " + clientAddress + ": " + WRONG_RESPONSE_MSG + "(" + action + " " + target + ")");
                }

                // 等待应用推送消息和客户端发送心跳信号
                logger.debug(clientAddress + " " + "Keep alive interval sent, waiting for notify and/or keep alive...");
                clientLogon(socket, accountId, accountName, appId, msgKey, function (err) {

                    if (err) {
                        socket.end(/*PNTP_FLAG+*/SERVER_ERROR_MSG);
                        return callback("[SOCKET] write to client " + clientAddress + ": " + err);
                    }
                    // 更新客户状态
                    clientStatus = CLIENT_STATUS_ALIVEINT_ACK;
                    callback();
                });

                break;

            default:

                // 无法理解的动作
                callback(clientAddress + " " + "Unknown action: " + action + " " + target);
                break;
        }

        function nextStep() {
            if (!secureMessage) { // 不需要发送消息密钥

                // 更新客户状态
                clientStatus = CLIENT_STATUS_MSGKEY_ACK;

                // 发送心跳周期
                var ss = KEEP_ALIVE_INTERVAL.toString();
                if (TRACK_SOCKET) logger.trace("[SOCKET] write to client " + clientAddress + ": Sending keep alive interval...");
                socket.write(/*PNTP_FLAG+*/SET_ALIVEINT_CMD.format(BODY_BYTE_LENGTH ? Buffer.byteLength(ss) : ss.length, ss));
            } else { // 需要发送消息密钥

                // 更新客户状态
                clientStatus = CLIENT_STATUS_USERNAME_GOT;

                // 发送消息密钥
                msgKey = crypt.makeMsgKey();
                if (secureLogin) ss = crypt.rsaEncrypt(msgKey, protectKey.public);
                if (TRACK_SOCKET) logger.trace("[SOCKET] write to client " + clientAddress + ": Sending message key...");
                socket.write(/*PNTP_FLAG+*/SET_MSGKEY_CMD.format(secureLogin ? "true" : "false",
                    BODY_BYTE_LENGTH ? Buffer.byteLength(ss) : ss.length, ss));
            }
        }
    }
}

// 处理同客户端的连接#2
function handleClientConnection2(socket, appId, accountId, accountName, msgKey, keepAlive, msgConfirmed, forwardMsg, queryPublicAccounts, followPublicAccount, unfollowPublicAccount, getFollowedPublicAccounts, handleError, handleClose, logger) {

    var clientAddress = socket.remoteAddress + "[" + socket.remotePort + "]";

    handleConnection(socket, handlePacket, handleError, handleClose, logger);

    function handlePacket(socket, action, target, fields, body, logger, callback) {

        // 检查动作和目标
        if (action == "SET" && target == "ALIVE") {

            // 收到心跳信号, 回复
            keepAlive();
            if (TRACK_SOCKET) logger.trace("[SOCKET] write to client " + clientAddress + ": " + SET_ALIVE_ACK);
            socket.write(/*PNTP_FLAG+*/SET_ALIVE_ACK);
            callback();
        } else if (action == "PUSH" && target == "MSG") {

            // 收到消息确认信号
            msgConfirmed();
            callback();
        } else if (action == "SEND" && target == "MSG") {

            // 收到发送消息请求
            var receiver = fields[FIELD_MSG_ACCOUNT.toUpperCase()];
            var sendId = fields[FIELD_MSG_ID.toUpperCase()];
            var msgSecure = (fields[FIELD_MSG_SECURE.toUpperCase()].toUpperCase() == "TRUE");
            var msgText = body;
            if (msgSecure) {

                // 解密体部内容
                msgText = crypt.desDecrypt(body, msgKey);
            }
            forwardMsg(appId, accountId, accountName, receiver, msgText, sendId, function (err) {

                var ss = (err ? "0," + err : "");
                if (TRACK_SOCKET) logger.trace("[SOCKET] write to client " + clientAddress + ": Send message " + (err ? "ERROR" : "OK"));
                socket.write(/*PNTP_FLAG+*/err ? SEND_MSG_FAILED_RES.format(sendId, 2 + (BODY_BYTE_LENGTH ? Buffer.byteLength(err) : err.length), 0, err) :
                    SEND_MSG_SUCCESS_RES.format(sendId));
                callback();
            });
        } else if (action == "QUERY" && target == "PUBLIC") {

            // 收到查询公众号请求
            var reqId = fields[FIELD_REQ_ID.toUpperCase()];
            var publicAccount = body;
            queryPublicAccounts(publicAccount, function (err, publicAccounts) {

                var ss = (err ? "0," + err : JSON.stringify(publicAccounts));
                if (TRACK_SOCKET) logger.trace("[SOCKET] write to client " + clientAddress + ": Query public accounts " + (err ? "ERROR" : "OK"));
                socket.write(/*PNTP_FLAG+*/err ? QUERY_PUBLIC_FAILED_RES.format(reqId, 2 + (BODY_BYTE_LENGTH ? Buffer.byteLength(err) : err.length), 0, err) :
                    QUERY_PUBLIC_SUCCESS_RES.format(reqId, (BODY_BYTE_LENGTH ? Buffer.byteLength(ss) : ss.length), ss));
                callback();
            });
        } else if (action == "FOLLOW" && target == "PUBLIC") {

            // 收到关注公众号请求
            var publicAccount = fields[FIELD_PUBLIC_ACCOUNT.toUpperCase()];
            followPublicAccount(accountId, publicAccount, function (err) {

                var ss = (err ? "0," + err : "");
                if (TRACK_SOCKET) logger.trace("[SOCKET] write to client " + clientAddress + ": Follow public account " + (err ? "ERROR" : "OK"));
                socket.write(/*PNTP_FLAG+*/err ? FOLLOW_PUBLIC_FAILED_RES.format(publicAccount, 2 + (BODY_BYTE_LENGTH ? Buffer.byteLength(err) : err.length), 0, err) :
                    FOLLOW_PUBLIC_SUCCESS_RES.format(publicAccount));

                var PUBLIC_ACCOUNT_FOLLOWED_EVENT = JSON.stringify({title: 'FOLLOWED_EVENT', body: accountName, generate_time: new Date()});
                forwardMsg(appId, accountId, accountName, publicAccount, PUBLIC_ACCOUNT_FOLLOWED_EVENT, "followPublicAccount", callback);
            });
        } else if (action == "UNFOLLOW" && target == "PUBLIC") {

            // 收到取消关注公众号请求
            var publicAccount = fields[FIELD_PUBLIC_ACCOUNT.toUpperCase()];
            unfollowPublicAccount(accountId, publicAccount, function (err) {

                var ss = (err ? "0," + err : "");
                if (TRACK_SOCKET) logger.trace("[SOCKET] write to client " + clientAddress + ": Unfollow public account " + (err ? "ERROR" : "OK"));
                socket.write(/*PNTP_FLAG+*/err ? UNFOLLOW_PUBLIC_FAILED_RES.format(publicAccount, 2 + (BODY_BYTE_LENGTH ? Buffer.byteLength(err) : err.length), 0, err) :
                    UNFOLLOW_PUBLIC_SUCCESS_RES.format(publicAccount));

                var PUBLIC_ACCOUNT_UNFOLLOWED_EVENT = JSON.stringify({title: 'UNFOLLOWED_EVENT', body: accountName, generate_time: new Date()});
                forwardMsg(appId, accountId, accountName, publicAccount, PUBLIC_ACCOUNT_UNFOLLOWED_EVENT, "unfollowPublicAccount", callback);
            });
        } else if (action == "GET" && target == "FOLLOWED") {

            // 收到获取已关注的公众号请求
            getFollowedPublicAccounts(accountId, function (err, publicAccounts) {

                var ss = (err ? "0," + err : JSON.stringify(publicAccounts));
                if (TRACK_SOCKET) logger.trace("[SOCKET] write to client " + clientAddress + ": Get fellowed public accounts " + (err ? "ERROR" : "OK"));
                socket.write(/*PNTP_FLAG+*/err ? GET_FOLLOWED_FAILED_RES.format(2 + (BODY_BYTE_LENGTH ? Buffer.byteLength(err) : err.length), 0, err) :
                    GET_FOLLOWED_SUCCESS_RES.format((BODY_BYTE_LENGTH ? Buffer.byteLength(ss) : ss.length), ss));
                callback();
            });
        } else if (action == "CLOSE" && target == "CONN") {

            // 客户端主动断开连接
            logger.warn("[" + accountName + "] Close connection: " + body);
            callback();
        } else {

            // 无法理解的动作
            callback("[" + accountName + "] Unknown action: " + action + " " + target);
        }
    }
}

// 处理同服务器端的连接
function handleServerConnection(socket, clientId, getAppInfo, getUserInfo, setMsgKey, setLogon, setKeepAliveInterval, keepAlive, msgReceived, handleError, handleClose, logger) {
    var appInfo;

    handleConnection(socket, handlePacket, handleError, handleClose, logger, true);

    function handlePacket(socket, action, target, fields, body, logger, callback) {

        if (action == "GET" && target == "APPID") {

            // 应用认证请求, 发送当前应用ID及密码
            appInfo = getAppInfo();
            var password = utils.md5(appInfo.password);
            logger.debug("[" + clientId + "] Received application certificate request, sending...");
            socket.write(/*PNTP_FLAG+*/GET_APPID_RES.format((BODY_BYTE_LENGTH ? Buffer.byteLength(appInfo.id) : appInfo.id.length) + 1 + (BODY_BYTE_LENGTH ? Buffer.byteLength(password) : password.length),
                appInfo.id, password));
            callback();
        } else if (action == "SET" && target == "APPID") {

            // 应用认证回复
            var appOk = (fields[FIELD_ACTION_SUCCESS.toUpperCase()].toUpperCase() == "TRUE");
            if (appOk) {

                logger.debug("[" + clientId + "] Application certificate passed");
                callback();
            } else {

                callback("[" + clientId + "] Application certificate failed: " + body);
            }
        } else if (action == "GET" && target == "USERNAME") {

            // 用户认证请求, 发送用户名及密码
            logger.debug("[" + clientId + "] Received user certificate request, sending...");
            var userInfo = getUserInfo();
            if (fields[FIELD_LOGIN_SECURE.toUpperCase()].toUpperCase() == "TRUE") {

                // 安全登录
                var protectKey = appInfo.protectKey;
                if (fields[FIELD_LOGIN_PASSWORD.toUpperCase()].toUpperCase() == "TRUE") {

                    // 需要登录密码
                    var password = utils.md5(userInfo.password);
                    var ss = crypt.rsaEncrypt(userInfo.name + "," + password, protectKey.public);
                    logger.debug("[" + clientId + "] Getting username...");
                    socket.write(/*PNTP_FLAG+*/GET_USERNAME_RES.format("true", "true", BODY_BYTE_LENGTH ? Buffer.byteLength(ss) : ss.length, ss));
                } else {

                    // 不需要登录密码
                    var username = crypt.rsaEncrypt(userInfo.name, protectKey.public);
                    logger.debug("[" + clientId + "] Getting username...");
                    socket.write(/*PNTP_FLAG+*/GET_USERNAME_RES.format("true", "false", BODY_BYTE_LENGTH ? Buffer.byteLength(username) : username.length,
                        username));
                }
            } else {

                // 非安全登录
                if (fields[FIELD_LOGIN_PASSWORD.toUpperCase()].toUpperCase() == "TRUE") {

                    // 需要登录密码
                    var password = utils.md5(userInfo.password);
                    logger.debug("[" + clientId + "] Getting username...");
                    socket.write(/*PNTP_FLAG+*/GET_USERNAME_RES.format("false", "true", (BODY_BYTE_LENGTH ? Buffer.byteLength(userInfo.name) : userInfo.name.length) + 1 + (BODY_BYTE_LENGTH ? Buffer.byteLength(password) : password.length),
                        userInfo.name + "," + password));
                } else {

                    // 不需要登录密码
                    logger.debug("[" + clientId + "] Getting username...");
                    socket.write(/*PNTP_FLAG+*/GET_USERNAME_RES.format("false", "false", BODY_BYTE_LENGTH ? Buffer.byteLength(userInfo.name) : userInfo.name.length,
                        userInfo.name));
                }
            }
            callback();
        } else if (action == "SET" && target == "USERNAME") {

            // 用户认证回复
            var usernameOk = (fields[FIELD_ACTION_SUCCESS.toUpperCase()].toUpperCase() == "TRUE");
            if (usernameOk) {

                logger.debug("[" + clientId + "] User certificate passed");
                callback();
            } else {

                callback("[" + clientId + "] User certificate failed: " + body);
            }
        } else if (action == "SET" && target == "MSGKEY") {

            // 收到消息密钥
            var msgKey = body;
            if (fields[FIELD_LOGIN_SECURE.toUpperCase()].toUpperCase() == "TRUE") {

                var protectKey = appInfo.protectKey;
                msgKey = crypt.rsaDecrypt(msgKey, protectKey.private);
            }
            logger.debug("[" + clientId + "] " + SET_MSGKEY_ACK);
            socket.write(/*PNTP_FLAG+*/SET_MSGKEY_ACK);

            setMsgKey(msgKey);
            logger.debug("[" + clientId + "] Received message key: " + msgKey);
            callback();
        } else if (action == "SET" && target == "ALIVEINT") {

            // 设置心跳周期
            var keepAliveInterval = parseInt(body);
            logger.debug("[" + clientId + "] " + SET_ALIVEINT_ACK);
            socket.write(/*PNTP_FLAG+*/SET_ALIVEINT_ACK);

            setLogon();

            setKeepAliveInterval(keepAliveInterval);
            logger.debug("[" + clientId + "] Keep alive interval(ms): " + keepAliveInterval);
            callback();
        } else if (action == "SET" && target == "ALIVE") {

            // 收到心跳回复信号
            keepAlive();
            callback();
        } else if (action == "PUSH" && target == "MSG") {

            // 收到消息
            var secure = (fields[FIELD_MSG_SECURE.toUpperCase()].toUpperCase() == "TRUE");
            var receipt = (fields[FIELD_MSG_RECEIPT.toUpperCase()].toUpperCase() == "TRUE");
            var msg = body;

            //logger.debug("[" + clientId + "] One message received");
            msgReceived(msg, secure);
            if (receipt) {

                // 确认已收到消息
                logger.debug("[" + clientId + "] " + PUSH_MSG_ACK);
                socket.write(/*PNTP_FLAG+*/PUSH_MSG_ACK);
            }
            callback();
        } else if (action == "CLOSE" && target == "CONN") {

            // 服务器主动断开连接
            callback("[" + clientId + "] Server kickoff me: " + body);
        } else {

            // 无法理解的动作
            callback("[" + clientId + "] Unknown action: " + action + " " + target);
        }
    }
}

exports.CLIENT_STATUS_JUST_CONNECTED = CLIENT_STATUS_JUST_CONNECTED;
exports.CLIENT_STATUS_APPID_GOT = CLIENT_STATUS_APPID_GOT;
exports.CLIENT_STATUS_USERNAME_GOT = CLIENT_STATUS_USERNAME_GOT;
exports.CLIENT_STATUS_MSGKEY_ACK = CLIENT_STATUS_MSGKEY_ACK;
exports.CLIENT_STATUS_ALIVEINT_ACK = CLIENT_STATUS_ALIVEINT_ACK;

exports.INPUT_RETURN = INPUT_RETURN;

exports.FIELD_BODY_LENGTH = FIELD_BODY_LENGTH;
exports.FIELD_ACTION_SUCCESS = FIELD_ACTION_SUCCESS;
exports.FIELD_LOGIN_SECURE = FIELD_LOGIN_SECURE;
exports.FIELD_LOGIN_PASSWORD = FIELD_LOGIN_PASSWORD;
exports.FIELD_MSG_ACCOUNT = FIELD_MSG_ACCOUNT;
exports.FIELD_MSG_ID = FIELD_MSG_ID;
exports.FIELD_MSG_SECURE = FIELD_MSG_SECURE;
exports.FIELD_MSG_RECEIPT = FIELD_MSG_RECEIPT;

exports.INVALID_ACTION_LINE_MSG = INVALID_ACTION_LINE_MSG;
exports.INVALID_FIELD_LINE_MSG = INVALID_FIELD_LINE_MSG;
exports.INVALID_LENGTH_VALUE_MSG = INVALID_LENGTH_VALUE_MSG;
exports.EXTRA_BODY_MSG = EXTRA_BODY_MSG;
exports.TOO_BIG_BODY_MSG = TOO_BIG_BODY_MSG;

exports.WRONG_RESPONSE_MSG = WRONG_RESPONSE_MSG;
exports.WRONG_REQUEST_MSG = WRONG_REQUEST_MSG;

exports.INVALID_SET_APPID_RES_BODY_MSG = INVALID_SET_APPID_RES_BODY_MSG;
exports.INVALID_APPID_MSG = INVALID_APPID_MSG;
exports.INVALID_SET_USERNAME_RES_BODY_MSG = INVALID_SET_USERNAME_RES_BODY_MSG;
exports.INVALID_USERNAME_MSG = INVALID_USERNAME_MSG;
exports.SERVER_ERROR_MSG = SERVER_ERROR_MSG;
exports.LOGIN_TIMEOUT_MSG = LOGIN_TIMEOUT_MSG;
exports.INACTIVE_TIMEOUT_MSG = INACTIVE_TIMEOUT_MSG;

//exports.PNTP_FLAG = PNTP_FLAG;
exports.CLOSE_CONN_RES = CLOSE_CONN_RES;

exports.GET_APPID_REQ = GET_APPID_REQ;
exports.GET_APPID_RES = GET_APPID_RES
exports.GET_APPID_SUCCESS_REP = GET_APPID_SUCCESS_REP;
exports.GET_APPID_FAILED_REP = GET_APPID_FAILED_REP;

exports.GET_USERNAME_REQ = GET_USERNAME_REQ;
exports.GET_USERNAME_RES = GET_USERNAME_RES;
exports.GET_USERNAME_SUCCESS_REP = GET_USERNAME_SUCCESS_REP;
exports.GET_USERNAME_FAILED_REP = GET_USERNAME_FAILED_REP;

exports.SET_MSGKEY_CMD = SET_MSGKEY_CMD;
exports.SET_MSGKEY_ACK = SET_MSGKEY_ACK;
exports.SET_ALIVEINT_CMD = SET_ALIVEINT_CMD;
exports.SET_ALIVEINT_ACK = SET_ALIVEINT_ACK;

exports.SET_ALIVE_REQ_HEAD = SET_ALIVE_REQ_HEAD;
exports.SET_ALIVE_REQ = SET_ALIVE_REQ;
exports.SET_ALIVE_ACK = SET_ALIVE_ACK;

exports.PUSH_MSG_CMD = PUSH_MSG_CMD;
exports.PUSH_MSG_ACK = PUSH_MSG_ACK;

exports.SEND_MSG_REQ = SEND_MSG_REQ;

exports.QUERY_PUBLIC_REQ = QUERY_PUBLIC_REQ;

exports.FOLLOW_PUBLIC_REQ = FOLLOW_PUBLIC_REQ;

exports.UNFOLLOW_PUBLIC_REQ = UNFOLLOW_PUBLIC_REQ;

exports.GET_FOLLOWED_REQ = GET_FOLLOWED_REQ;

exports.handleClientConnection = handleClientConnection;
exports.handleClientConnection2 = handleClientConnection2;
exports.handleServerConnection = handleServerConnection;
