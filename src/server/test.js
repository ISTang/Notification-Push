/**
 * Created by istang on 14-2-23.
 */

const P_PATH = __dirname + '/p';

var child_process = require('child_process');

var p;
function start() {

    fork();

    function onError(err) {

        console.log("ERROR");

        // 无法向HTTP进程发送消息
        console.log("ERROR:"+err.toString());
        if (p) {
            console.log(this.x);

            //this.kill();
            //fork();
        }
    }

    function onExit(code, signal) {

        console.log(this.x);

        // HTTP进程被终止
        console.log("Child process terminated(code="+code+") due to receipt of signal "+signal);
        //fork();
        //console.log("Child process restarted");
    }

    function fork() {
        p = child_process.fork(P_PATH);
        p.x= "hello";

        p.on("error", onError);
        p.on("exit", onExit);
    }
}


function main(fn) {
    fn();
}

void main(function() {
    start();
});