var AWS = require('aws-sdk');
var parseString = require('xml2js').parseString;
var util = require('util');
var https = require('https');
var uuid = require('uuid');
var Readable = require('stream').Readable;
var Transform = require('stream').Transform;

var PROVISIONED_CONCURRENT_LAMBDAS = 100;
var PROCESS_BATCH_SIZE = 5;
var DEFAULT_MAX_CONCURRENT_LAMBDAS = 100;
var DEFAULT_BATCH_SIZE = PROVISIONED_CONCURRENT_LAMBDAS * PROCESS_BATCH_SIZE / DEFAULT_MAX_CONCURRENT_LAMBDAS;
var DEFAULT_REGION = 'us-east-1';
var MAX_LAMBDA_EXECUTION_TIME = 60000;

// ListStream
util.inherits(ListStream, Transform);

function ListStream(opt) {
  if (typeof(opt) === 'undefined'){
    opt = {};
  }
  opt.objectMode = true;
  Transform.call(this, opt);
  this._nextMarker = null;
  this._lastKey = null;
  if (!opt.region)
    opt.region = DEFAULT_REGION;
  this._region = opt.region;
  this.s3 = new AWS.S3({region: this._region});
}

ListStream.prototype._transform = function(obj, encoding, cb){
  var self = this;
  self._list(obj, cb);
};

ListStream.prototype._list = function(obj, cb){
  var self = this;
  
  var params = {
    Bucket: obj.bucket,
    Prefix: obj.prefix
  };
  if (self._nextMarker !== null){
    params.Marker = self._nextMarker;
  }
  self.s3.listObjects(params, function(err, data) {
    if (err){
      console.error(err, err.stack);
      self.emit('error', err);
      return;
    }
    for (var i = 0, len = data.Contents.length; i < len; i++){
      self._lastKey = data.Contents[i].Key;
      if (data.Contents[i].Size > 0){
        self.push(data.Contents[i].Key);
        console.log('Found key ' + data.Contents[i].Key);
      }
    }
    
    if (data.NextMarker){
      self._nextMarker = data.NextMarker;
    }
    else {
      self._nextMarker = self._lastKey;
    }
    
    if (!data.IsTruncated || !self._nextMarker){
      self._nextMarker = null;
      self._lastKey = null;
      cb();
      self.end();
    }
    else {
      self._list(obj, cb);
    }
  });
};

// LambdaStream
util.inherits(LambdaStream, Transform);

function LambdaStream(opt){
  if (typeof(opt) === 'undefined'){
    opt = {};
  }
  opt.objectMode = true;
  Transform.call(this, opt);
  if (opt.no_agg){
    this._noAgg = true;
  }
  else {
    this._noAgg = false;
  }

  this._bucket = opt.bucket;
  if (!this._bucket) throw Error('No bucket provided.');
  var agent = new https.Agent();
  if (!opt.max_lambdas)
    opt.max_lambdas = DEFAULT_MAX_CONCURRENT_LAMBDAS;
  agent.maxSockets = opt.max_lambdas;

  if (!opt.region)
    opt.region = DEFAULT_REGION;

  this._lambda = new AWS.Lambda({
    region: opt.region,
    httpOptions: {
      agent: agent,
      timeout: MAX_LAMBDA_EXECUTION_TIME
    }
  });
  this._region = opt.region;
  this._batch = [];
  this._batch_size = DEFAULT_BATCH_SIZE;
  this._done = false;
  this._lambdas = {};
  this._total_ms_taken = 0;
  this._totalWords = 0;
  this._totalLines = 0;
  this._totalKeys = 0;
  this._keyCounter = 0;
}

LambdaStream.prototype._checkDone = function(){
  var self = this;
  if (self._done && !Object.keys(self._lambdas).length){
    // I am the final Lambda
    self.end();
    self.emit('end');
  }
};

LambdaStream.prototype._flush = function(cb){
  var self = this;
  console.log('Flushing with length: ' + self._batch.length);
  if (self._batch.length){
    self._invoke(cb);
    self._batch = [];
  }
  else {
    cb();
  }
};

LambdaStream.prototype._transform = function(obj, encoding, cb) {
  var self = this;
  
  if (typeof(obj) === 'object' && obj.done){
    self._done = true;
    self._flush(cb);
    return;
  }
  obj = obj.toString();
  
  self._batch.push(obj);

  if (self._batch.length >= self._batch_size){
    self._flush(cb);
  }
  else {
    cb();
  }
};

LambdaStream.prototype.logToClient = function(msg){
  this.push(JSON.stringify({logdata: msg}));
};

LambdaStream.prototype._invoke = function(cb){
  var self = this;
  
  var batch = self._batch.slice();
  var tail = 'Tail';
  var params = {
    FunctionName: 'blog_cascade',
    InvocationType: 'RequestResponse',
    LogType: tail, //None or Tail
    Payload: JSON.stringify({
      keys: batch,
      bucket: self._bucket,
      region: self._region,
      burst_rate: Math.floor((PROVISIONED_CONCURRENT_LAMBDAS - DEFAULT_MAX_CONCURRENT_LAMBDAS) / DEFAULT_MAX_CONCURRENT_LAMBDAS),
      batch_size: PROCESS_BATCH_SIZE,
      no_agg: self._noAgg
    })
  };
  
  self._keyCounter += self._batch.length;
  var lambda_id = uuid.v4();

  var message = 'Invoking lambda ' + lambda_id +
    ' with outstanding ' + Object.keys(self._lambdas).length +
    ' and keys ' + self._keyCounter;
  console.log(message);
  self.logToClient('Invoking lambda ' + lambda_id);
  self._lambdas[lambda_id] = self._lambda.invoke(params, function(err, obj) {
    // Mark that this is complete
    delete self._lambdas[lambda_id];
    console.log('Completed lambda ' + lambda_id +
      ' with outstanding ' + Object.keys(self._lambdas).length +
      ' and keys ' + self._keyCounter);
    self.logToClient('Lambda ' + lambda_id +
      ' finished keys ' + JSON.stringify(batch));
    if (err){
      console.error('Error running Lambda: ' + err);
      self.logToClient('Lambda ' + lambda_id + ' error: ' + err +
        log.toString());
      console.error('Log:', log.toString());
      self.emit('error', err);
      self._checkDone();
      return;
    }

    if (tail === 'Tail'){
      var log = (new Buffer(obj.LogResult, 'base64')).toString();
      var matches = log.match(/Billed Duration: (\d+) ms/);
      if (matches && matches.length > 1){
        self._total_ms_taken += parseInt(matches[1]);
      }
    }
    
    self._totalKeys += batch.length;
    var result = JSON.parse(obj.Payload);
    if (result.length && (result[0].length > 0 || result[0] > 0)){
      if (self._noAgg){
        // For each Cascade
        for (var i = 0, len = result.length; i < len; i++){
          // For each Wordcount
          for (var j = 0, jlen = result[i][0].length; j < jlen; j++){
            var perLambdaResult = result[i][0][j];
            self.push(JSON.stringify(
              [ 
                self._totalKeys, 
                self._keyCounter, 
                perLambdaResult[0], 
                perLambdaResult[1],
                perLambdaResult[2]
              ]
            ));
          }
          for (var j = 0, jlen = result[i][1].length; j < jlen; j++){
            self.logToClient('WARNING: ' + result[i][1][j]);
          }
        }
      }
      else {
        self._totalWords += parseInt(result[0]);
        self._totalLines += parseInt(result[1]);
        self._total_ms_taken += parseInt(result[2]);
        self.push(JSON.stringify(
          [self._totalKeys, self._keyCounter, 
            self._totalWords, self._totalLines, self._total_ms_taken]
        ));
        for (var i = 0, len = result[3].length; i < len; i++){
          self.logToClient('WARNING: ' + result[3][i]);
        }
      }
    }
    else if (result.length && result[0].errorMessage){
      self.logToClient('WARNING: ' + result[0].errorMessage);
    }
    self._checkDone();
  });
  cb();
};

util.inherits(ServerSentStream, Transform);

function ServerSentStream(opt) {
  Transform.call(this, opt);
}

ServerSentStream.prototype._transform = function(chunk, encoding, cb) {
  this.push('data: ' + chunk + '\n\n');
  cb();
};

module.exports = {
  ListStream: ListStream, 
  LambdaStream: LambdaStream, 
  ServerSentStream: ServerSentStream
};