var net = require("net");
var config = require(__dirname + '/config');
var utils = require(__dirname + '/utils');
var db = require(__dirname + '/db');
var channel = require(__dirname + '/channel');
var protocol = require(__dirname + '/client/protocol');
var crypt = require(__dirname + '/client/crypt');

//var $ = require('jquery');
var fs = require('fs');
var url = require("url");
var http = require("http");
var express = require('express');
var request = require('request');
var feedparser = require('feedparser');
var async = require('async');
var htmlparser = require("htmlparser");
var doT = require("dot");
//
var BufferHelper = require('bufferhelper');//用于拼接BUffer，防止中文单词断裂
var iconv = require('iconv-lite');  //用于转码

const PLATFORM_SERVER = config.PLATFORM_SERVER;
const PLATFORM_PORT = config.PLATFORM_PORT;
//
const APP_PORT = config.APP_PORT;
const RETRY_INTERVAL = config.RETRY_INTERVAL; // ms
//
const APP_ID = config.APP_ID;
const APP_PASSWORD = config.APP_PASSWORD;
const APP_PROTECTKEY = config.APP_PROTECTKEY;
//
const PLATFORM_USERNAME = config.PLATFORM_USERNAME;
const PLATFORM_PASSWORD = config.PLATFORM_PASSWORD;
//
const POLL_RSS_INTERVAL = config.POLL_RSS_INTERVAL;
const FETCH_TIMEOUT = config.FETECH_TIMEOUT;
//
const GRACE_EXIT_TIME = config.GRACE_EXIT_TIME;
//
const HTTPD_PORT = config.HTTPD_PORT;

const IMAGE_URL_NOT_CONATINS = config.IMAGE_URL_NOT_CONATINS;

const BODY_BYTE_LENGTH = config.BODY_BYTE_LENGTH;

const HTML_TEMPLATE = config.HTML_TEMPLATE;
const makeHtml = doT.template(HTML_TEMPLATE);

const TEXT_ARTICLE = config.TEXT_ARTICLE;

var webapp = express();

// 定义共享环境
webapp.configure(function () {
    webapp.engine('.html', require('ejs').__express);
    webapp.set('views', __dirname + '/views');
    webapp.set('view engine', 'html');

    webapp.use(express.logger());
    webapp.use(express.bodyParser()); // can't coexists with req.on(...)!
    webapp.use(express.methodOverride());
    webapp.use(webapp.router);
});
// 定义开发环境
webapp.configure('development', function () {
    webapp.use(express.static(__dirname + '/public'));
    webapp.use(express.errorHandler({dumpExceptions: true, showStack: true}));
    webapp.use(express.logger('dev'));
});
// 定义生产环境
webapp.configure('production', function () {
    var oneYear = 31557600000;
    webapp.use(express.static(__dirname + '/public', { maxAge: oneYear }));
    webapp.use(express.errorHandler());
});

webapp.set('env', 'development');

var redis;

Date.prototype.Format = utils.DateFormat;
String.prototype.trim = utils.StringTrim;
String.prototype.format = utils.StringFormat;

var logger = config.log4js.getLogger('app');
logger.setLevel(config.LOG_LEVEL);

function pushMessage(channel, msgTitle, msgBody, msgUrl, pubDate, logoUrl, imageUrls, callback) {

    var attachments = [];
    if (logoUrl) {
        attachments.push({title: "logo", type: 'image/xxx', filename: 'logo', url: logoUrl});
    }
    for (var imageIndex in imageUrls) {
        if (imageIndex != "each") {
            var imageUrl = imageUrls[imageIndex];
            attachments.push({title: "image" + imageIndex, type: 'image/xxx', filename: 'image' + imageIndex, url: imageUrl});
        }
    }
    var bodyText = JSON.stringify({user: {username: PLATFORM_USERNAME, password: utils.md5(PLATFORM_PASSWORD)}, type: TEXT_ARTICLE ? "text" : "html", title: (!msgTitle || msgTitle == "" ? channel : msgTitle), body: (msgBody == "" ? msgTitle : TEXT_ARTICLE ? msgBody : makeHtml({head: '', body: msgBody})),
        attachments: attachments, url: msgUrl, generate_time: pubDate, need_receipt: true});

    var options = url.parse("http://" + PLATFORM_SERVER + ":" + PLATFORM_PORT + "/application/" + APP_ID + "/message");
    options.method = "POST";
    options.headers = {
        'Content-Type': 'application/json;charset=utf-8',
        'Content-Length': Buffer.byteLength(bodyText)
    };
    var req = http.request(options, function (res) {
        res.setEncoding('utf8');
        var str = '';
        res.on('data', function (chunk) {
            str += chunk;
        });
        res.on('end', function () {
            var result = JSON.parse(str);
            if (result.success) callback();
            else callback(result.errmsg);
        });
    });
    req.on("error", function (err) {

        callback(err.message);
    });
    req.write(bodyText);
    req.end();
}

function getArticles(channel, url, since, handleResult) {

    if (!since) {

        since = utils.DateAdd("d", -7, new Date());
    }
    //var reqObj = {'uri': url,
    //    'headers': {'If-Modified-Since' : since.toString()/*,
    //     'If-None-Match' : <your cached 'etag' value>*/}};
    http.get(url,function (res) {

        if (res.statusCode != 200) {

            return handleResult("状态码: " + res.statusCode);
        }

        res.setTimeout(FETCH_TIMEOUT, function () {
            return handleResult("响应超时");
        });

        var bufferHelper = new BufferHelper();

        res.on('data', function (chunk) {

            //logger.trace("接收到数据\""+chunk+"\"");
            bufferHelper.concat(chunk);
        });
        res.on('end', function () {

            var text = bufferHelper.toBuffer();
            var encoding = utils.parseXmlEncoding(text);
            if (encoding != null) {
                try {
                    text = iconv.decode(text, encoding);
                } catch (e) {
                    logger.warn(e);
                }
            }

            //logger.trace("解析数据...");
            feedparser.parseString(text, function (err, meta, articles) {

                if (err) return handleResult(err);
                var logo = (meta.image.url ? meta.image.url : null);
                var result = [];
                async.forEachSeries(articles, function (article, callback) {
                    article.channel = channel;
                    article.logo = logo;
                    article.images = [];
                    result.push(article);

                    if (TEXT_ARTICLE) {
                        var handler = new htmlparser.DefaultHandler(function (error, dom) {
                            if (!error) {
                                article.description = "";
                                for (var i in dom) {
                                    if (dom[i].type == "text") {
                                        article.description += (dom[i].data || "");
                                    } else if (dom[i].type == "tag" && dom[i].name == "img") {
                                        var imageSrc = dom[i].attribs.src;
                                        if (imageSrc && checkImageUrl(imageSrc)) {
                                            article.images.push(dom[i].attribs.src);
                                        }
                                    }
                                    if (dom[i].children) {
                                        getImagesFromChildren(dom[i].children, article.images);
                                    }
                                }
                                function getImagesFromChildren(children, images) {
                                    for (var i in children) {
                                        if (children[i].type == "tag" && children[i].name == "img") {
                                            var imageSrc = children[i].attribs.src;
                                            if (imageSrc && checkImageUrl(imageSrc)) {
                                                images.push(children[i].attribs.src);
                                            }
                                        }
                                        if (children[i].children) {
                                            getImagesFromChildren(children[i].children, images);
                                        }
                                    }
                                }

                                function checkImageUrl(url) {
                                    for (var i in IMAGE_URL_NOT_CONATINS) {
                                        var word = IMAGE_URL_NOT_CONATINS[i];
                                        if (url.indexOf(word) != -1) return false;
                                    }
                                    return true;
                                }

                                article.description = (article.description || "").trim();
                                if (article.description == "null") article.description = "";
                                callback();
                            }
                        });
                        var parser = new htmlparser.Parser(handler);
                        parser.parseComplete(article.description);
                    } else {
                        callback();
                    }
                }, function (err) {

                    handleResult(err, result);
                });
            });
        });
    }).on('error', function (e) {

            handleResult(e.message);
        });
}

function getAllArticles(handleResult) {

    var now = new Date();
    db.getAllChannels(function (err, channles) {

        if (err) return handleResult(err);
        var result = [];
        async.forEachSeries(channles, function (channel, callback) {

            logger.trace("获取频道 " + channel.title + "(" + channel.url + ")上的文章...");
            getArticles(channel.title, channel.url, channel.since, function (err, articles) {
                if (err) {

                    logger.warn(err);

                    return callback(); // 忽略错误
                }
                logger.trace("从频道 " + channel.url + " 获取到 " + articles.length + " 篇文章。");
                async.forEachSeries(articles, function (article, callback) {

                    db.existsArticle(article, function (err, exists) {

                        if (err) return callback(err);

                        if (!exists) {

                            db.saveArticle(article, function (err) {

                                if (err) return callback(err);

                                result.push(article);

                                callback();
                            });
                        } else {

                            callback();
                        }
                    });
                }, function (err) {

                    if (!err) {

                        db.updateFetchTime(channel, now, callback);
                    } else {

                        callback(err);
                    }
                });
            });
        }, function (err) {

            handleResult(err, result);
        });
    });
}

function getAllArticlesAndPush() {

    logger.trace("获取最新文章并推送...");
    getAllArticles(function (err, articles) {

        if (err) return logger.error(err);

        logger.trace("本次共获取到 " + articles.length + " 篇新文章。");
        async.forEachSeries(articles, function (article, callback) {

            pushMessage(article.channel, article.title, article.description, article.link, article.pubDate, article.logo, article.images, callback);
        }, function (err) {

            if (err) logger.error(err);

            logger.trace("等待 " + (POLL_RSS_INTERVAL / 1000) + " 秒钟...");
            setTimeout(getAllArticlesAndPush, POLL_RSS_INTERVAL);
        });
    });
}

function getAppInfo() {
    return {id: APP_ID, password: APP_PASSWORD, protectKey: APP_PROTECTKEY};
}

/**
 * 格式化消息
 * @param receiver 消息接收者
 * @param msgId 消息ID
 * @param body 消息内容(JSON)
 * @param secure 消息是否已加密
 * @returns {string}
 */
function formatMessage(receiver, msgId, body, secure) {

    return protocol.SEND_MSG_REQ.format(receiver, msgId, secure ? "true" : "false", BODY_BYTE_LENGTH ? Buffer.byteLength(body) : body.length, body);
}

function startWorker(clientId, clientPassword, onConnected, onNewMessage) {

    var socket;
    var clientLogon = false; // 是否登录成功

    var clientLogging = false;

    var msgKey;
    var maxInactiveTime;

    var lastActiveTime;

    var keepAliveId = null;
    var ensureAliveId = null;

    function getUserInfo() {

        return {name: clientId, password: clientPassword};
    }

    function setMsgKey(key) {

        msgKey = key;
    }

    function setLogon() {
        onConnected(socket);

        clientLogon = true;
        clientLogging = false;
        lastActiveTime = new Date();

        ensureAliveId = setTimeout(ensureActive, maxInactiveTime);
    }

    function setKeepAliveInterval(n) {

        if (keepAliveId != null) {

            clearInterval(keepAliveId);
        }

        maxInactiveTime = n * 3;

        keepAliveId = setInterval(function () {

            if (!clientLogon) return;

            var diff = new Date().getTime() - lastActiveTime.getTime();
            if (diff >= n) {

                logger.debug("[" + clientId + "] Keeping alive...");
                socket.write(protocol.SET_ALIVE_REQ);
            }
        }, n);
    }

    function keepAlive() {

        logger.debug("[" + clientId + "] Server still alive");
        lastActiveTime = new Date();
    }

    function msgReceived(msg, secure) {

        lastActiveTime = new Date();

        if (secure) {
            crypt.desDecrypt(msg, msgKey, function (err, data) {
                if (err) return logger.error("[" + clientId + "] " + err);
                var msgObj = JSON.parse(data);
                onNewMessage(socket, msgObj);
                logger.debug("[" + clientId + "] " + msgObj.generate_time + " " + (msgObj.title != null ? msgObj.title : "---") + ": " + msgObj.body);
            });
        } else {
            var msgObj = JSON.parse(msg);
            onNewMessage(socket, msgObj);
            logger.debug("[" + clientId + "] " + msgObj.generate_time + " " + (msgObj.title != null ? msgObj.title : "---") + ": " + msgObj.body);
        }
    }

    function connectToServer() {

        if (clientLogon || clientLogging) {

            return;
        }

        clientLogging = true;

        logger.debug("[" + clientId + "] Connecting..." + PLATFORM_SERVER + "[" + APP_PORT + "]");
        socket = net.createConnection(APP_PORT, PLATFORM_SERVER);

        protocol.handleServerConnection(socket, clientId, getAppInfo, getUserInfo, setMsgKey, setLogon, setKeepAliveInterval, keepAlive, msgReceived,
            handleError, handleClose, logger);

        function handleClose() {

            if (keepAliveId != null) {

                clearInterval(keepAliveId);
                keepAliveId = null;
            }

            if (ensureAliveId != null) {

                clearInterval(ensureAliveId);
                ensureAliveId = null;
            }

            if (clientLogon) {

                logger.debug("[" + clientId + "] Disconnected");
                clientLogon = false;
            }

            if (clientLogging) {

                clientLogging = false;
            }
        }

        function handleError() {

            logger.error(">>>[" + clientId + "] Network error<<<");
        }
    }

    function ensureActive() {

        if (!clientLogon) return;

        var diff = (new Date().getTime() - lastActiveTime.getTime()); //ms
        if (diff >= maxInactiveTime) {

            logger.warn("[" + clientId + "] Server inactive timeout");

            socket.end(protocol.CLOSE_CONN_RES.format(protocol.INACTIVE_TIMEOUT_MSG.length, protocol.INACTIVE_TIMEOUT_MSG));
            return;
        }
        ensureAliveId = setTimeout(ensureActive, maxInactiveTime);
    }

    connectToServer();
    setInterval(connectToServer, RETRY_INTERVAL);
}

var exitTimer = null;
function aboutExit() {

    if (exitTimer) return;

    exitTimer = setTimeout(function () {
        process.exit(0);

    }, GRACE_EXIT_TIME);
}

function main(fn) {
    fn();
}

void main(function () {
    process.on('SIGINT', aboutExit);
    process.on('SIGTERM', aboutExit);

    // 解析 OPML 并增加频道
    // curl --header "Content-Type:application/json;charset=utf-8" -d "{\"url\":\"http://www.cctv.com/rss/01/index.xml\"}" http://localhost:5678/opml
    webapp.post('/opml', function (req, res) {

        var opmlUrl = req.body.url;
        logger.debug("OPML: " + opmlUrl);
        channel.getChannelsFromOpml(opmlUrl, function (err, channels) {

            if (err) {
                return res.json({success: false, errcode: 1, errmsg: err});
            }

            db.addChannels(channels, function (err) {

                if (!err) {
                    res.json({success: true, count: channels.length});
                } else {
                    res.json({success: false, errcode: 2, errmsg: err});
                }
            });
        });
    });

    // 手动增加频道
    // curl --header "Content-Type:application/json;charset=utf-8" -d "{\"group\":\"网易新闻\",\"title\":\"头条新闻\",\"description\":\"[网易新闻]头条新闻\",\"url\":\"http://news.163.com/special/00011K6L/rss_newstop.xml\"}" http://localhost:5678/channel
    webapp.post('/channel', function (req, res) {

        var channel = req.body;
        db.addChannel(channel, function (err) {

            if (!err) {
                res.json({success: true});
            } else {
                res.json({success: false, errcode: 1, errmsg: err});
            }
        });
    });

    // 删除频道
    // curl -X DELETE http://localhost:5678/channel/408B8FC7-0937-4EF3-893C-982AD6461F6A
    webapp.delete('/channel/:id', function (req, res) {

        var channelId = req.params.id;
        db.removeChannel(channelId, function (err) {

            if (!err) {
                res.json({success: true});
            } else {
                res.json({success: false, errcode: 1, errmsg: err});
            }
        });
    });

    // 修改频道
    // curl --header "Content-Type:application/json;charset=utf-8" -X PUT -d "{\"description\":\"ddd\"}" http://localhost:5678/channel/408B8FC7-0937-4EF3-893C-982AD6461F6A
    webapp.put('/channel/:id', function (req, res) {

        var channelId = req.params.id;
        var channel = req.body;
        db.updateChannel(channelId, channel, function (err) {

            if (!err) {
                res.json({success: true});
            } else {
                res.json({success: false, errcode: 1, errmsg: err});
            }
        });
    });

    // 获取所有频道
    // curl http://localhost:5678/channels
    webapp.get('/channels', function (req, res) {

        db.getAllChannels(function (err, channels) {

            if (!err) {
                res.json({success: true, channels: channels});
            } else {
                res.json({success: false, errcode: 1, errmsg: err});
            }
        });
    });

    webapp.listen(HTTPD_PORT);
    logger.debug('服务器正在监听端口 ' + HTTPD_PORT + '...');

    // 启动消息推送客户端
    startWorker(PLATFORM_USERNAME, PLATFORM_PASSWORD, function (socket) {

        // 定时获取文章并推送
        getAllArticlesAndPush();

    }, function (socket, msgObj) {

        // 处理新消息
        logger.debug(JSON.stringify(msgObj));
        if (msgObj.title == null || "" == msgObj.title) {
            // 消息无标题
            switch (msgObj.body) {
                case 'FOLLOWED_EVENT':
                    // 关注事件
                    var followMsg = JSON.stringify({body: 'Hi，您已关注我，精彩资讯马上来！\r\n回复"订阅"可以定制感兴趣的频道。', generate_time: new Date()});
                    socket.write(formatMessage(msgObj.sender_name, "FOLLOWED_MSG", followMsg, false));
                    break;
                case 'UNFOLLOWED_EVENT':
                    // 取消关注事件
                    var unfollowMsg = JSON.stringify({body: '您已取消关注，祝您生活愉快！', generate_time: new Date()});
                    socket.write(formatMessage(msgObj.sender_name, "UNFOLLOWED_MSG", unfollowMsg, false));
                    break;
                case "订阅":
                    // 订阅请求
                    db.getAllChannels(function (err, channels) {

                        if (err) return logger.error(err);

                        var scripts = "<script language=\"javascript\">"
                            + "    function doSubmit() {"
                            + "      var result = \"\";"
                            + "      for(var i=0;i<document.form.channelIds.length;i++){"
                            + "      if(document.form.channelIds[i].checked){"
                            + "        if (result!=\"\") result += \",\";"
                            + "        result += document.form.channelIds[i].value;"
                            + "      }"
                            + "      alert(result);android.sendMessage(\"" + PLATFORM_USERNAME + "\",result);"
                            + "    }"
                            + "</script>";
                        //
                        var checkItems = "<form name=\"form\">请选择感兴趣的频道：<br/>";
                        for (var i = 0; i < channels.length; i++) {
                            var channel = channels[i];
                            var checkItem = "<input id=\"" + i + "\" type=\"checkbox\" name=\"channelIds\" value=\"" + channel.id + "\"/>"
                                + "<label for=\"" + i + "\">" + channel.title + "</label><br/>";
                            checkItems += checkItem;
                        }
                        checkItems += "<br/>&nbsp;&nbsp;<a onClick=\"doSubmit()\">立即订阅</a></form>";
                        //
                        var channelsMsg = JSON.stringify({type: 'html', body: makeHtml({head: scripts, body: checkItems}), generate_time: new Date()});
                        socket.write(formatMessage(msgObj.sender_name, "CHANNELS_MSG", channelsMsg, false));
                    });
                    break;
                default:
                    break;
            }
        } else {
            // 消息有标题
        }
    });
});
