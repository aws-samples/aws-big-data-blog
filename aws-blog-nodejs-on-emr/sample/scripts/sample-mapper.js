#!/usr/bin/env node

var events = require('events');
var emitter = new events.EventEmitter();

var line = '';
var lineEvent = 'line';
var dataReady = 'dataReady';

// escape all control characters so that they are plain text in the output
String.prototype.escape = function() {
	return this.replace('\n', '\\n').replace('\'', '\\\'').replace('\"', '\\"')
			.replace('\&', '\\&').replace('\r', '\\r').replace('\t', '\\t')
			.replace('\b', '\\b').replace('\f', '\\f');
}

// append an array to this one
Array.prototype.appendArray = function(arr) {
	this.push.apply(this, arr);
}

// data is complete, write it to the required output channel
emitter.on(dataReady, function(arr) {
	var dateComponents = arr[9].split(' ');
	var d = [dateComponents[1],dateComponents[2],dateComponents[3]].join(' ');
	
	var interaction = {
		key_date : d,
		content: {
			objectId : arr[0],
			hash : arr[1],
			id : arr[2],
			author_id : arr[3],
			author_avatar : arr[4],
			author_link : arr[5],
			author_name : arr[6],
			author_username : arr[7],
			content : arr[8],
			created_at : arr[9],
			link : arr[10],
			schema_version : arr[11],
			source : arr[12]
		}
	};

	process.stdout.write(interaction.key_date + '\t' + JSON.stringify(interaction) + '\n');
});

// generate a JSON object from the captured input data, and then generate
// the required output
emitter.on(lineEvent, function(l) {
	var obj;

	// create the JSON object from the input event. if we cannot, then we discard
	// this item
	//
	// TODO Generate an exception here instead?
	if (!line || line == '') {
		return;
	}
	
	try {
		obj = JSON.parse(line);
	} catch (err) {
		process.stderr.write('Error Processing Line ' + line + '\n');
		process.stderr.write(err);
		return;
	}
	
	// generate an output set per interaction object
	for ( var i = 0; i < obj.interactions.length; i++) {
		// create some convenience objects for syntax
		var int = obj.interactions[i];
		var a = int.interaction.author;
		
		// pull out the bits of the object model we want to retain
		var output = [ obj.id, obj.hash, int.interaction.id, a.id,
				a.avatar, a.link, a.name, a.username,
				int.interaction.content.escape(), int.interaction.created_at,
				int.interaction.link, int.interaction.schema.version,
				int.interaction.source ];
		
		// raise an event that the output array is completed
		emitter.emit(dataReady, output);
	}
});

// fires on every block of data read from stdin
process.stdin.on('data', function(chunk) {
	// chunk and emit on newline
	lines = chunk.split("\n")
	
	if (lines.length > 0) {
		// append the first chunk to the existing buffer
		line += lines[0]
		
		if (lines.length > 1) {
			// emit the current buffer
			emitter.emit(lineEvent,line);

			// go through the rest of the lines and emit them, buffering the last
			for (i=1; i<lines.length; i++) {
				if (i < lines.length) {
					emitter.emit(lineEvent,lines[i]);
				} else {
					line = lines[i];
				}
			}
		}
	}
});

// fires when stdin is completed being read
process.stdin.on('end', function() {
	emitter.emit(lineEvent,line);
});

// set up the encoding for STDIN
process.stdin.setEncoding('utf8');

// resume STDIN - paused by default
process.stdin.resume();