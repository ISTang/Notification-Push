/**
 * Created with JetBrains WebStorm.
 * User: joebin
 * Date: 13-3-15
 * Time: 下午10:26
 */
var fs = require('fs');
var path = require('path');
var async = require('async');
//
var config = require(__dirname + '/../config');
var utils = require(__dirname + '/../utils');
var db = require(__dirname + '/../db');

// 导出函数
exports.broadcast = broadcastMessage;
exports.multicast = multicastMessage;
exports.send = sendMessage;
exports.pushMessage = pushMessage;
exports.getMessages = getMessages;
exports.clearMessages = clearMessages;
exports.uploadFiles = uploadFiles;

// 定义常量
const UPLOAD_DIR = config.UPLOAD_DIR;
const DOWNLOAD_URL_BASE = config.DOWNLOAD_URL_BASE;
const MAX_ATTACHMENT_COUNT = config.MAX_ATTACHMENT_COUNT;
const IMAGE_MIME_REGEX = config.IMAGE_MIME_REGEX;
const MIN_EXPIRATION_TIME = config.MIN_EXPIRATION_TIME;
//

const USER_AVATAR = config.USER_AVATAR;

Date.prototype.Format = utils.DateFormat;

var logger = config.log4js.getLogger('message');
logger.setLevel(config.LOG_LEVEL);

// 广播消息
function broadcastMessage(req, res) {
    if (!req.user.passed) {
        res.json({
            success: false,
            errcode: -1,
            errmsg: req.user.reason
        });
        return;
    }

    var appId = req.params.id;

    if (!req.user.permissions.push_message && !req.user.isPublic) {
        res.json({
            success: false,
            errcode: -2,
            errmsg: 'No permission to push message!'
        });
        return;
    }

    if (!req.user.permissions.applications[appId].broadcast && !req.user.isPublic) {
        res.json({
            success: false,
            errcode: -3,
            errmsg: 'No permission to broadcast message to application ' + appId + '!'
        });
        return;
    }

    var message = req.body; //JSON.parse(req.rawBody);
    message.sender_id = req.user.id;
    message.sender_name = req.user.name;

    var attachmentsString = '[';
    if (message.attachments != null) {
        message.attachments.forEach(function (attachment) {
            attachmentsString += '(title=' + attachment.title + ',type=' + attachment.type +
                ',filename=' + attachment.filename + ',url=' + attachment.url + ')';
        });
    }
    attachmentsString += ']';

    logger.trace('Broadcasting message to application ' + req.params.id + (req.user.isPublic ? '(followers)' : '(all)') +
        ': title=' + (message.title != null ? message.title : '') +
        ', body=' + message.body +
        ', type=' + (message.type != null ? message.type : 'text') +
        ', url=' + (message.url != null ? message.url : '') +
        ', generate_time=' + (message.generate_time != null ? message.generate_time : 'now') +
        ', send_time=' + (message.send_time != null ? message.send_time : 'now') +
        ', expiration=' + (message.expiration != null ? message.expiration : 'never') +
        ', callback=' + (message.callback != null ? message.callback : '') +
        ', need_receipt=' + (message.need_receipt != false ? 'yes' : 'no') +
        ', attachments=' + attachmentsString +
        ', sender_id=' + req.user.id +
        ', sender_name=' + req.user.name +
        ''
    );

    pushMessageByBroadcast(message, appId, (req.user.isPublic ? req.user.id : null), res);
}

// 群发消息
function multicastMessage(req, res) {
    if (!req.user.passed) {
        res.json({
            success: false,
            errcode: -1,
            errmsg: req.user.reason
        });
        return;
    }

    var appId = req.params.id;

    if (!req.user.permissions.push_message && !req.user.isPublic) {
        res.json({
            success: false,
            errcode: -2,
            errmsg: 'No permission to push message!'
        });
        return;
    }

    if (!req.user.permissions.applications[appId].multicast && !req.user.isPublic) {
        res.json({
            success: false,
            errcode: -3,
            errmsg: 'No permission to multicast message to application ' + appId + '!'
        });
        return;
    }

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
    message.sender_id = req.user.id;
    message.sender_name = req.user.name;

    var attachmentsString = '[';
    if (message.attachments != null) {
        message.attachments.forEach(function (attachment) {
            attachmentsString += '(title=' + attachment.title + ',type=' + attachment.type +
                ',filename=' + attachment.filename + ',url=' + attachment.url + ')';
        });
    }
    attachmentsString += ']';

    logger.trace('Multicasting message to application ' + req.params.id + (req.user.isPublic ? '(followers)' : '(all)') +
        ': accounts=' + accountsStirng +
        ', title=' + (message.title != null ? message.title : '') +
        ', body=' + message.body +
        ', type=' + (message.type != null ? message.type : 'text') +
        ', url=' + (message.url != null ? message.url : '') +
        ', send_time=' + (message.send_time != null ? message.send_time : 'now') +
        ', generate_time=' + (message.generate_time != null ? message.generate_time : 'now') +
        ', expiration=' + (message.expiration != null ? message.expiration : 'never') +
        ', callback=' + (message.callback != null ? message.callback : '') +
        ', need_receipt=' + (message.need_receipt != false ? 'yes' : 'no') +
        ', attachments=' + attachmentsString +
        ', sender_id=' + req.user.id +
        ', sender_name=' + req.user.name +
        ''
    );

    pushMessageByMulticast(message, appId, accountNames, res);
}

// 发送消息
function sendMessage(req, res) {
    if (!req.user.passed) {
        res.json({
            success: false,
            errcode: -1,
            errmsg: req.user.reason
        });
        return;
    }

    var appId = req.params.id;

    if (!req.user.permissions.push_message && !req.user.isPublic) {
        res.json({
            success: false,
            errcode: -2,
            errmsg: 'No permission to push message!'
        });
        return;
    }

    if (!req.user.permissions.applications[appId].send && !req.user.isPublic) {
        res.json({
            success: false,
            errcode: -3,
            errmsg: 'No permission to send message to application ' + appId + '!'
        });
        return;
    }

    var accountName = req.params.name;

    var message = req.body; //JSON.parse(req.rawBody);
    message.sender_id = req.user.id;
    message.sender_name = req.user.name;

    var attachmentsString = '[';
    if (message.attachments != null) {
        message.attachments.forEach(function (attachment) {
            attachmentsString += '(title=' + attachment.title + ',type=' + attachment.type +
                ',filename=' + attachment.filename + ',url=' + attachment.url + ')';
        });
    }
    attachmentsString += ']';

    logger.trace('Sending message to ' + req.params.name + '@application_' + req.params.id +
        ': title=' + (message.title != null ? message.title : '') +
        ', body=' + message.body +
        ', type=' + (message.type != null ? message.type : 'text') +
        ', url=' + (message.url != null ? message.url : '') +
        ', send_time=' + (message.send_time != null ? message.send_time : 'now') +
        ', generate_time=' + (message.generate_time != null ? message.generate_time : 'now') +
        ', expiration=' + (message.expiration != null ? message.expiration : 'never') +
        ', callback=' + (message.callback != null ? message.callback : '') +
        ', need_receipt=' + (message.need_receipt != false ? 'yes' : 'no') +
        ', attachments=' + attachmentsString +
        ', sender_id=' + req.user.id +
        ', sender_name=' + req.user.name +
        ''
    );

    pushMessageBySend(message, appId, accountName, res);
}

function pushMessage(req, res) {
    // 验证消息字段
    if (req.body.msgBody == "") {
        res.json({
            success: false,
            errcode: 1,
            errmsg: "消息内容为空"
        });
        return;
    }
    if (req.body.appId == "") {
        res.json({
            success: false,
            errcode: 2,
            errmsg: "未提供应用ID"
        });
        return;
    }
    // 初始化消息
    var message = {
        type: "text",
        body: req.body.msgBody,
        need_receipt: true,
        sender_id: req.user.id,
        sender_name: req.user.name
    };
    if (req.body.msgTitle != "")
        message.title = req.body.msgTitle;
    if (req.body.msgUrl != "")
        message.url = req.body.msgUrl;
    if (req.body.expiration != "") {
        var date = utils.DateParse2(req.body.expiration);
        if (date == "Invalid Date") {
            res.json({
                success: false,
                errcode: 3,
                errmsg: "过期时间无效"
            });
            return;
        }
        var now = new Date();
        var diff = date.getTime() - now.getTime();
        if (diff < MIN_EXPIRATION_TIME) {
            res.json({
                success: false,
                errcode: 4,
                errmsg: "过期时间必须不得少于 " + (MIN_EXPIRATION_TIME / (1000 * 60)) +
                    " 分钟\r\r前台传入时间：" + req.body.expiration +
                    "\r系统当前时间：" + now.Format("yyyy-MM-dd HH:mm:ss")
            });
            return;
        }
        message.expiration = date.Format("yyyyMMddHHmmss");
    }
    //
    var tmpAttachments = [];
    for (var i = 1; i < MAX_ATTACHMENT_COUNT; i++) {
        var attachment = req.files["attachment" + i];
        if (attachment) tmpAttachments.push(attachment);
    }
    var attachments = [];
    async.forEach(tmpAttachments, function (tmpAttachment, callback) {
        // 检查MIME类型
        if (!IMAGE_MIME_REGEX.test(tmpAttachment.type)) {
            return callback("不支持的附件类型 '" + tmpAttachment.type + "'");
        }
        // 确定原始文件的基本名和扩展名
        var baseName, extName;
        var filenameFields = tmpAttachment.name.split(".");
        if (filenameFields.length > 1) {
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
        var filename = tmpPathFields[tmpPathFields.length - 1] + "." + extName;
        // 生成附件对象
        var attachment = {
            title: baseName,
            type: tmpAttachment.type,
            filename: tmpAttachment.name,
            url: DOWNLOAD_URL_BASE + filename
        };
        // 确定文件上传后的路径并移动文件
        var targetPath = path.join(UPLOAD_DIR, filename);
        fs.writeFileSync(targetPath, fs.readFileSync(tmpPath, ''));
        fs.unlinkSync(tmpPath);
        attachments.push(attachment);
        callback();
    }, function (err) {
        if (err) {
            res.json({
                success: false,
                errcode: 5,
                errmsg: err.toString()
            });
        } else {
            if (attachments.length != 0) message.attachments = attachments;
            handleMessage(req.body.pushMethod, message, req.body.appId, req.body.receivers, req, res);
        }
    });

    function handleMessage(method, message, appId, receiverNames, req, res) {
        switch (method) {
            case "broadcast":
                if (!req.user.permissions.applications[appId].broadcast && !req.user.isPublic) {
                    res.json({
                        success: false,
                        errcode: -1,
                        errmsg: 'No permission to broadcast message to application ' + appId + '!'
                    });
                    return;
                }
                pushMessageByBroadcast(message, appId, (req.user.isPublic ? req.user.id : null), res);
                break;
            case "multicast":
                if (!req.user.permissions.applications[appId].multicast && !req.user.isPublic) {
                    res.json({
                        success: false,
                        errcode: -1,
                        errmsg: 'No permission to multicast message to application ' + appId + '!'
                    });
                    return;
                }
                var receivers = receiverNames.split(",");
                if (receivers.length == 0)
                    res.json({success: false, errcode: 6, errmsg: "无接收者"});
                else if (receivers.length < 2)
                    res.json({success: false, errcode: 7, errmsg: "接收者个数小于2"});
                else
                    pushMessageByMulticast(message, appId, receivers, res);
                break;
            case "send":
                if (!req.user.permissions.applications[appId].send && !req.user.isPublic) {
                    res.json({
                        success: false,
                        errcode: -1,
                        errmsg: 'No permission to send message to application ' + appId + '!'
                    });
                    return;
                }
                var receivers = receiverNames.split(",");
                if (receivers.length == 0)
                    res.json({success: false, errcode: 6, errmsg: "无接收者"});
                else if (receivers.length != 1)
                    res.json({success: false, errcode: 8, errmsg: "接收者不唯一"});
                else
                    pushMessageBySend(message, appId, receivers[0], res);
                break;
            default:
                // 不可能发生
                break;
        }
    }
}

function getMessages(req, res) {
    logger.trace('Get messages: ' +
        'sEcho=' + req.query.sEcho +
        ', sSearch=' + req.query.sSearch +
        ', iDisplayLength=' + req.query.iDisplayLength +
        ', iDisplayStart=' + req.query.iDisplayStart +
        ', iColumns=' + req.query.iSortingCols +
        ', sColumns=' + req.query.sColumns +
        ''
    );

    var iSortCol_0 = req.query.iSortCol_0;
    var sSortDir_0 = req.query.sSortDir_0;

    db.redisPool.acquire(function (err, redis) {
        if (err) {
            res.json({sEcho: parseInt(req.query.sEcho, 10), iTotalRecords: 1,
                iTotalDisplayRecords: 1, aaData: [
                    ['无法访问数据库: ' + err]
                ],
                sColumns: "messageId"});
        } else {
            db.getAllMessages(redis, function (err, messages) {

                db.redisPool.release(redis);

                if (err) {
                    return res.json({sEcho: parseInt(req.query.sEcho, 10), iTotalRecords: 1,
                        iTotalDisplayRecords: 1, aaData: [
                            ['数据库操作失败: ' + err]
                        ],
                        sColumns: "messageId"});
                }

                var filtered = [];
                if (req.query.sSearch != "") {
                    // 过滤
                    for (var i in messages) {
                        var message = messages[i];
                        if (message.messageId.indexOf(req.query.sSearch) != -1 ||
                            message.applicationName.indexOf(req.query.sSearch) != -1 ||
                            message.sender && message.sender.indexOf(req.query.sSearch) != -1 ||
                            message.receivers && message.receivers.indexOf(req.query.sSearch) != -1 ||
                            (message.type || "text").indexOf(req.query.sSearch) != -1 ||
                            message.title && message.title.indexOf(req.query.sSearch) != -1 ||
                            message.body.indexOf(req.query.sSearch) != -1 ||
                            //message.attachmentCount.toString().indexOf(req.query.sSearch)!=-1 ||
                            message.generateTime.indexOf(req.query.sSearch) != -1) {
                            filtered.push(message);
                        }
                    }
                } else {
                    // 不需要过滤
                    filtered = messages;
                }

                // 排序
                filtered.sort(function (x, y) {
                    switch (parseInt(iSortCol_0, 10)) {
                        case 0: // messageId
                            return compareString(x.messageId, y.mesageId);
                        case 1: // applicationName
                            return compareString(x.applicationName, y.applicationName);
                        case 2: // sender
                            return compareString(x.sender, y.sender);
                        case 3: // receivers
                            return compareString(x.receivers, y.receivers);
                        case 4: // type
                            return compareString(x.type, y.type);
                        case 5: // title
                            return compareString(x.title, y.title);
                        case 6: // body
                            return compareString(x.body, y.body);
                        case 7: // url
                            return compareString(x.url, y.url);
                        case 8: // attachments
                            return compareInteger(x.attachmentCount, y.attachmentCount);
                        case 9: // generateTime
                            return compareString(x.generateTime, y.generateTime);
                    }
                });

                // 分页
                var iDisplayStart = parseInt(req.query.iDisplayStart, 10);
                var iDisplayLength = parseInt(req.query.iDisplayLength, 10);
                var paged = filtered.slice(iDisplayStart, Math.min(iDisplayStart + iDisplayLength, filtered.length));
                var result = [];
                for (var i in paged) {
                    var message = paged[i];
                    var attachments = message.attachments;
                    result.push([message.messageId, message.applicationName, message.sender, message.receivers,
                        message.type, message.title, message.body, message.url, attachments, message.generateTime]);
                }

                return res.json({sEcho: parseInt(req.query.sEcho, 10), iTotalRecords: messages.length,
                    iTotalDisplayRecords: filtered.length, aaData: result,
                    sColumns: "messageId,applicationName,sender,receivers,type,title,body,url,attachments,generateTime"});
            });
        }
    });

    function compareString(s1, s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return (sSortDir_0 == 'asc' ? 0 : 1);
        if (s2 == null) return (sSortDir_0 == 'asc' ? 1 : 0);

        return (sSortDir_0 == 'asc' ? s1.localeCompare(s2) : s2.localeCompare(s1));
    }

    function compareInteger(i1, i2) {
        if (sSortDir_0 == 'asc') return (i1 < i2 ? 0 : 1);
        else return (i1 > i2 ? 0 : 1);
    }
}

function uploadFiles(req, res) {
    logger.trace("Uploading files..."+JSON.stringify(req.files));
    var downloadUrls = [];
    async.forEachSeries(req.files, function (name, callback) {
    	var file = req.files[name];
    	logger.trace("Found "+name+"("+file.type+"): "+file.name);
        // 检查MIME类型
        if (!IMAGE_MIME_REGEX.test(file.type)) {
            logger.warn("Unsupported file type: "+file.type);
            return callback("不支持的文件类型 '" + file.type + "'");
        }
        // 确定原始文件的基本名和扩展名
        var baseName, extName;
        var filenameFields = file.name.split(".");
        if (filenameFields.length > 1) {
            // 有扩展名
            extName = filenameFields.pop();
            baseName = filenameFields.join(".");
        } else {
            // 无扩展名
            extName = "";
            baseName = filenameFields[0];
        }
        // 获得文件的临时路径
        var tmpPath = file.path;
        var tmpPathFields = tmpPath.split(path.sep);
        // 确定保存后的文件名
        var filename = tmpPathFields[tmpPathFields.length - 1] + "." + extName;
        // 生成下载URL
        var downloadUrl = DOWNLOAD_URL_BASE + filename;
        logger.trace("Download url: "+downloadUrl);
        // 确定文件上传后的路径并移动文件
        var targetPath = path.join(UPLOAD_DIR, filename);
        fs.writeFileSync(targetPath, fs.readFileSync(tmpPath, ''));
        fs.unlinkSync(tmpPath);
        downloadUrls.push(downloadUrl);
        callback();
    }, function (err) {
        if (err) {
            res.json({
                success: false,
                errcode: 1,
                errmsg: err.toString()
            });
        } else {
            res.json({
                success: true,
                urls: downloadUrls
            });
        }
    });
}

function pushMessageByBroadcast(message, appId, publicAccountId, res) {
    message.generate_time = new Date().Format("yyyyMMddHHmmss");
    var needReceipt = message.need_receipt;

    db.redisPool.acquire(function (err, redis) {
        if (err) {
            res.json({success: false, errcode: 1, errmsg: err});
        } else {
            db.getUserAvatar(redis, message.sender_id, function (err, avatar) {
                if (err) {
                    db.redisPool.release(redis);
                    res.json({success: false, errcode: 2, errmsg: err});
                    return;
                }
                if (!message.attachments) message.attachments = [];
                message.attachments.unshift({title: "sender-avatar", type: "image/xxx", filename: "sender-avatar", url: (avatar || USER_AVATAR)});
                logger.trace("Saving message for application " + appId + (publicAccountId ? " and public account " + publicAccountId : ""));
                db.saveMessage(redis, appId, publicAccountId, message, [], function (err, msgId) {
                    if (err) {
                        db.redisPool.release(redis);
                        return res.json({success: false, errcode: 2, errmsg: err});
                    }

                    logger.trace("Message id: " + msgId);

                    delete message.sender_id;
                    delete message.need_receipt;

                    db.getActiveConnections(redis, appId, publicAccountId, null, function (err, connectionInfos) {

                        db.redisPool.release(redis);

                        if (err) return res.json({success: false, errcode: 3, errmsg: err});

                        connectionInfos.forEach(function (connectionInfo) {
                            if (!message.send_time) {
                                // 立即发送
                                logger.trace("Publishing message " + msgId + " on connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "]...");
                                publishMessage(connectionInfo, msgId, message, needReceipt);
                            } else {
                                // 延迟发送
                                var sendTime = utils.DateParse(message.send_time);
                                var timeout = sendTime.getTime() - new Date();
                                logger.trace("Will publish message " + msgId + " on connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "] after " + timeout + " ms");
                                setTimeout(function () {
                                    logger.trace("Publishing message " + msgId + " on connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "]...");
                                    publishMessage(connectionInfo, msgId, message, needReceipt);
                                }, timeout);
                            }
                        });

                        res.json({success: true});
                    });
                });
            });
        }
    });
}

function pushMessageByMulticast(message, appId, accountNames, res) {
    var now = new Date();
    message.generate_time = now.Format("yyyyMMddHHmmss");
    var needReceipt = message.need_receipt;

    logger.trace("Saving message for application " + appId);
    db.redisPool.acquire(function (err, redis) {
        if (err) {
            res.json({success: false, errcode: 1, errmsg: err});
        } else {
            var accountIds = [];
            async.forEachSeries(accountNames, function (accountName, callback) {
                db.getAccountIdByName(redis, accountName, function (err, accountId) {

                    if (err) return callback(err);
                    if (!accountId) return callback("Account " + accountName + " not exists");
                    accountIds.push(accountId);
                    callback();
                });
            }, function (err) {
                if (err) {
                    db.redisPool.release(redis);
                    res.json({success: false, errcode: 2, errmsg: err});
                    return;
                }
                db.getUserAvatar(redis, message.sender_id, function (err, avatar) {
                    if (err) {
                        db.redisPool.release(redis);
                        res.json({success: false, errcode: 3, errmsg: err});
                        return;
                    }
                    if (!message.attachments) message.attachments = [];
                    message.attachments.unshift({title: "sender-avatar", type: "image/xxx", filename: "sender-avatar", url: (avatar || USER_AVATAR)});
                    db.saveMessage(redis, appId, null, message, accountIds, function (err, msgId) {
                        if (err) {
                            db.redisPool.release(redis);
                            return res.json({success: false, errcode: 3, errmsg: err});
                        }
                        logger.trace("Message id: " + msgId);

                        delete message.sender_id;
                        delete message.need_receipt;

                        async.forEachSeries(accountIds, function (accountId, callback) {
                            db.getActiveConnections(redis, appId, null, accountId, function (err, connectionInfos) {

                                if (err) return callback(err);

                                connectionInfos.forEach(function (connectionInfo) {
                                    if (!message.send_time) {
                                        // 立即发送
                                        logger.trace("Publishing message " + msgId + " to connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "]...");
                                        publishMessage(connectionInfo, msgId, message, needReceipt);
                                    } else {
                                        // 延迟发送
                                        var sendTime = utils.DateParse(message.send_time);
                                        var timeout = sendTime.getTime() - new Date();
                                        logger.trace("Will publish message " + msgId + " to connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "] after " + timeout + " ms");
                                        setTimeout(function () {
                                            logger.trace("Publishing message " + msgId + " to connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "]...");
                                            publishMessage(connectionInfo, msgId, message, needReceipt);
                                        }, timeout);
                                    }
                                });
                                callback();
                            });
                        }, function (err) {
                            db.redisPool.release(redis);
                            if (err) res.json({success: false, errcode: 2, errmsg: err});
                            else res.json({success: true});
                        });
                    });
                });
            });
        }
    });
}

function pushMessageBySend(message, appId, accountName, res) {
    var now = new Date();
    message.generate_time = now.Format("yyyyMMddHHmmss");
    var needReceipt = message.need_receipt;

    logger.trace("Saving message for application " + appId);
    db.redisPool.acquire(function (err, redis) {
        if (err) {
            res.json({success: false, errcode: 1, errmsg: err});
        } else {
            db.getAccountIdByName(redis, accountName, function (err, accountId) {

                if (err) {
                    db.redisPool.release(redis);
                    return res.json({success: false, errcode: 2, errmsg: err});
                }
                if (!accountId) {
                    db.redisPool.release(redis);
                    return res.json({success: false, errcode: 3, errmsg: "Account " + accountName + " not exists"});
                }

                db.getUserAvatar(redis, message.sender_id, function (err, avatar) {
                    if (err) {
                        db.redisPool.release(redis);
                        res.json({success: false, errcode: 4, errmsg: err});
                        return;
                    }
                    if (!message.attachments) message.attachments = [];
                    message.attachments.unshift({title: "sender-avatar", type: "image/xxx", filename: "sender-avatar", url: (avatar || USER_AVATAR)});
                    db.saveMessage(redis, appId, null, message, [accountId], function (err, msgId) {
                        if (err) {
                            db.redisPool.release(redis);
                            return res.json({success: false, errcode: 4, errmsg: err});
                        }
                        logger.trace("Message id: " + msgId);

                        delete message.sender_id;
                        delete message.need_receipt;

                        db.getActiveConnections(redis, appId, null, accountId, function (err, connectionInfos) {

                            db.redisPool.release(redis);

                            if (err) return res.json({success: false, errcode: 4, errmsg: err});
                            ;

                            connectionInfos.forEach(function (connectionInfo) {
                                if (!message.send_time) {
                                    // 立即发送
                                    logger.trace("Publishing message " + msgId + " to connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "]...");
                                    publishMessage(connectionInfo, msgId, message, needReceipt);
                                } else {
                                    // 延迟发送
                                    var sendTime = utils.DateParse(message.send_time);
                                    var timeout = sendTime.getTime() - new Date();
                                    logger.trace("Will publish message " + msgId + " to connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "] after " + timeout + " ms");
                                    setTimeout(function () {
                                        logger.trace("Publishing message " + msgId + " to connection " + connectionInfo.connId + "[" + connectionInfo.channelId + "]...");
                                        publishMessage(connectionInfo, msgId, message, needReceipt);
                                    }, timeout);
                                }
                            });

                            res.json({success: true});
                        });
                    });
                });
            });
        }
    });
}

function publishMessage(connectionInfo, msgId, message, needReceipt) {
    var o = {
        connId: connectionInfo.connId,
        msgId: msgId,
        message: message,
        msgKey: connectionInfo.msgKey,
        needReceipt: needReceipt};
    db.redisPool.acquire(function (err, msgPub) {
        if (err) {
            logger.fatal(err);
            process.exit(1);
        } else {
            msgPub.publish(connectionInfo.channelId, JSON.stringify(o));
            db.redisPool.release(msgPub);
        }
    });
}

function clearMessages(callback) {
    db.redisPool.acquire(function (err, redis) {
        if (err) {
            calback(err);
        } else {
            db.clearMessages(redis, function (err) {
                db.redisPool.release(redis);
                callback(err);
            });
        }
    });
}
