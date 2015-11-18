/***
Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

Licensed under the Amazon Software License (the "License").
You may not use this file except in compliance with the License.
A copy of the License is located at

http://aws.amazon.com/asl/

or in the "license" file accompanying this file. This file is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
express or implied. See the License for the specific language governing
permissions and limitations under the License.
***/

'use strict';


var AWS = require('aws-sdk');
var config = require('./config');
var producer = require('./twitter_stream_producer');

// var kinesis = new AWS.Kinesis({region: config.kinesis.region});
var kinesis_firehose = new AWS.Firehose({apiVersion: '2015-08-04', region: config.region});
// console.log(kinesis_firehose.listDeliveryStreams());
producer(kinesis_firehose).run();
