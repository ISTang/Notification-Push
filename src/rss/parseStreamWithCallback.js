/*!
 * node-feedparser
 * Copyright(c) 2013 Dan MacTough <danmactough@gmail.com>
 * MIT Licensed
 */

var FeedParser = require('feedparser')
  , fs = require('fs')
  , feed = 'feeds/rss2sample.xml';

FeedParser.parseStream(fs.createReadStream(feed), function (err, meta, articles) {
  if (err) return console.error(err);
  console.log('===== %s =====', meta.title);
  articles.forEach(function (article) {
    console.log('Got article: %s', article.title || article.description);
  });
});