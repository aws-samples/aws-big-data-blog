#!/usr/bin/python

# Copyright 2017-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Amazon Software License (the "License"). You may not use this file
# except in compliance with the License. A copy of the License is located at
#
#     http://aws.amazon.com/asl/
#
# or in the "license" file accompanying this file. This file is distributed on an "AS IS"
# BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or implied. See the License
# for the specific language governing permissions and limitations under the License. 

# Import modules
from   loremipsum import get_sentences
import boto3
import names
import random
import string
import signal
import math
import time
import sys

# Global variables
dynamodb_table  = "TwitterAnalysis"
provisioned_wcu = 1

# Initiate DynamoDB client
client = boto3.client('dynamodb')

# Signal handler, Ctrl+c to quit
def signal_handler(signal, frame):
    print "\n"
    sys.exit(0)

signal.signal(signal.SIGINT, signal_handler)

# Actions
insert_to_ddb   = True;
print_to_screen = True;

# Start the loop to generate simulated tweets
while(True) :
    # Generate fake tweet
    user_id    = names.get_first_name()
    tweet_id   = str(random.randint(pow(10,16),pow(10,17)-1))
    created_at = time.strftime("%a %b %d %H:%M:%S +0000 %Y", time.gmtime())
    language   = random.choice(['de', 'en', 'es', 'fr', 'id', 'nl', 'pt', 'sk'])
    text       = str(get_sentences(1)[0])

    # Store tweet in DynamoDB
    if insert_to_ddb == True :
        res = client.put_item(
          TableName=dynamodb_table,
            Item={
              'user_id'   : { 'S' : user_id    },
              'tweet_id'  : { 'N' : tweet_id   },
              'created_at': { 'S' : created_at },
              'language'  : { 'S' : language   },
              'text'      : { 'S' : text       }
              })

    # Print output to screen
    if print_to_screen == True :
        print "insert_to_ddb: %s" % insert_to_ddb
        print "user_id      : %s" % user_id
        print "tweet_id     : %s" % tweet_id
        print "created_at   : %s" % created_at
        print "language     : %s" % language
        print "text         : %s" % (text[:77] + '...' if len(text) > 80 else text)
        print "\n==========================================="

    # Loop control
    time.sleep(1.0/provisioned_wcu)
