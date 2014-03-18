define(["../../src/dojo/_base/declare"], function(declare) {

    return declare("cory.UrlParser", null, {

        parse2Pathname: function(/String/url) {

            var pathname = dojo.url.parse(url).pathname;

            console.debug("The current path name is: \"" + pathname + "\".");

            return pathname;

        }

    });

});
