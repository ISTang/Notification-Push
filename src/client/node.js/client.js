var net = require("net");

const SERVER_HOST = "218.200.212.120"; //118.244.9.191
const SERVER_PORT = 3456;
const NEWLINE = "\r\n";
const MAX_INACTIVE_TIME = 1000 * 60; // ms
const RETRY_INTERVAL = MAX_INACTIVE_TIME / 3; // ms

// 文字常量
const SERVER_BUSY_MSG = "Server busy";
const ID_PROMPT = "Your ID: ";
const PASSWORD_PROMPT = "Your password: ";
const INVALID_ID_MSG = "Invalid ID. Your ID(6 digits): ";
const INVALID_PASSWORD_MSG = "Invalid password. Your password: ";
const WAIT_NOTIFY_MSG = "Waiting please...";
const KEEP_ALIVE_MSG = "I'm alive";
const CLIENT_OFFLINE_MSG = "Are you offline?";

// 错误代码
const ERROR_INVALID_CLIENTINFO = 1;

var client;
var clientId = (process.argv.length >= 4 ? process.argv[2] : "111111"); // 客户ID
var clientPassword = (process.argv.length >= 4 ? process.argv[3] : "111111"); // 客户密码
var clientLogon = false; // 是否登录成功

// 对Date的扩展，将 Date 转化为指定格式的String
// 月(M)、日(d)、小时(h)、分(m)、秒(s)、季度(q) 可以用 1-2 个占位符， 
// 年(y)可以用 1-4 个占位符，毫秒(S)只能用 1 个占位符(是 1-3 位的数字) 
// 例子： 
// (new Date()).Format("yyyy-MM-dd hh:mm:ss.S") ==> 2006-07-02 08:09:04.423 
// (new Date()).Format("yyyy-M-d h:m:s.S")      ==> 2006-7-2 8:9:4.18 
Date.prototype.Format = function (fmt) { //author: meizz 
    var o = {
        "M+": this.getMonth() + 1, //月份 
        "d+": this.getDate(), //日 
        "H+": this.getHours(), //小时 
        "m+": this.getMinutes(), //分 
        "s+": this.getSeconds(), //秒 
        "q+": Math.floor((this.getMonth() + 3) / 3), //季度 
        "S": this.getMilliseconds() //毫秒 
    };
    if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var k in o)
		if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
    return fmt;
}

/*
 * Print log
 */
function log(msg) {

    var now = new Date();
    var strDatetime = now.Format("yyyy-MM-dd HH:mm:ss");  ;
    var buffer = "[" + strDatetime + "] " + msg;
    console.log(buffer);
}

function main(fn){
	fn();
}

/*
 * Cleans the input of carriage return, newline
 */
function cleanInput(data) {
    return data.replace(/(\r\n|\n|\r)/g, "");
}

function connectToServerOrKeepAlive() {

    if (clientLogon) {
	
		//log("Keep alive");
        client.write(KEEP_ALIVE_MSG + NEWLINE);
		return;
	}

    log("Connecting..." + SERVER_HOST + "[" + SERVER_PORT + "]");
    client = net.createConnection(SERVER_PORT, SERVER_HOST);

    client.on('data', function (data) {

        var cleanedData = cleanInput(data.toString());

        switch (cleanedData) {
            case SERVER_BUSY_MSG:
                log("Server busy");
                break;
            case ID_PROMPT:
                log("Sending client ID: " + clientId);
                client.write(clientId + NEWLINE);
                break;
            case INVALID_ID_MSG:
                log("Invalid client ID: " + clientPassword);
                invalidClientInfo = true;
                process.exit(ERROR_INVALID_CLIENTINFO);
                break;
            case PASSWORD_PROMPT:
                log("Sending client password: " + clientPassword);
                client.write(clientPassword + NEWLINE);
                break;
            case INVALID_PASSWORD_MSG:
                log("Invalid client password: " + clientPassword);
                process.exit(ERROR_INVALID_CLIENTINFO);
                break;
            case WAIT_NOTIFY_MSG:
                log("I logon");
                clientLogon = true;
                break;
            case CLIENT_OFFLINE_MSG:
                log("Server kickoff me");
                clientLogon = false;
                break;
            default:
                log("Received text '" + cleanedData + "' from server");
                break;
        }
    });

    client.on("end", function () {

        if (clientLogon) {

            log("Disconnected");
            clientLogon = false;
        }
    });

    client.on("error", function () {

        if (clientLogon) {

            log("Network error");
            clientLogon = false;
        }
    });
}

void main(function(){
	
		connectToServerOrKeepAlive();
		setInterval(connectToServerOrKeepAlive, RETRY_INTERVAL);
});

