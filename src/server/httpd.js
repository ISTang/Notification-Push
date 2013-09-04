/**
 * Created with JetBrains WebStorm.
 * User: joebin
 * Date: 13-3-14
 * Time: 下午9:35
 */
var fs = require('fs');
var express = require('express');
//
var passport = require('passport')
  , LocalStrategy = require('passport-local').Strategy
  , ensureLoggedIn = require('connect-ensure-login').ensureLoggedIn;
var user = require('connect-roles');
//
var config = require(__dirname + '/config');
var utils = require(__dirname + '/utils');
var db = require(__dirname + '/db');
//
var appman = require(__dirname + "/manage/application");
var accman = require(__dirname + "/manage/account");
var msgpush = require(__dirname + "/manage/message");
var connman = require(__dirname + "/manage/connection");

// 定义常量
const HTTPD_PORT = config.HTTPD_PORT;
const LOG_ENABLED = config.LOG_ENABLED;
const GRACE_EXIT_TIME = config.GRACE_EXIT_TIME;

var webapp = express();

// 定义共享环境
webapp.configure(function () {
    webapp.engine('.html', require('ejs').__express);
    webapp.set('views', __dirname + '/views');
    webapp.set('view engine', 'html');

    /*webapp.use(function (req, res, next) {
        req.setEncoding('utf-8');
        var data = '';
        req.on('data', function (chunk) {
        	data += chunk;
        });
        req.on('end', function () {
        	req.rawBody = data;
        	next();
        });
    });*/
    webapp.use(express.cookieParser());
    webapp.use(express.bodyParser());
    webapp.use(express.methodOverride());
    //
    webapp.use(express.session({ secret: 'keyboard cat' }));
    webapp.use(passport.initialize());
    webapp.use(passport.session());
    //
    webapp.use(webapp.router);
});
// 定义开发环境
webapp.configure('development', function () {
    webapp.use(express.logger('dev'));
    webapp.use(express.static(__dirname + '/public'));
    webapp.use(express.errorHandler({dumpExceptions: true, showStack: true}));
});
// 定义生产环境
webapp.configure('production', function () {
    var oneYear = 31557600000;
    webapp.use(express.logger());
    webapp.use(express.static(__dirname + '/public', { maxAge: oneYear }));
    webapp.use(express.errorHandler());
});

webapp.set('env', 'development');

passport.use(new LocalStrategy(
	function(username, password, done) {
		db.redisPool.acquire(function(err, redis) {
			if (err) {
				log(err);
				return done(err);
			}
			db.checkUsername(redis, username, utils.md5(password), false, function(result) {
				db.redisPool.release(redis);
				if (!result.passed) {
					log("Login check failed: "+result.reason );
					return done(null, false, { message: result.reason }); 
				}
				log("Login check passed.");
				done(null, {id:result.accountId,name:result.accountName,phone:result.phoneNumber,email:result.emailAddress});
			});
		});
	}
));

passport.serializeUser(function(user, done) {
	done(null, user.id);
});

passport.deserializeUser(function(id, done) {
	db.redisPool.acquire(function(err, redis) {
		if (err) {
			log(err);
			return done(err);
		}
		db.getAccountInfo(redis, id, function(err, result) {
			db.redisPool.release(redis);
			done(err, result);
		});
	});
});

webapp.use(user);

user.use('view connections', function(req) {
    return true;
});

user.use('view applications', function(req) {
    return true;
});

user.use('view accounts', function(req) {
    return true;
});

user.use('view messages', function(req) {
    return true;
});

user.use('list applications', function(req) {
    return true;
});

user.use('list accounts', function(req) {
    return true;
});

user.use('push message', function(req) {
    return true;
});

user.use('clear messages', function(req) {
    return true;
});

/*user.use(function(req) {
  if (req.user.role === 'admin') {
    return true;
  }
});*/

//optionally controll the access denid page displayed
user.setFailureHandler(function (req, res, action){
	var accept = req.headers.accept || '';
	res.status(403);
	if (~accept.indexOf('html')) {
		res.render('access-denied', {action: action});
	} else {
		res.json({
			success: false,
			errcode: -1,
			errmsg: 'Access Denied - You don\'t have permission to: ' + action
		});
	}
});


var logStream =   fs.createWriteStream(__dirname+"/logs/httpd.log", {"flags": "a"}) ;

Date.prototype.Format = utils.DateFormat;

/*
 * Print log
 */
function log(msg) {

    var now = new Date();
    var strDatetime = now.Format("yyyy-MM-dd HH:mm:ss");
    var buffer = "[" + strDatetime + "] " + msg + "[httpd]";
    if (logStream != null) logStream.write(buffer + "\r\n");
    if ( LOG_ENABLED) console.log(buffer);
}

function main(fn) {
    fn();
}

var exitTimer = null;
function aboutExit() {

    if (exitTimer) return;

    exitTimer = setTimeout(function () {

        log("web app exit...");

        process.exit(0);

    }, GRACE_EXIT_TIME);
}

// 程序入口
void main(function () {
    process.on('SIGINT', aboutExit);
    process.on('SIGTERM', aboutExit);

    webapp.get('/login', function (req, res) {
		res.setHeader("Content-Type", "text/html");
		res.render('login', {
			pageTitle: '消息推送中心 - 登录'
		});
    });
    webapp.post('/login',  passport.authenticate('local', { successRedirect: '/',
									   failureRedirect: '/login',
									   failureFlash: false }));
    
    // 1.应用接入
    //
    // 1)注册新应用
    // curl --header "Content-Type:application/json;charset=utf-8" -d "{\"need_login\":true}" http://localhost:4567/application/appname
    webapp.post('/application/:name', appman.checkNewApplicationInfo, appman.registerNew);
    //
    // 2)修改应用信息
    // curl --header "Content-Type:application/json;charset=utf-8" -X PUT -d "{\"name\":\"appname\"}" http://localhost:4567/application/123
    webapp.put('/application/:id', appman.checkId, appman.checkApplicationUpdateInfo, appman.updateOld);
    //
    // 3)删除应用
    // curl --header "Content-Type:application/json;charset=utf-8" -X DELETE -d "{\"name\":\"appname\"}" http://localhost:4567/application/123
    webapp.delete('/application/:id', appman.checkId/*appman.checkIdAndName*/, appman.deleteSingle);
    //
    // 4)获取应用列表(包括ID和名称)
    //  curl http://localhost:4567/applications
    webapp.get('/applications', appman.listAll);
    //
    // 5)获取应用信息
    //  curl http://localhost:4567/application/123
    webapp.get('/application/:id', appman.checkId, appman.getSingle);
    //
    // 6)检查应用名称
    //  curl http://localhost:4567/application/name/appname
    webapp.get('/application/name/:name', appman.existsName);
	// 7)获取应用信息(适合web提交)
    webapp.get('/applications/AjaxHandler', ensureLoggedIn('/login'), user.can('view applications'), appman.getApplications);

    // 2.用户账号
    //
    // 1)新建账号
    // curl --header "Content-Type:application/json;charset=utf-8" -d "{\"phone\":\"13811111111\"}" http://localhost:4567/account/accname
    webapp.post('/account/:name', accman.checkNewAccountInfo, accman.createNew);
    //
    // 2)修改账号信息
    // curl --header "Content-Type:application/json;charset=utf-8" -X PUT -d "{\"phone\":\"13811111111\"}" http://localhost:4567/account/123
    webapp.put('/account/:id', accman.checkId, accman.checkAccountUpdateInfo, accman.updateOld);
    //
    // 3)禁用账号
    // curl -X PUT http://localhost:4567/account/123/disable
    webapp.put('/account/:id/disable', accman.checkId, accman.disableSingle);
    //
    // 4)允许账号
    // curl -X PUT http://localhost:4567/account/123/enable
    webapp.put('/account/:id/enable', accman.checkId, accman.enableSingle);
    //
    // 5)删除账号
    // curl -X DELETE http://localhost:4567/account/123
    webapp.delete('/account/:id', accman.checkId, accman.deleteSingle);
    //
    // 6)获取账号数量
    // curl http://localhost:4567/accounts/count
    webapp.get('/accounts/count', accman.countAll);
    //
    // 7)获取账号列表
    // curl http://localhost:4567/accounts
    webapp.get('/accounts', accman.listSome);
    //
    // 8)获取账号信息
    // curl http://localhost:4567/account/123
    webapp.get('/account/:id', accman.checkId, accman.getSingle);
    //
    // 9)检查账号名称
    // curl http://localhost:4567/account/name/accname
    webapp.get('/account/name/:name', accman.existsName);
    //
    // 10)检查电话号码
    // curl http://localhost:4567/account/phone/13811111111
    webapp.get('/account/phone/:phone', accman.existsPhoneNumber);
    //
    // 11)检查邮箱地址
    // curl http://localhost:4567/account/email/test@tets.com
    webapp.get('/account/email/:email', accman.existsEmailAddress);
	// 12)获取账号(适合web提交)
    webapp.get('/accounts/AjaxHandler', user.can('list accounts'), accman.getAccounts);

    // 3.消息推送
    //
    // 1)广播消息
    // curl --header "Content-Type:application/json;charset=utf-8" -d "{\"body\":\"hello, everyone\"}" http://localhost:4567/application/4083AD3D-0F41-B78E-4F5D-F41A515F2667/message
    webapp.post('/application/:id/message', appman.checkId, msgpush.broadcast);
    //
    // 2)群发消息
    // curl --header "Content-Type:application/json;charset=utf-8" -d "{\"accounts\":[{\"name\":\"accname\"}],\"message\":{\"body\":\"hello\",\"attachments\":[{\"title\":\"at1\",\"type\":\"application/oct-stream\",\"filename\":\"at1.bin\",\"url\":\"http://test.com/att1\"}]}}" http://localhost:4567/application/123/accounts/message
    webapp.post('/application/:id/accounts/message', appman.checkId, msgpush.multicast);
    // 3)发送消息
    // curl --header "Content-Type:application/json;charset=utf-8" -d "{\"body\":\"hello\",\"attachments\":[{\"title\":\"at1\",\"type\":\"application/oct-stream\",\"filename\":\"at1.bin\",\"url\":\"http://test.com/att1\"}]}" http://localhost:4567/application/123/account/accname/message
    webapp.post('/application/:id/account/:name/message', appman.checkId, accman.checkName, msgpush.send);
    // 4)推送消息(适合web提交)
    webapp.post('/pushmsg', ensureLoggedIn('/login'), user.can('push message'), msgpush.pushMessage);
    // 5)清除所有消息(适合web提交)
    webapp.delete('/allMessages', ensureLoggedIn('/login'), user.can('clear messages'), function (req, res) {
        msgpush.clearMessages(function (err) {
            if (err) {
                res.json({
                    success: false,
                    errcode: 1,
                    errmsg: err
                });
            } else {
                res.json({
                    success: true
                });
            }
        });
    });
	// 6)获取消息(适合web提交)
    webapp.get('/messages/AjaxHandler', ensureLoggedIn('/login'), user.can('view messages'), msgpush.getMessages);

	// 获取连接消息(适合web提交)
    webapp.get('/connections/AjaxHandler', ensureLoggedIn('/login'), user.can('view connections'), connman.getConnections);

    webapp.get('/', ensureLoggedIn('/login'), user.can('view connections'), function (req, res) {
        log("打开首页...");
		res.setHeader("Content-Type", "text/html");
		res.render('index', {
			pageTitle: '消息推送中心 - 首页'
		});
    });

    webapp.get('/appman', ensureLoggedIn('/login'), user.can('view applications'), function (req, res) {
        log("打开应用管理页面...");
		res.setHeader("Content-Type", "text/html");
		res.render('appman', {
			pageTitle: '消息推送中心 - 应用管理'
		});
    });

    webapp.get('/accman', ensureLoggedIn('/login'), user.can('view accounts'), function (req, res) {
        log("打开账号管理页面...");
		res.setHeader("Content-Type", "text/html");
		res.render('accman', {
			pageTitle: '消息推送中心 - 账号管理'
		});
    });

    webapp.get('/msgman', ensureLoggedIn('/login'), user.can('view messages'), function (req, res) {
        log("打开消息管理页面...");
		res.setHeader("Content-Type", "text/html");
		res.render('msgman', {
			pageTitle: '消息推送中心 - 消息管理'
		});
    });

    webapp.get('/appInfos', ensureLoggedIn('/login'), user.can('list applications'), function (req, res) {
        log("获取应用信息...");
        appman.getApplicationInfos(function (err, appInfos) {
            if (!err) {
                res.json({
                    success: true,
                    count: (err ? 0 : appInfos.length),
                    appInfos: appInfos
                });
            } else {
                res.json({
                    success: false,
                    errcode: 1,
                    errmsg: err
                });
            }
        });
    });

    webapp.listen(HTTPD_PORT);
    log('HTTPD process is running at port ' + HTTPD_PORT + '...');
});
