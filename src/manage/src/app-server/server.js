require(["dojo/node!fs"], function(fs){
    var helloworld = fs.readFileSync("helloworld.json");
    console.log(JSON.parse(helloworld));
});
