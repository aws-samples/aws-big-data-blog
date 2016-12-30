
//web service to generate JSON from XML
var http = require('http');
var qs = require('querystring');
var fs = require('fs');
var xml2js = require('xml2js');
const util = require('util');

//Port to listen for requests as TCP
const PORT=8001;
var conn_count = 0;
var parser = new xml2js.Parser({explicitArray : false});

//We need a function which handles requests and send response
function handleRequest(request, response){
    conn_count++;
    if (request.method == 'POST') {
                var body = [];
                request.on('data', function(chunk) {
                  body.push(chunk);
                }).on('end', function() {
                  body = Buffer.concat(body).toString();
                  // at this point, `body` has the entire request body stored in it as a string
                  });

        console.log("Processing Connection: "+conn_count);

        request.on('end', function () {
		parser.parseString(body, function (err, jsonResult) {	
			var jsonStr = JSON.stringify(jsonResult, null, 4);
			//console.log(jsonStr);
			var json=jsonResult;
		response.end(jsonStr);
		});
        });
    } else {
    response.end('Issue processing: ' + request.url);
    }
}

//Create a server
var server = http.createServer(handleRequest);

//Lets start our server
server.listen(PORT, function(){
    //Callback triggered when server is successfully listening. Hurray!
    console.log("Server listening on: http://localhost:%s", PORT);
});

