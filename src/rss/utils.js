/**
 * Created with JetBrains WebStorm.
 * User: joebin
 * Date: 13-6-5
 * Time: 下午8:45
 * To change this template use File | Settings | File Templates.
 */
var uuid = require('node-uuid');

exports.DateFormat = DateFormat;
exports.DateAdd = DateAdd;
exports.StringFormat = StringFormat;
exports.StringTrim = StringTrim;
exports.parseXmlEncoding = parseXmlEncoding;

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

function DateAdd(interval,number,date)
{
    /*
     *--------------- DateAdd(interval,number,date) -----------------
     * DateAdd(interval,number,date)
     * 功能:实现VBScript的DateAdd功能.
     * 参数:interval,字符串表达式，表示要添加的时间间隔.
     * 参数:number,数值表达式，表示要添加的时间间隔的个数.
     * 参数:date,时间对象.
     * 返回:新的时间对象.
     * var now = new Date();
     * var newDate = DateAdd("d",5,now);
     *--------------- DateAdd(interval,number,date) -----------------
     */
    switch(interval)
    {
        case "y" : {
            date.setFullYear(date.getFullYear()+number);
            return date;
            break;
        }
        case "q" : {
            date.setMonth(date.getMonth()+number*3);
            return date;
            break;
        }
        case "m" : {
            date.setMonth(date.getMonth()+number);
            return date;
            break;
        }
        case "w" : {
            date.setDate(date.getDate()+number*7);
            return date;
            break;
        }
        case "d" : {
            date.setDate(date.getDate()+number);
            return date;
            break;
        }
        case "h" : {
            date.setHours(date.getHours()+number);
            return date;
            break;
        }
        case "m" : {
            date.setMinutes(date.getMinutes()+number);
            return date;
            break;
        }
        case "s" : {
            date.setSeconds(date.getSeconds()+number);
            return date;
            break;
        }
        default : {
            date.setDate(d.getDate()+number);
            return date;
            break;
        }
    }
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

function parseXmlEncoding(xmlText) {
    var reg = /\s*<\?xml\s*version\s*=\s*"1\.0"\s*encoding\s*=\s*"(.+?)"\s*\?>.*/gm;
    var arr = reg.exec(xmlText);
    return (arr?arr[1]:null);
}

function main(fn) {
    fn();
}

void main(function () {
    //console.log(uuid.v4().toUpperCase());

    //console.log(parseXmlEncoding(" <?xml  version=\"1.0\"  encoding=\"GB2312\" ?>\r\nddd"));
});
