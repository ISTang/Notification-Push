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
//
exports.getApplications = getApplications;
exports.getApplicationInfos = getApplicationInfos;

Date.prototype.Format = utils.DateFormat;

var logger = config.log4js.getLogger('application');
logger.setLevel(config.LOG_LEVEL);

// 注册新应用
function registerApplication(req, res) {
    var application = req.body; //JSON.parse(req.rawBody);
    logger.trace('Registering application ' + req.params.name +
        ': need_login=' + (application.need_login ? 'yes' : 'no') +
        ', need_login_password=' + (application.need_login_password ? 'yes' : 'no') +
        ', auto_create_account=' + (application.auto_create_account != false ? 'yes' : 'no') +
        ', protect_login=' + (application.protect_login ? 'yes' : 'no') +
        ', encrypt_message=' + (application.encrypt_message ? 'yes' : 'no') +
        ''
    );

    var appId = uuid.v4().toUpperCase();
    var appPassword = crypt.makeAppKey();
    crypt.makeRsaKeys(function (err, protectKey) {
        if (err) return res.json({success: false, reason: err});
        saveNewApplicationInfo(protectKey);
    });
    function saveNewApplicationInfo(protectKey) {
        var pass = utils.md5(appPassword);
        db.redisPool.acquire(function (err, redis) {
            if (err) {
                res.json({success: false, reason: err});
            } else {
                db.saveNewApplicationInfo(redis, appId, req.params.name, pass, application.need_login, application.need_login_password,
                    application.auto_create_account, application.protect_login, protectKey, application.encrypt_message,
                    new Date().Format("yyyyMMddHHmmss"), function (err) {
                        db.redisPool.release(redis);
                        if (err) res.json({success: false, reason: err});
                        else res.json({success: true, id: appId, password: appPassword,
                            public_key: protectKey.public, private_key: protectKey.private});
                    }
                );
            }
        });
    }
}

// 修改应用信息
function updateApplication(req, res) {
    var updateInfo = req.body; //JSON.parse(req.rawBody);
    logger.trace('Updating application ' + req.params.id +
        ': name=' + updateInfo.name +
        ', new_name=' + (!updateInfo.new_name ? '<unchanged>' : updateInfo.new_name) +
        ', need_login=' + (updateInfo.need_login == null ? '<unchanged>' : (updateInfo.need_login ? 'yes' : 'no')) +
        ', need_login_password=' + (updateInfo.need_login_password == null ? '<unchanged>' : (updateInfo.need_login_password ? 'yes' : 'no')) +
        ', auto_create_account=' + (updateInfo.auto_create_account == null ? '<unchanged>' : (updateInfo.auto_create_account != false ? 'yes' : 'no')) +
        ', protect_login=' + (updateInfo.protect_login == null ? '<unchanged>' : (updateInfo.protect_login ? 'yes' : 'no')) +
        ', encrypt_message=' + (updateInfo.encrypt_message == null ? '<unchanged>' : (updateInfo.encrypt_message ? 'yes' : 'no')) +
        ''
    );
    db.redisPool.acquire(function (err, redis) {
        if (err) {
            res.json({success: false, reason: err});
        } else {
            db.updateApplicationInfo(redis, req.params.id, updateInfo.name, updateInfo.new_name,
                updateInfo.need_login, updateInfo.need_login_password,
                updateInfo.auto_create_account, updateInfo.protect_login, updateInfo.encrypt_message,
                new Date().Format("yyyyMMddHHmmss"),
                function (err) {
                    db.redisPool.release(redis);
                    if (err) res.json({success: false, reason: err});
                    else res.json({success: true});
                }
            );
        }
    });
}

// 删除应用(下线)
function deleteApplication(req, res) {
    //var deleteInfo = req.body; //JSON.parse(req.rawBody);
    logger.trace('Deleting application ' + req.params.id +
        //': name=' + deleteInfo.name +
        ''
    );

    db.redisPool.acquire(function (err, redis) {
        if (err) {
            res.json({success: false, reason: err});
        } else {
            db.deleteApplication(redis, req.params.id,
                function (err) {
                    db.redisPool.release(redis);
                    if (err) res.json({success: false, reason: err});
                    else res.json({success: true});
                }
            );
        }
    });
}

// 获取应用列表(包括ID和名称)
function listApplications(req, res) {
    logger.trace('Getting applications summary(id&name) ' +
        ''
    );

    db.redisPool.acquire(function (err, redis) {
        if (err) {
            res.json({success: false, reason: err});
        } else {
            db.getApplicationInfos(redis,
                function (err, applicationInfos) {
                    db.redisPool.release(redis);
                    if (err) res.json({success: false, reason: err});
                    else res.json({success: true, count: applicationInfos.count, list: applicationInfos.list});
                }
            );
        }
    });
}

// 获取应用信息
function getApplication(req, res) {
    logger.trace('Getting application info ' + req.params.id +
        ''
    );

    db.redisPool.acquire(function (err, redis) {
        if (err) {
            res.json({success: false, reason: err});
        } else {
            db.getApplicationInfo(redis, req.params.id,
                function (err, applicationInfo) {
                    db.redisPool.release(redis);
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
    });
}

// 判断指定应用名称是否存在
function existsApplicationName(req, res) {
    logger.trace('Checking application name ' + req.params.name +
        ''
    );

    db.redisPool.acquire(function (err, redis) {
        if (err) {
            res.json({success: false, reason: err});
        } else {
            db.existsApplicationName(redis, req.params.name, function (err, exists) {
                db.redisPool.release(redis);
                if (err) return res.json({success: false, reason: err});
                res.json({success: true, exists: exists});
            });
        }
    });
}

// 检查应用ID
function checkApplicationId(req, res, next) {

    db.redisPool.acquire(function (err, redis) {
        if (err) {
            next(err);
        } else {
            db.existsApplicationId(redis, req.params.id, function (err, exists) {
                db.redisPool.release(redis);
                if (err) return next(err);
                else if (!exists) return res.json({success: false, errcode: 1, errmsg: "Application id " + req.params.id + " not exists"});
                else next();
            });
        }
    });
}

// 检查应用ID及名称
function checkApplicationIdAndName(req, res, next) {

    var appInfo = req.body; //JSON.parse(req.rawBody);
    if (!appInfo.name) return res.json({success: false, errcode: 1, errmsg: "No application name provided"});

    db.redisPool.acquire(function (err, redis) {
        if (err) {
            next(err);
        } else {
            db.existsApplicationId(redis, req.params.id, function (err, exists) {
                if (err) {
                    db.redisPool.release(redis);
                    return next(err);
                } else if (!exists) {
                    db.redisPool.release(redis);
                    return res.json({success: false, errcode: 2, errmsg: "Application id " + req.params.id + " not exists"});
                } else {
                    db.getApplicationName(redis, req.params.id, function (err, name) {
                        db.redisPool.release(redis);
                        if (err) return next(err);
                        if (name != appInfo.name) return res.json({success: false, errcode: 3, errmsg: "Application name " + appInfo.name + " not matched"});
                        next();
                    });
                }
                ;
            });
        }
    });
}

// 检查新应用信息
function checkNewApplicationInfo(req, res, next) {
    var application = req.body; //JSON.parse(req.rawBody);
    logger.trace('Checking new application info: ' + req.params.name +
        ', need_login=' + (application.need_login ? 'yes' : 'no') +
        ', need_login_password=' + (application.need_login_password ? 'yes' : 'no') +
        ', auto_create_account=' + (application.auto_create_account != false ? 'yes' : 'no') +
        ', protect_login=' + (application.protect_login ? 'yes' : 'no') +
        ', encrypt_message=' + (application.encrypt_message ? 'yes' : 'no') +
        ''
    );

    db.redisPool.acquire(function (err, redis) {
        if (err) {
            next(err);
        } else {
            db.existsApplicationName(redis, req.params.name, function (err, exists) {
                db.redisPool.release(redis);
                if (err) return next(err);
                if (exists) return res.json({success: false, errcode: 1, errmsg: "Application name " + req.params.name + " exists"});
                next();
            });
        }
    });
}


// 检查应用修改信息
function checkApplicationUpdateInfo(req, res, next) {
    var updateInfo = req.body; //JSON.parse(req.rawBody);
    logger.trace('Checking update info for application ' + req.params.id +
        ': name=' + updateInfo.name +
        ', new_name=' + (!updateInfo.new_name ? '<unchanged>' : updateInfo.new_name) +
        ', need_login=' + (updateInfo.need_login == null ? '<unchanged>' : (updateInfo.need_login ? 'yes' : 'no')) +
        ', need_login_password=' + (updateInfo.need_login_password == null ? '<unchanged>' : (updateInfo.need_login_password ? 'yes' : 'no')) +
        ', auto_create_account=' + (updateInfo.auto_create_account == null ? '<unchanged>' : (updateInfo.auto_create_account != false ? 'yes' : 'no')) +
        ', protect_login=' + (updateInfo.protect_login == null ? '<unchanged>' : (updateInfo.protect_login ? 'yes' : 'no')) +
        ', encrypt_message=' + (updateInfo.encrypt_message == null ? '<unchanged>' : (updateInfo.encrypt_message ? 'yes' : 'no')) +
        ''
    );

    db.redisPool.acquire(function (err, redis) {
        if (err) {
            next(err);
        } else {
            db.checkApplicationUpdateInfo(redis, req.params.id, updateInfo.name, updateInfo.new_name, function (err, reason) {
                db.redisPool.release(redis);
                if (err) return next(err);
                if (reason) return res.json({success: false, errcode: 1, errmsg: reason});
                next();
            });
        }
    });
}

function getApplications(req, res) {
    logger.trace('Get applications: ' +
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
                sColumns: "appId"});
        } else {
            db.getAllApplications(redis, function (err, applications) {

                db.redisPool.release(redis);

                if (err) {
                    return res.json({sEcho: parseInt(req.query.sEcho, 10), iTotalRecords: 1,
                        iTotalDisplayRecords: 1, aaData: [
                            ['数据库操作失败: ' + err]
                        ],
                        sColumns: "appId"});
                }

                var filtered = [];
                if (req.query.sSearch != "") {
                    // 过滤
                    for (var i in applications) {
                        var application = applications[i];
                        if (application.appId.indexOf(req.query.sSearch) != -1 ||
                            application.name.indexOf(req.query.sSearch) != -1 ||
                            (application.needLogin == true ? "是" : "否").indexOf(req.query.sSearch) != -1 ||
                            (application.needLoginPassword == true ? "是" : "否").indexOf(req.query.sSearch) != -1 ||
                            (application.autoCreateAccount != false ? "是" : "否").indexOf(req.query.sSearch) != -1 ||
                            (application.protectLogin == true ? "是" : "否").indexOf(req.query.sSearch) != -1 ||
                            (application.secureMessage == true ? "是" : "否").indexOf(req.query.sSearch) != -1 ||
                            application.createTime.indexOf(req.query.sSearch) != -1) {
                            filtered.push(application);
                        }
                    }
                } else {
                    // 不需要过滤
                    filtered = applications;
                }


                // 排序
                filtered.sort(function (x, y) {
                    switch (parseInt(iSortCol_0, 10)) {
                        case 0: // appId
                            return compareString(x.appId, y.appId);
                        case 1: // name
                            return compareBoolean(x.name, y.name);
                        case 2: // needLogin
                            return compareBoolean(x.needLogin, y.needLogin);
                        case 3: // needLoginPassword
                            return compareBoolean(x.needLoginPassword, y.needLoginPassword);
                        case 4: // autoCreateAccount
                            return compareBoolean(x.autoCreateAccount, y.autoCreateAccount);
                        case 5: // secureMessage
                            return compareBoolean(x.secureMessage, y.secureMessage);
                        case 6: // protectLogin
                            return compareBoolean(x.protectLogin, y.protectLogin);
                        case 7: // createTime
                            return compareString(x.createTime, y.createTime);
                    }
                });

                // 分页
                var iDisplayStart = parseInt(req.query.iDisplayStart, 10);
                var iDisplayLength = parseInt(req.query.iDisplayLength, 10);
                var paged = filtered.slice(iDisplayStart, Math.min(iDisplayStart + iDisplayLength, filtered.length));
                var result = [];
                for (var i in paged) {
                    var application = paged[i];
                    result.push([application.appId, application.name, application.needLogin == true ? "是" : "否", application.needLoginPassword == true ? "是" : "否",
                        application.autoCreateAccount == false ? "否" : "是", application.protectLogin == true ? "是" : "否",
                        application.secureMessage == true ? "是" : "否", application.createTime]);
                }

                return res.json({sEcho: parseInt(req.query.sEcho, 10), iTotalRecords: applications.length,
                    iTotalDisplayRecords: filtered.length, aaData: result,
                    sColumns: "appId,name,needLogin,needLoginPassword,autoCreateAccount,protectLogin,secureMessage,createTime"});
            });
        }
    });

    function compareString(s1, s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return (sSortDir_0 == 'asc' ? 0 : 1);
        if (s2 == null) return (sSortDir_0 == 'asc' ? 1 : 0);

        return (sSortDir_0 == 'asc' ? s1.localeCompare(s2) : s2.localeCompare(s1));
    }

    function compareBoolean(b1, b2) {
        return compareString(b1 ? "是" : "否", b2 ? "是" : "否");
    }
}

function getApplicationInfos(callback) {
    db.redisPool.acquire(function (err, redis) {
        if (err) {
            callback(err);
        } else {
            db.getApplicationInfos(redis, callback);
        }
    });
}
