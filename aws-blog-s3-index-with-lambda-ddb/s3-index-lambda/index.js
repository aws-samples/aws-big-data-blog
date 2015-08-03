var AWS = require('aws-sdk');
var s3 = new AWS.S3(2006-03-01);
var dynamodb = new AWS.DynamoDB('2012-08-10');

var keyRegex = /[^/]+\/([^/]+)\/[^/]+\/([^-]+)-([^.]+).data/;
 
exports.handler = function(event, context) {
	var record = event.Records[0];
	var object = record.s3.object;
	var bucket = record.s3.bucket.name;
    var key = decodeURIComponent(object.key.replace(/\+/g, " "));
	
    console.log("Indexing " + bucket + "/" + key);

	try {
		addIndexEntry();
	} catch(err) {
		context.done("Exception thrown: " + err);
		return;
	}	

	function addIndexEntry() {
		var indexItem = {};
		var match = keyRegex.exec(key);
		if(!match) {
			context.done("Key did not match pattern");
			return;
		}
		
		var serverId = match[1];
		var custId = match[2];
		var ts = match[3];
	
		indexItem['Key'] = {S: key};
		indexItem['Size'] = {N: object.size.toString()};
		indexItem['ServerID'] = {S: serverId};
		indexItem['CustID'] = {S: custId};
		indexItem['TS-ServerID'] = {S: new Date(parseInt(ts)).toISOString() + serverId};
	
		var s3HeadParams = {
			Bucket: bucket,
			Key: key
		};

		s3.headObject(s3HeadParams, function(err, data) {
			if(err) {
				context.done("Error fetching object metadata: " + err);
				return;
			} else {
				if(data.Metadata.hastransaction === 'true') {
					indexItem.HasTransaction = {S: 'true'};
				}
				putItem();
			}
		});
	
		function putItem() {
			var table = bucket + '-index';
			var putParams = {
				TableName: table,
				Item: indexItem
			};
		
			dynamodb.putItem(putParams, function(err, data){
				if(err) {
					context.done("Error adding index item to " + table + "\n" + err);
				} else {
					context.done();
				}
				return;
			});
		}
	}
};