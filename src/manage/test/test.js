(function() {
    this.dojoConfig = {
        has: {
            "host-node": 1
        },
       paths: {
            cory: "./cory"
        }
    };

    require("../src/dojo/dojo");

    //var d = dojo;

    ["http", "url", "fs", "formidable"].forEach(function(requiredName) {

        //d[requiredName] = 
        require(requiredName);

    });

    define("Main", [

        "dojo/_base/kernel",

        "cory/Server",

        "cory/UrlParser",

        "cory/Router",

        "cory/Page"

    ], function(dojo, Server, UrlParser, Router) {

        //这里将地址和类中的方法进行绑定。

        //其中cory.Page是页面类，后面的.displayIndex、.upload、.show都是该类的方法。

        var pathMap = {

            "/": "cory.Page.displayIndex",

            "/index": "cory.Page.displayIndex",

            "/upload": "cory.Page.upload",

            "/show": "cory.Page.show"

        };

        var server = new Server();

        server.set("port", 8888);

        server.start(function(request, response) {

            var router = new Router();

            router.set("request", request);


            router.set("response", response);

            router.rout(pathMap, new UrlParser().parse2Pathname(request.url));

        });

    });

			
})();
