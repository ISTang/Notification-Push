var path = require("path");
var log4js = require('log4js');
log4js.clearAppenders();
log4js.loadAppender('file');
log4js.configure({
    appenders: [
        { type: 'file', filename: 'logs/app.log', "maxLogSize": 10485760, category: 'app' }
    ]
});
exports.log4js = log4js;
exports.LOG_LEVEL = 'DEBUG';

exports.REDIS_SERVER = 'localhost';
exports.REDIS_PORT = 7379;

exports.HTTPD_PORT = 4568;

exports.PLATFORM_SERVER = "127.0.0.1";
exports.PLATFORM_PORT = 2345;
exports.APP_PORT = 1234;
exports.RETRY_INTERVAL = 1000; // ms
//
exports.APP_ID = "4083AD3D-0F41-B78E-4F5D-F41A515F2667";
exports.APP_PASSWORD = "@0Vd*4Ak";
exports.APP_PROTECTKEY = {public: "n9SfmcRs", private: "n9SfmcRs"};
//
exports.PLATFORM_USERNAME = 'autorss';
exports.PLATFORM_PASSWORD = 'gzdx342';

exports.POLL_RSS_INTERVAL = 1000*60*3;
exports.FETECH_TIMEOUT = 1000*30;

exports.GRACE_EXIT_TIME = 1500;

exports.IMAGE_URL_NOT_CONATINS = ['email','facebook','linkedin','googleplus','twitter','mf','a2','a2t', 'emailthis2', 'bookmark'];

exports.MAX_INACTIVE_TIME = 1000 * 60 * 60 * 24; // 心跳检测最长时间(ms)

exports.TRACK_SOCKET = true;
exports.SHOW_PACKET = true;

exports.BODY_BYTE_LENGTH = false;

exports.HTML_TEMPLATE = "<head><meta charset=\"UTF-8\">{{=it.head}}</head><body>{{it.body}}</body>";
