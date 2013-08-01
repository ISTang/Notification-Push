//引入依赖包
var net = require("net");
var fs = require("fs");
var async = require('async');
var _redis = require("redis");
var http = require("http");
var url = require("url");
//
var config = require(__dirname + '/config');
var utils = require(__dirname + '/utils');
var crypt = require(__dirname + '/client/crypt');
var protocol = require(__dirname + '/client/protocol');
var db = require(__dirname + '/db');

// 系统参数
const MAX_INACTIVE_TIME = config.MAX_INACTIVE_TIME;
const KEEPALVE_INIIALDELAY = config.KEEPALVE_INIIALDELAY;
const KEEPALVE_PROBEINTERVAL = config.KEEPALVE_PROBEINTERVAL;
const KEEPALVE_FAILURECOUNT = config.KEEPALVE_FAILURECOUNT;

const GRACE_EXIT_TIME = config.GRACE_EXIT_TIME;
const REDIS_SERVER = config.REDIS_SERVER;
const REDIS_PORT = config.REDIS_PORT;
//
const LOG_ENABLED = config.LOG_ENABLED;
const RECEIVE_RECEIPT_TIMEOUT = config.RECEIVE_RECEIPT_TIMEOUT;

// 体部长度是否采用字节单位
const BODY_BYTE_LENGTH = config.BODY_BYTE_LENGTH;

var logStream = LOG_ENABLED ? fs.createWriteStream("logs/notify.log", {"flags": "a"}) : null;

var loginIndex;
var myIndex;

var clientConns = []; // connId->账号/socket/上次活动时间
var queuedMsgs = []; // connId->待发送的消息表
var pendingMsgs = []; // connId->待确认的消息

var msgSub, msgPub;

Date.prototype.Format = utils.DateFormat;
String.prototype.trim = utils.StringTrim;
String.prototype.format = utils.StringFormat;

// socket.setKeepAlive(enable=false, [initialDelay], [probeInterval], [failureCount])
//
// 允许/取消 keep-alive 功能，并可以改写默认的 keep-alive 参数
//
// initialDelay: 设置接收到最后一个数据包后发出第一个 keepalive 探测之间的间隔时间(ms)
// probeInterval: 设置初始探测之后的 keepalive 探测间隔时间(ms)
// failureCount: 设置在 OS 关闭连接之前失败探测的最大次数
//
// probeInterval 和 failureCount 在 Windows OS 上不起作用(在win7下起作用)。
// 设置为0，相应的设置保持默认或最新值。
//
net.Socket.prototype.setKeepAlive = function(setting, msecs, interval, count) {
    if (this._handle && this._handle.setKeepAlive)
        this._handle.setKeepAlive(setting, ~~(msecs / 1000), ~~(interval / 1000), count);
}

/*
 * Print log
 */
function log(msg) {

    var now = new Date();
    var strDatetime = now.Format("yyyy-MM-dd HH:mm:ss");
    var buffer = "[" + strDatetime + "] " + msg + "[notify]";
    if (logStream != null) logStream.write(buffer + "\r\n");
    console.log(buffer);
}

function main(fn) {
    fn();
}

/**
 * 格式化消息
 * @param body 消息内容
 * @param secure 消息需要加密
 * @param receipt 消息需要确认
 * @returns {string}
 */
function formatMessage(body, secure, receipt) {

    return protocol.PUSH_MSG_CMD.format(secure ? "true" : "false", receipt ? "true" : "false", BODY_BYTE_LENGTH?Buffer.byteLength(body):body.length, body);
}

process.on('message', function (m, socket) {

    if (m.type === 'index') {

        loginIndex = m.loginIndex;
        myIndex = m.notifyIndex;
        //log('Notify process ' + loginIndex + '-' + myIndex + ' is running...');

        var channelId = "notify-" + loginIndex + "-" + myIndex;
        msgSub.subscribe(channelId);

        msgSub.on("message", function (pattern, content) {

            log("Received message at channel " + channelId + ": " + content);

            var o = JSON.parse(content);
            sendMessage(o.connId, o.msgId, o.message, o.msgKey, o.needReceipt);
        });

        msgSub.on("error", function (err) {
            log("Error " + err);
        });

        log("Waiting for message at channel " + channelId + "...");
    } else if (m.type === 'client') {

        var appId = m.appId;
        var accountId = m.accountId;
        var accountName = m.accountName;
        var connId = m.connId;
        var msgKey = m.msgKey;

        clientConns[connId] = {accountName:accountName, socket:socket, lastActiveTime: new Date()};
        db.recordLatestActivity(connId, "刚登录");

        if (KEEPALVE_PROBEINTERVAL!=0)
            socket.setKeepAlive(KEEPALVE_INIIALDELAY, KEEPALVE_PROBEINTERVAL, KEEPALVE_FAILURECOUNT);

        function keepAlive() {

            // 收到客户端心跳包
            clientConns[connId].lastActiveTime = new Date();
            log("[" + accountName + "] still alive");
            db.recordLatestActivity(connId, "接收到心跳信号");
        }

        function msgConfirmed() {

            // 收到客户端消息确认信号
            var pendingMsg = pendingMsgs[connId];
            if (typeof pendingMsg!="undefined") {

                var now = new Date();
                clientConns[connId].lastActiveTime = now;

                log("[" + accountName + "] received message " + pendingMsg.msgId);
                delete pendingMsgs[connId];

                db.recordMessageReceiptTime(connId, pendingMsg.msgId, now, function (err) {

                    if (err) return log(err);

                    log("[" + accountName + "] message " + pendingMsg.msgId + " confirmed");
                    db.recordLatestActivity(connId, "已确认一条消息");

                    var msgs = queuedMsgs[connId];
                    if (typeof msgs!="undefined") {
                        var nextMsg = msgs.shift();
                        if (nextMsg) {
                            log("One more message leaved the send queue on connection "+connId);
                            if (msgs.length==0) {
                                delete queuedMsgs[connId];
                            }
                            sendMessage(connId, nextMsg.msgId, nextMsg.message, nextMsg.msgKey, true);
                            return;
                        }
                    }
                    log("No more message(s) to send on connection "+connId);
                });

                if (pendingMsg.message.callback) {

                    var options = url.parse(pendingMsg.message.callback);
                    options.method = "POST";
                    //
                    log("Callback for message " + pendingMsg.msgId + "...");
                    var req = http.request(options, function (res) {
                        log("Callback STATUS: " + res.statusCode);
                    });
                    req.on("error", function (err) {
                        log("Callback error: " + err.message);
                    });
                    req.write(JSON.stringify({success: true}));
                    req.end();
                }
            } else {
                log("Unexpected message confirmation received for connection "+connId);
            }
        }

        function forwardMsg(appId, sender, receiver, msgText, sendId, handleResult) {

            var now = new Date();
            var message = JSON.parse(msgText);
            message.generate_time = now.Format("yyyyMMddHHmmss");
            message.sender = sender;

            log("Saving message for application " + appId);
            var msgId = db.saveMessage(appId, message, []);
            log("Message id: " + msgId);

            db.getAccountIdByName(receiver, function (err, accountId) {

                if (err) return handleResult(err);
                if(!accountId) return handleResult("Receiver " + receiver + " not exists");

                db.addMessageToAccounts(msgId, [accountId], appId, now);

                db.getActiveConnections(appId, accountId, function (err, connectionInfo) {

                    if (err) return handleResult(err);

                    log("Sending message " + msgId + " to connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "]...");
                    publishMessage(connectionInfo, msgId, message, sendId);
                    handleResult(null);
                });
            });

            function publishMessage(connectionInfo, msgId, message, sendId) {
                var o = {
                    connId: connectionInfo.connId,
                    msgId: msgId,
                    message: message,
                    msgKey: connectionInfo.msgKey,
                    sendId: sendId,
                    needReceipt: true};
                msgPub.publish(connectionInfo.channelId, JSON.stringify(o));
            }
        }

        function handleError(e) {

            log(">>>[" + accountName + "]" + e.toString() + "<<<");
        }

        function handleClose(hadError) {

            if (typeof clientConns[connId] != "undefined") {

                log("[" + accountName + "] " + (hadError ? "error" : "disconnected"));

                db.removeLoginInfo(connId, function (err) {

                    if (err) log(err);

                    clientConns.splice(clientConns.indexOf(connId), 1);
                });
            }
        }

        protocol.handleClientConnection2(socket, appId, accountName, msgKey,
            keepAlive, msgConfirmed, forwardMsg, handleError, handleClose, log);

        sendOfflineMessages(appId, accountId, config.MAX_OFFLINE_DAYS, connId, msgKey);
    }
});

function sendOfflineMessages(appId, accountId, maxOfflineDays, connId, msgKey) {

    log("Sending offline messages to account "+accountId+"...");
    db.getOfflineMessages(appId, accountId, maxOfflineDays, function (err, msgs) {

        if (err) return log(err);

        log("Total "+msgs.length+" offline message for account "+accountId+" found");
        msgs.forEach(function(msg) {

            sendMessage(connId, msg.msgId, msg.message, msgKey, msg.needReceipt);
        });
    });
}

function sendMessage(connId, msgId, msg, msgKey, needReceipt) {
    // TODO 增加对延迟发送消息的支持

    log("Sending message " + msgId + " on connection " + connId + "...");

    if (needReceipt) {
        if (typeof pendingMsgs[connId]!="undefined") {
            var msgs = queuedMsgs[connId];
            if (typeof msgs=="undefined") {
                msgs = [];
                queuedMsgs[connId] = msgs;
            }
            msgs.push({msgId:msgId,message:msg,msgKey:msgKey});
            return;
        }
    }

    if (needReceipt) {
        pendingMsgs[connId] = {msgId:msgId, message:msg};
    }
    var secure = (msgKey != null);
    var msgBody = JSON.stringify(msg);
    if (secure) msgBody = crypt.desEncrypt(msgBody, msgKey);
    var msgStr = formatMessage(msgBody, secure, needReceipt);
    //
    var socket = clientConns[connId].socket;
    socket.write(/*protocol.PNTP_FLAG+*/msgStr);
    db.recordMessageSentTime(connId, msgId, new Date(), needReceipt, function (err) {
        if (err) return log(err);
        log("Message " + msgId + " on connection " + connId + " sent");
        if (needReceipt) {
            db.recordLatestActivity(connId, "已发送一条消息，等待确认...");
            setTimeout(function() {
                var pendingMsg = pendingMsgs[connId];
                if (typeof pendingMsg!="undefined") {
                    log("Send message " + msgId + " on connection " + connId + " timeout");
                    if (typeof clientConns[connId]!="undefined") {
                        db.removeLoginInfo(connId, function (err) {
                            if (err) log(err);
                            var socket = clientConns[connId].socket;
                            clientConns.splice(clientConns.indexOf(connId), 1);
                            try {
                                socket.end(/*protocol.PNTP_FLAG+*/protocol.CLOSE_CONN_RES.format(protocol.INACTIVE_TIMEOUT_MSG.length, protocol.INACTIVE_TIMEOUT_MSG));
                            } catch (err) {
                                log(err);
                            }
                        });
                    }
                }
            }, RECEIVE_RECEIPT_TIMEOUT);
        } else {
            db.recordLatestActivity(connId, "已发送一条不需要确认的消息");
        }
    });
}

var exitTimer = null;
function aboutExit() {

    if (exitTimer) return;

    exitTimer = setTimeout(function () {

        log('Notify process will exit...');

        process.exit(0);

    }, GRACE_EXIT_TIME);
}

void main(function () {
    process.on('SIGINT', aboutExit)
    process.on('SIGTERM', aboutExit)

    msgSub = _redis.createClient(REDIS_PORT, REDIS_SERVER);
    msgSub.on("error", function (err) {
        log("Error " + err);
    });

    msgPub = _redis.createClient(REDIS_PORT, REDIS_SERVER);
    msgPub.on("error", function (err) {
        log("Error " + err);
    });

    function ensureActive() {

        var now = new Date();

		var inactiveConnIds = [];
        for (var connId in clientConns) {

            var lastActiveTime = clientConns[connId].lastActiveTime;
            var diff = (now.getTime() - lastActiveTime.getTime()); //ms
            if (diff > MAX_INACTIVE_TIME*2) {
			    inactiveConnIds.push(connId);
            }
        }
		
		for ( var i in inactiveConnIds) {
		    var connId = inactiveConnIds[i];
			                log("[" + clientConns[connId].accountName + "] inactive timeout");
			db.removeLoginInfo(connId, function (err) {
				if (err) log(err);
				var socket = clientConns[connId].socket;
				clientConns.splice(clientConns.indexOf(connId), 1);
				try {
					socket.end(/*protocol.PNTP_FLAG+*/protocol.CLOSE_CONN_RES.format(protocol.INACTIVE_TIMEOUT_MSG.length, protocol.INACTIVE_TIMEOUT_MSG));
				} catch (err) {
					log(err);
				}
			});
		}
    }
    setInterval(ensureActive, MAX_INACTIVE_TIME / 2);
});
