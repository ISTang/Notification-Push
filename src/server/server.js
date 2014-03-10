// 引入依赖包
var net = require("net");
var child_process = require('child_process');
var fs = require('fs');
//
var config = require(__dirname + '/config');
var utils = require(__dirname + '/utils');
var db = require(__dirname + '/db');
//
const LOGIN_PATH = __dirname + '/login';
const HTTPD_PATH = __dirname + '/httpd';

// 系统参数
const SERVER_PORT = config.SERVER_PORT;
const LOGIN_NUMBER = config.LOGIN_NUMBER;
const GRACE_EXIT_TIME = config.GRACE_EXIT_TIME;
const AUTO_START_HTTPD = config.AUTO_START_HTTPD;

Date.prototype.Format = utils.DateFormat;
String.prototype.trim = utils.StringTrim;
String.prototype.format = utils.StringFormat;

var logger = config.log4js.getLogger('server');
logger.setLevel(config.LOG_LEVEL);

function main(fn) {
    fn();
}

var loginProcessPool = [];
function startLoginPool(handle) {

    for (var i = 0; i < LOGIN_NUMBER; i++) {

        var loginProcess = fork(i);

        loginProcessPool.push(loginProcess);
    }

    function onError(err) {

        // 无法向登录进程发送消息
        var loginIndex = this.loginIndex;
        logger.error("#"+loginIndex+" login process: "+err.toString());
        this.kill();

        var loginProcess = fork(loginIndex);
        loginProcessPool[loginIndex] = loginProcess;
        logger.warn("#" + loginIndex + " login process restarted(onError)");
   }

    function onExit(code, signal) {

        // 登录进程被终止
        var loginIndex = this.loginIndex;
        logger.warn("#" + loginIndex + " login process: terminated(" + code + ")" + (signal ? " due to receipt of signal " + signal : ""));

        var loginProcess = fork(loginIndex);
        loginProcessPool[loginIndex] = loginProcess;
        logger.warn("#" + loginIndex + " login process restarted(onExit)");
    }

    function fork(loginIndex) {
        logger.warn("Forking #" + loginIndex + " login process...");
        var loginProcess = child_process.fork(LOGIN_PATH);
        logger.warn("#" + loginIndex + " login process forked.");
        loginProcess.loginIndex = loginIndex;

        loginProcess.on("error", onError);
        loginProcess.on("exit", onExit);

        loginProcess.send({"server": true, loginIndex: loginIndex}, handle);

        return loginProcess;
    }
}

var httpdProcess;
function startHttpServer() {

    fork();

    function onError(err) {

        // 无法向HTTP进程发送消息
        logger.error(err.toString());
        this.kill();
        fork();
        logger.info("httpd process restarted(onError)");
    }

    function onExit(code, signal) {

        // HTTP进程被终止
        logger.warn("HTTP child process terminated(code=" + code + ")" + (signal ? " due to receipt of signal " + signal : ""));
        fork();
        logger.info("httpd process restarted(onExit)");
    }

    function fork() {
        httpdProcess = child_process.fork(HTTPD_PATH);

        httpdProcess.on("error", onError);
        httpdProcess.on("exit", onExit);
    }
}

var exitTimer = null;
function aboutExit() {

    if (exitTimer) return;

    loginProcessPool.forEach(function (c) {

        c.kill();
    })
    exitTimer = setTimeout(function () {

        logger.info("LISTEN server exit...");

        process.exit(0);

    }, GRACE_EXIT_TIME);
}

function startListenServer() {

    var tcpServer = net.createServer();
    tcpServer.listen(SERVER_PORT, function () {

        logger.info('SERVER process is running at port ' + SERVER_PORT + '...');

        startLoginPool(tcpServer._handle);
        tcpServer.close();
    });
}

void main(function () {
    process.on('SIGINT', aboutExit);
    process.on('SIGTERM', aboutExit);

    process.setMaxListeners(0);

    db.redisPool.acquire(function (err, redis) {
        if (err) {
            logger.fatal(err);
            process.exit(1);
        } else {
            logger.info('Cleaning data...');
            db.cleanData(redis, function (err) {
                db.redisPool.release(redis);
                if (err) {
                    logger.fatal(err);
                    process.exit(2);
                } else {
                    startListenServer();
                    if (AUTO_START_HTTPD) startHttpServer();
                }
            });
        }
    });
});

