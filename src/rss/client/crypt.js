/**
 * Created with JetBrains WebStorm.
 * User: joebin
 * Date: 13-3-17
 * Time: 下午12:40
 */
var randomstring = require("randomstring");
var assert = require('assert');
var crypto = require('crypto');
//var rsaJson = require('rsa-json');
//
var utils = require(__dirname + '/../utils');

exports.rsaEncrypt = rsaEncrypt;
exports.rsaDecrypt = rsaDecrypt;
exports.desEncrypt = desEncrypt;
exports.desDecrypt = desDecrypt;
exports.makeAppKey = makeAppKey;
exports.makeRsaKeys = makeRsaKeys;
exports.makeMsgKey = makeMsgKey;

const ALGORITHM_DES = 'des-cbc'; // des-ede3-cbc, aes-128-ecb
const ENCODING = 'base64'; // binary, hex, or base64

const APPKEY_SIZE = 8;
const PROTECTKEY_SIZE = 8;
const MSGKEY_SIZE = 8;

/**
 * TODO RSA加密
 *
 * @param str
 * @param key
 * @param callback
 * @returns {string}
 */
function rsaEncrypt(str, key, callback) {

    return desEncrypt(str, key, callback);
}

/**
 * TODO RSA解密
 *
 * @param str
 * @param key
 * @param callback
 * @returns {string}
 */
function rsaDecrypt(str, key, callback) {

    return desDecrypt(str, key, callback);
}

/**
 * DES加密
 *
 * @param str
 * @param key
 * @param callback
 * @returns {string} 加密后数据格式: data_length(4B)+str+padding
 */
function desEncrypt(str, key, callback) {

    var dataLength = Buffer.byteLength(str);
    var paddingLength = (8 - (4 + dataLength) % 8) % 8;
    var buffer = new Buffer(8 + 4 + dataLength + paddingLength);
    buffer.fill(0, 0, 8);
    buffer.writeInt32BE(dataLength, 8);
    buffer.write(str, 8 + 4, dataLength);
    buffer.fill(0, 8 + 4 + dataLength);

    try {
        var cipher = crypto.createCipheriv(ALGORITHM_DES, key, '12345678');
        cipher.setAutoPadding(false);
        var cryptedData = cipher.update(buffer.toString('binary'), /*IN*/'binary', /*OUT*/ENCODING);
        cryptedData += cipher.final(ENCODING);
        if (callback) callback(null, cryptedData);
        else return cryptedData;
    } catch (e) {
        if (callback) return callback(e);
        return "";
    }
}

/**
 * DES解密
 *
 * @param str
 * @param key
 * @param callback
 * @returns {string}
 */
function desDecrypt(str, key, callback) {

    str = str.replace("\r", "");
    str = str.replace("\n", "");

    try {
        var decipher = crypto.createDecipheriv(ALGORITHM_DES, key, '12345678');
        decipher.setAutoPadding(false);
        var decryptedData = decipher.update(str, /*IN*/ENCODING, /*OUT*/'binary');
        decryptedData += decipher.final('binary');

        var buffer = new Buffer(decryptedData, 'binary');
        var length = buffer.readInt32BE(8); // 跳过多余的8个字符
        if (length > buffer.length - 8 - 4) throw "DES decrypt error";
        var result = buffer.slice(8 + 4, 8 + 4 + length).toString();

        if (callback) callback(null, result);
        else return result;
    } catch (e) {
        if (callback) return callback(e);
        return '';
    }
}

/**
 * 生成应用密钥
 *
 * @returns {string}
 */
function makeAppKey() {

    return randomstring.generate(APPKEY_SIZE);
}

/**
 * TODO 生成RSA密钥对
 */
function makeRsaKeys(handleResult) {
    // rsaJson({bits:1024}, handleResult);
    var key = randomstring.generate(PROTECTKEY_SIZE);
    handleResult(null, {public: key, private: key});
}

/**
 * 生成消息密钥
 *
 * @returns {string}
 */
function makeMsgKey() {

    return randomstring.generate(MSGKEY_SIZE);
}

function main(fn) {
    fn();
}

function test() {
    var key = "n9SfmcRs";
    var data = "15555215554,E7961F4A347DB79144BB21CE2427D53A";
    console.log("data:" + data);
    var en = rsaEncrypt(data, key);
    console.log("en:" + en);
    var de = rsaDecrypt(en, key);
    console.log("de:" + de);
    assert.equal(data, de);

    /*makeRsaKeys(function (err, keys) {
     if (err) return console.log(err);
     console.log(keys.public);
     console.log(keys.private);
     })*/
}

void main(function () {
    //test();
    //var key = makeMsgKey();
    //console.log("key: "+key);
});

