/**
 * Created with JetBrains WebStorm.
 * User: joebin
 * Date: 13-3-15
 * Time: 下午6:17
 */
var fs = require('fs');
var uuid = require('node-uuid');
//
var config = require(__dirname + '/../config');
var utils = require(__dirname + '/../utils');
var crypt = require(__dirname + '/../client/crypt');
var db = require(__dirname + '/../db');

// 导出函数
exports.registerNew = registerApplication;
exports.updateOld = updateApplication;
exports.deleteSingle = deleteApplication;
exports.listAll = listApplications;
exports.getSingle = getApplication;
//
exports.existsName = existsApplicationName;
//
exports.checkId = checkApplicationId;
exports.checkIdAndName = checkApplicationIdAndName;
exports.checkNewApplicationInfo = checkNewApplicationInfo;
exports.checkApplicationUpdateInfo = checkApplicationUpdateInfo;

const LOG_ENABLED = config.LOG_ENABLED;

var logStream = LOG_ENABLED ? fs.createWriteStream("logs/application.log", {"flags": "a"}) : null;
Date.prototype.Format = utils.DateFormat;

/*
 * Print log
 */
function log(msg) {
    var now = new Date();
    var strDatetime = now.Format("yyyy-MM-dd HH:mm:ss");
    var buffer = "[" + strDatetime + "] " + msg + "[application]";
    if (logStream != null) logStream.write(buffer + "\r\n");
    console.log(buffer);
}

// 注册新应用
function registerApplication(req, res) {
    var application = req.body; //JSON.parse(req.rawBody);
    log('Registering application ' + req.params.name +
        ': need_login=' + (application.need_login ? 'yes' : 'no') +
        ', need_login_password=' + (application.need_login_password ? 'yes' : 'no') +
        ', auto_create_account=' + (application.auto_create_account != false ? 'yes' : 'no') +
        ', protect_login=' + (application.protect_login ? 'yes' : 'no') +
        ', encrypt_message=' + (application.encrypt_message ? 'yes' : 'no') +
        ''
    );

    var appId = uuid.v4().toUpperCase();
    var appPassword = crypt.makeAppKey();
    if (application.protect_login) {
        crypt.makeRsaKeys(function (err, protectKey) {
            if (err) return res.json({success: false, reason: err});
            saveNewApplicationInfo(protectKey);
        });
    } else {
        saveNewApplicationInfo();
    }
    function saveNewApplicationInfo(protectKey) {
        var pass = utils.md5(appPassword);
        db.saveNewApplicationInfo(appId, req.params.name, pass, application.need_login, application.need_login_password,
            application.auto_create_account, application.protect_login, protectKey, application.encrypt_message,
            new Date().Format("yyyyMMddHHmmss"),
            function (err) {
                if (err) res.json({success: false, reason: err});
                else res.json({success: true, id: appId, password: appPassword});
            }
        );
    }
}

// 修改应用信息
function updateApplication(req, res) {
    var updateInfo = req.body; //JSON.parse(req.rawBody);
    log('Updating application ' + req.params.id +
        ': name=' + updateInfo.name +
        ', new_name=' + (!updateInfo.new_name ? '<unchanged>' : updateInfo.new_name) +
        ', need_login=' + (updateInfo.need_login == null ? '<unchanged>' : (updateInfo.need_login ? 'yes' : 'no')) +
        ', need_login_password=' + (updateInfo.need_login_password == null ? '<unchanged>' : (updateInfo.need_login_password ? 'yes' : 'no')) +
        ', auto_create_account=' + (updateInfo.auto_create_account == null ? '<unchanged>' : (updateInfo.auto_create_account != false ? 'yes' : 'no')) +
        ', protect_login=' + (updateInfo.protect_login == null ? '<unchanged>' : (updateInfo.protect_login ? 'yes' : 'no')) +
        ', encrypt_message=' + (updateInfo.encrypt_message == null ? '<unchanged>' : (updateInfo.encrypt_message ? 'yes' : 'no')) +
        ''
    );
    db.updateApplicationInfo(req.params.id, updateInfo.name, updateInfo.new_name, updateInfo.need_login, updateInfo.need_login_password,
        updateInfo.auto_create_account, updateInfo.protect_login, updateInfo.encrypt_message,
        new Date().Format("yyyyMMddHHmmss"),
        function (err) {
            if (err) res.json({success: false, reason: err});
            else res.json({success: true});
        }
    );
}

// 删除应用(下线)
function deleteApplication(req, res) {
    //var deleteInfo = req.body; //JSON.parse(req.rawBody);
    log('Deleting application ' + req.params.id +
        //': name=' + deleteInfo.name +
        ''
    );

    db.deleteApplication(req.params.id,
        function (err) {
            if (err) res.json({success: false, reason: err});
            else res.json({success: true});
        }
    );
}

// 获取应用列表(包括ID和名称)
function listApplications(req, res) {
    log('Getting applications summary(id&name) ' +
        ''
    );

    db.getApplicationInfos(
        function (err, applicationInfos) {
            if (err) res.json({success: false, reason: err});
            else res.json({success: true, count: applicationInfos.count, list: applicationInfos.list});
        }
    );
}

// 获取应用信息
function getApplication(req, res) {
    log('Getting application info ' + req.params.id +
        ''
    );

    db.getApplicationInfo(req.params.id,
        function (err, applicationInfo) {
            if (err)
				res.json({success: false, reason: err});
            else
				res.json({success: true, name: applicationInfo.name, password: applicationInfo.password,
					need_login: applicationInfo.needLogin, 
					need_login_password: applicationInfo.needLoginPassword, auto_create_account: applicationInfo.autoCreateAccount,
					protect_login: applicationInfo.protectLogin, encrypt_message: applicationInfo.encryptMessage,
					public_key: (applicationInfo.protectKey ? applicationInfo.protectKey.public : ""),
					private_key: (applicationInfo.protectKey ? applicationInfo.protectKey.private : "")});
        }
    );
}

// 判断指定应用名称是否存在
function existsApplicationName(req, res) {
    log('Checking application name ' + req.params.name +
        ''
    );

    db.existsApplicationName(req.params.name, function (err, exists) {
        if (err) return res.json({success: false, reason: err});
        res.json({success: true, exists: exists});
    });
}

// 检查应用ID
function checkApplicationId(req, res, next) {

    db.existsApplicationId(req.params.id, function (err, exists) {
        if (err) return next(err);
        else if (!exists) return res.json({success:false,errcode:1,errmsg:"Application id " + req.params.id + " not exists"});
        else next();
    });
}

// 检查应用ID及名称
function checkApplicationIdAndName(req, res, next) {

    var appInfo = req.body; //JSON.parse(req.rawBody);
    if (!appInfo.name) return res.json({success:false,errcode:1,errmsg:"No application name provided"});

    db.existsApplicationId(req.params.id, function (err, exists) {
        if (err) return next(err);
        else if (!exists) return res.json({success:false,errcode:2,errmsg:"Application id " + req.params.id + " not exists"});
        else {
            db.getApplicationName(req.params.id, function (err, name) {
                if (err) return next(err);
                if (name!=appInfo.name) return res.json({success:false,errcode:3,errmsg:"Application name " + appInfo.name + " not matched"});
                next();
            });
        };
    });
}

// 检查新应用信息
function checkNewApplicationInfo(req, res, next) {
    var application = req.body; //JSON.parse(req.rawBody);
    log('Checking new application info: ' + req.params.name +
        ', need_login=' + (application.need_login ? 'yes' : 'no') +
        ', need_login_password=' + (application.need_login_password ? 'yes' : 'no') +
        ', auto_create_account=' + (application.auto_create_account != false ? 'yes' : 'no') +
        ', protect_login=' + (application.protect_login ? 'yes' : 'no') +
        ', encrypt_message=' + (application.encrypt_message ? 'yes' : 'no') +
        ''
    );

    db.existsApplicationName(req.params.name, function (err, exists) {
        if (err) return next(err);
        if (exists) return res.json({success:false,errcode:1,errmsg:"Application name " + req.params.name + " exists"});
        next();
    });
}


// 检查应用修改信息
function checkApplicationUpdateInfo(req, res, next) {
    var updateInfo = req.body; //JSON.parse(req.rawBody);
    log('Checking update info for application ' + req.params.id +
        ': name=' + updateInfo.name +
        ', new_name=' + (!updateInfo.new_name ? '<unchanged>' : updateInfo.new_name) +
        ', need_login=' + (updateInfo.need_login == null ? '<unchanged>' : (updateInfo.need_login ? 'yes' : 'no')) +
        ', need_login_password=' + (updateInfo.need_login_password == null ? '<unchanged>' : (updateInfo.need_login_password ? 'yes' : 'no')) +
        ', auto_create_account=' + (updateInfo.auto_create_account == null ? '<unchanged>' : (updateInfo.auto_create_account != false ? 'yes' : 'no')) +
        ', protect_login=' + (updateInfo.protect_login == null ? '<unchanged>' : (updateInfo.protect_login ? 'yes' : 'no')) +
        ', encrypt_message=' + (updateInfo.encrypt_message == null ? '<unchanged>' : (updateInfo.encrypt_message ? 'yes' : 'no')) +
        ''
    );

    db.checkApplicationUpdateInfo(req.params.id, updateInfo.name, updateInfo.new_name, function (err, reason) {
        if (err) return next(err);
        if (reason) return res.json({success:false,errcode:1,errmsg:reason});
        next();
    });
}
