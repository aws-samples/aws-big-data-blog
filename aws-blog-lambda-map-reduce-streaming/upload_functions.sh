#!/bin/sh
zip Libs.zip cascade.js wordcount.js &&
aws lambda upload-function \
   --region us-east-1 \
   --function-name blog_cascade \
   --function-zip Libs.zip \
   --role $1 \
   --mode event \
   --handler cascade.handler \
   --runtime nodejs \
   --timeout 60 \
   --debug \
   --memory-size 1024 &&
aws lambda upload-function \
   --region us-east-1 \
   --function-name wordcount \
   --function-zip Libs.zip \
   --role $1 \
   --mode event \
   --handler wordcount.handler \
   --runtime nodejs \
   --timeout 60 \
   --debug \
   --memory-size 1024
