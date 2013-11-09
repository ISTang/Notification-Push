/**
 * Created with JetBrains WebStorm.
 * User: joebin
 * Date: 13-3-18
 * Time: 下午11:01
 */
// 引入依赖包
var fs = require('fs');
var async = require('async');
var uuid = require('node-uuid');
var randomstring = require("randomstring");
var lockFile = require('lockfile');
var _redis = require("redis");
var poolModule = require('generic-pool');
//
var config = require(__dirname + '/config');
var utils = require(__dirname + '/utils');

exports.checkAppId = checkAppId; // login
exports.checkUsername = checkUsername; // login
exports.saveNewAccountInfo = saveNewAccountInfo; // login also
exports.saveLoginInfo = saveLoginInfo; // login
//
exports.removeLoginInfo = removeLoginInfo; // notify
exports.recordMessageSentTime = recordMessageSentTime; // notify
exports.recordMessageReceiptTime = recordMessageReceiptTime; // notify
exports.recordLatestActivity = recordLatestActivity; // notify
exports.getOfflineMessages = getOfflineMessages; // notify
//
exports.saveMessage = saveMessage; // notify also
exports.getAccountIdByName = getAccountIdByName; // notify also
exports.getActiveConnections = getActiveConnections; // notify also
//
exports.getApplicationById = getApplicationById;
exports.getMessageById = getMessageById;
exports.getMessageAllById = getMessageAllById;
//
exports.checkNewAccountInfo = checkNewAccountInfo;
exports.checkAccountUpdateInfo = checkNewAccountInfo;
exports.updateAccountInfo = updateAccountInfo;
exports.updateAccountStatus = updateAccountStatus;
exports.deleteAccount = deleteAccount;
exports.getAccountCount = getAccountCount;
exports.getAccountInfos = getAccountInfos;
exports.getAccountInfo = getAccountInfo;
exports.existsUsername = existsUsername;
exports.existsAccountId = existsAccountId;
//
exports.updateUserAvatar = updateUserAvatar;
exports.getUserAvatar = getUserAvatar;
//
exports.checkApplicationUpdateInfo = checkApplicationUpdateInfo;
exports.saveNewApplicationInfo = saveNewApplicationInfo;
exports.updateApplicationInfo = updateApplicationInfo;
exports.deleteApplication = deleteApplication;
exports.getApplicationInfos = getApplicationInfos;
exports.getApplicationInfo = getApplicationInfo;
exports.getApplicationName = getApplicationName;
exports.existsApplicationName = existsApplicationName;
exports.existsApplicationId = existsApplicationId;

exports.getActiveConnectionCount = getActiveConnectionCount;

exports.getAllConnections = getAllConnections;
exports.getAllApplications = getAllApplications;
exports.getAllAccounts = getAllAccounts;
exports.getAllMessages = getAllMessages;

exports.queryPublicAccounts = queryPublicAccounts;
exports.followPublicAccount = followPublicAccount;
exports.unfollowPublicAccount = unfollowPublicAccount;
exports.getFollowedPublicAccounts = getFollowedPublicAccounts;

exports.clearMessages = clearMessages;
exports.cleanData = cleanData;

exports.getAccountPermissions = getAccountPermissions;
exports.getAccountType = getAccountType;

const REDIS_SERVER = config.REDIS_SERVER;
const REDIS_PORT = config.REDIS_PORT;
//
const CREATE_ACCOUNT_LOCK_FILE = config.CREATE_ACCOUNT_LOCK_FILE;
//
const RANDOM_ACCOUNTNAME_SIZE = config.RANDOM_ACCOUNTNAME_SIZE;
//
const EMAIL_ADDRESS_FORMAT = config.EMAIL_ADDRESS_FORMAT;
const PHONE_NUMBER_FORMAT = config.PHONE_NUMBER_FORMAT;

const MAX_INACTIVE_TIME = config.MAX_INACTIVE_TIME;

Date.prototype.Format = utils.DateFormat;
String.prototype.trim = utils.StringTrim;
String.prototype.format = utils.StringFormat;

var logger = config.log4js.getLogger('db');
logger.setLevel(config.LOG_LEVEL);

/**
 * 应用认证
 *
 * @param appId 应用ID
 * @param password 应用密码
 * @param handleCheckResult
 */
function checkAppId(redis, appId, password, handleCheckResult) {

    var checkResult = {};

    async.series([
        function (callback) {

            redis.sismember("application:set", appId, function (err, ismember) {
                if (err) return callback(err);
                if (!ismember) {
                    return callback('No application with id ' + appId + ' found');
                }
                callback();
            });
        },
        function (callback) {

            redis.exists("application:" + appId + ":delete_time", function (err, exists) {
                if (err) return callback(err);
                if (exists) {
                    return callback('Application with id ' + appId + ' has been deleted.');
                }
                callback();
            });
        },
        function (callback) {

            redis.get("application:" + appId + ":name", function (err, name) {
                if (err) return callback(err);
                if (!name) {
                    checkResult.internalError = true;
                    return callback('No application name with id ' + appId + ' found.');
                }
                checkResult.name = name;
                callback();
            });
        },
        function (callback) {

            redis.get("application:" + appId + ":password", function (err, pass) {
                if (err) return callback(err);
                if (!pass) {
                    checkResult.internalError = true;
                    return callback('No application password with id ' + appId + ' found');
                }
                if (password != pass) {
                    return callback('Wrong application password(md5) "' + password + '" with id ' + appId);
                }
                callback();
            });
        },
        function (callback) {

            redis.get("application:" + appId + ":client:need_login", function (err, needLogin) {
                if (err) return callback(err);
                checkResult.needLogin = (needLogin == 1);
                callback();
            });
        },
        function (callback) {

            redis.get("application:" + appId + ":client:need_password", function (err, needPassword) {
                if (err) return callback(err);
                checkResult.needPassword = (needPassword == 1);
                callback();
            });
        },
        function (callback) {

            redis.get("application:" + appId + ":account:auto_create", function (err, autoCreateAccount) {
                if (err) return callback(err);
                checkResult.autoCreateAccount = !(autoCreateAccount == 0);
                callback();
            });
        },
        function (callback) {

            redis.get("application:" + appId + ":login:need_protect", function (err, needProtect) {
                if (err) return callback(err);
                checkResult.needProtect = (needProtect == 1);
                callback();
            });
        },
        function (callback) {

            if (checkResult.needProtect) {
                redis.hgetall("application:" + appId + ":login:protect_key", function (err, protectKey) {
                    if (err) return callback(err);
                    checkResult.protectKey = protectKey;
                    callback();
                });
            } else {
                callback();
            }
        },
        function (callback) {

            redis.get("application:" + appId + ":message:need_encrypt", function (err, secureMessage) {
                if (err) return callback(err);
                checkResult.secureMessage = (secureMessage == 1);
                callback();
            });
        }
    ], function (err) {
        if (err) {
            checkResult.passed = false;
            checkResult.reason = err;
        } else {
            checkResult.passed = true;
        }
        handleCheckResult(checkResult);
    });
}

/**
 * 用户认证
 *
 * @param username 用户名(账号名称/电话或邮箱地址)
 * @param password 用户密码
 * @param autoCreateAccount 是否自动创建账号
 * @param handleCheckResult
 */
function checkUsername(redis, username, password, autoCreateAccount, handleCheckResult) {

    var checkResult = {};

    async.series([
        function (callback) {

            redis.get("user:" + username, function (err, accountId) {
                if (err) return callback(err);
                if (!accountId) {
                    if (!autoCreateAccount) {
                        return callback('No username ' + username + ' found');
                    } else {
                        createAccountByUsername(redis, username, password, function (err, accountId) {
                            if (err) return callback(err);
                            checkResult.accountId = accountId;
                            return callback();
                        });
                        return;
                    }
                }
                checkResult.accountId = accountId;
                callback();
            });
        },
        function (callback) {

            redis.sismember("account:set", checkResult.accountId, function (err, exists) {
                if (err) return callback(err);
                if (!exists) {
                    checkResult.internalError = true;
                    return callback('No account with id ' + checkResult.accountId + ' found.');
                }
                callback();
            });
        },
        function (callback) {

            redis.get("account:" + checkResult.accountId + ":disabled", function (err, disabled) {
                if (err) return callback(err);
                if (disabled == 1) {
                    return callback('Account with id ' + checkResult.accountId + ' has been disabled.');
                }
                callback();
            });
        },
        function (callback) {

            redis.get("account:" + checkResult.accountId + ":password", function (err, pass) {
                if (err) return callback(err);
                if (!pass) {
                    checkResult.internalError = true;
                    return callback('No account password with id ' + checkResult.accountId + ' found');
                }
                if (password != pass) {
                    return callback('Wrong account password(md5) "' + password + '" with id ' + checkResult.accountId);
                }
                callback();
            });
        },
        function (callback) {

            redis.get("account:" + checkResult.accountId + ":name", function (err, accountName) {
                if (err) return callback(err);
                if (!accountName) {
                    checkResult.internalError = true;
                    return callback('No account name with id ' + checkResult.accountId + ' found');
                }
                checkResult.accountName = accountName;
                callback();
            });
        },
        function (callback) {

            redis.get("account:" + checkResult.accountId + ":phone", function (err, phoneNumber) {
                if (err) return callback(err);
                checkResult.phoneNumber = phoneNumber;
                callback();
            });
        },
        function (callback) {

            redis.get("account:" + checkResult.accountId + ":email", function (err, emailAddress) {
                if (err) return callback(err);
                checkResult.emailAddress = emailAddress;
                callback();
            });
        },
        function (callback) {

            redis.get("account:" + checkResult.accountId + ":type", function (err, type) {
                if (err) return callback(err);
                checkResult.type = type;
                callback();
            });
        }
    ], function (err) {
        if (err) {
            checkResult.passed = false;
            checkResult.passed = false;
            checkResult.reason = err;
        } else {
            checkResult.passed = true;
        }
        handleCheckResult(checkResult);
    });
}

/**
 * 保存新账号信息
 * @param id 账号ID
 * @param name 账号名称
 * @param phone 电话号码(允许为空)
 * @param email 邮箱地址(允许为空)
 * @param password 登录密码
 * @param createTime 创建时间
 * @param handleResult
 */
function saveNewAccountInfo(redis, id, name, phone, email, password, createTime, handleResult) {
    logger.debug("Locking file " + CREATE_ACCOUNT_LOCK_FILE + "...");
    lockFile.lock(CREATE_ACCOUNT_LOCK_FILE, {}, function (err) {

        if (err) return handleResult(err);

        redis.set("account:" + id + ":name", name);
        if (phone) redis.set("account:" + id + ":phone", phone);
        if (email) redis.set("account:" + id + ":email", email);
        redis.set("account:" + id + ":password", password);
        redis.set("account:" + id + ":create_time", createTime);

        redis.sadd("account:set", id);

        redis.set("user:" + name, id);
        if (phone) redis.set("user:" + phone, id);
        if (email) redis.set("user:" + email, id);

        logger.debug("Unlocking file " + CREATE_ACCOUNT_LOCK_FILE + "...");
        lockFile.unlock(CREATE_ACCOUNT_LOCK_FILE, function (err) {

            handleResult(err, id);
        });
    });
}

/**
 * 保存登录信息
 * @param connId 连接ID
 * @param accountId 账号ID
 * @param appId 应用ID
 * @param msgKey 消息密钥(空代表消息不需要加密)
 * @param channelId 消息通道ID
 * @param clientAddress 客户地址
 * @param handleResult(err)
 */
function saveLoginInfo(redis, connId, accountId, appId, msgKey, channelId, clientAddress, handleResult) {

    var beginTimeStr = new Date().Format("yyyyMMddHHmmss");

    // 账号最后一次登录应用的时间
    redis.set("account:" + accountId + ":application:" + appId + ":last_logon", beginTimeStr);

    // 连接信息
    redis.hset("connection:" + connId, "application_id", appId, redis.print);
    redis.hset("connection:" + connId, "account_id", accountId, redis.print);
    if (msgKey != null) redis.hset("connection:" + connId, "key", msgKey, redis.print);
    redis.hset("connection:" + connId, "begin_time", beginTimeStr, redis.print);
    redis.hset("connection:" + connId, "channel_id", channelId, redis.print);
    redis.hset("connection:" + connId, "client_address", clientAddress, redis.print);
    //
    redis.sadd("connection:set", connId);
    redis.sadd("application:" + appId + ":connections", connId);
    redis.sadd("account:" + accountId + ":connections", connId);

    redis.smembers("account:" + accountId + ":followed", function (err, publicAccountIds) {
        if (err) return handleResult(err);
        async.forEachSeries(publicAccountIds, function (publicAccountId, callback) {
            redis.sadd("application:" + appId + ":account:" + publicAccountId + ":connections", connId);
            callback();
        }, function (err) {
            handleResult(err);
        });
    });
}

/**
 * 删除登录信息
 * @param connId 连接ID
 * @param handleResult(err)
 */
function removeLoginInfo(redis, connId, handleResult) {

    var accountId;

    async.series([
        function (callback) {
            redis.hgetall("connection:" + connId, function (err, connectionInfo) {

                if (err) return callback(err);
                if (connectionInfo) {
                    // 连接信息
                    redis.srem("connection:set", connId);
                    redis.srem("application:" + connectionInfo.application_id + ":connections", connId);
                    redis.srem("account:" + connectionInfo.account_id + ":connections", connId);
                    //
                    redis.del("connection:" + connId);
                }
                callback();
            });
        }, function (callback) {
            redis.hget("connection:" + connId, "account_id", function (err, value) {
                if (err) return callback(err);
                accountId = value;
            });
        }, function (callback) {
            redis.smembers("account:" + accountId + ":followed", function (err, publicAccountIds) {
                async.forEachSeries(publicAccountIds, function (publicAccountId, callback) {
                    redis.srem("application:" + appId + ":account:" + publicAccountId + ":connections", connId);
                    callback();
                }, function (err) {
                    callback(err);
                });
            });
        }], function (err) {
        handleResult(err);
    });
}

/**
 * 根据用户名创建新账号
 * @param username 用户名(账号名称/电话号码/邮箱地址)
 * @param passwrod 密码
 * @param handleResult
 */
function createAccountByUsername(redis, username, password, handleResult) {
    var accountId = uuid.v4().toUpperCase();
    var accountName, phoneNumber, emailAddress;

    if (username.match(EMAIL_ADDRESS_FORMAT)) {

        // Email address
        accountName = randomstring.generate(RANDOM_ACCOUNTNAME_SIZE);
        emailAddress = username;
    } else if (username.match(PHONE_NUMBER_FORMAT)) {

        // Phone number
        accountName = randomstring.generate(RANDOM_ACCOUNTNAME_SIZE);
        phoneNumber = username;
    } else {

        // Account name
        accountName = username;
    }

    var createTime = new Date().Format("yyyyMMddHHmmss");
    saveNewAccountInfo(redis, accountId, accountName, phoneNumber, emailAddress, password, createTime, handleResult);
}

/**
 * 记录消息发送时间
 * @param connId 连接ID
 * @param msgId 消息ID
 * @param sentTime 发送时间
 * @param needReceipt 是否需要确认
 */
function recordMessageSentTime(redis, connId, msgId, sentTime, needReceipt, handleResult) {
    redis.hgetall("connection:" + connId, function (err, connectionInfo) {
        if (err) return handleResult(err);
        if (!connectionInfo) return handleResult("No connection info with id " + connId);
        redis.hset("account:" + connectionInfo.account_id + ":application:" + connectionInfo.application_id + ":message:" + msgId,
            "sent_time", sentTime.Format("yyyyMMddHHmmss"), redis.print);
        if (!needReceipt) {
            redis.zadd("account:" + connectionInfo.account_id + ":application:" + connectionInfo.application_id + ":sent_messages", sentTime.getTime(), msgId);
        }

        handleResult();
    });
}

/**
 * 记录消息确认时间
 * @param connId 连接ID
 * @param msgId 消息ID
 * @param receiptTime 确认时间
 */
function recordMessageReceiptTime(redis, connId, msgId, receiptTime, handleResult) {
    redis.hgetall("connection:" + connId, function (err, connectionInfo) {
        if (err) return handleResult(err);
        if (!connectionInfo) return handleResult();
        redis.hset("account:" + connectionInfo.account_id + ":application:" + connectionInfo.application_id + ":message:" + msgId,
            "receipt_time", receiptTime.Format("yyyyMMddHHmmss"), redis.print);
        redis.zadd("account:" + connectionInfo.account_id + ":application:" + connectionInfo.application_id + ":sent_messages", receiptTime.getTime(), msgId);

        handleResult();
    });
}

/**
 * 记录最新活动
 * @param connId 连接ID
 * @param activity 活动内容
 * @param handleResult(err)
 */
function recordLatestActivity(redis, connId, activity, handleResult) {
    redis.hset("connection:" + connId, "latest_activity", activity, redis.print);
    redis.hset("connection:" + connId, "latest_activity_time", new Date().Format("yyyyMMddHHmmss"), redis.print);

    handleResult();
}

/**
 * 获取离线消息
 * @param appId 应用ID
 * @param accountId 账号ID
 * @param days 最多天数
 * @param handleResult(err, msgs)
 */
function getOfflineMessages(redis, appId, accountId, accountName, days, handleResult) {
    var now = new Date();
    var tmpKey = "accountId:" + accountId + ":application:" + appId + ":" + now.getTime() + ":messages";
    redis.zunionstore(tmpKey, 2,
        "application:" + appId + ":messages",
        "account:" + accountId + ":application:" + appId + ":messages");
    redis.smembers("account:" + accountId + ":followed", function (err, publicAccountIds) {
        if (err) return handleResult(err);
        async.forEachSeries(publicAccountIds, function (publicAccountId, callback) {
            redis.zunionstore(tmpKey, 2,
                tmpKey,
                "application:" + appId + ":account:" + publicAccountId + ":messages");
            callback();
        }, function (err) {
            if (err) return handleResult(err);
            redis.zrangebyscore(tmpKey, now.getTime() - (days * 24 * 60 * 60 * 1000), now.getTime(), function (err, msgIds) {
                if (err) return handleResult(err);
                var msgs = [];
                async.forEachSeries(msgIds, function (msgId, callback) {
                    if (err) return callback(err);
                    redis.zrank("account:" + accountId + ":application:" + appId + ":sent_messages", msgId, function (err, rank) {
                        if (err) return callback(err);
                        if (rank == null)
                            getMessageById(redis, msgId, function (err, message, needReceipt) {
                                if (err) return callback(err);
                                if (message.senderId == accountId) return callback(); // 屏蔽自己发的消息(离线消息不会发给自己)
                                message.generate_time = message.generateTime;
                                delete message.generateTime;
                                redis.get("account:" + message.senderId + ":name", function (err, senderName) {
                                    if (err) return callback(err);
                                    message.sender_name = senderName;
                                    delete message.senderId;
                                    if (!message.expiration || (now.getTime() < utils.DateParse(message.expiration).getTime())) {
                                        msgs.push({msgId: msgId, message: message, needReceipt: needReceipt});
                                    }
                                    callback();
                                });
                            });
                        else
                            callback();
                    });
                }, function (err) {
                    redis.del(tmpKey);
                    handleResult(err, msgs);
                });
            });
        });
    });
}

/**
 * 保存消息
 * @param appId 应用ID
 * @param publicAccountId 公众号ID
 * @param message 消息对象
 * @param accountIds 账号ID表(空代表广播消息)
 * @param handuleResult(err, msgId)
 */
function saveMessage(redis, appId, publicAccountId, message, accountIds, handleResult) {
    var now = new Date();
    var generateTime = message.generate_time || now.Format("yyyyMMddHHmmss");
    var msgId = uuid.v4().toUpperCase();

    redis.hmset("message:" + msgId,
        "title", message.title || "",
        "body", message.body,
        "body:type", message.type || "text",
        "url", message.url || "",
        "send_time", message.send_time || "",
        "expiration", message.expiration || "",
        "callback", message.callback || "",
        "generate_time", generateTime,
        "need_receipt", message.need_receipt ? 1 : 0,
        "sender_id", message.sender_id, function (err, result) {
            if (err) {
                return handleResult(err);
            }
        });

    if (message.attachments != null) {
        message.attachments.forEach(function (attachment) {
            var attId = uuid.v4();
            //
            redis.hset("attachment:" + attId, "title", attachment.title, redis.print);
            redis.hset("attachment:" + attId, "type", attachment.type, redis.print);
            redis.hset("attachment:" + attId, "filename", attachment.filename, redis.print);
            redis.hset("attachment:" + attId, "url", attachment.url, redis.print);
            //
            redis.sadd("attachment:set", attId);
            redis.sadd("message:" + msgId + ":attachments", attId);
        });
    }

    //
    redis.zadd("message:set", now.getTime(), msgId);
    redis.hset("message:" + msgId + ":meta", "application_id", appId, redis.print);

    if (accountIds && accountIds.length != 0) {
        // 群发/私信
        accountIds.forEach(function (accountId) {
            redis.zadd("account:" + accountId + ":application:" + appId + ":messages", now.getTime(), msgId);
        });
        //
        redis.hset("message:" + msgId + ":meta", "receiver_ids", accountIds.join(","), redis.print);
    } else {
        // 广播消息
        if (publicAccountId) {
            // 公众号消息
            logger.debug("public account id: " + publicAccountId);
            redis.zadd("application:" + appId + ":account:" + publicAccountId + ":messages", now.getTime(), msgId);
        } else {
            // 应用消息
            redis.zadd("application:" + appId + ":messages", now.getTime(), msgId);
        }
    }

    handleResult(null, msgId);
}

/**
 * 根据用户名获取账号ID
 * @param username 用户名(账号名称/电话号码/邮箱地址)
 * @param handleResult
 */
function getAccountIdByName(redis, username, handleResult) {
    redis.get("user:" + username, function (err, username) {
        handleResult(err, username);
    });
}

/**
 * 获取属于指定应用(及账号)的活动连接
 * @param appId 应用ID
 * @param publicAccountId 公众号ID
 * @param accountId 账号ID
 * @param handleResult
 */
function getActiveConnections(redis, appId, publicAccountId, accountId, handleResult) {
    if (accountId == null) {
        if (publicAccountId) {
            // 公众号
            redis.smembers("application:" + appId + ":account:" + publicAccountId + ":connections", handleConnIds);
        } else {
            // 全应用
            redis.smembers("application:" + appId + ":connections", handleConnIds);
        }
    } else {
        redis.sinter("application:" + appId + ":connections", "account:" + accountId + ":connections", handleConnIds);
    }

    function handleConnIds(err, connIds) {
        if (err) return handleResult(err);
        var connectionInfos = [];
        async.forEachSeries(connIds, function (connId, callback) {
            redis.hgetall("connection:" + connId, function (err, connectionInfo) {
                if (err) return callback(err);
                if (!connectionInfo) /*return callback("Connection " + connId + " not exists")*/;
                else connectionInfos.push({channelId: connectionInfo.channel_id, connId: connId, msgKey: connectionInfo.key});
                callback();
            });
        }, function (err) {
            handleResult(err, connectionInfos);
        });
    }
}

/**
 * 根据ID获取应用
 * @param appId 应用ID
 * @param handleResult
 */
function getApplicationById(redis, appId, handleResult) {
    var application = {};
    async.series([
        function (callback) {

            redis.get("application:" + appId + ":name", function (err, name) {
                if (err) return callback(err);
                application.name = name;
                callback();
            });
        },
        function (callback) {

            redis.get("application:" + appId + ":message:need_encrypt", function (err, needEncrypt) {
                if (err) return callback(err);
                application.secureMessage = (needEncrypt == 1);
                callback();
            });
        }
    ], function (err) {
        handleResult(err, application);
    });
}

/**
 * 根据ID获取消息
 * @param msgId 消息ID
 * @param handleResult
 */
function getMessageById(redis, msgId, handleResult) {
    var message = {};
    var needReceipt;

    async.series([
        function (callback) {
            redis.hmget("message:" + msgId, "title", "body", "body:type", "url", "callback",
                "generate_time", "send_time", "expiration", "need_receipt", "sender_id", function (err, arr) {
                    if (err) return callback(err);
                    if (arr[0]) message.title = arr[0];
                    message.body = arr[1];
                    if (arr[2]) message.type = arr[2] ? arr[2] : "text";
                    if (arr[3]) message.url = arr[3];
                    if (arr[4]) message.callback = arr[4];
                    if (arr[5]) message.generateTime = arr[5];
                    if (arr[6]) message.sendTime = arr[6];
                    if (arr[7]) message.expiration = arr[7];
                    needReceipt = (arr[8] == 1);
                    if (arr[9]) message.senderId = arr[9];
                    callback();
                });
        },
        function (callback) {
            message.attachments = [];
            redis.smembers("message:" + msgId + ":attachments", function (err, attachments) {

                if (err) return callback(err);
                if (attachments) {
                    async.forEachSeries(attachments, function (attachment, callback) {
                        redis.hgetall("attachment:" + attachment, function (err, hash) {
                            if (err) return callback(err);
                            message.attachments.push(hash);
                            callback();
                        });
                    }, function (err) {
                        callback(err);
                    });
                } else {
                    callback();
                }
            });
        }
    ], function (err) {
        handleResult(err, message, needReceipt);
    });
}

/**
 * 根据ID获取消息所有内容
 * @param msgId 消息ID
 * @param handleResult
 */
function getMessageAllById(redis, msgId, handleResult) {
    var message = {messageId: msgId};

    async.series([
        function (callback) {
            redis.hmget("message:" + msgId, "title", "body", "body:type", "url", "callback",
                "generate_time", "send_time", "expiration", "need_receipt", "sender_id", function (err, arr) {
                    if (err) return callback(err);
                    if (arr[0]) message.title = arr[0];
                    message.body = arr[1];
                    if (arr[2]) message.type = arr[2] ? arr[2] : "text";
                    if (arr[3]) message.url = arr[3];
                    if (arr[4]) message.callback = arr[4];
                    if (arr[5]) message.generateTime = arr[5];
                    if (arr[6]) message.sendTime = arr[6];
                    if (arr[7]) message.expiration = arr[7];
                    needReceipt = (arr[8] == 1);
                    ;
                    if (arr[9]) message.senderId = arr[9];
                    callback();
                });
        },
        function (callback) {
            message.attachments = [];
            redis.smembers("message:" + msgId + ":attachments", function (err, attachments) {

                if (err) return callback(err);
                if (attachments) {
                    async.forEachSeries(attachments, function (attachment, callback) {
                        redis.hgetall("attachment:" + attachment, function (err, hash) {
                            if (err) return callback(err);
                            message.attachments.push(hash);
                            callback();
                        });
                    }, function (err) {
                        callback(err);
                    });
                } else {
                    callback();
                }
            });
        }
    ], function (err) {
        handleResult(err, message);
    });
}

/**
 * 检查新账号信息
 * @param name 账号名称
 * @param phone 电话号码(可选)
 * @param email 邮箱地址(可选)
 * @param handleResult
 */
function checkNewAccountInfo(redis, name, phone, email, handleResult) {
    var reason;
    async.series([
        function (callback) {
            redis.exists("user:" + name, function (err, exists) {
                if (err) callback(err);
                else if (exists) {
                    var ss = "Account name " + name + " exists";
                    if (reason) reason += ";" + ss;
                    else reason = ss;
                }
                callback();
            });
        },
        function (callback) {
            if (!phone) return callback();
            redis.exists("user:" + phone, function (err, exists) {
                if (err) callback(err);
                else if (exists) {
                    var ss = "Phone number " + phone + " exists";
                    if (reason) reason += ";" + ss;
                    else reason = ss;
                }
                else if (!phone.match(PHONE_NUMBER_FORMAT)) {
                    var ss = "Invalid phone number: " + phone;
                    if (reason) reason += ";" + ss;
                    else reason = ss;
                }
                callback();
            });
        },
        function (callback) {
            if (!email) return callback();
            redis.exists("user:" + email, function (err, exists) {
                if (err) callback(err);
                else if (exists) {
                    var ss = "Email address " + email + " exists";
                    if (reason) reason += ";" + ss;
                    else reason = ss;
                }
                else if (!email.match(EMAIL_ADDRESS_FORMAT)) {
                    var ss = "Invalid email address: " + email;
                    if (reason) reason += ";" + ss;
                    else reason = ss;
                }
                callback();
            });
        }
    ], function (err) {
        handleResult(err, reason);
    });
}

/**
 * 修改账号信息
 * @param id 账号ID
 * @param newName 账号新名称(允许为空)
 * @param newPhone 新电话号码(允许为空)
 * @param newEmail 新邮箱地址(允许为空)
 * @param newPassword 新登录密码
 * @param updateTime 修改时间
 * @param handleResult
 */
function updateAccountInfo(redis, id, newName, newPhone, newEmail, newPassword, updateTime, handleResult) {
    if (newName) redis.set("account:" + id + ":name", newName);
    if (newPhone) redis.set("account:" + id + ":phone", newPhone);
    if (newEmail) redis.set("account:" + id + ":email", newEmail);
    if (newPassword) redis.set("account:" + id + ":password", newPassword);
    redis.set("account:" + id + ":update_time", updateTime);

    async.series([
        function (callback) {
            if (newName) {
                redis.get("account:" + id + ":name", function (err, oldName) {
                    if (err) return callback(err);
                    redis.del("user:" + oldName);
                    redis.set("user:" + newName, id);
                    callback();
                });
            } else {
                callback();
            }
        },
        function (callback) {
            if (newPhone) {
                redis.get("account:" + id + ":phone", function (err, oldPhone) {
                    if (err) return callback(err);
                    if (oldPhone) redis.del("user:" + oldPhone);
                    redis.set("user:" + newPhone, id);
                    callback();
                });
            } else {
                callback();
            }
        },
        function (callback) {
            if (newEmail) {
                redis.get("account:" + id + ":email", function (err, oldEmail) {
                    if (err) return callback(err);
                    if (oldEmail) redis.del("user:" + oldEmail);
                    redis.set("user:" + newEmail, id);
                    callback();
                });
            } else {
                callback();
            }
        }
    ], function (err) {
        handleResult(err);
    });
}

/**
 * 修改账号状态
 * @param id 账号ID
 * @param enabled是否启用
 * @param updateTime 修改时间
 * @param handleResult
 */
function updateAccountStatus(redis, id, enabled, updateTime, handleResult) {
    redis.set("account:" + id + ":disabled", (enabled ? 0 : 1));
    redis.set("account:" + id + ":update_time", updateTime);

    handleResult();
}

/**
 * 删除指定账号
 * @param id 账号ID
 * @param handleResult
 */
function deleteAccount(redis, id, handleResult) {
    async.series([
        function (callback) {
            async.series([
                function (callback) {
                    redis.get("account:" + id + ":name", function (err, name) {
                        if (err) return callback(err);
                        redis.del("user:" + name);
                        callback();
                    });
                },
                function (callback) {
                    redis.get("account:" + id + ":phone", function (err, phone) {
                        if (err) return callback(err);
                        if (phone) redis.del("user:" + phone);
                        callback();
                    });
                },
                function (callback) {
                    redis.get("account:" + id + ":email", function (err, email) {
                        if (err) return callback(err);
                        if (email) redis.del("user:" + email);
                        callback();
                    });
                }
            ], function (err) {
                if (err) {
                    redis.discard(_redis.print);
                    return callback(err);
                }
                callback();
            });
        },
        function (callback) {
            redis.smembers("application:set", function (err, appIds) {
                async.forEachSeries(appIds, function (appId, callback) {
                    redis.del("account:" + id + ":application" + appId + ":last_logon");
                    callback();
                }, callback);
            });
        },
        function (callback) {
            redis.del("account:" + id + ":name");
            redis.del("account:" + id + ":phone");
            redis.del("account:" + id + ":email");
            redis.del("account:" + id + ":password");
            redis.del("account:" + id + ":diabled");
            redis.del("account:" + id + ":create_time");
            redis.del("account:" + id + ":update_time");
            //
            redis.srem("account:set", id);
            callback();
        }
    ], function (err) {
        handleResult(err);
    });
}

/**
 * 获取账号数量
 * @param handleResult
 */
function getAccountCount(redis, handleResult) {
    redis.scard("account:set", function (err, count) {
        handleResult(err, count);
    });
}

/**
 * 获取一组账号信息
 * @param start 起始记录号(默认为0)
 * @param records 最多记录数(默认为所有)
 * @param handleResult
 */
function getAccountInfos(redis, start, records, handleResult) {
    redis.smembers("account:set", function (err, accounts) {
        if (err) return handleResult(err);
        if (!start) start = 0;
        var filteredAccounts = accounts.slice(start, records);
        var accountInfos = [];
        async.forEachSeries(filteredAccounts, function (account, callback) {
            getAccountInfo(redis, account, function (err, accountInfo) {
                if (err) return callback(err);
                accountInfos.push(accountInfo);
                callback();
            });
        }, function (err) {
            handleResult(err, {count: accountInfos.length, list: accountInfos});
        });
    });
}

/**
 * 获取单个账号信息
 * @param accountId 账号ID
 * @param handleResult
 */
function getAccountInfo(redis, accountId, handleResult) {
    var accountInfo = {};
    accountInfo.id = accountId;
    async.series([
        function (callback) {
            redis.get("account:" + accountId + ":name", function (err, name) {
                if (err) return callback(err);
                accountInfo.name = name;
                callback();
            })
        },
        function (callback) {
            redis.get("account:" + accountId + ":phone", function (err, phone) {
                if (err) return callback(err);
                accountInfo.phone = phone;
                callback();
            })
        },
        function (callback) {
            redis.get("account:" + accountId + ":email", function (err, email) {
                if (err) return callback(err);
                accountInfo.email = email;
                callback();
            })
        },
        function (callback) {
            redis.get("account:" + accountId + ":disabled", function (err, disabled) {
                if (err) return callback(err);
                accountInfo.disabled = (disabled == 1);
                callback();
            })
        },
        function (callback) {
            redis.get("account:" + accountId + ":type", function (err, type) {
                if (err) return callback(err);
                accountInfo.isPublic = (type || false);
                callback();
            })
        }
    ], function (err) {
        handleResult(err, accountInfo);
    });
}

/**
 * 检测指定的用户名是否已存在
 * @param username 用户名(账号名称/电话号码/邮箱地址)
 * @param handleResult
 */
function existsUsername(redis, username, handleResult) {
    redis.exists("user:" + username, function (err, exists) {
        handleResult(err, exists);
    });
}

/**
 * 检测指定的账号ID是否已存在
 * @param accountId 账号ID
 * @param handleResult
 */
function existsAccountId(redis, accountId, handleResult) {
    redis.sismember("account:set", accountId, function (err, exists) {
        handleResult(err, exists);
    });
}

/**
 * 修改用户头像
 */
function updateUserAvatar(redis, accountId, avatar, callback) {
    redis.set("account:" + accountId + ":avatar", avatar, callback);
}

/**
 * 获取用户头像
 */
function getUserAvatar(redis, accountId, callback) {
    redis.get("account:" + accountId + ":avatar", callback);
}


/**
 * 检查应用更新信息
 * @param id 应用ID
 * @param name 应用名称
 * @param newName 应用新名称
 * @param handleResult
 */
function checkApplicationUpdateInfo(redis, id, name, newName, handleResult) {
    redis.get("application:" + id + ":name", function (err, oldName) {
        if (err) return handleResult(err);
        if (oldName != name) return handleResult(null, "Application name not match");
        existsApplicationName(newName, function (err, exists) {
            handleResult(err, exists ? "Application name " + newName + " used" : null);
        });
    });
}

/**
 * 保存新应用信息
 * @param id 应用ID
 * @param name 应用名称
 * @param name 应用密码(MD5)
 * @param needLogin 用户需要登录
 * @param needLoginPassword 用户登录需要密码
 * @param autoCreateAccount 自动创新新账号
 * @param protectLogin 安全登录
 * @param protectKey 安全登录密钥对
 * @param encryptMessage 安全消息
 * @param createTime 创建时间
 * @param handleResult
 */
function saveNewApplicationInfo(redis, id, name, password, needLogin, needLoginPassword, autoCreateAccount, protectLogin, protectKey, encryptMessage, createTime, handleResult) {
    redis.set("application:" + id + ":name", name);
    redis.set("application:" + id + ":password", password);
    if (needLogin) redis.set("application:" + id + ":client:need_login", 1);
    if (needLoginPassword) redis.set("application:" + id + ":client:need_password", 1);
    if (!autoCreateAccount) redis.set("application:" + id + ":account:auto_create", 0);
    if (protectLogin) {
        redis.set("application:" + id + ":login:need_protect", 1);
        redis.hset("application:" + id + ":login:protect_key", "public", protectKey.public, redis.print);
        redis.hset("application:" + id + ":login:protect_key", "private", protectKey.private, redis.print);
    }
    if (encryptMessage) redis.set("application:" + id + ":message:need_encrypt", 1);

    redis.set("application:" + id + ":create_time", createTime);

    redis.sadd("application:set", id);

    handleResult();
}

/**
 * 修改应用信息
 * @param id 应用ID
 * @param name 应用名称(用于校验)
 * @param newName 应用新名称(允许为空)
 * @param needLogin 用户需要登录
 * @param needLoginPassword 用户登录需要密码
 * @param autoCreateAccount 自动创新新账号
 * @param protectLogin 安全登录
 * @param encryptMessage 安全消息
 * @param updateTime 修改时间
 * @param callback
 */
function updateApplicationInfo(redis, id, name, newName, needLogin, needLoginPassword, autoCreateAccount, protectLogin, encryptMessage, updateTime, callback) {
    redis.get("application:" + id + ":name", function (err, oldName) {
        if (err) return callback(err);
        if (oldName != name) return callback("Incorrect application name");

        if (newName) redis.set("application:" + id + ":name", newName);
        if (needLogin != null) redis.set("application:" + id + ":client:need_login", needLogin ? 1 : 0);
        if (needLoginPassword != null) redis.set("application:" + id + ":client:need_password", needLoginPassword ? 1 : 0);
        if (autoCreateAccount != null) redis.set("application:" + id + ":account:auto_create", autoCreateAccount ? 1 : 0);
        if (protectLogin != null) redis.set("application:" + id + ":login:need_protect", protectLogin ? 1 : 0);
        if (encryptMessage != null) redis.set("application:" + id + ":message:need_encrypt", encryptMessage ? 1 : 0);
        redis.set("application:" + id + ":update_time", updateTime);

        callback();
    });
}

/**
 * 删除指定应用(下线)
 * @param id 应用ID
 * @param handleResult
 */
function deleteApplication(redis, id, handleResult) {
    redis.set("application:" + id + ":delete_time", new Date().Format("yyyyMMddHHmmss"));
    handleResult();
}

/**
 * 获取所有应用基本信息
 * @param handleResult
 */
function getApplicationInfos(redis, handleResult) {
    logger.trace("获取所有应用基本信息...");
    redis.smembers("application:set", function (err, appIds) {
        if (err) return handleResult(err);
        var applicationInfos = [];
        async.forEachSeries(appIds, function (appId, callback) {
            var applicationInfo = {id: appId};
            var deleted = false;
            async.series([
                function (callback) {
                    redis.get("application:" + appId + ":name", function (err, name) {
                        if (err) return callback(err);
                        applicationInfo.name = name;
                        callback();
                    })
                },
                function (callback) {
                    redis.get("application:" + appId + ":delete_time", function (err, deleteTime) {
                        if (err) return callback(err);
                        deleted = (deleteTime ? true : false);
                        callback();
                    });
                }
            ], function (err) {
                if (err) return callback(err);
                if (!deleted) {
                    applicationInfos.push(applicationInfo);
                }
                callback();
            });
        }, function (err) {
            handleResult(err, {count: applicationInfos.length, list: applicationInfos});
        });
    });
}

/**
 * 获取单个应用详细信息
 * @param appId 应用ID
 * @param handleResult
 */
function getApplicationInfo(redis, appId, handleResult) {
    var applicationInfo = {};
    async.series([
        function (callback) {
            redis.get("application:" + appId + ":name", function (err, name) {
                if (err) return callback(err);
                applicationInfo.name = name;
                callback();
            });
        },
        function (callback) {
            redis.get("application:" + appId + ":password", function (err, password) {
                if (err) return callback(err);
                applicationInfo.password = password;
                callback();
            });
        },
        function (callback) {
            redis.get("application:" + appId + ":client:need_login", function (err, needLogin) {
                if (err) return callback(err);
                applicationInfo.needLogin = (needLogin == 1);
                callback();
            });
        },
        function (callback) {
            redis.get("application:" + appId + ":client:need_password", function (err, needLoginPassword) {
                if (err) return callback(err);
                applicationInfo.needLoginPassword = (needLoginPassword == 1);
                callback();
            });
        },
        function (callback) {
            redis.get("application:" + appId + ":account:auto_create", function (err, autoCreateAccount) {
                if (err) return callback(err);
                applicationInfo.autoCreateAccount = !(autoCreateAccount == 0);
                callback();
            });
        },
        function (callback) {
            redis.get("application:" + appId + ":login:need_protect", function (err, protectLogin) {
                if (err) return callback(err);
                applicationInfo.protectLogin = (protectLogin == 1);
                callback();
            });
        },
        function (callback) {
            redis.get("application:" + appId + ":message:need_encrypt", function (err, encryptMessage) {
                if (err) return callback(err);
                applicationInfo.encryptMessage = (encryptMessage == 1);
                callback();
            });
        },
        function (callback) {
            redis.hgetall("application:" + appId + ":login:protect_key", function (err, protectKey) {
                if (err) return callback(err);
                if (protectKey) {
                    applicationInfo.protectKey = protectKey;
                } else {
                    applicationInfo.protectKey = {public: "", private: ""};
                }
                callback();
            });
        }
    ], function (err) {
        handleResult(err, applicationInfo);
    });
}

/**
 * 根据ID获取应用名称
 * @param appId 应用ID
 * @param handleResult
 */
function getApplicationName(redis, appId, handleResult) {
    redis.get("application:" + appId + ":name", function (err, name) {
        handleResult(err, name);
    });
}

/**
 * 检测指定的应用名称是否已存在
 * @param name 应用名称
 * @param handleResult
 */
function existsApplicationName(redis, name, handleResult) {
    redis.smembers("application:set", function (err, appIds) {
        if (err) {
            return handleResult(err);
        }

        async.forEachSeries(appIds, function (appId, callback) {
            redis.get("application:" + appId + ":name", function (err, appName) {
                if (err) return callback(err);
                if (appName == name) return callback("exists");
                callback();
            })
        }, function (err) {
            if (err) {
                if (err == "exists") handleResult(null, true);
                else handleResult(err);
            } else {
                handleResult(err, false);
            }
        });
    });
}

/**
 * 检测指定的应用ID是否已存在
 * @param appId 应用ID
 * @param handleResult
 */
function existsApplicationId(redis, appId, handleResult) {
    redis.sismember("application:set", appId, function (err, exists) {
        handleResult(err, exists);
    });
}

/**
 * 获取活动连接数
 * @param handleResult
 */
function getActiveConnectionCount(redis, handleResult) {
    redis.scard("connection:set", function (err, count) {
        handleResult(err, count);
    });
}

/**
 * 获取所有连接信息
 * @param handleResult
 */
function getAllConnections(redis, handleResult) {
    logger.trace("获取所有连接信息...");
    redis.smembers("connection:set", function (err, connIds) {
        if (err) return handleResult(err);
        var now = new Date();
        var connections = [];
        async.forEachSeries(connIds, function (connId, callback) {
            redis.hgetall("connection:" + connId, function (err, connectionInfo) {
                if (err) return callback(err);
                if (!connectionInfo) return callback(/*"Connection " + connId + " not exists"*/);
                var applicationName, accountInfo;
                async.series([
                    function (callback) {
                        redis.get("application:" + connectionInfo.application_id + ":name", function (err, name) {
                            if (err) return callback(err);
                            applicationName = name;
                            callback();
                        });
                    },
                    function (callback) {
                        redis.get("account:" + connectionInfo.account_id + ":phone", function (err, name) {
                            if (err) return callback(err);
                            accountInfo = (name ? name : "");
                            callback();
                        });
                    },
                    function (callback) {
                        if (accountInfo) return callback();
                        redis.get("account:" + connectionInfo.account_id + ":email", function (err, name) {
                            if (err) return callback(err);
                            accountInfo = (name ? name : "");
                            callback();
                        });
                    },
                    function (callback) {
                        if (accountInfo) return callback();
                        redis.get("account:" + connectionInfo.account_id + ":name", function (err, name) {
                            if (err) return callback(err);
                            accountInfo = (name ? name : "");
                            callback();
                        });
                    }
                ], function (err) {
                    if (err) return callback(err);
                    if (typeof connectionInfo.latest_activity_time == "undefined") return callback();
                    var inactiveTime = (now.getTime() - utils.DateParse(connectionInfo.latest_activity_time).getTime());
                    if (inactiveTime > MAX_INACTIVE_TIME * 2) {
                        removeLoginInfo(redis, connId, function (err) {
                            if (err) logger.warn(err);
                        });
                    } else {
                        var arr = utils.timeDiff(now, utils.DateParse(connectionInfo.begin_time));
                        var duration = arr[1] + ":" + arr[2] + ":" + arr[3];
                        if (arr[0] != 0) duration = arr[0] + "天 " + duration;
                        connections.push({
                            connId: connId,
                            clientAddress: connectionInfo.client_address,
                            accountInfo: accountInfo,
                            applicationName: applicationName,
                            beginTime: connectionInfo.begin_time,
                            duration: duration,
                            msgChannel: connectionInfo.channel_id,
                            latestActivity: connectionInfo.latest_activity ? "[" + connectionInfo.latest_activity_time + "]" +
                                connectionInfo.latest_activity : ""});
                    }
                    callback();
                });
            });
        }, function (err) {
            handleResult(err, connections);
        });
    });
}

/**
 * 获取所有应用信息
 * @param handleResult
 */
function getAllApplications(redis, handleResult) {
    logger.trace("获取所有应用信息...");
    redis.smembers("application:set", function (err, appIds) {
        if (err) return handleResult(err);
        var applications = [];
        async.forEachSeries(appIds, function (appId, callback) {
            var application = {};
            application.appId = appId;
            async.series([
                function (callback) {
                    redis.get("application:" + appId + ":name", function (err, name) {
                        if (err) return callback(err);
                        application.name = name;
                        callback();
                    });
                },
                function (callback) {
                    redis.get("application:" + appId + ":password", function (err, password) {
                        if (err) return callback(err);
                        application.appPassword = password;
                        callback();
                    });
                },
                function (callback) {
                    redis.get("application:" + appId + ":client:need_login", function (err, needLogin) {
                        if (err) return callback(err);
                        application.needLogin = (needLogin == 1);
                        callback();
                    });
                },
                function (callback) {
                    redis.get("application:" + appId + ":client:need_password", function (err, needLoginPassword) {
                        if (err) return callback(err);
                        application.needLoginPassword = (needLoginPassword == 1);
                        callback();
                    });
                },
                function (callback) {
                    redis.get("application:" + appId + ":account:auto_create", function (err, autoCreateAccount) {
                        if (err) return callback(err);
                        application.autoCreateAccount = !(autoCreateAccount == 0);
                        callback();
                    });
                },
                function (callback) {
                    redis.get("application:" + appId + ":login:need_protect", function (err, protectLogin) {
                        if (err) return callback(err);
                        application.protectLogin = (protectLogin == 1);
                        callback();
                    });
                },
                function (callback) {
                    redis.hgetall("application:" + appId + ":login:protect_key", function (err, protectKey) {
                        if (err) return callback(err);
                        application.protectKey = (protectKey ? protectKey : {public: "", private: ""});
                        callback();
                    });
                },
                function (callback) {
                    redis.get("application:" + appId + ":message:need_encrypt", function (err, secureMessage) {
                        if (err) return callback(err);
                        application.secureMessage = (secureMessage == 1);
                        callback();
                    });
                },
                function (callback) {
                    redis.get("application:" + appId + ":create_time", function (err, createTime) {
                        if (err) return callback(err);
                        application.createTime = createTime;
                        callback();
                    });
                },
                function (callback) {
                    redis.get("application:" + appId + ":update_time", function (err, updateTime) {
                        if (err) return callback(err);
                        application.updateTime = (updateTime ? updateTime : "");
                        callback();
                    });
                },
                function (callback) {
                    redis.get("application:" + appId + ":delete_time", function (err, deleteTime) {
                        if (err) return callback(err);
                        application.deleteTime = (deleteTime ? deleteTime : "");
                        callback();
                    });
                }
            ], function (err) {
                if (err) return callback(err);
                if (!application.deleteTime) {
                    applications.push(application);
                }
                callback();
            });
        }, function (err) {
            handleResult(err, applications);
        });
    });
}

/**
 * 获取所有账号信息
 * @param handleResult
 */
function getAllAccounts(redis, handleResult) {
    logger.trace("获取所有账号信息...");
    redis.smembers("account:set", function (err, accountIds) {
        if (err) return handleResult(err);
        var accounts = [];
        async.forEach(accountIds, function (accountId, callback) {
            var account = {};
            account.accountId = accountId;
            async.series([
                function (callback) {
                    redis.get("account:" + accountId + ":name", function (err, name) {
                        if (err) return callback(err);
                        account.name = name;
                        callback();
                    });
                },
                function (callback) {
                    redis.get("account:" + accountId + ":phone", function (err, phoneNumber) {
                        if (err) return callback(err);
                        account.phoneNumber = (phoneNumber ? phoneNumber : "");
                        callback();
                    });
                },
                function (callback) {
                    redis.get("account:" + accountId + ":email", function (err, emailAddress) {
                        if (err) return callback(err);
                        account.emailAddress = (emailAddress ? emailAddress : "");
                        callback();
                    });
                },
                function (callback) {
                    redis.get("account:" + accountId + ":password", function (err, password) {
                        if (err) return callback(err);
                        account.password = (password ? password : "");
                        callback();
                    });
                },
                function (callback) {
                    redis.get("account:" + accountId + ":disabled", function (err, disabled) {
                        if (err) return callback(err);
                        account.disabled = (disabled == 1);
                        callback();
                    });
                },
                function (callback) {
                    redis.get("account:" + accountId + ":create_time", function (err, createTime) {
                        if (err) return callback(err);
                        account.createTime = createTime;
                        callback();
                    });
                },
                function (callback) {
                    redis.get("account:" + accountId + ":update_time", function (err, updateTime) {
                        if (err) return callback(err);
                        account.updateTime = (updateTime ? updateTime : "");
                        callback();
                    });
                }
            ], function (err) {
                if (err) return callback(err);
                accounts.push(account);
                callback();
            });
        }, function (err) {
            handleResult(err, accounts);
        });
    });
}

/**
 * 获取所有消息
 * @param handleResult
 */
function getAllMessages(redis, handleResult) {
    logger.trace("获取所有消息...");
    redis.zrange("message:set", 0, -1, function (err, messageIds) {
        if (err) return handleResult(err);
        logger.trace("总共获取到 " + messageIds.length + " 条消息");
        var messages = [];
        async.forEachSeries(messageIds, function (messageId, callback) {
            logger.trace("获取 ID 为 " + messageId + " 的消息内容...");
            getMessageAllById(redis, messageId, function (err, message) {
                if (err) return callback(err);
                logger.trace("已获取到 ID 为 " + messageId + " 的消息内容");
                var applicationId, senderId, senderType = 0, receiverIds;
                async.series([
                    function (callback) {
                        logger.trace("获取 ID 为 " + messageId + " 消息的应用ID和接收者ID...");
                        redis.hgetall("message:" + messageId + ":meta", function (err, meta) {
                            if (err) return callback(err);
                            logger.trace("已获取到 ID 为 " + messageId + " 的消息的应用ID和接收者ID");
                            applicationId = meta.application_id;
                            receiverIds = meta.receiver_ids;
                            callback();
                        });
                    },
                    function (callback) {
                        logger.trace("获取 ID 为 " + applicationId + " 应用的名称...");
                        redis.get("application:" + applicationId + ":name", function (err, applicationName) {
                            if (err) return callback(err);
                            logger.trace("已获取到 ID 为 " + applicationId + " 应用的名称");
                            message.applicationName = applicationName;
                            callback();
                        });
                    },
                    function (callback) {
                        if (!message.senderId) return callback();
                        logger.trace("获取 ID 为 " + message.senderId + " 账号的名称...");
                        redis.get("account:" + message.senderId + ":name", function (err, sender) {
                            if (err) return callback(err);
                            logger.trace("已获取到 ID 为 " + message.senderId + " 账号的名称: " + sender);
                            message.sender = sender;
                            senderId = message.senderId;
                            delete message.senderId;
                            callback();
                        });
                    },
                    function (callback) {
                        if (!senderId) return callback();
                        logger.trace("获取 ID 为 " + senderId + " 账号的类型...");
                        redis.get("account:" + senderId + ":type", function (err, type) {
                            if (err) return callback(err);
                            logger.trace("已获取到 ID 为 " + message.senderId + " 账号的类型: " + type);
                            senderType = (type ? parseInt(type, 10) : 0);
                            callback();
                        });
                    },
                    function (callback) {
                        if (receiverIds == null || receiverIds.length == 0) {
                            message.receivers = (senderType == 0 ? "(全部)" : "(关注者)");
                            return callback();
                        }
                        var receivers = [];
                        async.forEachSeries(receiverIds.split(","), function (receiverId, callback) {
                            logger.trace("获取 ID 为 " + receiverId + " 账号的名称...");
                            redis.get("account:" + receiverId + ":name", function (err, receiver) {
                                if (err) return callback(err);
                                logger.trace("已获取到 ID 为 " + receiverId + " 账号的名称");
                                receivers.push(receiver);
                                callback();
                            });
                        }, function (err) {
                            if (err) return callback(err);
                            message.receivers = receivers.join(",");
                            delete message.receiver_ids;
                            callback();
                        });
                    }
                ], function (err) {
                    if (err) return callback(err);
                    message.attachmentCount = (message.attachments ? message.attachments.length : 0);
                    messages.push(message);
                    callback();
                });
            });
        }, function (err) {
            handleResult(err, messages);
        });
    });
}

/**
 * 查询公众号
 */
function queryPublicAccounts(redis, publicAccount, callback) {
    logger.debug("Query public accounts for \"" + (publicAccount || "") + "\"...");
    var publicAccountIds;
    var publicAccounts = [];
    async.series(
        [
            function (callback) {
                redis.smembers("account:public:set", function (err, accountIds) {
                    if (err) return callback(err);
                    publicAccountIds = accountIds;
                    logger.debug("Total " + accountIds.length + " found.");
                    callback();
                });
            },
            function (callback) {
                async.forEachSeries(publicAccountIds, function (publicAccountId, callback) {
                    logger.debug("Query account " + publicAccountId + "...");
                    var publicAccount = {};
                    var accountDisabled;
                    async.series(
                        [
                            function (callback) {
                                redis.get("account:" + publicAccountId + ":name", function (err, name) {
                                    if (err) return callback(err);
                                    publicAccount.name = name;
                                    logger.debug("Account name: " + name);
                                    callback();
                                });
                            },
                            function (callback) {
                                redis.get("account:" + publicAccountId + ":disabled", function (err, disabled) {
                                    if (err) return callback(err);
                                    accountDisabled = (disabled == 1);
                                    logger.debug("Account disabled: " + accountDisabled);
                                    callback();
                                });
                            },
                            function (callback) {
                                redis.get("account:" + publicAccountId + ":avatar", function (err, avatar) {
                                    if (err) return callback(err);
                                    publicAccount.avatar = (avatar || "");
                                    callback();
                                });
                            },
                            function (callback) {
                                redis.get("account:" + publicAccountId + ":desc", function (err, desc) {
                                    if (err) return callback(err);
                                    publicAccount.desc = (desc || "");
                                    callback();
                                });
                            },
                            function (callback) {
                                redis.get("account:" + publicAccountId + ":type", function (err, type) {
                                    if (err) return callback(err);
                                    var value = parseInt(type, 10);
                                    publicAccount.type = isNaN(value) ? 0 : value;
                                    logger.debug("Account type: " + publicAccount.type);
                                    callback();
                                });
                            }
                        ], function (err) {
                            if (err) return callback(err);
                            if (!accountDisabled && publicAccount.type != 0/* && publicAccount.name.indexOf(publicAccount||"")!=-1*/) {
                                publicAccounts.push(publicAccount);
                                logger.debug("Public account " + publicAccount.name + " available.");
                            } else {
                                logger.debug("Public account " + publicAccount.name + " ignored!");
                            }
                            callback();
                        });

                }, function (err) {
                    callback(err);
                });
            }
        ], function (err) {
            callback(err, publicAccounts);
        }
    );
}

/**
 * 关注公众号
 */
function followPublicAccount(redis, accountId, publicAccount, callback) {
    var publicAccountId;
    async.series(
        [
            function (callback) {
                redis.get("user:" + publicAccount, function (err, accountId) {
                    if (err) return callback(err);
                    if (!accountId) return callback("No user with account name " + publicAccount);
                    publicAccountId = accountId;
                    callback();
                });
            },
            function (callback) {
                redis.sadd("account:" + publicAccountId + ":followers", accountId, callback);
            },
            function (callback) {
                redis.sadd("account:" + accountId + ":followed", publicAccountId, callback);
            }
        ], function (err) {
            callback(err);
        }
    );
}

/**
 * 取消关注公众号
 */
function unfollowPublicAccount(redis, accountId, publicAccount, callback) {
    var publicAccountId;
    async.series(
        [
            function (callback) {
                redis.get("user:" + publicAccount, function (err, accountId) {
                    if (err) return callback(err);
                    if (!accountId) return callback("No user with account name " + publicAccount);
                    publicAccountId = accountId;
                    callback();
                });
            },
            function (callback) {
                redis.srem("account:" + publicAccountId + ":followers", accountId, callback);
            },
            function (callback) {
                redis.srem("account:" + accountId + ":followed", publicAccountId, callback);
            }
        ], function (err) {
            callback(err);
        }
    );
}

/**
 * 获取已关注的公众号
 */
function getFollowedPublicAccounts(redis, accountId, callback) {
    var publicAccountIds;
    var publicAccounts = [];
    async.series(
        [
            function (callback) {
                redis.smembers("account:" + accountId + ":followed", function (err, accountIds) {
                    if (err) return callback(err);
                    publicAccountIds = accountIds;
                    callback();
                });
            },
            function (callback) {
                async.forEachSeries(publicAccountIds, function (publicAccountId, callback) {
                    var publicAccount = {};
                    async.series(
                        [
                            function (callback) {
                                redis.get("account:" + publicAccountId + ":name", function (err, name) {
                                    if (err) return callback(err);
                                    publicAccount.name = name;
                                    callback();
                                });
                            },
                            function (callback) {
                                redis.get("account:" + publicAccountId + ":avatar", function (err, avatar) {
                                    if (err) return callback(err);
                                    publicAccount.avatar = (avatar || "");
                                    callback();
                                });
                            },
                            function (callback) {
                                redis.get("account:" + publicAccountId + ":desc", function (err, desc) {
                                    if (err) return callback(err);
                                    publicAccount.desc = (desc || "");
                                    callback();
                                });
                            },
                            function (callback) {
                                redis.get("account:" + publicAccountId + ":type", function (err, type) {
                                    if (err) return callback(err);
                                    var value = parseInt(type, 10);
                                    publicAccount.type = isNaN(value) ? 0 : value;
                                    callback();
                                });
                            }
                        ], function (err) {
                            if (err) return callback(err);
                            publicAccounts.push(publicAccount);
                            callback();
                        });

                }, function (err) {
                    callback(err);
                });
            }
        ], function (err) {
            callback(err, publicAccounts);
        }
    );
}

/**
 * 清除消息
 */
function clearMessages(redis, callback) {
    async.series([
        function (callback) {
            redis.keys("application:*:messages", function (err, keys) {
                if (err) return callback(err);
                async.forEach(keys, function (key, callback) {
                    redis.del(key);
                    callback();
                }, function (err) {
                    callback(err);
                });
            })
        },
        function (callback) {
            redis.keys("account:*:application:*:messages", function (err, keys) {
                if (err) return callback(err);
                async.forEach(keys, function (key, callback) {
                    redis.del(key);
                    callback();
                }, function (err) {
                    callback(err);
                });
            })
        },
        function (callback) {
            redis.keys("account:*:application:*:message", function (err, keys) {
                if (err) return callback(err);
                async.forEach(keys, function (key, callback) {
                    redis.del(key);
                    callback();
                }, function (err) {
                    callback(err);
                });
            })
        },
        function (callback) {
            redis.keys("account:*:application:*:sent_messages", function (err, keys) {
                if (err) return callback(err);
                async.forEach(keys, function (key, callback) {
                    redis.del(key);
                    callback();
                }, function (err) {
                    callback(err);
                });
            })
        },
        function (callback) {
            redis.keys("message:*", function (err, keys) {
                if (err) return callback(err);
                async.forEach(keys, function (key, callback) {
                    redis.del(key);
                    callback();
                }, function (err) {
                    callback(err);
                });
            })
        },
        function (callback) {
            redis.keys("attachment:*", function (err, keys) {
                if (err) return callback(err);
                async.forEach(keys, function (key, callback) {
                    redis.del(key);
                    callback();
                }, function (err) {
                    callback(err);
                });
            })
        }
    ], function (err) {
        callback(err);
    });
}

/**
 * 清理临时数据
 */
function cleanData(redis, callback) {
    async.parallel([
        function (callback) {
            redis.keys("application:*:connections", function (err, keys) {
                if (err) return callback(err);
                async.forEach(keys, function (key, callback) {
                    logger.debug("Deleting key " + key + "...");
                    redis.del(key);
                    callback();
                }, function (err) {
                    callback(err);
                });
            })
        },
        function (callback) {
            redis.keys("account:*:connections", function (err, keys) {
                if (err) return callback(err);
                async.forEach(keys, function (key, callback) {
                    logger.debug("Deleting key " + key + "...");
                    redis.del(key);
                    callback();
                }, function (err) {
                    callback(err);
                });
            })
        },
        function (callback) {
            redis.keys("connection:*", function (err, keys) {
                if (err) return callback(err);
                async.forEach(keys, function (key, callback) {
                    logger.debug("Deleting key " + key + "...");
                    redis.del(key);
                    callback();
                }, function (err) {
                    callback(err);
                });
            })
        }
    ], function (err) {
        callback(err);
    });
}

function getAccountPermissions(redis, accountId, callback) {
    var permissions = {applications: []};
    async.series([
        function (callback) {
            logger.trace("Getting basic permissions of account " + accountId + "...");
            redis.hgetall('account:' + accountId + ':permissions', function (err, value) {
                if (err) return callback(err);
                if (value) {
                    permissions.view_connections = (value.view_connections == 1);
                    permissions.view_applications = (value.view_applications == 1);
                    permissions.view_accounts = (value.view_accounts == 1);
                    permissions.view_messages = (value.view_messages == 1);
                    permissions.list_applications = (value.list_applications == 1);
                    permissions.list_accounts = (value.list_accounts == 1);
                    permissions.push_message = !(value.push_message == 0);
                    permissions.clear_messages = (value.clear_messages == 1);
                } else {
                    permissions.view_connections = false;
                    permissions.view_applications = false;
                    permissions.view_accounts = false;
                    permissions.view_messages = false;
                    permissions.list_applications = false;
                    permissions.list_accounts = false;
                    permissions.push_message = true;
                    permissions.clear_messages = false;
                }
                callback();
            });
        },
        function (callback) {
            logger.trace("Getting all application infos...");
            getApplicationInfos(redis, function (err, result) {
                if (err) return callback(err);
                logger.trace("application infos: " + JSON.stringify(result));
                async.forEachSeries(result.list, function (app, callback) {
                    logger.trace("Getting application permissions of account " + accountId + " and application " + app.id + "...");
                    redis.hgetall('account:' + accountId + ':application:' + app.id + ':permissions', function (err, appPermissions) {
                        if (err) return callback(err);
                        logger.trace("Application permissions found.");
                        if (appPermissions) {
                          logger.trace("Application permissions: " + JSON.stringify(appPermissions));
                          permissions.applications[app.id] = {
                                broadcast: (appPermissions.broadcast == 1),
                                multicast: (appPermissions.multicast == 1),
                                send: !(appPermissions.send == 0)
                            };
                        } else {
                            permissions.applications[app.id] = {
                                broadcast: false,
                                multicast: false,
                                send: false
                            };
                        }
                        callback();
                    });
                }, function (err) {
                    callback(err);
                });
            });
        }],
        function (err) {
            if (!err) logger.trace("permissions: " + JSON.stringify(permissions));
            callback(err, permissions);
        }
    );
}

function getAccountType(redis, accountId, callback) {

    redis.get("account:" + accountId + ":type", callback)
}

function main(fn) {
    fn();
}

void main(function () {
    exports.redisPool = poolModule.Pool({
        name: "redis",
        create: function (callback) {
            var redis = _redis.createClient(REDIS_PORT, REDIS_SERVER);
            redis.on("ready", function (err) {
                callback(null, redis);
            });
            redis.on("error", function (err) {
                callback(err);
            });
        },
        destroy: function (redis) {
            redis.end();
        },
        max: 100,
        // optional. if you set this, make sure to drain()
        min: 1,
        // specifies how long a resource can stay idle in pool before being removed
        idleTimeoutMillis: 1000 * 30,
        // if true, logs via console.log - can also be a function
        log: false
    });
});

