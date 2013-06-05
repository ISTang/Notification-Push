// 引入依赖包
var child_process = require('child_process');

for (var i=100000; i<100001; i++) {

    var child = child_process.fork('client.js', [i.toString(), i.toString()]);
}
