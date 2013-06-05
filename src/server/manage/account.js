/**
 * Created with JetBrains WebStorm.
 * User: joebin
 * Date: 13-3-15
 * Time: 下午8:22
 */
var fs = require('fs');
var uuid = require('node-uuid');
//
var config = require(__dirname + '/../config');
var utils = require(__dirname + '/../utils');
var db = require(__dirname + '/../db');

// 导出函数
exports.createNew = createAccount;
exports.updateOld = updateAccount;
exports.disableSingle = disableAccount;
exports.enableSingle = enableAccount;
exports.deleteSingle = deleteAccount;
exports.countAll = countAccounts;
exports.listSome = listAccounts;
exports.getSingle = getAccount;
exports.existsName = existsAccountName;
exports.existsPhoneNumber = existsPhoneNumber;
exports.existsEmailAddress = existsEmailAddress;
//
exports.checkId = checkAccountId;
exports.checkName = checkAccountName;
//
exports.checkNewAccountInfo = checkNewAccountInfo;
exports.checkAccountUpdateInfo = checkAccountUpdateInfo;

const LOG_ENABLED = config.LOG_ENABLED;
const DEFAULT_LOGINPASWORD = "666666";

var logStream = LOG_ENABLED ? fs.createWriteStream("logs/account.log", {"flags": "a"}) : null;
Date.prototype.Format = utils.DateFormat;

/*
 * Print log
 */
function log(msg) {
    var now = new Date();
    var strDatetime = now.Format("yyyy-MM-dd HH:mm:ss");
    var buffer = "[" + strDatetime + "] " + msg + "[account]";
    if (logStream != null) logStream.write(buffer + "\r\n");
    console.log(buffer);
}

// 新建账号
function createAccount(req, res) {
    var account = req.body; //JSON.parse(req.rawBody);
    log('Creating new account ' + req.params.name +
        ': phone=' + (account.phone != null ? account.phone : '') +
        ', email=' + (account.email != null ? account.email : '') +
        ', password=' + (account.password != null ? account.password : DEFAULT_LOGINPASWORD) +
        ''
    );

    var accountId = uuid.v4().toUpperCase();
    var password = (account.password != null ? account.password : DEFAULT_LOGINPASWORD);
    db.saveNewAccountInfo(accountId, req.params.name, account.phone, account.email, utils.md5(password),
        new Date().Format("yyyyMMddHHmmss"),
        function (err) {
            if (err) res.json({success: false, reason: err});
            else res.json({success: true, id: accountId});
        }
    );
}

// 修改账号信息
function updateAccount(req, res) {
    var updateInfo = req.body; //JSON.parse(req.rawBody);
    log('Updating account ' + req.params.id +
        ': new_name=' + (!updateInfo.new_name ? '<unchanged>' : updateInfo.new_name) +
        ', new_phone=' + (!updateInfo.new_phone ? '<unchanged>' : updateInfo.new_phone) +
        ', new_email=' + (!updateInfo.new_email ? '<unchanged>' : updateInfo.new_email) +
        ', new_password=' + (!updateInfo.new_password ? '<unchanged>' : updateInfo.new_password) +
        ''
    );

    var password = (updateInfo.new_password?utils.md5(updateInfo.new_password):null);
    db.updateAccountInfo(req.params.id, updateInfo.new_name, updateInfo.new_phone, updateInfo.new_email,
        password, new Date().Format("yyyyMMddHHmmss"),
        function (err) {
            if (err) res.json({success: false, reason: err});
            else res.json({success: true});
        }
    );
}

// 禁用账号
function disableAccount(req, res) {
    log('Disabling account ' + req.params.id +
        ''
    );

    db.updateAccountStatus(req.params.id, false, new Date().Format("yyyyMMddHHmmss"),
        function (err) {
            if (err) res.json({success: false, reason: err});
            else res.json({success: true});
        }
    );
}

// 启用账号
function enableAccount(req, res) {
    log('Enabling account ' + req.params.id +
        ''
    );

    db.updateAccountStatus(req.params.id, true, new Date().Format("yyyyMMddHHmmss"),
        function (err) {
            if (err) res.json({success: false, reason: err});
            else res.json({success: true});
        }
    );
}

// 删除账号
function deleteAccount(req, res) {
    log('Deleting account ' + req.params.id +
        ''
    );

    db.deleteAccount(req.params.id,
        function (err) {
            if (err) res.json({success: false, reason: err});
            else res.json({success: true});
        }
    );
}

// 获取账号数量
function countAccounts(req, res) {
    log('Counting all accounts ' +
        ''
    );

    db.getAccountCount(
        function (err, count) {
            if (err) res.json({success: false, reason: err});
            else res.json({success: true, count: count});
        }
    );
}

// 获取账号列表
function listAccounts(req, res) {
    var pageInfo = req.body; //JSON.parse(req.rawBody);
    log('Getting some accounts ' +
        ': start=' + (pageInfo.start == null ? '0' : pageInfo.start) +
        ', records=' + (pageInfo.records == null ? 'all' : pageInfo.records) +
        ''
    );

    db.getAccountInfos(pageInfo.start, pageInfo.records,
        function (err, accountInfos) {
            if (err) res.json({success: false, reason: err});
            else res.json({success: true, count: accountInfos.count, list: accountInfos.list});
        }
    );
}

// 获取账号信息
function getAccount(req, res) {
    log('Getting account info ' + req.params.id +
        ''
    );

    db.getAccountInfo(req.params.id,
        function (err, accountInfo) {
            if (err) res.json({success: false, reason: err});
            else res.json({success: true, name: accountInfo.name, phone: accountInfo.phone,
                email: accountInfo.email, disabled: accountInfo.disabled});
        }
    );
}

// 检测账号名称是否已存在
function existsAccountName(req, res) {
    log('Checking account name ' + req.params.name +
        ''
    );

    db.existsUsername(req.params.name, function (err, exists) {
        if (err) res.json({success: false, reason: err});
        else res.json({success: true, exists: (exists===1)});
    });
}

// 检测电话号码是否已存在
function existsPhoneNumber(req, res) {
    log('Checking phone number ' + req.params.phone +
        ''
    );

    db.existsUsername(req.params.phone, function (err, exists) {
        if (err) res.json({success: false, reason: err});
        else res.json({success: true, exists: (exists===1)});
    });
}

// 检测邮箱地址是否已存在
function existsEmailAddress(req, res) {
    log('Checking email address ' + req.params.email +
        ''
    );

    db.existsUsername(req.params.email, function (err, exists) {
        if (err) res.json({success: false, reason: err});
        else res.json({success: true, exists: (exists===1)});
    });
}

// 检查账号ID
function checkAccountId(req, res, next) {
    log('Checking account id ' + req.params.id +
        ''
    );

    db.existsAccountId(req.params.id, function (err, exists) {
        if (err) return next(err);
        else if (!exists) return res.json({success:false,errcode:1,errmsg:"Account id " + req.params.id + " not exists"});
        else next();
    });
}

// 检查账号名称
function checkAccountName(req, res, next) {
    log('Checking account name ' + req.params.name +
        ''
    );

    db.existsUsername(req.params.name, function (err, exists) {
        if (err) return next(err);
        else if (!exists) return res.json({success:false,errcode:1,errmsg:"Account name " + req.params.name + " not exists"});
        else next();
    });
}

// 检查新账号信息
function checkNewAccountInfo(req, res, next) {
    var account = req.body; //JSON.parse(req.rawBody);
    log('Checking new account info:' +
        ', name=' + req.params.name +
        ', phone=' + (account.phone != null ? account.phone : '') +
        ', email=' + (account.email != null ? account.email : '') +
        ''
    );

    db.checkNewAccountInfo(req.params.name, account.phone, account.email, function (err, reason) {
        if (err) return next(err);
        if (reason) return res.json({success:false,errcode:1,errmsg:reason});
        next();
    });
}

// 检查账号修改信息
function checkAccountUpdateInfo(req, res, next) {
    var updateInfo = req.body; //JSON.parse(req.rawBody);
    log('Checking update info for account ' + req.params.id +
        ': new_name=' + (!updateInfo.new_name ? '<unchanged>' : updateInfo.new_name) +
        ', new_phone=' + (!updateInfo.new_phone ? '<unchanged>' : updateInfo.new_phone) +
        ', new_email=' + (!updateInfo.new_email ? '<unchanged>' : updateInfo.new_email) +
        ''
    );

    db.checkAccountUpdateInfo(updateInfo.new_name, updateInfo.new_phone, updateInfo.new_email, function (err, reason) {
        if (err) return next(err);
        if (reason) return res.json({success:false,errcode:1,errmsg:reason});
        next();
    });
}
