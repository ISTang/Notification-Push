define(["../../src/dojo/_base/declare", "../../src/dojo/Stateful"], function(declare, Stateful) {

    return declare("cory.Router", Stateful, {

        request: null,

        response: null,

        //根据不同的url地址名称，路由到不同的页面逻辑处理。

        //返回是否路由成功。

        rout: function(/Object/path, /String/pathname) {

            console.debug("About to route a request for " + pathname);

            var _classAndFunc = path[pathname];

            var res = this.get("response");

            if (!_classAndFunc) {

                console.debug("But this path was not found.");

                if (res) {

                    res.writeHead(200, {"Content-Type": "text/plain"});

                    res.write("404 Not found");

                    res.end();

                }

            }

            var _lastIndex = _classAndFunc.lastIndexOf(".");

            var _className = _classAndFunc.substring(0, _lastIndex);

            var _funcName = _classAndFunc.substring(_lastIndex + 1);

            var _page = new dojo.getObject(_className)();

            _page.set("request", this.get("request"));

            _page.set("response", res);

            _page_funcName;

        }

    });

});
