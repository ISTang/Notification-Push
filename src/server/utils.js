/**
 * Created with JetBrains WebStorm.
 * User: joebin
 * Date: 13-3-15
 * Time: 下午6:56
 */
var crypto = require('crypto');
var fs = require('fs');
//var randomstring = require("randomstring");

// 导出函数
exports.DateFormat = DateFormat;
exports.DateParse = DateParse;
exports.DateParse2 = DateParse2;
exports.StringFormat = StringFormat;
exports.StringTrim = StringTrim;
exports.md5 = md5;
exports.makeFileChecksum = makeFileChecksum;

const DATE_FORMAT_REGEX = /(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})/g;
const DATE_FORMAT_REGEX2 = /(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})/g;

// 对Date的扩展，将 Date 转化为指定格式的String
// 月(M)、日(d)、小时(h)、分(m)、秒(s)、季度(q) 可以用 1-2 个占位符，
// 年(y)可以用 1-4 个占位符，毫秒(S)只能用 1 个占位符(是 1-3 位的数字)
// 例子：
// (new Date()).Format("yyyy-MM-dd hh:mm:ss.S") ==> 2006-07-02 08:09:04.423
// (new Date()).Format("yyyy-M-d h:m:s.S")			==> 2006-7-2 8:9:4.18
function DateFormat(fmt) { //author: meizz
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

function DateParse(str) {
    var fields = str.split(DATE_FORMAT_REGEX);
    var date = new Date(parseInt(fields[1]), parseInt(fields[2])-1, parseInt(fields[3]),
        parseInt(fields[4]), parseInt(fields[5]), parseInt(fields[6]));
    return date;
}

function DateParse2(str) {
    var fields = str.split(DATE_FORMAT_REGEX2);
    var date = new Date(parseInt(fields[1]), parseInt(fields[2])-1, parseInt(fields[3]),
        parseInt(fields[4]), parseInt(fields[5]), parseInt(fields[6]));
    return date;
}


function StringFormat() {
    var args = arguments;
    return this.replace(/\{(\d+)\}/g,
        function (m, i) {
            return args[i];
        });
}

function StringTrim() {
    return this.replace(/(^\s*)|(\s*$)/g, "");
}

function md5(str) {
    var md5sum = crypto.createHash('md5');
    md5sum.update(str);
    str = md5sum.digest('hex').toUpperCase();
    return str;
}

function makeFileChecksum(file, algo, handleResult) {
    if (!algo) algo = 'md5';

    var shasum = crypto.createHash(algo);

    var s = fs.ReadStream(file);
    s.on('data', function(d) { shasum.update(d); });
    s.on('end', function() {
        var d = shasum.digest('hex');
        handleResult(d);
    });
}

function main(fn) {
    fn();
}

void main(function () {

    //var guid = Guid.Empty;//NewGuid();
    //console.log("GUID: "+guid.toString("N"));

    //console.log("random string: "+randomstring.generate(8));

    /*var a = new Date();  var aa=a.getTime();
    var b = DateParse("20130327225800"); var bb= b.getTime();
    console.log(aa);
    console.log(bb);
    console.log(''+a+','+b+','+(a-b)/1000);*/

    /*makeFileChecksum("server.js", "md5", function (checksum) {
        console.log("checksum: "+checksum);
    });*/

    Date.prototype.DateFormat = DateFormat;

    var str = "2013-06-07 09:20:32";
    var date = DateParse2(str);
    console.log(str);
    console.log(date.DateFormat("yyyy-MM-dd HH:mm:ss"));

    var nowStr = "2013-06-07 09:16:06"
    var now = DateParse2(nowStr);
    console.log(nowStr);
    console.log(now.DateFormat("yyyy-MM-dd HH:mm:ss"));

    var diff = date.getTime()-now.getTime();
    var MIN_EXPIRATION_TIME = 1000 * 60 * 1;
    console.log(diff, diff/1000);
    if (diff<MIN_EXPIRATION_TIME) {
        log("Error");
    }
});
