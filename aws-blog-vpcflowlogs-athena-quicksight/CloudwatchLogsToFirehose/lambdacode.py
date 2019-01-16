import boto3
import gzip
import json
import logging
import os
from StringIO import StringIO

firehose = boto3.client('firehose')

def lambda_handler(event, context):
    
    encodedLogsData = str(event['awslogs']['data'])    
    decodedLogsData = gzip.GzipFile(fileobj = StringIO(encodedLogsData.decode('base64','strict'))).read()
    allEvents = json.loads(decodedLogsData)
    
    records = []
    
    for event in allEvents['logEvents']:
        
        logEvent = {
            'Data': str(event['message']) + "\n"
        }
        
        records.insert(len(records),logEvent)
        
        if len(records) > 499:
            firehose.put_record_batch(
                    DeliveryStreamName = os.environ['DELIVERY_STREAM_NAME'],
                    Records = records
                )
            
            records = []
    
    if len(records) > 0:
        firehose.put_record_batch(
                DeliveryStreamName = os.environ['DELIVERY_STREAM_NAME'],
                Records = records
            )
