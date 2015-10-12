#!/usr/bin/env node

// use RL to read lines from stdin
var readline = require('readline');
var rl = readline.createInterface({
	input : process.stdin
});

// variable used as a daily accumulator
var interactionSummary = {
	day : '',
	count : 0
};

// generate a JSON object from the captured input data, and then generate
// the required output
exports.processMapData = function(data) {
	lineElements = data.split('\t')
	var keyDate = lineElements[0];
	obj = JSON.parse(lineElements[1]);

	if (interactionSummary.day === '') {
		interactionSummary.day = keyDate;
		interactionSummary.count = 1;
	} else {
		if (keyDate !== interactionSummary.day) {
			process.stdout.write(JSON.stringify(interactionSummary) + '\n');

			interactionSummary.day = keyDate;
			interactionSummary.count = 1;
		} else {
			interactionSummary.count += 1;
		}
	}
};

// fire an event on each line read from RL
rl.on('line', function(line) {
	exports.processMapData(line);
});

// final event when the file is closed, to flush the final accumulated value
rl.on('close', function(line) {
	process.stdout.write(JSON.stringify(interactionSummary) + '\n');
})