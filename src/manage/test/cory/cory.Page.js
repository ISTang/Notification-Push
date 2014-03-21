define(["../../src/dojo/_base/declare", "../../src/dojo/Stateful"], function(declare, Stateful) {

    return declare("cory.Page", Stateful, {

        request: null,

        response: null,

        //@Page(path=["/", "/index"])

        displayIndex: function() {

            console.debug("\"cory.Page.displayIndex\" was called");

            var res = this.get("response");

            if (res) {

                var body =

                '<html>'+

                    '<head>'+

                        '<meta http-equiv="Content-Type" content="text/html; '+
<br/>                        'charset=UTF-8" />'+

                    '</head>'+

                    '<body>'+

                        '<form action="/upload" method="post" enctype="multipart/form-data">'+

                            '<input type="file" name="upload" />'+

                            '<input type="submit" value="Upload file" />'+

                        '</form>'+

                    '</body>'+

                '</html>';

                res.writeHead(200, {"Content-Type": "text/html"});

                res.write(body);

                res.end();

            }

        },

        //@Page(path=["/upload"])

        upload: function() {

            console.debug("\"cory.Page.upload\" was called");

            var req = this.get("request"),

                res = this.get("response");

            if (req && res) {

                var form = new dojo.formidable.IncomingForm();

                console.debug("about to parse");

                form.parse(req, function(error, fields, files) {

                    console.debug("parsing done.");

                    dojo.fs.renameSync(files.upload.path, "C:\Documents and Settings\Cory\Local Settings\Temp\test.png");

                    res.writeHead(200, {"Content-Type": "text/html"});

                    res.write("received image:");

                    res.write("");

                    res.end();

                });

            }

        },

        //@Page(path=["/show"])

        show: function() {

            console.debug("\"cory.Page.show\" was called");

            var res = this.get("response");

            if (res) {

                dojo.fs.readFile("C:\Documents and Settings\Cory\Local Settings\Temp\test.png", "binary", function(error, file){

                    if (error) {

                        res.writeHead(500, {"Content-Type": "text/plain"});

                        res.write(error + "\n");

                    } else {

                        res.writeHead(200, {"Content-Type": "image/x-png"});

                        res.write(file, "binary");

                    }

                    res.end();

                })

            }

        }

    });

});

