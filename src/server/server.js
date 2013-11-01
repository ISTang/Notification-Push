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

        var loginProcess = child_process.fork(LOGIN_PATH);

        loginProcess.on("error", onError);
        loginProcess.on("exit", onExit);

        loginProcess.send({"server": true, loginIndex: i}, handle);

        loginProcessPool.push(loginProcess);
    }

    function onError(error) {

        logger.error(error.toString());
        this.kill();
    }

    function onExit() {

        for (var index in loginProcessPool) {

            if (loginProcessPool[index] == this) {

                loginProcess = child_process.fork(LOGIN_PATH);

                loginProcess.on("error", onError);
                loginProcess.on("exit", onExit);

                loginProcess.send({"server": true, loginIndex: index}, handle);

                loginProcessPool[index] = loginProcess;

                logger.warn("#" + index + " login process restarted");
            }
        }
    }
}

var httpdProcess;
function startHttpServer() {

    httpdProcess = child_process.fork(HTTPD_PATH);

    httpdProcess.on("error", onError);
    httpdProcess.on("exit", onExit);

    function onError(error) {

        logger.error(error.toString());
        this.kill();
    }

    function onExit() {

        httpdProcess = child_process.fork(HTTPD_PATH);

        httpdProcess.on("error", onError);
        httpdProcess.on("exit", onExit);

        logger.info("httpd process restarted");
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
