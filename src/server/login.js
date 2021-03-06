// 引入依赖包
var net = require("net");
var child_process = require('child_process');
var fs = require('fs');
var uuid = require('node-uuid');
//
var config = require(__dirname + '/config');
var utils = require(__dirname + '/utils');
var protocol = require(__dirname + '/client/protocol');
var db = require(__dirname + '/db');

const NOTIFY_PATH = __dirname + '/notify.js';

// 系统参数
const NOTIFY_NUMBER = config.NOTIFY_NUMBER;
const LOGIN_TIMEOUT = config.LOGIN_TIMEOUT;
const GRACE_EXIT_TIME = config.GRACE_EXIT_TIME;
//
const TRACK_SOCKET = config.TRACK_SOCKET;

var myIndex;

var loginingSockets = []; // clientAddress->(socket, connectTime, accountName)

Date.prototype.Format = utils.DateFormat;
String.prototype.trim = utils.StringTrim;
String.prototype.format = utils.StringFormat;

var logger = config.log4js.getLogger('login');
logger.setLevel(config.LOG_LEVEL);

/**
 * 检查应用ID及密码
 *
 * @param appId应用ID
 * @param password 应用密码
 * @param handleResult 处理函数
 */
function checkAppId(appId, password, handleResult) {

    db.redisPool.acquire(function (err, redis) {
        if (err) {
            handleResult({passed: false, reason: err});
        } else {
            db.checkAppId(redis, appId, password, function (checkResult) {
                db.redisPool.release(redis);
                if (checkResult.passed) {
                    if (checkResult.needProtect) {
                        handleResult({ passed: true,
                            secureLogin: true,
                            protectKey: checkResult.protectKey,
                            needLogin: checkResult.needLogin,
                            needPassword: checkResult.needPassword,
                            autoCreateAccount: checkResult.autoCreateAccount,
                            secureMessage: checkResult.secureMessage
                        });
                    } else {
                        handleResult({ passed: true,
                            secureLogin: false,
                            needLogin: checkResult.needLogin,
                            needPassword: checkResult.needPassword,
                            autoCreateAccount: checkResult.autoCreateAccount,
                            secureMessage: checkResult.secureMessage
                        });
                    }
                } else {
                    handleResult({passed: false, reason: checkResult.reason});
                }
            });
        }
    });
}

/**
 * 检查用户身份
 *
 * @param username 用户名
 * @param password 用户密码(null表示不需要检查密码)
 * @param autoCreateAccount 是否自动创建账号
 * @param handleResult 处理函数
 */
function checkUsername(username, password, autoCreateAccount, handleResult) {

    if (!username || username=="") return handleResult({passed: false, reason: "No username provided!"});

    db.redisPool.acquire(function (err, redis) {
        if (err) {
            handleResult({passed: false, reason: err});
        } else {
            db.checkUsername(redis, username, password, autoCreateAccount, function (checkResult) {
                db.redisPool.release(redis);
                if (!checkResult.passed) {
                    handleResult({passed: false, reason: checkResult.reason});
                } else {
                    handleResult({passed: true, accountId: checkResult.accountId, accountName: checkResult.accountName});
                }
            });
        }
    });
}

var notifyProcessPool = [];
var nextNotifyProcessIndex = 0;
function startNotifyPool() {

    for (var i = 0; i < NOTIFY_NUMBER; i++) {

        var notifyProcess = fork(i);

        notifyProcessPool.push(notifyProcess);
    }

    function onError(err) {

        // 无法向通知进程发送消息
        var notifyIndex = this.notifyIndex;
        logger.error("#"+notifyIndex+" notify process: "+err.toString());
        this.kill();

        var notifyProcess = fork(notifyIndex);
        notifyProcessPool[notifyIndex] = notifyProcess;
        logger.warn("#" + notifyIndex + " notify process restarted(onError)");
    }

    function onExit(code, signal) {

        // 通知进程被终止
        var notifyIndex = this.notifyIndex;
        logger.warn("#"+notifyIndex+" notify process: terminated("+code+")"+(signal?" due to receipt of signal "+signal:""));

        var notifyProcess = fork(notifyIndex);
        notifyProcessPool[notifyIndex] = notifyProcess;
        logger.warn("#" + notifyIndex + " notify process restarted(onExit)");
    }

    function fork(index) {
        var notifyProcess = child_process.fork(NOTIFY_PATH);
        notifyProcess.notifyIndex = index;

        notifyProcess.on("error", onError);
        notifyProcess.on("exit", onExit);

        notifyProcess.send({type: "index", loginIndex: myIndex, notifyIndex: index});

        return notifyProcess;
    }
}

var server;
process.on('message', function (m, handle) {

    if (m.server) {

        myIndex = m.loginIndex;

        server.listen(handle, function (err) {

            if (err) {

                logger.fatal('"Process will exit: login process listen error');
                process.exit(1);
            } else {

                logger.info('Login process ' + myIndex + ' listen ok');

                startNotifyPool();
            }
        });
    }
});

function main(fn) {
    fn();
}

var server = null;
var exitTimer = null;
function aboutExit() {

    if (exitTimer) return;

    if (server != null) server.close();
    exitTimer = setTimeout(function () {


        logger.info('Login process will exit...');

        process.exit(0);

    }, GRACE_EXIT_TIME);
}

void main(function () {
    process.on('SIGINT', aboutExit)
    process.on('SIGTERM', aboutExit)

    server = net.createServer(function (socket) {

        //noinspection JSUnresolvedVariable
        var clientAddress = socket.remoteAddress + "[" + socket.remotePort + "]";
        if (TRACK_SOCKET) logger.trace("[SOCKET] client " + clientAddress + " connected");

        loginingSockets[clientAddress] = {"socket": socket, "connectTime": new Date(), "accountName": null};

        function clientLogon(socket, accountId, accountName, appId, msgKey, callback) {
            logger.trace("[" + accountName + "] logon");
            var connId = uuid.v4().toUpperCase();
            var channelId = "notify-" + myIndex + "-" + nextNotifyProcessIndex;
            db.redisPool.acquire(function (err, redis) {
                if (err) return callback(err);
                db.saveLoginInfo(redis, connId, accountId, appId, msgKey, channelId, clientAddress, function (err) {
                    db.redisPool.release(redis);
                    if (err) return callback(err);

                    var notifyProcess = notifyProcessPool[nextNotifyProcessIndex++];
                    nextNotifyProcessIndex = nextNotifyProcessIndex % NOTIFY_NUMBER;

                    //process.nextTick(function () {

                    try {
                        notifyProcess.send({"type": "client", "appId": appId, "accountId": accountId,
                            "accountName": accountName, connId: connId, msgKey: msgKey}, socket);
                        callback();
                    } catch (e) {
                        callback("Failed to send socket of client " + accountName + "(" + clientAddress + ")");
                    } finally {
                        delete loginingSockets[clientAddress];
                    }
                    //});
                });
            });
        }

        function handleError(e) {

            if (TRACK_SOCKET) logger.trace("[SOCKET] client " + clientAddress + ": " + e.toString());
        }

        function handleClose(hadError) {

            if (TRACK_SOCKET) logger.trace("[SOCKET] client " + clientAddress + ": " + (hadError ? "network error" : "disconnected"));
            delete loginingSockets[clientAddress];
        }

        // 发送应用认证请求
        if (TRACK_SOCKET) logger.trace("[SOCKET] write to client " + clientAddress + ": " + protocol.GET_APPID_REQ);
        socket.write(/*protocol.PNTP_FLAG+*/protocol.GET_APPID_REQ);

        // 处理连接
        protocol.handleClientConnection(socket, checkAppId, checkUsername, clientLogon,
            handleError, handleClose, logger);
    });

    setInterval(function () {

        var now = new Date();
        for (var clientAddress in loginingSockets) {

            if (typeof clientAddress == "undefined") continue;

            var socketInfo = loginingSockets[clientAddress];
            var connectTime = socketInfo.connectTime;
            var howLong = now.getTime() - connectTime.getTime();
            if (howLong > LOGIN_TIMEOUT) {

                var socket = socketInfo.socket;
                //var accountName = socketInfo.accountName;

                if (TRACK_SOCKET) logger.trace("[SOCKET] end client " + clientAddress + ": " + protocol.LOGIN_TIMEOUT_MSG);
                socket.end(protocol.CLOSE_CONN_RES.format(protocol.LOGIN_TIMEOUT_MSG.length, protocol.LOGIN_TIMEOUT_MSG));

                delete loginingSockets[clientAddress];
            }
        }
    }, LOGIN_TIMEOUT / 2);
});

