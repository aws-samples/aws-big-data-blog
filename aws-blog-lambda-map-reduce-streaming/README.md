# Streaming Map-reduce Using AWS Lambda

This code is used in the examples on the AWS Big Data Blog article "Building Scalable and Responsive Big Data Interfaces with AWS Lambda."

## Prerequisites
 - AWS account
 - AWS Command Line Interface (CLI)
 - node.js
 - An AMI to run the app on

## Installation
1. Create a role for Lambda execution.
1. Run the upload_functions.sh script using the role with permissions as the first and only argument to the script.
1. Install the required node.js modules with:
```
npm install
```
## Running
1. Run the app with: ```node app.js```
1. Browse to port 4001 on that machine. This should start the demo. The demo will initiate cascading Lambdas to search the public AWS bucket awssampledb and count the words. The results will stream to the browser.
1. If you want to run the demo on your own bucket or another public bucket, launch node with an environment variable like this: ```BUCKET=mybucket node app.js```.
