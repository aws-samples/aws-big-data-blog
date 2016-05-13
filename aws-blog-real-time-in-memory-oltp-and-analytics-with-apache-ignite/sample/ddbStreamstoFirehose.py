from __future__ import print_function

import base64
import json
import boto3

print('Loading function')

client = boto3.client('firehose')

def lambda_handler(event, context):
    #print("Received event: " + json.dumps(event, indent=2))
    for record in event['Records']:
        # Kinesis data is base64 encoded so decode here
        #print(json.dumps(record['dynamodb'], indent=2))
        payload = json.loads(json.dumps(record['dynamodb'], indent=2))
        csvrecord = ''
        if payload['NewImage'].get('OrderId') != None:
            csvrecord = csvrecord+str(payload['NewImage']['OrderId']['S'])+','
        if payload['NewImage'].get('OrderDate') != None:
            csvrecord = csvrecord+str(payload['NewImage']['OrderDate']['N'])+','
        if payload['NewImage'].get('ShipMethod') != None:
            csvrecord = csvrecord+str(payload['NewImage']['ShipMethod']['S'])+','
        if payload['NewImage'].get('BillAddress') != None:
            csvrecord = csvrecord+str(payload['NewImage']['BillAddress']['S']).replace('\n',' ')+','
        if payload['NewImage'].get('BillCity') != None:
            csvrecord = csvrecord+str(payload['NewImage']['BillCity']['S'])+','
        if payload['NewImage'].get('BillPostalCode') != None:
            csvrecord = csvrecord+str(payload['NewImage']['BillPostalCode']['N'])+','
        if payload['NewImage'].get('OrderQty') != None:
            csvrecord = csvrecord+str(payload['NewImage']['OrderQty']['N'])+','
        if payload['NewImage'].get('UnitPrice') != None:
            csvrecord = csvrecord+str(payload['NewImage']['UnitPrice']['N'])+','
        if payload['NewImage'].get('ProductCategory') != None:
            csvrecord = csvrecord+str(payload['NewImage']['ProductCategory']['S'])+','
        csvrecord = csvrecord[:-1]+'\n'
        response = client.put_record(
            DeliveryStreamName='<Firehose_Delivery_Stream_Name>',
            Record={
                'Data': csvrecord
            }
        )
    return response