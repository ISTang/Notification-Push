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
const LOG_ENABLED = config.LOG_ENABLED;
const AUTO_START_HTTPD = config.AUTO_START_HTTPD;

var logStream = fs.createWriteStream(__dirname+"/logs/server.log", {"flags":"a"});

Date.prototype.Format = utils.DateFormat;
String.prototype.trim = utils.StringTrim;
String.prototype.format = utils.StringFormat;

/*
 * Print log
 */
function log(msg) {
    var now = new Date();
    var strDatetime = now.Format("yyyy-MM-dd HH:mm:ss");
    var buffer = "[" + strDatetime + "] " + msg + "[server]";
    if (logStream!=null) logStream.write(buffer + "\r\n");
    if ( LOG_ENABLED) console.log(buffer);
}

function main(fn) {
    fn();
}

var loginProcessPool = [];
function startLoginPool(handle) {

    for (var i = 0; i < LOGIN_NUMBER; i++) {

        var loginProcess = child_process.fork(LOGIN_PATH);
		
        loginProcess.on("error", onError);
        loginProcess.on("exit", onExit);

        loginProcess.send({"server":true, loginIndex:i}, handle);

        loginProcessPool.push(loginProcess);
    }

	function onError(error) {

		log(error.toString());
		this.kill();
	}

	function onExit() {

		for (var index in loginProcessPool) {
		
			if (loginProcessPool[index]==this) {

				loginProcess = child_process.fork(LOGIN_PATH);
				
				loginProcess.on("error", onError);
				loginProcess.on("exit", onExit);

				loginProcess.send({"server":true, loginIndex:index}, handle);

				loginProcessPool[index] = loginProcess;

				log("#"+index+" login process restarted");
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

		log(error.toString());
		this.kill();
    }

	function onExit() {

		httpdProcess = child_process.fork(HTTPD_PATH);
		
		httpdProcess.on("error", onError);
		httpdProcess.on("exit", onExit);

		log("httpd process restarted");
	}
}

var exitTimer = null;
function aboutExit() {

    if (exitTimer) return;

    loginProcessPool.forEach(function (c) {

        c.kill();
    })
    exitTimer = setTimeout(function () {

        log("LISTEN server exit...");

        process.exit(0);

    }, GRACE_EXIT_TIME);
}

function startListenServer() {

    var tcpServer = net.createServer();
    tcpServer.listen(SERVER_PORT, function () {

        log('SERVER process is running at port ' + SERVER_PORT + '...');

        startLoginPool(tcpServer._handle);
        tcpServer.close();
    });
}

void main(function () {
    process.on('SIGINT', aboutExit);
    process.on('SIGTERM', aboutExit);

    process.setMaxListeners(0);

	db.redisPool.acquire(function(err, redis) {
		if (err) {
			log(err);
			process.exit(1);
		} else {
			log('Cleaning data...');
			db.cleanData(redis, function(err) {
				db.redisPool.release(redis);
				if (err) {
					log(err);
					process.exit(2);
				} else {
					startListenServer();
					if (AUTO_START_HTTPD) startHttpServer();
				}
			});
		}
	});
});
