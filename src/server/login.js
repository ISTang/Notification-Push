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
const LOG_ENABLED = config.LOG_ENABLED;

var myIndex;

var loginingSockets = []; // clientAddress->(socket, connectTime, accountName)

var logStream = LOG_ENABLED ? fs.createWriteStream("logs/login.log", {"flags": "a"}) : null;

Date.prototype.Format = utils.DateFormat;
String.prototype.trim = utils.StringTrim;
String.prototype.format = utils.StringFormat;

/*
 * Print log
 */
function log(msg) {

    var now = new Date();
    var strDatetime = now.Format("yyyy-MM-dd HH:mm:ss");
    var buffer = "[" + strDatetime + "] " + msg + "[login]";
    if (logStream != null) logStream.write(buffer + "\r\n");
    console.log(buffer);
}

/**
 * 检查应用ID及密码
 *
 * @param appId应用ID
 * @param password 应用密码
 * @param handleResult 处理函数
 */
function checkAppId(appId, password, handleResult) {

    db.checkAppId(appId, password, function (checkResult) {
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

/**
 * 检查用户身份
 *
 * @param username 用户名
 * @param password 用户密码(null表示不需要检查密码)
 * @param autoCreateAccount 是否自动创建账号
 * @param handleResult 处理函数
 */
function checkUsername(username, password, autoCreateAccount, handleResult) {

    db.checkUsername(username, password, autoCreateAccount, function (checkResult) {

        if (!checkResult.passed) {

            handleResult({passed: false, reason: checkResult.reason});
        } else {

            handleResult({passed: true, accountId: checkResult.accountId, accountName: checkResult.accountName});
        }
    });
}

var notifyProcessPool = [];
var nextNotifyProcessIndex = 0;
function startNotifyPool() {

    for (var i = 0; i < NOTIFY_NUMBER; i++) {

        var notifyProcess = child_process.fork(NOTIFY_PATH);

        notifyProcess.on("error", onError);
        notifyProcess.on("exit", onExit);

        notifyProcess.send({type:"index",loginIndex:myIndex,notifyIndex:i});

        notifyProcessPool.push(notifyProcess);
    }

    function onError(error) {

        log(error.toString());
        this.kill();
    }

    function onExit() {

        for (var index in notifyProcessPool) {

            if (notifyProcessPool[index] == this) {

                notifyProcess = child_process.fork(NOTIFY_PATH);

                notifyProcess.on("error", onError);
                notifyProcess.on("exit", onExit);

                notifyProcess.send({type:"index",loginIndex:myIndex,notifyIndex:index});

                notifyProcessPool[index] = notifyProcess;

                log("#" + index + " notify process restarted");
            }
        }
    }
}

var server;
process.on('message', function (m, handle) {

    if (m.server) {

        myIndex = m.loginIndex;

        server.listen(handle, function (err) {

            if (err) {

                log('Login process listen error');
            } else {

                log('Login process '+myIndex+' listen ok');

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

        log('Login process will exit...');

        process.exit(0);

    }, GRACE_EXIT_TIME);
}

void main(function () {
    process.on('SIGINT', aboutExit)
    process.on('SIGTERM', aboutExit)

    server = net.createServer(function (socket) {

        //noinspection JSUnresolvedVariable
        var clientAddress = socket.remoteAddress + "[" + socket.remotePort + "]";
        //log("One new login started from " + clientAddress);

        loginingSockets[clientAddress] = {"socket": socket, "connectTime": new Date(), "accountName": null};

        function clientLogon(socket, accountId, accountName, appId, msgKey, handleResult) {
            log("[" + accountName + "] logon");

            var connId = uuid.v4().toUpperCase();
            var channelId = "notify-"+myIndex+"-"+nextNotifyProcessIndex;
            db.saveLoginInfo(connId, accountId, appId, msgKey, channelId,clientAddress, handleResult);

            var notifyProcess = notifyProcessPool[nextNotifyProcessIndex++];
            nextNotifyProcessIndex = nextNotifyProcessIndex % NOTIFY_NUMBER;

            process.nextTick(function () {

                try {
                    notifyProcess.send({"type": "client", "appId": appId, "accountId": accountId,
                        "accountName": accountName, connId: connId, msgKey: msgKey}, socket);
                } catch (e) {
                    log("Failed to send socket of client " + accountName + "(" + clientAddress + ")");
                    try {
                        socket.end(protocol.CLOSE_CONN_RES.format(protocol.SERVER_ERROR_MSG.length, protocol.SERVER_ERROR_MSG));
                    } catch (ee) {
                        log(ee);
                    }
                }
            });
            delete loginingSockets[clientAddress];

            handleResult(null);
        }

        function handleError(e) {

            log(">>>[" + (this.accountName != null ? this.accountName : clientAddress) + "]" + e.toString() + "<<<");
        }

        function handleClose(hadError) {

            log("Client " + (this.accountName != null ? this.accountName : clientAddress) + " " + (hadError ? "network error" : "disconnected"));
            delete loginingSockets[clientAddress];
        }

        // 发送应用认证请求
        socket.write(/*protocol.PNTP_FLAG+*/protocol.GET_APPID_REQ);

        // 处理连接
        protocol.handleClientConnection(socket, checkAppId, checkUsername, clientLogon,
            handleError, handleClose, log);
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

                //log("Client " + (accountName != null ? accountName : clientAddress) + " login timeout");
                socket.end(protocol.CLOSE_CONN_RES.format(protocol.LOGIN_TIMEOUT_MSG.length, protocol.LOGIN_TIMEOUT_MSG));
            }
        }
    }, LOGIN_TIMEOUT / 2);
});
