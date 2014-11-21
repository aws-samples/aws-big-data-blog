var express = require('express');
var app = express();
var http = require('http').Server(app);
var io = require('socket.io')(http);

var redis = require('redis');
var redisClient;
var redisClient = redis.createClient(6379, process.env.PARAM1);
 
// serve static content from 'public'
app.use(express.static(__dirname + '/public'));

// serve the globe page
app.get('/globe', function(req, res) {
	res.sendfile('globe.html');
});

//serve the heatmap page
app.get('/heatmap', function(req, res) {
	res.sendfile('heatmap.html');
});

app.get('/', function(req, res) {
	res.sendfile('globe.html');
});

// log that we have subscribed to a channel
redisClient.on('subscribe', function(channel, count) {
	console.log('redis client subscribed');
});

// When we get a message from redis, we send the message down the socket to the client
redisClient.on('message', function(channel, message) {	
	var coord = JSON.parse(message);
	io.emit('tweet', coord);
});

// subscribe to listen to events from redis
redisClient.on("ready", function () {		
	redisClient.subscribe("loc");
});

// log that someone has connected via sockets (they are now listening for redis events)
io.on('connection', function(socket) {
	console.log('a user connected');
});

// start, either on the Beanstalk port, or 3000 for local development
var port = process.env.PORT || 3000;
http.listen(port, function() {
    console.log('server listening on port ' + port);
});