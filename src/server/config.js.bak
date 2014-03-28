var path = require("path");

var log4js = require('log4js');
log4js.clearAppenders();
log4js.loadAppender('file');
log4js.configure({
    appenders: [
        { type: 'file', filename: 'logs/server.log', "maxLogSize": 10485760, category: 'server' },
        { type: 'file', filename: 'logs/db.log', "maxLogSize": 10485760, category: 'db' },
        { type: 'file', filename: 'logs/login.log', "maxLogSize": 10485760, category: 'login' },
        { type: 'file', filename: 'logs/notify.log', "maxLogSize": 10485760, category: 'notify' },
        { type: 'file', filename: 'logs/httpd.log', "maxLogSize": 10485760, category: 'httpd' },
        { type: 'file', filename: 'logs/account.log', "maxLogSize": 10485760, category: 'account' },
        { type: 'file', filename: 'logs/application.log', "maxLogSize": 10485760, category: 'application' },
        { type: 'file', filename: 'logs/message.log', "maxLogSize": 10485760, category: 'message' },
        { type: 'file', filename: 'logs/connection.log', "maxLogSize": 10485760, category: 'connection' }
    ]
});
exports.log4js = log4js;

exports.SERVER_HOST = 'isajia.com'; // 118.244.9.191, 218.200.212.120
exports.SERVER_PORT = 1234;
exports.HTTPD_PORT = 2345;

exports.LOGIN_NUMBER = 1; // 5
exports.NOTIFY_NUMBER = 1; // 2

exports.LOGIN_TIMEOUT = 1000 * 60; // ms
exports.MAX_INACTIVE_TIME = 1000 * 60 * 5 * 1; // 心跳检测最长时间(ms)
exports.KEEPALVE_INIIALDELAY = 1000 * 30; // keepalive 首次探测时延(ms)
exports.KEEPALVE_PROBEINTERVAL = 1000 * 0; // keepalive 探测间隔时间(ms)--0表示不探测
exports.KEEPALVE_FAILURECOUNT = 2; // keepalive 探测失败最大次数(ms)

exports.GRACE_EXIT_TIME = 1500; // ms

//exports.MESSAGE_SEND_RATE = 2; // per min

exports.NEWLINE = "\r\n";
exports.CLIENT_OFFLINE_MSG = "Are you offline?";
exports.BROARDCAST_MSG = "You have a new message.";
exports.SERVER_ERROR_MSG = "Server error!";

exports.LOG_LEVEL = 'TRACE';
exports.TRACK_SOCKET = true;

exports.REDIS_SERVER = "localhost";
exports.REDIS_PORT = 7379;

exports.SHOW_PACKET = true;

exports.CREATE_ACCOUNT_LOCK_FILE = 'create-account.lock';

exports.RANDOM_ACCOUNTNAME_SIZE = 8;

exports.MAX_OFFLINE_DAYS = 1;

exports.UPLOAD_DIR = path.join(__dirname, "public", "files");
exports.DOWNLOAD_URL_BASE = "http://" + exports.SERVER_HOST + ":" + exports.HTTPD_PORT + "/files/";
exports.MAX_ATTACHMENT_COUNT = 4;

exports.EMAIL_ADDRESS_FORMAT = /^(\w+\.)?\w+@(\w+\.)+[A-Za-z]+$/;
exports.PHONE_NUMBER_FORMAT = /^1\d{10}$/i;
exports.IMAGE_MIME_REGEX = /^image\/.*$/i;

exports.AUTO_START_HTTPD = true;
exports.RECEIVE_RECEIPT_TIMEOUT = 1000 * 30;

exports.MIN_EXPIRATION_TIME = 1000 * 60 * 1;

exports.BODY_BYTE_LENGTH = false;

exports.USER_AVATAR = exports.DOWNLOAD_URL_BASE + "/user-avatar.png";
