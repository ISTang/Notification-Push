// 引入依赖包
var fs = require('fs');
var async = require('async');
var redis = require('redis');
//
var utils = require(__dirname + '/utils');
var log4js = require('log4js');
//log4js.configure('log4js_clean_data.json', {});
var logger = log4js.getLogger('normal');
logger.setLevel('INFO');

// 系统参数
const REDIS_HOST = 'localhost';
const REDIS_PORT = 6379;
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

function cleanAccounts(client, days, callback) {
var checkPoint = (utils.addDays(new Date(),-days)).Format("yyyyMMddHHmmss");
log("Check point: "+checkPoint);
var appIds;
async.series([
  function(callback) {
    client.smembers("application:set", function (err, app_ids) {
        if (err) return callback(err);
        log("Total " + app_ids.length + " app(s) found.");
        appIds = app_ids;
        callback();
    });
  },
  function(callback) {
    client.smembers("account:set", function (err, accountIds) {
        if (err) return callback(err);
        log("Total " + accountIds.length + " account(s) found.");
        async.forEachSeries(accountIds, function (accountId, callback) {
            var lastLogon;
            log("Checking account with id "+accountId+"...");
            async.forEachSeries(appIds, function(appId, callback) {
                client.get("account:"+accountId+":application:"+appId+":last_logon", function(err, value) {
                    if (err) return callback(err);
                    if (!value) return callback();
                    if (!lastLogon) lastLogon = value;
                    if (value>lastLogon) lastLogon = value;
                    callback();
                });
            }, function(err) {
                if (err) return callback(err);
                if (!lastLogon || lastLogon>=checkPoint) return callback();
                log("Deleting user info for account with id "+accountId+"...");
                async.series([
                    function(callback) {
                        client.get("account:"+accountId+":name", function(err, accountName) {
                          if (err) return callback(err);
                          client.del("user:"+accountName);
                          callback();
                        });
                    },
                    function(callback) {
                        client.get("account:"+accountId+":phone", function(err, accountPhone) {
                          if (err) return callback(err);
                          client.del("user:"+accountPhone);
                          callback();
                        });
                    },
                    function(callback) {
                        client.get("account:"+accountId+":email", function(err, accountEmail) {
                          if (err) return callback(err);
                          client.del("user:"+accountEmail);
                          callback();
                        });
                    }
                ], function(err) {
                    if (err) return callback(err);
                    log("Deleting account info for id "+accountId+"...");
                    client.del("account:"+accountId+":name");
                    client.del("account:"+accountId+":phone");
                    client.del("account:"+accountId+":eamil");
                    client.del("account:"+accountId+":password");
                    client.del("account:"+accountId+":disabled");
                    client.del("account:"+accountId+":avatar");
                    client.del("account:"+accountId+":type");
                    client.del("account:"+accountId+":desc");
                    client.del("account:"+accountId+":ceate_time");
                    client.del("account:"+accountId+":update_time");
                    client.srem("account:set", accountId);
                    async.forEachSeries(appIds, function(appId, callback) {
                        client.del("account:"+accountId+":application:"+appId+":last_logon");
                        callback();
                    }, function(err){
                        callback(err);
                    });
                });
            });
        }, function (err) {
            callback(err);
        });
    });
  }],
  function(err) {
      callback(err);
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
        log("Usage: clean_accounts <days> [redis_port]");
        process.exit(0);
    }

    log("Connecting...");
    var client = redis.createClient((process.argv.length > 3 ? parseInt(process.argv[3]) : REDIS_PORT), REDIS_HOST);
    client.on("error", function (err) {
        log(err);
    });
    client.on("connect", function () {
        log('Cleanning accounts...');
        cleanAccounts(this, parseInt(process.argv[2]), function (err) {
            if (err) log(err);
            else log('Done.');
            client.end();
            process.exit(0);
        });
    });
});
