/**
 * Created with JetBrains WebStorm.
 * User: joebin
 * Date: 13-6-5
 * Time: 下午8:42
 * To change this template use File | Settings | File Templates.
 */

exports.getAllSources = getAllSources;
exports.existsArticle = existsArticle;
exports.saveArticle = saveArticle;

var config = require(__dirname + '/config');
var utils = require(__dirname + '/utils');

var fs = require('fs');
var async = require('async');
var _redis = require("redis");
var uuid = require('node-uuid');

const REDIS_SERVER = config.REDIS_SERVER;
const REDIS_PORT = config/REDIS_PORT;

const LOG_ENABLED = config.LOG_ENABLED;


var logStream = LOG_ENABLED ? fs.createWriteStream("logs/db.log", {"flags": "a"}) : null;

var redis;

Date.prototype.Format = utils.DateFormat;
String.prototype.trim = utils.StringTrim;
String.prototype.format = utils.StringFormat;

/*
 * Print log
 */
function log(msg) {

    var now = new Date();
    var strDatetime = now.Format("yyyy-MM-dd HH:mm:ss");
    var buffer = "[" + strDatetime + "] " + msg + "[db]";
    if (logStream != null) logStream.write(buffer + "\r\n");
    console.log(buffer);
}

function getAllSources(handleResult) {
    log("获取所有源的 ID...");
    redis.smembers("source:set", function (err, sourceIds) {
        if (err) return handleResult(err);
        log("总共获取到 "+sourceIds.length+" 个源 ID。");
        var sources = [];
        async.forEachSeries(sourceIds, function (sourceId, callback) {
            log("获 ID 为 "+sourceId+" 的源信息...");
            redis.hgetall("source:"+sourceId, function (err, source) {
                if (err) return callback(err);
                log("源 ID: "+sourceId+"\r\n标题: "+source.title+"\r\n描述: "+source.description+"\r\nURL: "+source.url);
                sources.push(source);
                callback();
            });
        }, function (err) {
            handleResult(err, sources);
        });
    });
}

function existsArticle(article, handleResult) {
    log("检查文章链接 "+article.link+" 是否已存在...");
    redis.sismember("article:links", article.link, function (err, ismember) {
        if (err) return handleResult(err);
        log("文章链接 "+article.link+" "+(ismember?"已经":"不")+"存在。");
        handleResult(null, ismember);
    });
}

function saveArticle(article, callback) {
    log("保存文章 "+article.title+"["+article.link+"]...");
    var id = uuid.v4().toUpperCase();

    redis.hset("article:"+id, "title", article.title);
    redis.hset("article:"+id, "description", article.description);
    redis.hset("article:"+id, "link", article.link);
    redis.hset("article:"+id, "logo", article.logo);
    redis.hset("article:"+id, "timestamp", new Date().Format("yyyyMMddhhmmss"));

    redis.sadd("article:set", id);

    redis.sadd("article:links", article.link);

    log("已保存文章 "+article.title+"["+article.link+"]。");

    callback();
}

function main(fn) {
    fn();
}

void main(function () {
    log("正在连接数据库...");
    redis = _redis.createClient(REDIS_PORT, REDIS_SERVER);
    redis.on("connect", function (err) {
        log("已连接到数据库。");
    });
    redis.on("error", function (err) {
        log(err);
    });
});
