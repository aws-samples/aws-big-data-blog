# mirth-aws-example-app
This application contains functions which can be called via Mirth Connect
Example channel implementations will be added to repository

Instructions to [build this package using Maven](http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-project-maven.html).

Instructions for [invoking custom java code in Mirth Connect](http://www.mirthcorp.com/community/wiki/display/mirth/How+to+create+and+invoke+custom+Java+code+in+Mirth+Connect).

#####Setting Amazon S3 Bucket Destination
 * Under settings in Mirth Administrative Console, select Settings.
 * On the top menu list, select Configuration Map.
 * Click on Add to add a value.  
 * The Key should be s3BucketName with the value being the name of your Amazon S3 bucket. 
 * Create a directory and copy this jar file to the new directory. 

#####Add this new directory to your Mirth Connection configuration.
 * From within Mirth Connect Administrative Console, click settings.
 * Click Resources
 * Click Add Resource
 * Enter full directory path and add a description
 * Once complete, click, Reload Resource

#####Additional web services used by Mirth Channels
 * bb-webapp: HTTP listener used to process CCD/CDA XML into JSON for insertion into Amazon DynamoDB
 * hl7-webapp: HTTP listener used convert transformed HL7 messages in XML format to JSON for insertion into Amazon DynamoDB

#####For each Mirth Channel using these libraries, you must include this library resource in the channel as well as any destinations.
 * For each channel, enter the channel configuration.
 * Click, Set Dependencies
 * Click, Library Resources
 * Select the context the library should be used with.
 * Highlight the check box for the library to include.
 * Save and deploy channel

Example use can be found in the example Mirth Channels provided.

In Example 1, we assume the data received from connectorMessage.getRawData() is formatted JSON.

#####Example Javascript Writer in Mirth Connect using sample code:
```
//Example 1
//create unique ID during insert
//this unique ID can be written to channel map for use in other areas
var awsFileId = UUIDGenerator.getUUID();
channelMap.put("awsFileMapId", awsFileId);
var currentDate = DateUtil.getCurrentDate("MM-dd-yyyy-HH-mm");

//loading DynamoDB java code
var awsObj = new Packages.org.mirth.project.MCAWS();

var ccdJsonData = connectorMessage.getRawData();

//ccdJsonData - JSON formatted text
//"mirthdata" - DynamoDB table name
//awsFileId - unique ID for use as DynamoDB primary key
//currentDate - the current date generated above
awsObj.dynamoInsertJson(ccdJsonData,"mirthdata",awsFileId,currentDate);
```

Links to additional implementation examples will be provided when available.
