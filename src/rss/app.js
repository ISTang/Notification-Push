var config = require(__dirname + '/config');
var utils = require(__dirname + '/utils');
var db = require(__dirname + '/db');
var channel = require(__dirname + '/channel');

//var $ = require('jquery');
var fs = require('fs');
var url = require("url");
var http = require("http");
var express = require('express');
var request = require('request');
var feedparser = require('feedparser');
var async = require('async');
var htmlparser = require("htmlparser");
//
var BufferHelper = require('bufferhelper');//用于拼接BUffer，防止中文单词断裂
var iconv = require('iconv-lite');  //用于转码

const PLATFORM_SERVER = config.PLATFORM_SERVER;
const PLATFORM_PORT = config.PLATFORM_PORT;
const PLATFORM_USERNAME = 'autorss';
const PLATFORM_PASSWORD = 'gzdx342';
const APP_ID = "4083AD3D-0F41-B78E-4F5D-F41A515F2667";
//
const POLL_RSS_INTERVAL = config.POLL_RSS_INTERVAL;
const FETCH_TIMEOUT = config.FETECH_TIMEOUT;
//
const GRACE_EXIT_TIME = config.GRACE_EXIT_TIME;
//
const HTTPD_PORT = config.HTTPD_PORT;
//
const LOG_ENABLED = config.LOG_ENABLED;

const IMAGE_URL_NOT_CONATINS = config.IMAGE_URL_NOT_CONATINS;

var webapp = express();

// 定义共享环境
webapp.configure(function () {
    webapp.engine('.html', require('ejs').__express);
    webapp.set('views', __dirname + '/views');
    webapp.set('view engine', 'html');

    webapp.use(express.logger());
    webapp.use(function (req, res, next) {
        /*var data = '';
         req.setEncoding('utf-8');
         req.on('data', function (chunk) {
         data += chunk;
         });
         req.on('end', function () {
         req.rawBody = data;
         next();
         });*/
        //res.setHeader("Content-Type", "application/json;charset=utf-8");
        next();
    });
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

var logStream = fs.createWriteStream("logs/app.log", {"flags": "a"});

var redis;

Date.prototype.Format = utils.DateFormat;
String.prototype.trim = utils.StringTrim;
String.prototype.format = utils.StringFormat;

function log(msg) {

    var now = new Date();
    var strDatetime = now.Format("yyyy-MM-dd HH:mm:ss");
    var buffer = "[" + strDatetime + "] " + msg + "[app]";
    if (logStream != null) logStream.write(buffer + "\r\n");
    if (LOG_ENABLED) console.log(buffer);
}

function stripsTags(text)
{
  return $.trim($('<div>').html(text).text());
}

function pushMessage(channel, msgTitle, msgBody, msgUrl, pubDate, logoUrl, imageUrls, callback) {

    var attachments = [];
    if (logoUrl) {
       	attachments.push({title: "logo", type: 'image/xxx', filename: 'logo', url: logoUrl});
    }
    for (var imageIndex in imageUrls) {
	if (imageIndex!="each") {
		var imageUrl = imageUrls[imageIndex];
		attachments.push({title: "image"+imageIndex, type: 'image/xxx', filename: 'image'+imageIndex, url: imageUrl});
	}
    }
    var bodyText = JSON.stringify({user:{username:PLATFORM_USERNAME,password:PLATFORM_PASSWORD}, title: (!msgTitle||msgTitle==""?channel:msgTitle), body: (msgBody==""?msgTitle:msgBody),
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

        res.setTimeout(FETCH_TIMEOUT, function() {
            return handleResult("响应超时");  
        });

        var bufferHelper = new BufferHelper();

        res.on('data', function (chunk) {

            //log("接收到数据\""+chunk+"\"");
            bufferHelper.concat(chunk);
        });
        res.on('end', function () {

            var text = bufferHelper.toBuffer();
            var encoding = utils.parseXmlEncoding(text);
            if (encoding != null) {
                try {
                    text = iconv.decode(text, encoding);
                } catch(e) {
                    log(e);
                } 
            }

            //log("解析数据...");
            feedparser.parseString(text, function (err, meta, articles) {

                if (err) return handleResult(err);
                var logo = (meta.image.url ? meta.image.url : null);
                var result = [];
                async.forEachSeries(articles, function (article, callback) {
		    article.channel = channel;
                    article.logo = logo;
		    article.images = [];
                    result.push(article);

		    var handler = new htmlparser.DefaultHandler(function (error, dom) {
		      if (!error) {
				article.description = "";
				for (var i in dom) {
					if (dom[i].type=="text") {
						article.description += (dom[i].data||"");
					} else if (dom[i].type=="tag" && dom[i].name=="img") {
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
						if (children[i].type=="tag" && children[i].name=="img") {
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
						if (url.indexOf(word)!=-1) return false;
					}
					return true;
				}
				article.description = (article.description||"").trim();
				if (article.description=="null") article.description = "";
				callback();
			}
		    });
		    var parser = new htmlparser.Parser(handler);
		    parser.parseComplete(article.description);
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

            log("获取频道 "+channel.title+"("+channel.url+")上的文章...");
            getArticles(channel.title, channel.url, channel.since, function (err, articles) {
                if (err) {

                    log(err);

                    return callback(); // 忽略错误
                }
                log("从频道 "+channel.url+" 获取到 "+articles.length+" 篇文章。");
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

    log("获取最新文章并推送...");
    getAllArticles(function (err, articles) {

        if (err) return log(err);

        log("本次共获取到 " + articles.length + " 篇新文章。");
        async.forEachSeries(articles, function (article, callback) {

            pushMessage(article.channel, article.title, article.description, article.link, article.pubDate, article.logo, article.images, callback);
        }, function (err) {

            if (err) log(err);

            log("等待 " + (POLL_RSS_INTERVAL / 1000) + " 秒钟...");
            setTimeout(getAllArticlesAndPush, POLL_RSS_INTERVAL);
        });
    });
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
		log("OPML: " + opmlUrl);
		channel.getChannelsFromOpml(opmlUrl, function (err, channels) {

			if (err) {
				return res.json({success: false,errcode: 1,errmsg: err});
			}

			db.addChannels(channels, function (err) {

				if (!err) {
					res.json({success: true,count: channels.length});
				} else {
					res.json({success: false,errcode: 2,errmsg: err});
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
				res.json({success: false,errcode: 1,errmsg: err});
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
				res.json({success: false,errcode: 1,errmsg: err});
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
				res.json({success: false,errcode: 1,errmsg: err});
			}
		});
	});

	// 获取所有频道
	// curl http://localhost:5678/channels
	webapp.get('/channels', function (req, res) {

		db.getAllChannels(function (err, channels) {

			if (!err) {
				res.json({success: true, channels:channels});
			} else {
				res.json({success: false,errcode: 1,errmsg: err});
			}
		});
	});

	webapp.listen(HTTPD_PORT);
	log('服务器正在监听端口 ' + HTTPD_PORT + '...');

	//log("等待 " + (POLL_RSS_INTERVAL / 1000) + " 秒钟...");
	/*setTimeout(*/getAllArticlesAndPush()/*, POLL_RSS_INTERVAL)*/;
});
