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
//
exports.getAccounts = getAccounts;

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

function getAccounts(req, res) {
    log('Get accounts: ' +
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

	db.getAllAccounts(function (err, accounts) {
		
		if (err) return res.json({success: false, errcode: 1, errmsg: err});
		
		var filtered = [];
		if (req.query.sSearch!="") {
			// 过滤
			for (var i in accounts) {
				var account = accounts[i];
				if (account.accountId.indexOf(req.query.sSearch)!=-1 ||
					account.name.indexOf(req.query.sSearch)!=-1 ||
					account.phoneNumber && account.phoneNumber.indexOf(req.query.sSearch)!=-1 ||
					account.emailAddress && account.emailAddress.indexOf(req.query.sSearch)!=-1 ||
					(account.disabled?"禁用":"启用").indexOf(req.query.sSearch)!=-1 ||
					account.createTime.indexOf(req.query.sSearch)!=-1) {
					account.updateTime && account.updateTime.indexOf(req.query.sSearch)!=-1 ||
					filtered.push(account);
				}
			}
		} else {
			// 不需要过滤
			filtered = accounts;
		}
		
		// 排序
		filtered.sort(function(x, y) {
			switch (parseInt(iSortCol_0, 10)) {
			case 0: // accountId
				return compareString(x.accountId, y.accountId);
			case 1: // name
				return compareString(x.name, y.name);
			case 2: // phoneNumber
				return compareString(x.phoneNumber, y.phoneNumber);
			case 3: // emailAddress
				return compareString(x.emailAddress, y.emailAddress);
			case 4: // disabled
				return compareBoolean(x.disabled, y.disabled);
			case 5: // createTime
				return compareString(x.createTime, y.createTime);
			case 6: // updateTime
				return compareString(x.updateTime, y.updateTime);
			}
		});
		
		// 分页
		var iDisplayStart = parseInt(req.query.iDisplayStart, 10);
		var iDisplayLength = parseInt(req.query.iDisplayLength, 10);
		var paged = filtered.slice(iDisplayStart, Math.min(iDisplayStart+iDisplayLength, filtered.length));
		var result = [];
		for (var i in paged) {
			var account = paged[i];
			result.push([account.accountId,account.name,account.phoneNumber,account.emailAddress,
				account.disabled?"禁用":"启用", account.createTime, account.updateTime]);
		}
		
		return res.json({sEcho: parseInt(req.query.sEcho, 10), iTotalRecords: accounts.length, 
			iTotalDisplayRecords: filtered.length, aaData: result,
			sColumns: "accountId,name,phoneNumber,emailAddress,disabled,createTime,updateTime"}); 
	});

	function compareString(s1, s2) {
		if (s1==null && s2==null) return 0;
		if (s1==null) return (sSortDir_0=='asc'?0:1); 
		if (s2==null) return (sSortDir_0=='asc'?1:0); 
		
		return (sSortDir_0=='asc'?s1.localeCompare(s2):s2.localeCompare(s1));
	}
	function compareBoolean(b1, b2) {
		return compareString(b1?"禁用":"启用", b2?"禁用":"启用");
	}
}
