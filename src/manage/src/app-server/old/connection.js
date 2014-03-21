/**
 * Created with JetBrains WebStorm.
 * User: joebin
 * Date: 13-8-1
 * Time: 下午3:33
 */
var fs = require('fs');
var config = require(__dirname + '/../config');
var db = require(__dirname + '/../db');

// 导出函数
exports.getConnections = getConnections;

// 定义常量
//

var logger = config.log4js.getLogger('connection');
logger.setLevel(config.LOG_LEVEL);

function getConnections(req, res) {
    logger.trace('Get connections: ' +
        'sEcho=' + req.query.sEcho +
        ', sSearch=' + req.query.sSearch +
        ', iDisplayLength=' + req.query.iDisplayLength +
        ', iDisplayStart=' + req.query.iDisplayStart +
        ', iColumns=' + req.query.iSortingCols +
        ', sColumns=' + req.query.sColumns +
        ''
    );

    var iSortCol_0 = req.query.iSortCol_0;
    var sSortDir_0 = req.query.sSortDir_0;

    db.redisPool.acquire(function (err, redis) {
        if (err) {
            res.json({sEcho: parseInt(req.query.sEcho, 10), iTotalRecords: 1,
                iTotalDisplayRecords: 1, aaData: [
                    ['无法访问数据库: ' + err]
                ],
                sColumns: "connId"});
        } else {
            db.getAllConnections(redis, function (err, connections) {

                db.redisPool.release(redis);

                if (err) {
                    return res.json({sEcho: parseInt(req.query.sEcho, 10), iTotalRecords: 1,
                        iTotalDisplayRecords: 1, aaData: [
                            ['数据库操作失败: ' + err]
                        ],
                        sColumns: "connId"});
                }

                var filtered = [];
                if (req.query.sSearch != "") {
                    // 过滤
                    for (var i in connections) {
                        var connection = connections[i];
                        if (connection.connId.indexOf(req.query.sSearch) != -1 ||
                            connection.clientAddress.indexOf(req.query.sSearch) != -1 ||
                            connection.accountInfo.indexOf(req.query.sSearch) != -1 ||
                            connection.applicationName.indexOf(req.query.sSearch) != -1 ||
                            connection.beginTime.indexOf(req.query.sSearch) != -1 ||
                            connection.duration.indexOf(req.query.sSearch) != -1 ||
                            connection.latestActivity.indexOf(req.query.sSearch) != -1) {
                            filtered.push(connection);
                        }
                    }
                } else {
                    // 不需要过滤
                    filtered = connections;
                }

                // 排序
                filtered.sort(function (x, y) {
                    switch (parseInt(iSortCol_0, 10)) {
                        case 0: // connId
                            return compareString(x.connId, y.connId);
                        case 1: // clientAddress
                            return compareString(x.clientAddress, y.clientAddress);
                        case 2: // accountInfo
                            return compareString(x.accountInfo, y.accountInfo);
                        case 3: // applicationName
                            return compareString(x.applicationName, y.applicationName);
                        case 4: // beginTime
                            return compareString(x.beginTime, y.beginTime);
                        case 5: // duration
                            return compareString(x.duration, y.duration);
                        case 6: // latestActivity
                            return compareString(x.latestActivity, y.latestActivity);
                    }
                });

                // 分页
                var iDisplayStart = parseInt(req.query.iDisplayStart, 10);
                var iDisplayLength = parseInt(req.query.iDisplayLength, 10);
                var paged = filtered.slice(iDisplayStart, Math.min(iDisplayStart + iDisplayLength, filtered.length));
                var result = [];
                for (var i in paged) {
                    var connection = paged[i];
                    result.push([connection.connId, connection.clientAddress, connection.accountInfo, connection.applicationName,
                        connection.beginTime, connection.duration, connection.latestActivity]);
                }

                return res.json({sEcho: parseInt(req.query.sEcho, 10), iTotalRecords: connections.length,
                    iTotalDisplayRecords: filtered.length, aaData: result,
                    sColumns: "connId,clientAddress,accountInfo,applicationName,beginTime,duration,latestActivity"});
            });
        }
    });

    function compareString(s1, s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return (sSortDir_0 == 'asc' ? 0 : 1);
        if (s2 == null) return (sSortDir_0 == 'asc' ? 1 : 0);

        return (sSortDir_0 == 'asc' ? s1.localeCompare(s2) : s2.localeCompare(s1));
    }
}
