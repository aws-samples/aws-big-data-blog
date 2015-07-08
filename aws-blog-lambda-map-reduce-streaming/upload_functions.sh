#!/bin/bash
if [ ""$1 = "" ]
then
   echo "Did not receive first parameter: role"
   exit
elif [ ""$2 = "" ]
then
   echo "Did not receive second parameter: region"
   exit
fi
zip Libs.zip cascade.js wordcount.js &&
aws lambda create-function \
   --region $2 \
   --function-name blog_cascade \
   --zip-file fileb://Libs.zip \
   --role $1 \
   --handler cascade.handler \
   --runtime nodejs \
   --timeout 60 \
   --debug \
   --memory-size 1024 &&
aws lambda create-function \
   --region $2 \
   --function-name wordcount \
   --zip-file fileb://Libs.zip \
   --role $1 \
   --mode event \
   --handler wordcount.handler \
   --runtime nodejs \
   --timeout 60 \
   --debug \
   --memory-size 1024