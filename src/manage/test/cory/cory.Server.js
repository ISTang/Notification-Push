define([

    "../../src/dojo/_base/kernel",

    "../../src/dojo/_base/declare",

    "../../src/dojo/Stateful"

], function(dojo, declare, Stateful) {

    // summary:

    //  This server to listen the port which you setted.

    return declare("cory.Server", Stateful, {

        // default value

        port: 8888,

        _http: dojo.http,

        postscript: function(args) {

            this.inherited(arguments);

            if (args && args.port) {

                this.set("port", args.port);

            }

        },

        start: function(/Function?/callback) {

            console.debug("Server is starting...");

            var _port = this.get("port");

            this._http.createServer(function(request, response) {

                callback(request, response);

            }).listen(_port);

            console.debug("Server has started. The port is " + _port + ".");

        }

    });

});
