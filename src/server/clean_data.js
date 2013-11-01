// 引入依赖包
var fs = require('fs');
var async = require('async');
var redis = require('redis');
//
var utils = require(__dirname + '/utils');
var log4js = require('log4js');
log4js.configure('log4js_clean_data.json', {});
var logger = log4js.getLogger('normal');
logger.setLevel('INFO');

// 系统参数
const REDIS_HOST = 'localhost';
const REDIS_PORT = 7379;
//
const GRACE_EXIT_TIME = 1500;

Date.prototype.Format = utils.DateFormat;
String.prototype.trim = utils.StringTrim;
String.prototype.format = utils.StringFormat;

/*
 * Print log
 */
function log(msg) {
    logger.info(msg);
}

function cleanData(client, keysPattern, callback) {
    log("Finding keys \"" + keysPattern + "\"...");
    client.keys(keysPattern, function (err, keys) {
        if (err) return callback(err);
        log("Total " + keys.length + " key(s) found.");
        async.forEach(keys, function (key, callback) {
            log("Deleting key " + key + "...");
            client.del(key);
            callback();
        }, function (err) {
            callback(err);
        });
    });
}

function main(fn) {
    fn();
}

var exitTimer = null;
function aboutExit() {

    if (exitTimer) return;

    exitTimer = setTimeout(function () {

        log("Exitting...");

        process.exit(0);

    }, GRACE_EXIT_TIME);
}

void main(function () {
    process.on('SIGINT', aboutExit);
    process.on('SIGTERM', aboutExit);

    if (process.argv.length < 3) {
        log("Usage: clean_data <keysPattern> [redis_port]");
        process.exit(0);
    }

    log("Connecting...");
    var client = redis.createClient((process.argv.length > 3 ? parseInt(process.argv[3]) : REDIS_PORT), REDIS_HOST);
    client.on("error", function (err) {
        log(err);
    });
    client.on("connect", function () {
        log('Cleanning data...');
        cleanData(this, process.argv[2], function (err) {
            if (err) log(err);
            else log('Done.');
            client.end();
            process.exit(0);
        });
    });
});
