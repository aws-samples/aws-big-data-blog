var async = require('async');
var AWS = require('aws-sdk');
var zlib = require('zlib');
var es = require('event-stream');

var s3 = new AWS.S3();

// We will stop executing if we go past this number of seconds
var MAX_EXEC_TIME = 15;

exports.handler = function(event, context) {
  var startTime = process.hrtime();
  var id = context.invokeid;
  var srcBucket = event.bucket;
  var srcKeys = event.keys;
  if (!srcBucket || !srcKeys){
    context.error('Invalid args.');
  }
  
  // Word counting routine
  var countWords = function(srcKey, cb){
    try {      
      var lineCount = 0;
      var totalWords = 0;
      var finished = false;
      var warning = '';

      // Create our processor that splits lines/words
      var lineSplitter = es.split();
      lineSplitter.on('data', function(line){
        lineCount++;
        totalWords += line.split(/\W/).length;
        // Check our execution time
        var timeDiff = process.hrtime(startTime);
        if (timeDiff[0] >= MAX_EXEC_TIME){
          var msg = 'Hit max execution time of ' + MAX_EXEC_TIME +
            ' seconds, sending incomplete results.';
          console.error(msg);
          warning = msg;
          this.end();
        }
      })
      .on('error', function(err){
        console.error('Error in lineSplitter: ' + err);
      })
      .on('end', function(err){
        if (finished) return;
        if (err){
          console.error(err);
          cb(err, null);
          return;
        }
        console.log('Finished ' + srcKey + ' with ' +
          totalWords + ' in ' + lineCount + ' lines');
        cb(null, [totalWords, lineCount, warning]);
        finished = true;
      });

      console.log('Downloading key ' + srcKey);
      // Create our pipeline
      if (srcKey.match(/\.gz/)){
        s3.getObject({
          Bucket: srcBucket,
          Key: srcKey
        })
          .createReadStream()
          .pipe(zlib.createGunzip())
          .pipe(lineSplitter);
      }
      else {
        s3.getObject({
          Bucket: srcBucket,
          Key: srcKey
        })
          .createReadStream()
          .pipe(lineSplitter);
      }
    } catch (e) {
      console.error(e);
      cb(e, null);
    }
  };

  // Execute the countWords function in parallel on all given keys
  async.map(srcKeys, countWords, function (err, results) {
    if (err){
      console.error(err);
      context.fail(err);
    }
    var totalWords = 0;
    var totalLines = 0;
    var warnings = [];
    for (var i = 0, len = results.length; i < len; i++){
      totalWords += results[i][0];
      totalLines += results[i][1];
      if (results[i][2] !== ''){
        warnings.push(results[i][2]);
      }
    }
    console.log('Final total words: ' + totalWords);
    context.succeed(JSON.stringify([totalWords, totalLines, warnings]));
  });
};
