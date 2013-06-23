/**
 * Created with JetBrains WebStorm.
 * User: joebin
 * Date: 13-6-5
 * Time: 下午8:42
 * To change this template use File | Settings | File Templates.
 */

exports.addChannel = addChannel;
exports.addChannels = addChannels;
exports.removeChannel = removeChannel;
exports.updateChannel = updateChannel;
exports.getAllChannels = getAllChannels;
//
exports.existsArticle = existsArticle;
exports.saveArticle = saveArticle;
exports.updateFetchTime = updateFetchTime;
//
exports.openDatabase = openDatabase;

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

function addChannel(channel, callback) {

    log("保存频道 "+channel.title+" \""+channel.url+"\"...");
    redis.sismember("rss:channel:url:set", channel.url, function (err, exists) {

        if (err) return callback(err);

        if (exists) {

            log("频道 "+channel.title+" \""+channel.url+"\" 已经存在。");
            return callback();
        }

        var id = uuid.v4().toUpperCase();
        //
        redis.hset("rss:channel:"+id, "group", channel.group);
        redis.hset("rss:channel:"+id, "title", channel.title);
        redis.hset("rss:channel:"+id, "description", channel.description);
        redis.hset("rss:channel:"+id, "url", channel.url);
        //
        redis.sadd("rss:channel:id:set", id);
        redis.sadd("rss:channel:url:set", channel.url);

        callback();
    });
}

function addChannels(channels, callback) {

    async.forEachSeries(channels, function (channel, callback) {

        addChannel(channel, callback);
    }, function (err) {

        callback(err);
    });
}

function removeChannel(channelId, callback) {

    log("删除频道 "+channelId+"...");
    redis.hget("rss:channel:"+channelId, "url", function (err, url) {

        if (err) return callback(err);

        redis.srem("rss:channel:url:set", url);
        redis.srem("rss:channel:id:set", channelId);
        //
        redis.del("rss:channel:"+channelId);

        callback();
    });
}

function updateChannel(channelId, channel, callback) {

    log("修改频道 "+channelId+"...");
    redis.sismember("rss:channel:id:set", channelId, function (err, exists) {

        if (err) return callback(err);

        if (!exists) {

            log("频道 "+channelId+" 不存在!");
            return callback("该频道ID不存在");
        }

        if (typeof channel.group!="undefined")
            redis.hset("rss:channel:"+channelId, "group", channel.group);
        if (typeof channel.title!="undefined")
            redis.hset("rss:channel:"+channelId, "title", channel.title);
        if (typeof channel.description!="undefined")
            redis.hset("rss:channel:"+channelId, "description", channel.description);
        if (typeof channel.url!="undefined") {

            redis.hget("rss:channel:"+channelId, "url", function (err, oldUrl) {

                if (err) return callback(err);
                if (channel.url!=oldUrl) {

                    redis.hset("rss:channel:"+channelId, "url", channel.url);
                    redis.srem("rss:channel:url:set", oldUrl);
                    redis.sadd("rss:channel:url:set", channel.url);
                }
                callback();
            });
        } else {

            callback();
        }
    });
}

function getAllChannels(handleResult) {

    //log("获取所有频道的 ID...");
    redis.smembers("rss:channel:id:set", function (err, channelIds) {

        if (err) return handleResult(err);

        //log("总共获取到 "+channelIds.length+" 个频道 ID。");
        var channels = [];
        async.forEachSeries(channelIds, function (channelId, callback) {

            //log("获 ID 为 "+channelId+" 的频道信息...");
            redis.hgetall("rss:channel:"+channelId, function (err, channel) {

                if (err) return callback(err);

                //log("频道 ID: "+channelId+"\r\n标题: "+channel.title+"\r\n描述: "+(channel.description?channel.description:"(无)")+"\r\nURL: "+channel.url);
                channel.id = channelId;
                channels.push(channel);

                callback();
            });
        }, function (err) {

            handleResult(err, channels);
        });
    });
}

function existsArticle(article, handleResult) {

    //log("检查文章链接 "+article.link+" 是否已存在...");
    redis.sismember("rss:article:links", article.link, function (err, ismember) {

        if (err) return handleResult(err);

        //log("文章链接 "+article.link+" "+(ismember?"已经":"不")+"存在。");
        handleResult(null, ismember);
    });
}

function saveArticle(article, callback) {
    //log("保存文章 "+article.link+"...");
    var id = uuid.v4().toUpperCase();

    if (article.title) redis.hset("rss:article:"+id, "title", article.title);
    redis.hset("rss:article:"+id, "description", (article.description?article.description:""));
    redis.hset("rss:article:"+id, "link", article.link);
    if (article.logo) redis.hset("article:"+id, "logo", article.logo);
    redis.hset("rss:article:"+id, "timestamp", new Date().Format("yyyyMMddhhmmss"));

    redis.sadd("rss:article:set", id);

    redis.sadd("rss:article:links", article.link);

    //log("已保存文章 "+article.link+"。");

    callback();
}

function updateFetchTime(channel, since, callback) {
    redis.hset("rss:channel:"+channel.id, "since", since);
    callback();
}

function openDatabase(callback) {

    log("正在连接数据库...");
    redis = _redis.createClient(REDIS_PORT, REDIS_SERVER);
    redis.on("connect", function () {
        log("已连接到数据库。");
        callback();
    });
    redis.on("error", function (err) {
        log(err);
        callback(err);
    });
}

function main(fn) {
    fn();
}

void main(function () {

});
