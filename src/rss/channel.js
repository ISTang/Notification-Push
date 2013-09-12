/**
 * Created with JetBrains WebStorm.
 * User: joebin
 * Date: 13-6-21
 * Time: 下午9:56
 * To change this template use File | Settings | File Templates.
 */
exports.getChannelsFromOpml = getChannelsFromOpml;

var fs = require('fs');
var url = require('url');
var http = require('http');
var async = require('async');
var OpmlParser = require('opmlparser');
//
var BufferHelper = require('bufferhelper');//用于拼接BUffer，防止中文单词断裂
var iconv = require('iconv-lite');  //用于转码

var config = require(__dirname + '/config');
var utils = require(__dirname + '/utils');
var db = require(__dirname + '/db');

const LOG_ENABLED = config.LOG_ENABLED;

var logStream = fs.createWriteStream("logs/channel.log", {"flags": "a"});

Date.prototype.Format = utils.DateFormat;
String.prototype.trim = utils.StringTrim;
String.prototype.format = utils.StringFormat;
Array.prototype.each = function (callback) {
    for (var i = 0 , j = this.length; i < j; i++) {
        callback.call(this, this[i], i);
    }
};

function log(msg) {

    var now = new Date();
    var strDatetime = now.Format("yyyy-MM-dd HH:mm:ss");
    var buffer = "[" + strDatetime + "] " + msg + "[channel]";
    if (logStream != null) logStream.write(buffer + "\r\n");
    if (LOG_ENABLED) console.log(buffer);
}

function getChannelsFromOpml(opmlUrl, handleResult) {

    http.get(opmlUrl, function (res) {

        if (res.statusCode!=200) {

            return handleResult("状态码 "+res.statusCode);
        }

        var bufferHelper = new BufferHelper();
        res.on('data', function (chunk) {

            bufferHelper.concat(chunk);
        });
        res.on('end', function () {

            var text = bufferHelper.toBuffer();
            var encoding = utils.parseXmlEncoding(text);
            if (encoding!=null && encoding.toUpperCase()!="UTF-8")
                text = iconv.decode(text,encoding);
            getChannelsFromText(text, handleResult);
        });
    }).on('error', function (e) {

            handleResult(e.message);
        });

    function getChannelsFromText(text, handleResult) {

        var parser = OpmlParser.parseString(text);
        var err = null;
        var group;
        var channels = [];

        parser.on('error', function (error) {
            err = error;
        });

        parser.on('meta', function (meta) {
            group = meta.title;
        });

        parser.on('feed', function (feed) {
            var channel = {group:group, title: feed.title, description:"["+group+"]"+feed.title, url: feed.xmlurl};
            channels.push(channel);
        });

        parser.on('end', function (feed) {
            handleResult(err, channels);
        });
    }
}

function test(opmlUrl, callback) {

    log("从 OPML \"" + opmlUrl + "\" 获取 RSS 频道...");
    getChannelsFromOpml(opmlUrl, function (err, group, channels) {

        if (err) {
            log("错误: " + err);
            callback(err);
        }

        db.addChannels(group, channels, callback);
    });
}

function main(fn) {
    fn();
}

void main(function () {

    if (process.argv.length > 3 && process.argv[2] == "-opml") {

        db.openDatabase(function (err) {

            if (err) return;

            test(process.argv[3], function (err) {

                if (err) log(err);
                process.exit(0);
            });

        });
    }
});
