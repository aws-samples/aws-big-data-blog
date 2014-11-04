/*
 * Copyright 2013-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

var http    = require('http')
,   connect = require('connect')
,   redis  = require('redis');

function ticker(req,res) {
 req.socket.setTimeout(Infinity);
 
  var subscriber = redis.createClient(6379,process.argv[2]);
	
  subscriber.subscribe("pubsubCounters");
	
  // When we receive a message from the redis connection
  subscriber.on("message", function(channel, message) {
		res.json(message);
  });
	
  //send headers for event-stream connection
  res.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive'
  });

  res.json = function(obj) { res.write("data: "+obj+"\n\n"); }
  res.json(JSON.stringify({}));
 
  // The 'close' event is fired when a user closes their browser window.
  req.on("close", function() {
    subscriber.unsubscribe();
    subscriber.quit();
  });
}

connect()
    .use(connect.static(__dirname))
    .use(function(req,res) {
        if(req.url == '/eventCounters') {
            ticker(req,res);
        }
})
.listen(9000);