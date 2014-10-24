import urllib2
import ast
import psycopg2
import ConfigParser
import json
import threading
import sys
import re
import time
import datetime
import boto
import os
import boto.dynamodb2
from boto import kinesis
from boto.dynamodb2.fields import HashKey, RangeKey, KeysOnlyIndex, AllIndex
from boto.dynamodb2.table import Table
from boto.dynamodb2.items import Item
from boto.dynamodb2.types import NUMBER
from boto.s3.connection import S3Connection
from boto.sts.credentials import Credentials, FederationToken, AssumedRole
from boto.sts.credentials import DecodeAuthorizationMessage
from boto.kinesis.exceptions import ProvisionedThroughputExceededException

directory = "/home/ec2-user/.aws"
if not os.path.exists(directory):
	os.makedirs(directory)
	
#fetch config 
config = ConfigParser.ConfigParser()
try:
	config.read('/home/ec2-user/defaults.cfg')
	# build vars out of config
	dbUser = config.get('db','dbUser')
	dbPass = config.get('db','dbPassword')
	dbHost = config.get('db','dbHost')
	dbName = config.get('db','dbName')
	#dbPort = config.get('db01','port')
	stream_name = config.get('kenisis','steamName')
	tableName = config.get('dynamodb2','tableName')
	IAMRole = config.get('iam','roleName')
except:
	raise Exception('config not readable - see fetch config section') 

respURL = "http://169.254.169.254/latest/meta-data/iam/security-credentials/%s" % (IAMRole)
instURL = "http://169.254.169.254/latest/dynamic/instance-identity/document"

#metavirtual answers to many questions
inst = urllib2.urlopen(instURL).read()
inst = json.loads(inst)
region = str(inst['region'])
availabilityZone = str(inst['availabilityZone'])
instanceId = str(inst['instanceId'])

# replaces sts to get keys for instance 
resp=urllib2.urlopen(respURL).read()
resp=ast.literal_eval(resp)
id = str(resp['AccessKeyId'])
key = str(resp['SecretAccessKey'])
token = str(resp['Token'])

def makeauth():
	# replaces sts to get keys for instance 
	resp=urllib2.urlopen(respURL).read()
	resp=ast.literal_eval(resp)
	id = str(resp['AccessKeyId'])
	key = str(resp['SecretAccessKey'])
	token = str(resp['Token'])
	f = open('/home/ec2-user/.aws/credentials','w')
	f.write('[default]\n')
	f.write("aws_access_key_id = " + resp['AccessKeyId'])
	f.write("\naws_secret_access_key = " + resp['SecretAccessKey'])
	f.write("\naws_security_token = "+ resp['Token'])
	f.write("\n\n")
	f.write("[DynamoDB]\n")
	f.write("region = " + region)
	f.write("\n\n")
	f.close()

#write out keys for kinesis and dynamodb2 - auth didnt I get this to work without this file?
f = open('/home/ec2-user/.boto','w')
f.write('[Credentials]\n')
f.write("aws_access_key_id = " + resp['AccessKeyId'])
f.write("\naws_secret_access_key = " + resp['SecretAccessKey'])
f.write("\naws_security_token = "+ resp['Token'])
f.write("\n\n")
f.write("[DynamoDB]\n")
f.write("region = " + region)
f.write("\n\n")
f.close()
	# END SETUP

iter_type_at = 'AT_SEQUENCE_NUMBER'
iter_type_after = 'AFTER_SEQUENCE_NUMBER'
iter_type_trim = 'TRIM_HORIZON'
iter_type_latest = 'LATEST' 

db_args = {"host" : dbHost , "database" : dbName , "user" : dbUser , "password" : dbPass , "port" : "5439" }
ddb_args  = "region_name='%s',aws_access_key_id='%s',aws_secret_access_key='%s',aws_security_token='%s'"  % (region,id,key,token)	
kin_args  = {"aws_access_key_id" : id ,"aws_secret_access_key" : key } 

# is the dynamodb table there
try:
	print "Tring dynamodb connetion "
	ddb = boto.dynamodb.connect_to_region(ddb_args)
	print "connected to dynamodb\n"
except:
	print "dynamodb2 table is not ready\n"
	
# are the redshift hosts there
try:
	print "Tring Redshift endpint at : " + dbHost
	conn=psycopg2.connect(**db_args)
	cur = conn.cursor()
	print "Connected to redshift endpoint!\n"
except:
	print "Cannot connect to the redshift database.\n"
	
# connect to kinesis
try:
	print "Tring kinesis connection at: " + stream_name
	k_conn = boto.kinesis.connect_to_region(region, **kin_args)
	print "connected to kinesis\n"
except:
	print "failed to connect to stream\n"
	raise
	
#Existing redshift pointer in dynamodb?
tries = 0
result = []
try:
	bookmark= Table(tableName)
	db = bookmark.get_item(redShiftEndpoint=dbHost)
	sequenceNumber = db['sequenceNumber']
	shard_iterator_type=iter_type_after
	print "found a starting point - continuing for host :" + db['redShiftEndpoint']
	tries = 0
	while tries < 10:
		tries += 1
		time.sleep(1)
		try:
			makeauth()
			k_conn = boto.kinesis.connect_to_region(region)
			response = k_conn.describe_stream(stream_name)	
			if response['StreamDescription']['StreamStatus'] == "ACTIVE":
				break 
		except :
			print "error while trying to describe kinesis stream " + stream_name
			raise
		else:
			raise TimeoutError('Stream is still not active, aborting...')
	shard_ids = []
	stream_name = None 
	if response and 'StreamDescription' in response:
		stream_name = response['StreamDescription']['StreamName']					
		for shard_id in response['StreamDescription']['Shards']:
			shard_id = shard_id['ShardId']
			shard_iterator = k_conn.get_shard_iterator(stream_name, shard_id, shard_iterator_type, sequenceNumber)
			next_iterator = shard_iterator['ShardIterator']
			shard_ids.append({'shard_id' : shard_id ,'shard_iterator' : shard_iterator['ShardIterator'] })
	tries = 0
	result = []
	while tries < 100:
		
		try:
			response = k_conn.get_records(next_iterator, limit=1)
			next_iterator = response['NextShardIterator']
			bookmark= Table(tableName)
			if len(response['Records'])> 0:
				for res in response['Records']:
					dbrecord = bookmark.get_item(redShiftEndpoint=dbHost)
					print res['Data']
					dbrecord['next_iterator'] = next_iterator
					dbrecord['sequenceNumber'] = res['SequenceNumber']
					dbrecord.partial_save()

					try:
						with psycopg2.connect(**db_args) as conn:
							with conn.cursor() as curs:
								curs.execute(res['Data'])
					except:
						pass	
			else :
				print tries
		except ProvisionedThroughputExceededException as ptee:
			print (ptee.message)
			time.sleep(5)
except:
	print "No redshift tracks found - new redshift end point"
	print "Fresh install - starting from the top"
	try:
		bookmark= Table(tableName)
		dbrecord = Item(bookmark, data={"redShiftEndpoint" : dbHost , "next_iterator" : "0000", "sequenceNumber" : "0000"})
		dbrecord.save()
	except:
		pass
	shard_iterator_type=iter_type_trim
	tries = 0
	while tries < 10:
		tries += 1
		time.sleep(1)
		try:
			makeauth()
			k_conn = boto.kinesis.connect_to_region(region)
			response = k_conn.describe_stream(stream_name)	
			if response['StreamDescription']['StreamStatus'] == "ACTIVE":
				break 
		except :
			print "error while trying to describe kinesis stream " + stream_name
			raise
		else:
			raise TimeoutError('Stream is still not active, aborting...')
	shard_ids = []
	stream_name = None 
	if response and 'StreamDescription' in response:
		stream_name = response['StreamDescription']['StreamName']					
		for shard_id in response['StreamDescription']['Shards']:
			shard_id = shard_id['ShardId']
			shard_iterator = k_conn.get_shard_iterator(stream_name, shard_id, shard_iterator_type)
			next_iterator = shard_iterator['ShardIterator']
			shard_ids.append({'shard_id' : shard_id ,'shard_iterator' : shard_iterator['ShardIterator'] })
	tries = 0
	result = []
	while tries < 100:
		tries += 1
		try:
			response = k_conn.get_records(next_iterator, limit=1)
			next_iterator = response['NextShardIterator']
			bookmark= Table(tableName)
			if len(response['Records'])> 0:
				for res in response['Records']:
					dbrecord = bookmark.get_item(redShiftEndpoint=dbHost)
					dbrecord['next_iterator'] = next_iterator
					print res['Data']
					dbrecord['sequenceNumber'] = res['SequenceNumber']
					dbrecord.partial_save()
					try:
						with psycopg2.connect(**db_args) as conn:
							with conn.cursor() as curs:
								curs.execute(res['Data'])
					except:
						pass	
			else :
				print tries
		except ProvisionedThroughputExceededException as ptee:
			print (ptee.message)
			time.sleep(5)
		
