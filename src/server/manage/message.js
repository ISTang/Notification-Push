/**
 * Created with JetBrains WebStorm.
 * User: joebin
 * Date: 13-3-15
 * Time: 下午10:26
 */
var fs = require('fs');
var path = require('path');
var async = require('async');
var _redis = require('redis');
//
var config = require(__dirname + '/../config');
var utils = require(__dirname + '/../utils');
var db = require(__dirname + '/../db');

// 导出函数
exports.broadcast = broadcastMessage;
exports.multicast = multicastMessage;
exports.send = sendMessage;
exports.pushMessage = pushMessage;

// 定义常量
const UPLOAD_DIR = config.UPLOAD_DIR;
const DOWNLOAD_URL_BASE = config.DOWNLOAD_URL_BASE;
const MAX_ATTACHMENT_COUNT = config.MAX_ATTACHMENT_COUNT;
const IMAGE_MIME_REGEX = config.IMAGE_MIME_REGEX;
const MIN_EXPIRATION_TIME = config.MIN_EXPIRATION_TIME;
//
const REDIS_SERVER = config.REDIS_SERVER;
const REDIS_PORT = config.REDIS_PORT;
//
const LOG_ENABLED = config.LOG_ENABLED;

var logStream = LOG_ENABLED ? fs.createWriteStream("logs/message.log", {"flags": "a"}) : null;

var msgPub;

Date.prototype.Format = utils.DateFormat;

/*
 * Print log
 */
function log(msg) {
    var now = new Date();
    var strDatetime = now.Format("yyyy-MM-dd HH:mm:ss");
    var buffer = "[" + strDatetime + "] " + msg + "[message]";
    if (logStream != null) logStream.write(buffer + "\r\n");
    console.log(buffer);
}

// 广播消息
function broadcastMessage(req, res) {
    var appId = req.params.id;

    var message = req.body; //JSON.parse(req.rawBody);
    var attachmentsString = '[';
    if (message.attachments != null) {
        message.attachments.forEach(function (attachment) {
            attachmentsString += '(title=' + attachment.title + ',type=' + attachment.type +
                ',filename=' + attachment.filename + ',url=' + attachment.url + ')';
        });
    }
    attachmentsString += ']';

    log('Broadcasting message to application ' + req.params.id +
        ': title=' + (message.title != null ? message.title : '') +
        ', body=' + message.body +
        ', type=' + (message.type != null ? message.type : 'text') +
        ', url=' + (message.url != null ? message.url : '') +
        ', send_time=' + (message.send_time != null ? message.send_time : 'now') +
        ', expiration=' + (message.expiration != null ? message.expiration : 'never') +
        ', callback=' + (message.callback != null ? message.callback : '') +
        ', need_receipt=' + (message.need_receipt != false ? 'yes' : 'no') +
        ', attachments=' + attachmentsString +
        ''
    );

    pushMessageByBroadcast(message, appId, res);
}

// 群发消息
function multicastMessage(req, res) {
    var appId = req.params.id;

    //var input = req.body; //JSON.parse(req.rawBody);
    var accounts = req.body.accounts;
    var accountNames = [];
    var accountsStirng = '[';
    accounts.forEach(function (account) {
        accountsStirng += '(name=' + account.name + ')';
        accountNames.push(account.name);
    });
    accountsStirng += ']';

    var message = req.body.message;
    var attachmentsString = '[';
    if (message.attachments != null) {
        message.attachments.forEach(function (attachment) {
            attachmentsString += '(title=' + attachment.title + ',type=' + attachment.type +
                ',filename=' + attachment.filename + ',url=' + attachment.url + ')';
        });
    }
    attachmentsString += ']';

    log('Multicasting message to application ' + req.params.id +
        ': accounts=' + accountsStirng +
        ', title=' + (message.title != null ? message.title : '') +
        ', body=' + message.body +
        ', type=' + (message.type != null ? message.type : 'text') +
        ', url=' + (message.url != null ? message.url : '') +
        ', send_time=' + (message.send_time != null ? message.send_time : 'now') +
        ', expiration=' + (message.expiration != null ? message.expiration : 'never') +
        ', callback=' + (message.callback != null ? message.callback : '') +
        ', need_receipt=' + (message.need_receipt != false ? 'yes' : 'no') +
        ', attachments=' + attachmentsString +
        ''
    );

    pushMessageByMulticast(message, appId, accountNames, res);
}

// 发送消息
function sendMessage(req, res) {
    var appId = req.params.id;
    var accountName = req.params.name;

    var message = req.body; //JSON.parse(req.rawBody);
    var attachmentsString = '[';
    if (message.attachments != null) {
        message.attachments.forEach(function (attachment) {
            attachmentsString += '(title=' + attachment.title + ',type=' + attachment.type +
                ',filename=' + attachment.filename + ',url=' + attachment.url + ')';
        });
    }
    attachmentsString += ']';

    log('Sending message to ' + req.params.name + '@application_' + req.params.id +
        ': title=' + (message.title != null ? message.title : '') +
        ', body=' + message.body +
        ', type=' + (message.type != null ? message.type : 'text') +
        ', url=' + (message.url != null ? message.url : '') +
        ', send_time=' + (message.send_time != null ? message.send_time : 'now') +
        ', expiration=' + (message.expiration != null ? message.expiration : 'never') +
        ', callback=' + (message.callback != null ? message.callback : '') +
        ', need_receipt=' + (message.need_receipt != false ? 'yes' : 'no') +
        ', attachments=' + attachmentsString +
        ''
    );

    pushMessageBySend(message, appId, accountName, res);
}

function pushMessage(req, res) {
    // 验证消息字段
    if (req.body.msgBody=="") {
        res.json({
            success:false,
            errcode: 1,
            errmsg: "消息内容为空"
        });
        return;
    }
    if (req.body.appId=="") {
        res.json({
            success:false,
            errcode: 2,
            errmsg: "未提供应用ID"
        });
        return;
    }
    // 初始化消息
    var message = {
        type: "text",
        body: req.body.msgBody,
        need_receipt: true
    };
    if (req.body.msgTitle!="")
        message.title = req.body.msgTitle;
    if (req.body.msgUrl!="")
        message.url = req.body.msgUrl;
    if (req.body.expiration!="") {
        var date = utils.DateParse2(req.body.expiration);
        if (date=="Invalid Date") {
            res.json({
                success:false,
                errcode: 3,
                errmsg: "过期时间无效"
            });
            return;
        }
		var now = new Date();
        var diff = date.getTime()-now.getTime();
        if (diff<MIN_EXPIRATION_TIME) {
            res.json({
                success:false,
                errcode: 4,
                errmsg: "过期时间必须不得少于 "+(MIN_EXPIRATION_TIME/(1000*60))+
					" 分钟\r\r前台传入时间："+req.body.expiration+
					"\r系统当前时间："+now.Format("yyyy-MM-dd HH:mm:ss")
            });
            return;
        }
        message.expiration = date.Format("yyyyMMddHHmmss");
    }
    //
    var tmpAttachments = [];
    for (var i=1; i<MAX_ATTACHMENT_COUNT; i++) {
        var attachment = req.files["attachment"+i];
        if (attachment) tmpAttachments.push(attachment);
    }
    var attachments = [];
    async.forEach(tmpAttachments, function (tmpAttachment, callback) {
        // 检查MIME类型
        if (!IMAGE_MIME_REGEX.test(tmpAttachment.type)) {
            return callback("不支持的附件类型 '"+tmpAttachment.type+"'");
        }
        // 确定原始文件的基本名和扩展名
        var baseName, extName;
        var filenameFields = tmpAttachment.name.split(".");
        if (filenameFields.length>1) {
            // 有扩展名
            extName = filenameFields.pop();
            baseName = filenameFields.join(".");
        } else {
            // 无扩展名
            extName = "";
            baseName = filenameFields[0];
        }
        // 获得文件的临时路径
        var tmpPath = tmpAttachment.path;
        var tmpPathFields = tmpPath.split(path.sep);
        // 确定保存后的文件名
        var filename = tmpPathFields[tmpPathFields.length-1]+"."+extName;
        // 生成附件对象
        var attachment = {
            title: baseName,
            type: tmpAttachment.type,
            filename: tmpAttachment.name,
            url: DOWNLOAD_URL_BASE + filename
        };
        // 确定文件上传后的路径并移动文件
        var targetPath = path.join(UPLOAD_DIR , filename);
        fs.writeFileSync(targetPath,fs.readFileSync(tmpPath, ''));
        fs.unlinkSync(tmpPath);
        attachments.push(attachment);
        callback();
    }, function (err) {
        if (err) {
            res.json({
                success:false,
                errcode: 5,
                errmsg: err.toString()
            });
        } else {
            if (attachments.length!=0) message.attachments = attachments;
            handleMessage(req.body.pushMethod, message, req.body.appId, req.body.receivers, res);
        }
    });

    function handleMessage(method, message, appId, receiverNames, res) {
        switch (method) {
            case "broadcast":
                pushMessageByBroadcast(message, appId, res);
                break;
            case "multicast":
                var receivers = receiverNames.split(",");
                if (receivers.length==0)
                    res.json({success:false,errcode:6,errmsg:"无接收者"});
                else if (receivers.length<2)
                    res.json({success:false,errcode:7,errmsg:"接收者个数小于2"});
                else
                    pushMessageByMulticast(message, appId, receivers, res);
                break;
            case "send":
                var receivers = receiverNames.split(",");
                if (receivers.length==0)
                    res.json({success:false,errcode:6,errmsg:"无接收者"});
                else if (receivers.length!=1)
                    res.json({success:false,errcode:8,errmsg:"接收者不唯一"});
                else
                    pushMessageBySend(message, appId, receivers[0], res);
                break;
            default:
                // 不可能发生
                break;
        }
    }
}

function pushMessageByBroadcast(message, appId, res) {
    message.generate_time = new Date().Format("yyyyMMddHHmmss");
    var needReceipt = message.need_receipt;

    log("Saving message for application " + appId);
    var msgId = db.saveMessage(appId, message);
    log("Message id: " + msgId);

    delete message.need_receipt;

    db.getActiveConnections(appId, null, function (err, connectionInfos) {

        if (err) return res.json({success: false, errcode: 1, errmsg: err});

        connectionInfos.forEach(function (connectionInfo) {
            if (!message.send_time) {
                // 立即发送
                log("Publishing message " + msgId + " on connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "]...");
                publishMessage(connectionInfo, msgId, message, needReceipt);
            } else {
                // 延迟发送
                var sendTime = utils.DateParse(message.send_time);
                var timeout = sendTime.getTime() - new Date();
                log("Will publish message " + msgId + " on connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "] after " + timeout + " ms");
                setTimeout(function () {
                    log("Publishing message " + msgId + " on connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "]...");
                    publishMessage(connectionInfo, msgId, message, needReceipt);
                }, timeout);
            }
        });

        res.json({success: true});
    });
}

function pushMessageByMulticast(message, appId, accountNames, res) {
    var now = new Date();
    message.generate_time = now.Format("yyyyMMddHHmmss");
    var needReceipt = message.need_receipt;

    log("Saving message for application " + appId);
    var msgId = db.saveMessage(appId, message, []);
    log("Message id: " + msgId);

    delete message.need_receipt;

    async.forEachSeries(accountNames, function(accountName, callback) {
        db.getAccountIdByName(accountName, function (err, accountId) {

            if (err) return callback(err);
            if (!accountId) return callback("Account "+accountName+" not exists");

            db.addMessageToAccounts(msgId, [accountId], appId, now);

            db.getActiveConnections(appId, accountId, function (err, connectionInfos) {

                if (err) return callback(err);

                connectionInfos.forEach(function(connectionInfo) {
                    if (!message.send_time) {
                        // 立即发送
                        log("Publishing message " + msgId + " to connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "]...");
                        publishMessage(connectionInfo, msgId, message, needReceipt);
                    } else {
                        // 延迟发送
                        var sendTime = utils.DateParse(message.send_time);
                        var timeout = sendTime.getTime() - new Date();
                        log("Will publish message " + msgId + " to connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "] after " + timeout + " ms");
                        setTimeout(function () {
                            log("Publishing message " + msgId + " to connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "]...");
                            publishMessage(connectionInfo, msgId, message, needReceipt);
                        }, timeout);
                    }
                });
                callback();
            });
        });
    }, function (err) {
        if (err) res.json({success: false, errcode: 1, errmsg: err});
        else res.json({success: true});
    });
}

function pushMessageBySend(message, appId, accountName, res) {
    var now = new Date();
    message.generate_time = now.Format("yyyyMMddHHmmss");
    var needReceipt = message.need_receipt;

    log("Saving message for application " + appId);
    var msgId = db.saveMessage(appId, message, []);
    log("Message id: " + msgId);

    delete message.need_receipt;

    db.getAccountIdByName(accountName, function (err, accountId) {

        if (err) return res.json({success: false, errcode: 1, errmsg: err});
        if (!accountId) return res.json({success: false, errcode: 2, errmsg: "Account " + accountName + " not exists"});

        db.addMessageToAccounts(msgId, [accountId], appId, now);

        db.getActiveConnections(appId, accountId, function (err, connectionInfos) {

            if (err) return res.json({success: false, errcode: 3, errmsg: err});;

            connectionInfos.forEach(function(connectionInfo) {
                if (!message.send_time) {
                    // 立即发送
                    log("Publishing message " + msgId + " to connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "]...");
                    publishMessage(connectionInfo, msgId, message, needReceipt);
                } else {
                    // 延迟发送
                    var sendTime = utils.DateParse(message.send_time);
                    var timeout = sendTime.getTime() - new Date();
                    log("Will publish message " + msgId + " to connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "] after " + timeout + " ms");
                    setTimeout(function () {
                        log("Publishing message " + msgId + " to connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "]...");
                        publishMessage(connectionInfo, msgId, message, needReceipt);
                    }, timeout);
                }
            });

            res.json({success: true});
        });
    });
}

function publishMessage(connectionInfo, msgId, message, needReceipt) {
    var o = {
        connId: connectionInfo.connId,
        msgId: msgId,
        message: message,
        msgKey: connectionInfo.msgKey,
        needReceipt: needReceipt};
    msgPub.publish(connectionInfo.channelId, JSON.stringify(o));
}

function main(fn) {
    fn();
}

void main(function () {

    msgPub = _redis.createClient(REDIS_PORT, REDIS_SERVER);
    msgPub.on("error", function (err) {
        log("Error " + err);
    });
});
