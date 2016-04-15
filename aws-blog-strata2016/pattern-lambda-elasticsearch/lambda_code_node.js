/* == Imports == */
var AWS = require('aws-sdk');
var path = require('path');

/* == Globals == */
var esDomain = {
    region: 'us-east-1',
    endpoint: 'search-kinesis-es-4wzi4sdumnz7y2vud7pxdegyxu.us-east-1.es.amazonaws.com',
    index: 'strata2016',
    doctype: 'sensors'
};
var endpoint = new AWS.Endpoint(esDomain.endpoint);
/*
 * The AWS credentials are picked up from the environment.
 * They belong to the IAM role assigned to the Lambda function.
 * Since the ES requests are signed using these credentials,
 * make sure to apply a policy that allows ES domain operations
 * to the role.
 */
var creds = new AWS.EnvironmentCredentials('AWS');


/* Lambda "main": Execution begins here */
exports.handler = function(event, context) {
    console.log(JSON.stringify(event, null, '  '));
    event.Records.forEach(function(record) {
        var rawData = new Buffer(record.kinesis.data, 'base64');
        var arr = rawData.toString().split(",");
        var result = '{ "device_id" : '+arr[0]+',"temperature" : '+arr[1]+',"timestamp" : "'+arr[2]+'" }'
        postToES(result, context);
        console.log('This is the incoming data: ' + result)
    });
}


/*
 * Post the given document to Elasticsearch
 */
function postToES(doc, context) {
    var req = new AWS.HttpRequest(endpoint);

    req.method = 'POST';
    req.path = path.join('/', esDomain.index, esDomain.doctype);
    req.region = esDomain.region;
    req.headers['presigned-expires'] = false;
    req.headers['Host'] = endpoint.host;
    req.body = doc;

    var signer = new AWS.Signers.V4(req , 'es');  // es: service code
    signer.addAuthorization(creds, new Date());

    var send = new AWS.NodeHttpClient();
    send.handleRequest(req, null, function(httpResp) {
        var respBody = '';
        httpResp.on('data', function (chunk) {
            respBody += chunk;
        });
        httpResp.on('end', function (chunk) {
            console.log('Response: ' + respBody);
            context.succeed('Lambda added document ' + doc);
        });
    }, function(err) {
        console.log('Error: ' + err);
        context.fail('Lambda failed with error ' + err);
    });
}
