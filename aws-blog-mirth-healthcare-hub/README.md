# aws-blog-mirth-healthcare-hub

This repository contains code samples and Mirth Connect channels in support of blog post <LINK>

Information on [Mirth Connect can be found here.](https://www.mirth.com/)

####Two Java applications built with Maven are included
 * mirth-aws-dicom-app
 * mirth-aws-sample-app

Instructions to [build these packages using Maven](http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-project-maven.html).

Instructions for [invoking custom java code in Mirth Connect](http://www.mirthcorp.com/community/wiki/display/mirth/How+to+create+and+invoke+custom+Java+code+in+Mirth+Connect).

#####Quick build instructions once Maven and Java are installed and ready per instructions in AWS documentation.
```
git clone GIT URL
cd mirth-aws-sample-app
mvn clean package
```

#####mirth-aws-sample-app includes public functions:
 * putImageS3: copies a binary file to Amazon S3
 * putMirthS3: copies text to an Amazon S3 bucket file
 * dynamoInsertHl7Json: inserts HL7 JSON into Amazon DynamoDB
 * dynamoInsertJson: inserts CCD/CDA JSON into Amazon DynamoDB
 * dynamoInsertDicom: inserts DICOM JSON into Amazon DynamoDB
 * listBucketItems: lists items in an Amazon S3 bucket
 * createTmpFile:  creates a temporary file with text contents 

#####mirth-aws-dicom-app includes public functions:
 * dicomAttachProcess: saves DICOM image attachment as separate JPEG
 * dicomFileProcess: returns JSON formatted DICOM data

#####Setting Amazon S3 Bucket Destination
 * Under settings in Mirth Administrative Console, select Settings.
 * On the top menu list, select Configuration Map.
 * Click on Add to add a value.  
 * The Key should be s3BucketName with the value being the name of your Amazon S3 bucket. 
 * Create a directory and copy this jar file to the new directory. 

#####Add a new directory to your Mirth Connection configuration.
 * Create a new directory for your compiled Maven jar files and copy them to that directory
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

#####Mirth Channels
 * CCD-Pull.xml: File reader for processing CCD/CDA files
 * CCD-to-BB.xml: HTTP Sender which posts CDA data to bb-webapp and receives JSON response
 * BB-to-DynamoDB.xml: Saves CDA JSON response from bb-webapp to Amazon DynamoDB
 * CCD-to-S3.xml: Copies JSON converted CDA data for EMR/Athena use 
 * DICOM-Pull.xml: Processes DICOM files placed in directory
 * DICOM-to-JSON.xml: Converts DICOM data to JSON and stores in Amazon DynamoDB
 * DICOM-Image.xml: Saves DICOM image attachment to Amazon S3
 * HL7-Inbound.xml: File reader for HL7 messages as input and converts HL7 to JSON via hl7-webapp
 * HL7-to-DynamoDB.xml: Inserts JSON version of HL7 message into Amazon DynamoDB
