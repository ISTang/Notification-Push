var path = require("path");

exports.SERVER_HOST = '192.168.1.103'; // 218.200.212.120
exports.SERVER_PORT = 3456;
exports.HTTPD_PORT = 4567;

exports.LOGIN_NUMBER = 5; // 5
exports.NOTIFY_NUMBER = 2; // 2

exports.LOGIN_TIMEOUT = 1000 * 60; // ms
exports.MAX_INACTIVE_TIME = 1000 * 60 * 5 * 1; // 心跳检测最长时间(ms)
exports.KEEPALVE_INIIALDELAY = 1000 * 30; // keepalive 首次探测时延(ms)
exports.KEEPALVE_PROBEINTERVAL = 1000 * 30; // keepalive 探测间隔时间(ms)--0表示不探测
exports.KEEPALVE_FAILURECOUNT = 2; // keepalive 探测失败最大次数(ms)

exports.GRACE_EXIT_TIME = 1500; // ms

exports.MESSAGE_SEND_RATE = 2; // per min

exports.NEWLINE = "\r\n";
exports.CLIENT_OFFLINE_MSG = "Are you offline?";
exports.BROARDCAST_MSG = "You have a new message.";
exports.SERVER_ERROR_MSG = "Server error!";

exports.LOG_ENABLED = true;

exports.REDIS_SERVER = "localhost";
exports.REDIS_PORT = 6379;

exports.SHOW_PACKET = true;

exports.CREATE_ACCOUNT_LOCK_FILE = 'create-account.lock';

exports.RANDOM_ACCOUNTNAME_SIZE = 8;

exports.MAX_OFFLINE_DAYS = 3;

exports.UPLOAD_DIR = path.join(__dirname,"public", "files");
exports.DOWNLOAD_URL_BASE = "http://"+exports.SERVER_HOST+":"+exports.HTTPD_PORT+"/files/";
exports.MAX_ATTACHMENT_COUNT = 4;

exports.EMAIL_ADDRESS_FORMAT = /^(\w+\.)?\w+@(\w+\.)+[A-Za-z]+$/;
exports.PHONE_NUMBER_FORMAT = /^1\d{10}$/i;
exports.IMAGE_MIME_REGEX = /^image\/.*$/i;

exports.AUTO_START_HTTPD = false;
exports.RECEIVE_RECEIPT_TIMEOUT = 1000 * 60;

exports.MIN_EXPIRATION_TIME = 1000 * 60 * 1;

exports.BODY_BYTE_LENGTH = true;
