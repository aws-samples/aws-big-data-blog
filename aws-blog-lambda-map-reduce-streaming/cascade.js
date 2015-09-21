var async = require('async');
var AWS = require('aws-sdk');
var https = require('https');

var DEFAULT_CONCURRENT_LAMBDAS = 100;
var DEFAULT_BATCH_SIZE = 5;
var DEFAULT_REGION = 'us-east-1';

/* Params:
bucket - (string) Bucket to pull keys from
keys - (array) Array of keys to batch to Lambdas
burst_rate - (int) Optional Maximum concurrent Lambdas to allow. This should
  match your provisioned maximum concurrent lambdas (default is 100).
region - (string) Optional region to connect to (defaults to us-east-1)
batch_size - (int) Optional batch size for the wordcount Lambdas
no_agg - (bool) Optional flag for propagating granular (not agg) results
*/

exports.handler = function(event, context) {
  var agent = new https.Agent();
  agent.maxSockets = event.burst_rate;
  if (typeof(agent.maxSockets) === 'undefined' || agent.maxSockets < 1) 
    agent.maxSockets = DEFAULT_CONCURRENT_LAMBDAS;

  var bucket = event.bucket;
  if (typeof(bucket) === 'undefined')
    context.fail('No bucket provided in params.');
  var allKeys = event.keys;
  if (typeof(allKeys) === 'undefined')
    context.fail('No keys provided.');
  var region = event.region;
  if (typeof(region) === 'undefined')
    region = DEFAULT_REGION;
  var batchSize = event.batch_size;
  if (typeof(batchSize) === 'undefined')
    batchSize = DEFAULT_BATCH_SIZE;
  var noAgg = false;
  if (event.no_agg)
    noAgg = true;

  var lambda = new AWS.Lambda({
    region: region,
    httpOptions: {
      agent: agent,
      timeout: 60000
    }
  });

  var lambdaBilledMS = 0;

  function invoke(keys, cb){
    var tail = 'Tail';
    var params = {
      FunctionName: 'wordcount',
      InvocationType: 'RequestResponse',
      LogType: tail, //None or Tail
      Payload: JSON.stringify({
        bucket: bucket,
        keys: keys,
        no_agg: noAgg
      })
    };
    var status = lambda.invoke(params, function(err, obj){
      if (err){
        console.error(err);
        cb(err, null);
        return;
      }
      if (tail === 'Tail'){
        var log = (new Buffer(obj.LogResult, 'base64')).toString();
        var matches = log.match(/Billed Duration: (\d+) ms/);
        if (matches && matches.length > 1){
          lambdaBilledMS += parseInt(matches[1]);
        }
      }
      cb(null, JSON.parse(obj.Payload));
    });
  }

  // Chop our given list of keys up into batch_size'd batches
  var batches = [];
  var batch = [];
  for (var i = 0, len = allKeys.length; i < len; i++){
    batch.push(allKeys[i]);
    if (batch.length >= batchSize){
      batches.push(batch.slice());
      batch = [];
    }
  }
  if (batch.length){
    batches.push(batch.slice());
  }

  // Invoke each batch in parallel, returning aggregated result when
  //   all are finished.
  async.map(batches, invoke,
    function (err, results) {
      if (err) {
        console.error('error on invoke', err);
        context.fail('async.map error: ' + err.toString());
        return;
      }

      var warnings = [];
      
      if (noAgg){
        context.succeed(results);
      }
      else {
        var totalWords = 0;
        var totalLines = 0;
        
        for (var i = 0, len = results.length; i < len; i++){
          if (typeof(results[i]) === 'object' && results[i].errorMessage){  
            console.error('Batch ' + JSON.stringify(batches[i]) +
              ' got error ' + results[i].errorMessage);
            continue;
          }
          try {
            var result = results[i];
            totalWords += result[0];
            totalLines += result[1];
            warnings.push.apply(warnings, result[2]);
          } catch (e){
            console.error('Unable to parse JSON from ' + results[i]);
          }
        }
        context.succeed([
          totalWords,
          totalLines,
          lambdaBilledMS,
          warnings
        ]);
      }
  });
};