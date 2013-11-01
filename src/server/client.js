var net = require("net");
var config = require(__dirname + '/config');
var utils = require(__dirname + '/utils');
var protocol = require(__dirname + '/client/protocol');
var crypt = require(__dirname + '/client/crypt');
var async = require('async');

const SERVER_HOST = config.SERVER_HOST;
const SERVER_PORT = config.SERVER_PORT;
const RETRY_INTERVAL = 1000; // ms

Date.prototype.Format = utils.DateFormat;

/*
 * Print log
 */
function log(msg) {

    var now = new Date();
    var strDatetime = now.Format("yyyy-MM-dd HH:mm:ss");
    var buffer = "[" + strDatetime + "] " + msg;
    console.log(buffer);
}

const appId = "4083AD3D-0F41-B78E-4F5D-F41A515F2667";
const appPassword = "@0Vd*4Ak";
const protectKey = {public: "n9SfmcRs", private: "n9SfmcRs"};

function getAppInfo() {
    return {id: appId, password: appPassword, protectKey: protectKey};
}

function startWorker(clientId, clientPassword, onConnected) {

    var socket;
    var clientLogon = false; // 是否登录成功

    var clientLogging = false;

    var msgKey;
    var maxInactiveTime;

    var lastActiveTime;

    var keepAliveId = null;
    var ensureAliveId = null;

    function getUserInfo() {

        return {name: clientId, password: clientPassword};
    }

    function setMsgKey(key) {

        msgKey = key;
    }

    function setLogon() {
        onConnected();

        clientLogon = true;
        clientLogging = false;
        lastActiveTime = new Date();

        ensureAliveId = setTimeout(ensureActive, maxInactiveTime);
    }

    function setKeepAliveInterval(n) {

        if (keepAliveId != null) {

            clearInterval(keepAliveId);
        }

        maxInactiveTime = n * 3;

        keepAliveId = setInterval(function () {

            if (!clientLogon) return;

            var diff = new Date().getTime() - lastActiveTime.getTime();
            if (diff >= n) {

                log("[" + clientId + "] Keeping alive...");
                socket.write(protocol.SET_ALIVE_REQ);
            }
        }, n);
    }

    function keepAlive() {

        log("[" + clientId + "] Server still alive");
        lastActiveTime = new Date();
    }

    function msgReceived(msg, secure) {

        lastActiveTime = new Date();

        if (secure) {
            crypt.desDecrypt(msg, msgKey, function (err, data) {
                if (err) return log("[" + clientId + "] " + err);
                var msgObj = JSON.parse(data);
                log("[" + clientId + "] " + msgObj.generate_time + " " + (msgObj.title != null ? msgObj.title : "---") + ": " + msgObj.body);
            });
        } else {
            var msgObj = JSON.parse(msg);
            log("[" + clientId + "] " + msgObj.generate_time + " " + (msgObj.title != null ? msgObj.title : "---") + ": " + msgObj.body);
        }
    }

    function connectToServer() {

        if (clientLogon || clientLogging) {

            return;
        }

        clientLogging = true;

        log("[" + clientId + "] Connecting..." + SERVER_HOST + "[" + SERVER_PORT + "]");
        socket = net.createConnection(SERVER_PORT, SERVER_HOST);

        protocol.handleServerConnection(socket, clientId, getAppInfo, getUserInfo, setMsgKey, setLogon, setKeepAliveInterval, keepAlive, msgReceived,
            handleError, handleClose, log);

        function handleClose() {

            if (keepAliveId != null) {

                clearInterval(keepAliveId);
                keepAliveId = null;
            }

            if (ensureAliveId != null) {

                clearInterval(ensureAliveId);
                ensureAliveId = null;
            }

            if (clientLogon) {

                log("[" + clientId + "] Disconnected");
                clientLogon = false;
            }

            if (clientLogging) {

                clientLogging = false;
            }
        }

        function handleError() {

            log(">>>[" + clientId + "] Network error<<<");
        }
    }

    function ensureActive() {

        if (!clientLogon) return;

        var diff = (new Date().getTime() - lastActiveTime.getTime()); //ms
        if (diff >= maxInactiveTime) {

            log("[" + clientId + "] Server inactive timeout");

            socket.end(protocol.CLOSE_CONN_RES.format(protocol.INACTIVE_TIMEOUT_MSG.length, protocol.INACTIVE_TIMEOUT_MSG));
            return;
        }
        ensureAliveId = setTimeout(ensureActive, maxInactiveTime);
    }

    connectToServer();
    setInterval(connectToServer, RETRY_INTERVAL);
}

function main(fn) {
    fn();
}

// linux/mac: ulimit -n MAX_FILES
// 设置最大允许打开的文件数
void main(function () {

    var clientId = (process.argv.length >= 4 ? process.argv[2] : "testacc"); // 客户ID
    var clientPassword = (process.argv.length >= 4 ? process.argv[3] : "testpass"); // 客户密码

    var workerCount = (process.argv.length >= 5 ? parseInt(process.argv[4]) : 1);
    log("Total " + workerCount + " worker to start...");

    var clientIds = [];
    for (var i = 0; i < workerCount; i++) {
        clientIds.push(clientId + (i + 1));
    }

    async.forEach(clientIds, function (clientId, callback) {
        startWorker(clientId, clientPassword, callback);
    }, function (err) {
        if (err) return log(err);
        log("All clientts logon");
    });
});
