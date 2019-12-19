const AWS = require('aws-sdk');
const s3 = new AWS.S3('2006-03-01');
const dynamodb = new AWS.DynamoDB('2012-08-10');

const keyRegex = /[^/]+\/([^/]+)\/[^/]+\/([^-]+)-([^.]+).data/;

const INDEX_TABLE = process.env.INDEX_TABLE;
 
exports.handler = async function(event) {
    console.log(`Received event: ${JSON.stringify(event)}`);
    try {
        return handleEvent(event);
    } catch(err) {
        console.error("An error occured while processing the event");
        console.error(err);
    }
};

async function handleEvent(event) {
	const record = event.Records[0];
	const object = record.s3.object;
	const bucket = record.s3.bucket.name;
    const key = decodeURIComponent(object.key.replace(/\+/g, " "));
    const match = keyRegex.exec(key);
    if(!match) {
        console.log("Key did not match pattern. Skipping.");
        return;
    }

    console.log(`Indexing ${bucket}/${key}`);

    const indexItem = {};
    
    const serverId = match[1];
    const custId = match[2];
    const ts = match[3];

    indexItem['Key'] = {S: key};
    indexItem['Size'] = {N: object.size.toString()};
    indexItem['ServerID'] = {S: serverId};
    indexItem['CustID'] = {S: custId};
    indexItem['TS-ServerID'] = {S: new Date(parseInt(ts)).toISOString() + serverId};

    const s3HeadParams = {
        Bucket: bucket,
        Key: key
    };

    console.log("Fetching S3 metadata");
    const s3Response = await s3.headObject(s3HeadParams).promise();

    if(s3Response.Metadata.hastransaction === 'true') {
        indexItem.HasTransaction = {S: 'true'};
    }
    
    console.log("Putting index item");
    return putItem(indexItem);
}

function putItem(indexItem) {
    var putParams = {
        TableName: INDEX_TABLE,
        Item: indexItem
    };

    return dynamodb.putItem(putParams).promise();
}