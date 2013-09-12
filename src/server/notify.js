//引入依赖包
var net = require("net");
var fs = require("fs");
var async = require('async');
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
//
const LOG_ENABLED = config.LOG_ENABLED;
const TRACK_SOCKET = config.TRACK_SOCKET;
const RECEIVE_RECEIPT_TIMEOUT = config.RECEIVE_RECEIPT_TIMEOUT;

// 体部长度是否采用字节单位
const BODY_BYTE_LENGTH = config.BODY_BYTE_LENGTH;

const USER_AVATAR = config.USER_AVATAR;

var logStream = fs.createWriteStream(__dirname + "/logs/notify.log", {"flags": "a"});

var loginIndex;
var myIndex;

var clientConns = []; // connId->账号/socket/上次活动时间/clientAddress
var queuedMsgs = []; // connId->待发送的消息表
var pendingMsgs = []; // connId->待确认的消息

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
net.Socket.prototype.setKeepAlive = function (setting, msecs, interval, count) {
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
    if (LOG_ENABLED) console.log(buffer);
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

    return protocol.PUSH_MSG_CMD.format(secure ? "true" : "false", receipt ? "true" : "false", BODY_BYTE_LENGTH ? Buffer.byteLength(body) : body.length, body);
}

process.on('message', function (m, socket) {

    if (m.type === 'index') {

        loginIndex = m.loginIndex;
        myIndex = m.notifyIndex;
        //log('Notify process ' + loginIndex + '-' + myIndex + ' is running...');

        var channelId = "notify-" + loginIndex + "-" + myIndex;
        db.redisPool.acquire(function (err, msgSub) {
            if (err) {
                log("Process will exit: " + err);
                process.exit(1);
            } else {
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
            }
        });
    } else if (m.type === 'client') {

        var clientAddress = socket.remoteAddress + "[" + socket.remotePort + "]";

        var appId = m.appId;
        var accountId = m.accountId;
        var accountName = m.accountName;
        var connId = m.connId;
        var msgKey = m.msgKey;

        clientConns[connId] = {accountName: accountName, socket: socket, lastActiveTime: new Date(),
            clientAddress: socket.remoteAddress + "[" + socket.remotePort + "]"};
        db.redisPool.acquire(function (err, redis) {
            if (err) {
                log("Process will exit: " + err);
                process.exit(2);
            } else {
                db.recordLatestActivity(redis, connId, "刚登录", function (err) {
                    db.redisPool.release(redis);
                    if (err) {
                        log("Process will exit: " + err);
                        process.exit(3);
                    }
                });
            }
        });

        if (KEEPALVE_PROBEINTERVAL != 0)
            socket.setKeepAlive(KEEPALVE_INIIALDELAY, KEEPALVE_PROBEINTERVAL, KEEPALVE_FAILURECOUNT);

        function keepAlive() {

            // 收到客户端心跳包
            clientConns[connId].lastActiveTime = new Date();
            log("Client " + clientConns[connId].clientAddress +"[" + accountName + "] still alive");
            db.redisPool.acquire(function (err, redis) {
                if (err) {
                    log("Process will exit: " + err);
                    process.exit(4);
                } else {
                    db.recordLatestActivity(redis, connId, "接收到心跳信号", function (err) {
                        db.redisPool.release(redis);
                        if (err) {
                            log("Process will exit: " + err);
                            process.exit(5);
                        }
                    });
                }
            });
        }

        function msgConfirmed() {

            // 收到客户端消息确认信号
            var pendingMsg = pendingMsgs[connId];
            if (typeof pendingMsg != "undefined") {

                var now = new Date();
                clientConns[connId].lastActiveTime = now;

                log("Client " + clientConns[connId].clientAddress +"[" + accountName + "] received message " + pendingMsg.msgId);
                delete pendingMsgs[connId];

                db.redisPool.acquire(function (err, redis) {
                    if (err) {
                        log("Process will exit: " + err);
                        process.exit(6);
                    } else {
                        db.recordMessageReceiptTime(redis, connId, pendingMsg.msgId, now, function (err) {
                            if (err) {
                                db.redisPool.release(redis);
                                log("Process will exit: " + err);
                                process.exit(7);
                            }

                            db.recordLatestActivity(redis, connId, "已确认一条消息", function (err) {
                                if (err) {
                                    db.redisPool.release(redis);
                                    log("Process will exit: " + err);
                                    process.exit(8);
                                }
                                var msgs = queuedMsgs[connId];
                                if (typeof msgs != "undefined") {
                                    var nextMsg = msgs.shift();
                                    if (nextMsg) {
                                        if (msgs.length == 0) {
                                            delete queuedMsgs[connId];
                                        }
                                        sendMessage(connId, nextMsg.msgId, nextMsg.message, nextMsg.msgKey, true);
                                        db.redisPool.release(redis);
                                        return;
                                    }
                                }
                                db.redisPool.release(redis);
                                log("Client " + clientConns[connId].clientAddress +": no more message(s) to send");
                            });
                        });
                    }
                });

                if (pendingMsg.message.callback) {

                    var options = url.parse(pendingMsg.message.callback);
                    options.method = "POST";
                    //
                    log("Client " + clientConns[connId].clientAddress +": callback for message " + pendingMsg.msgId + "...");
                    var req = http.request(options, function (res) {
                        log("Client " + clientConns[connId].clientAddress +": callback STATUS is " + res.statusCode);
                    });
                    req.on("error", function (err) {
                        log("Client " + clientConns[connId].clientAddress +": callback error \"" + err.message + "\"");
                    });
                    req.write(JSON.stringify({success: true}));
                    req.end();
                }
            } else {
                log("Client " + clientConns[connId].clientAddress +": received unexpected message confirmation");
            }
        }

        function forwardMsg(appId, senderId, senderName, receiver, msgText, sendId, handleResult) {

            var now = new Date();
            var message = JSON.parse(msgText);
            message.generate_time = now.Format("yyyyMMddHHmmss");
            message.sender_id = senderId; // 不会发送到客户端
	    message.sender_name = senderName;

            log("Saving message for application " + appId);
            db.redisPool.acquire(function (err, redis) {
                if (err) {
                    handleResult(err);
                } else {
		async.series([
		    function (callback) {
                        db.getUserAvatar(redis, senderId, function (err, avatar) {
                                if (err) return callback(err);
				if (!message.attachments) message.attachments = [];
				message.attachments.push({title:"sender-avatar",type:"image/xxx",filename:"sender-avatar",url:(avatar||USER_AVATAR)});
				callback();
			});
		    },
		    function (callback) {
			db.getAccountIdByName(redis, receiver, function (err, receiverId) {
				if (err) return callback(err);
				if (!receiverId) return callback("Receiver " + receiver + " not exists");
						
				db.saveMessage(redis, appId, message, [senderId,receiverId], function (err, msgId) {
					if (err) return callback(err);
					log("Message id: " + msgId);
					delete message.sender_id;
                                        async.series([
                                                function(callback){
							db.getActiveConnections(redis, appId, receiverId, function (err, connectionInfos) {
								if (err) {
									return callback(err);
								}

								for(var i in connectionInfos) {
									var connectionInfo = connectionInfos[i];
									log("Sending message " + msgId + " to connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "]...");
									publishMessage(connectionInfo, msgId, message, sendId);
								}

								callback();
							});
                                                },
                                                function(callback) {
                                                        db.getActiveConnections(redis, appId, senderId, function (err, connectionInfos) {
                                                                if (err) {
                                                                        return callback(err);
                                                                }

                                                                for(var i in connectionInfos) {
                                                                        var connectionInfo = connectionInfos[i];
                                                                        if (connectionInfo.connId!=connId) {
                                                                        	log("Sending message " + msgId + " to connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "]...");
                                                                        	publishMessage(connectionInfo, msgId, message, sendId);
									}
                                                                }

                                                                callback();
                                                        });
                                                }],
                                                function(err) {
                                                        callback(err);
                                                });

                                                function publishMessage(connectionInfo, msgId, message, sendId) {
                                                        var o = {
                                                                connId: connectionInfo.connId,
                                                                msgId: msgId,
                                                                message: message,
                                                                msgKey: connectionInfo.msgKey,
                                                                sendId: sendId,
                                                                needReceipt: true };
                                                        redis.publish(connectionInfo.channelId, JSON.stringify(o));
                                                }
				});
			});
		    }],
		    function (err) {
			db.redisPool.release(redis);
			handleResult(err);
		    });
                }
            });
        }

        function handleError(e) {

            if (TRACK_SOCKET) log("[SOCKET] client " + clientAddress + ": " + e.toString());
        }

        function handleClose(hadError) {

            if (typeof clientConns[connId] != "undefined") {

                if (TRACK_SOCKET) log("[SOCKET] client " + clientAddress + ": " + (hadError ? "error" : "disconnected"));

                db.redisPool.acquire(function (err, redis) {
                    if (err) {
                        log("Process will exit: " + err);
                        process.exit(9);
                    } else {
                        db.removeLoginInfo(redis, connId, function (err) {
                            db.redisPool.release(redis);
                            if (err) {
                                log("Process will exit: " + err);
                                process.exit(10);
                            }
                            clientConns.splice(clientConns.indexOf(connId), 1);
                        });
                    }
                });
            }
        }

        protocol.handleClientConnection2(socket, appId, accountId, accountName, msgKey,
            keepAlive, msgConfirmed, forwardMsg, handleError, handleClose, log);

        sendOfflineMessages(appId, accountId, config.MAX_OFFLINE_DAYS, connId, msgKey);
    }
});

function sendOfflineMessages(appId, accountId, maxOfflineDays, connId, msgKey) {

    //log("Sending offline messages to account " + accountId + "...");
    db.redisPool.acquire(function (err, redis) {
        if (err) {
            log("Process will exit: " + err);
            process.exit(11);
        } else {
            db.getOfflineMessages(redis, appId, accountId, maxOfflineDays, function (err, msgs) {
                db.redisPool.release(redis);
                if (err) {
                    log("Process will exit: " + err);
                    process.exit(12);
                }
                //log("Total " + msgs.length + " offline message for account " + accountId + " found");
                msgs.forEach(function (msg) {
                    sendMessage(connId, msg.msgId, msg.message, msgKey, msg.needReceipt);
                });
            });
        }
    });
}

function sendMessage(connId, msgId, msg, msgKey, needReceipt) {
    // TODO 增加对延迟发送消息的支持

    log("Sending message " + msgId + " on connection " + connId + "...");

    if (needReceipt) {
        if (typeof pendingMsgs[connId] != "undefined") {
            var msgs = queuedMsgs[connId];
            if (typeof msgs == "undefined") {
                msgs = [];
                queuedMsgs[connId] = msgs;
            }
            msgs.push({msgId: msgId, message: msg, msgKey: msgKey});
            return;
        }
    }

    if (needReceipt) {
        pendingMsgs[connId] = {msgId: msgId, message: msg};
    }
    var secure = (msgKey != null);
    var msgBody = JSON.stringify(msg);
    if (secure) msgBody = crypt.desEncrypt(msgBody, msgKey);
    var msgStr = formatMessage(msgBody, secure, needReceipt);
    //
    var socket = clientConns[connId].socket;
    var clientAddress = socket.remoteAddress + "[" + socket.remotePort + "]";
    if (TRACK_SOCKET) log("[SOCKET] write to client " + clientAddress + ": " + msgStr);
    socket.write(/*protocol.PNTP_FLAG+*/msgStr);
    db.redisPool.acquire(function (err, redis) {
        if (err) {
            log("Process will exit: " + err);
            process.exit(13);
        } else {
            db.recordMessageSentTime(redis, connId, msgId, new Date(), needReceipt, function (err) {
                if (err) {
                    log("Process will exit: " + err);
                    process.exit(14);
                }
                log("Message " + msgId + " on connection " + connId + " sent");
                if (needReceipt) {
                    db.recordLatestActivity(redis, connId, "已发送一条消息，等待确认...", function (err) {
                        if (err) {
                            db.redisPool.release(redis);
                            log("Process will exit: " + err);
                            process.exit(15);
                        }
                        setTimeout(function () {
                            var pendingMsg = pendingMsgs[connId];
                            if (typeof pendingMsg != "undefined") {
                                log("Send message " + msgId + " on connection " + connId + " timeout");
                                if (typeof clientConns[connId] != "undefined") {
                                    db.removeLoginInfo(redis, connId, function (err) {
                                        db.redisPool.release(redis);
                                        if (err) {
                                            log("Process will exit: " + err);
                                            process.exit(16);
                                        }
                                        var socket = clientConns[connId].socket;
                                        var clientAddress = socket.remoteAddress + "[" + socket.remotePort + "]";
                                        clientConns.splice(clientConns.indexOf(connId), 1);
                                        try {
                                            if (TRACK_SOCKET) log("[SOCKET] end client " + clientAddress + ": " + protocol.INACTIVE_TIMEOUT_MSG);
                                            socket.end(/*protocol.PNTP_FLAG+*/protocol.CLOSE_CONN_RES.format(protocol.INACTIVE_TIMEOUT_MSG.length, protocol.INACTIVE_TIMEOUT_MSG));
                                        } catch (err) {
                                            log(err);
                                        }
                                    });
                                }
                            }
                        }, RECEIVE_RECEIPT_TIMEOUT);
                    });
                } else {
                    db.recordLatestActivity(redis, connId, "已发送一条不需要确认的消息", function (err) {
                        db.redisPool.release(redis);
                        if (err) {
                            log("Process will exit: " + err);
                            process.exit(17);
                        }
                    });
                }
            });
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

    function ensureActive() {

        var now = new Date();

        var inactiveConnIds = [];
        for (var connId in clientConns) {

            var lastActiveTime = clientConns[connId].lastActiveTime;
            var diff = (now.getTime() - lastActiveTime.getTime()); //ms
            if (diff > MAX_INACTIVE_TIME * 2) {
                inactiveConnIds.push(connId);
            }
        }

        db.redisPool.acquire(function (err, redis) {
            if (err) {
                log("Process will exit: " + err);
                process.exit(18);
            }
            async.forEachSeries(inactiveConnIds, function (connId, callback) {
                var clientAddress = clientConns[connId].clientAddress;
                log("Client " + clientAddress + "[" + clientConns[connId].accountName + "] inactive timeout");
                db.removeLoginInfo(redis, connId, function (err) {
                    if (err) return callback(err);
                    var socket = clientConns[connId].socket;
                    clientConns.splice(clientConns.indexOf(connId), 1);
                    try {
                        if (TRACK_SOCKET) log("[SOCKET] end client " + clientAddress + ": " + protocol.INACTIVE_TIMEOUT_MSG);
                        socket.end(/*protocol.PNTP_FLAG+*/protocol.CLOSE_CONN_RES.format(protocol.INACTIVE_TIMEOUT_MSG.length, protocol.INACTIVE_TIMEOUT_MSG));
                    } catch (err) {
                        log(err);
                    }
                    callback();
                });
            }, function (err) {
                db.redisPool.release(redis);
                if (err) {
                    log("Process will exit: " + err);
                    process.exit(19);
                }
            });
        });
    }

    setInterval(ensureActive, MAX_INACTIVE_TIME / 2);
});
