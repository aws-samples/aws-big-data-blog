var express = require('express');
var fs = require('fs');
var ListStream = require('./streams').ListStream;
var LambdaStream = require('./streams').LambdaStream;
var ServerSentStream = require('./streams').ServerSentStream;

var BUCKET = process.env.BUCKET;
if (!BUCKET)
  BUCKET = 'awssampledb';
var LISTEN_PORT = 4001;
if (process.env.LISTEN_PORT)
  LISTEN_PORT = process.env.LISTEN_PORT;

var REGION = 'us-east-1';

if (typeof(process.env.region) !== 'undefined'){
  REGION = process.env.region;
}
else {
  console.log('Defaulting region to us-east-1, specify with the environment variable REGION to change.');
}

var app = express();

function printError(err){
  console.error(err, err.stack);
}

app.get('/', function(req, res){
  res.set('Content-Type', 'text/html');
  fs.createReadStream('index.html').pipe(res);
});

app.get('/scatter', function(req, res){
  res.set('Content-Type', 'text/html');
  fs.createReadStream('scatter.html').pipe(res);
});

app.get('/stream', function(req, res){
  req.socket.setTimeout(0);

  //send headers for event-stream connection
  res.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive'
  });
  res.write('\n');

  var lambdaStream = new LambdaStream({
    region: REGION,
    bucket: BUCKET,
    no_agg: req.query.no_agg
  })
    .on('error', printError);
  var listStream = new ListStream({
    region: REGION, 
    bucket: BUCKET, 
    prefix: req.query.prefix})
    .on('error', printError)
    .on('finish', function(){
      // Manually trigger the end() call to let internal async complete.
      lambdaStream.write({done:true});
    });
  var serverSentStream = new ServerSentStream()
    .on('finish', function(){
      res.write('id: final\n' + 'data: ' + 
        JSON.stringify({complete:true}) + '\n\n');
    });

  listStream
  .pipe(lambdaStream, { end: false }) // Do not propagate end
  .pipe(serverSentStream)
  .pipe(res);
  listStream.write({
    bucket: BUCKET,
    prefix: req.query.prefix
  });
});

var server = app.listen(LISTEN_PORT, function () {

  var host = server.address().address;
  var port = server.address().port;

  console.log('Example  app listening at http://%s:%s', host, port);

});