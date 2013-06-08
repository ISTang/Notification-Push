var config = require(__dirname+'/config');
var utils = require(__dirname+'/utils');
var db = require(__dirname+'/db');

var fs = require('fs');
var url = require("url");
var http = require("http");
var request = require('request');
var feedparser = require('feedparser');
var async = require('async');

const PLATFORM_SERVER = config.PLATFORM_SERVER;
const PLATFORM_PORT = config.PLATFORM_PORT;
const APP_ID = "4083AD3D-0F41-B78E-4F5D-F41A515F2667";

const AUTO_RSS_INTERVAL = config.AUTO_RSS_INTERVAL;

const GRACE_EXIT_TIME = config.GRACE_EXIT_TIME;

const LOG_ENABLED = config.LOG_ENABLED;


var logStream = LOG_ENABLED ? fs.createWriteStream("logs/app.log", {"flags": "a"}) : null;

var redis;

Date.prototype.Format = utils.DateFormat;
String.prototype.trim = utils.StringTrim;
String.prototype.format = utils.StringFormat;

function log(msg) {

    var now = new Date();
    var strDatetime = now.Format("yyyy-MM-dd HH:mm:ss");
    var buffer = "[" + strDatetime + "] " + msg + "[app]";
    if (logStream != null) logStream.write(buffer + "\r\n");
    console.log(buffer);
}

function pushMessage(msgTitle, msgBody, msgUrl, logoUrl, callback) {
    var bodyText = JSON.stringify({title: msgTitle, body: msgBody,
        attachments: [{title:"logo",type:'image/xxx',filename:'logo',url:logoUrl}], url: msgUrl});

    var options = url.parse("http://"+PLATFORM_SERVER+":"+PLATFORM_PORT+"/application/"+APP_ID+"/message");
    options.method = "POST";
    options.headers = {
        'Content-Type': 'application/json;charset=utf-8',
        'Content-Length': Buffer.byteLength(bodyText)
    };
    var req = http.request(options, function (res) {
        callback();
    });
    req.on("error", function (err) {
        callback(err.message);
    });
    req.write(bodyText);
    req.end();
}

function getArticles(url, since, handleResult) {
    if (!since) {
        since = utils.DateAdd("d", -7, new Date());
    }
    var reqObj = {'uri': url,
        'headers': {'If-Modified-Since' : since.toString()/*,
         'If-None-Match' : <your cached 'etag' value>*/}};
    feedparser.parseStream(request(reqObj), function (err, meta, articles) {
        if (err) return handleResult(err);
        var logo = meta.image.url;
        var result = [];
        async.forEachSeries(articles, function (article, callback) {
            article.logo = logo;
            result.push(article);
            callback();
        }, function (err) {
            handleResult (err, result);
        });
    });
}

function getAllArticles(handleResult) {
    var now = new Date();
    db.getAllSources(function (err, sources) {
        if (err) return handleResult(err);
        var result = [];
        async.forEachSeries(sources, function (source, callback) {
            log("获取源 "+source.url+" 上的文章...");
            getArticles(source.url, source.since, function (err, articles) {
                if (err) {
                    log(err);
                    return callback(); // 忽略错误
                }
                log("从源 "+source.url+" 获取到 "+articles.length+" 篇文章。");
                async.forEachSeries(articles, function (article, callback) {
                    db.existsArticle(article, function (err, exists) {
                        if (err) return callback(err);
                        if (!exists) {
                            db.saveArticle(article, function (err) {
                                if (err) return callback(err);
                                result.push(article);
                                callback();
                            });
                        } else {
                            callback();
                        }
                    });
                }, function (err) {
                    if (!err) {
                        db.updateFetchTime(source, now, callback);
                    } else {
                        callback(err);
                    }
                });
            });
        }, function (err) {
            handleResult(err, result);
        });
    });
}

function getAllArticlesAndPush() {
    log("获取最新文章并推送...");
    getAllArticles(function (err, articles) {
        if (err) return log(err);
        async.forEachSeries(articles, function (article, callback) {
            pushMessage(article.title, article.description, article.link, article.logo, callback);
        }, function (err) {
            if (err) log(err);

            log("等待 "+(AUTO_RSS_INTERVAL/1000)+" 秒钟...");
            setTimeout(getAllArticlesAndPush, AUTO_RSS_INTERVAL);
        });
    });
}

var exitTimer = null;
function aboutExit() {

    if (exitTimer) return;

    exitTimer = setTimeout(function () {
        process.exit(0);

    }, GRACE_EXIT_TIME);
}

function main(fn) {
    fn();
}

void main(function () {
    process.on('SIGINT', aboutExit);
    process.on('SIGTERM', aboutExit);

    log("等待 "+(AUTO_RSS_INTERVAL/1000)+" 秒钟...");
    setTimeout(getAllArticlesAndPush, AUTO_RSS_INTERVAL);
});
