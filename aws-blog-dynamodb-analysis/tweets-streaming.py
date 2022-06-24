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
from   twitter import Twitter, OAuth, TwitterHTTPError, TwitterStream
import boto3
import signal
import time
import sys

# Twitter security credentials 
ACCESS_TOKEN    = "...01234..."
ACCESS_SECRET   = "...i7RkW..."
CONSUMER_KEY    = "...be4Ma..."
CONSUMER_SECRET = "...btcar..."

# Global variables.
dynamodb_table     = "TwitterAnalysis"
expires_after_days = 30

# Authenticate and initialize stream
oauth  = OAuth(ACCESS_TOKEN, ACCESS_SECRET, CONSUMER_KEY, CONSUMER_SECRET)
stream = TwitterStream(auth=oauth)
tweets = stream.statuses.sample()

# Initiate DynamoDB client
client = boto3.client('dynamodb')

# Signal handler, Ctrl+c to quit
def signal_handler(signal, frame):
    print "\n"
    sys.exit(0)

signal.signal(signal.SIGINT, signal_handler)

# Routing. Also for easy block commenting.
insert_to_ddb   = True;
print_to_screen = True;

# Start the loop to get the tweets.
for tweet in tweets :
    try :
        # Get tweet data
        user_id      = tweet["user"]["screen_name"]
        tweet_id     = tweet["id_str"]
        created_at   = tweet["created_at"]
        timestamp_ms = tweet["timestamp_ms"]
        language     = tweet["lang"]
        text         = tweet["text"]
        hts          = tweet["entities"]["hashtags"]

        # Expire items in the future, calculated in milliseconds
        ttl_value    = str((int(timestamp_ms)/1000)+(expire_after_days*86400000))

        # Process hashtags
        hashtags = ['None']
        if len(hts) != 0 :
            hashtags.pop()
            for ht in hts :
                hashtags.append(str(ht["text"]))

        # Store tweet in DynamoDB
        if insert_to_ddb == True :
            res = client.put_item(
              TableName=dynamodb_table,
              Item={
                'user_id'   : { 'S' : user_id    },
                'tweet_id'  : { 'N' : tweet_id   },
                'created_at': { 'S' : created_at },
                'ttl_value' : { 'N' : ttl_value  },
                'language'  : { 'S' : language   },
                'text'      : { 'S' : text       },
                'hashtags'  : { 'SS': hashtags   }
              }) 

        # Print output to screen
        if print_to_screen == True :
            print "insert_to_ddb: %s" % insert_to_ddb
            print "user_id      : %s" % user_id
            print "tweet_id     : %s" % tweet_id
            print "created_at   : %s" % created_at
            print "timestamp_ms : %s" % timestamp_ms
            print "language     : %s" % language
            print "text         : %s" % (text[:77] + '...' if len(text) > 80 else text)
            print "hashtags     : %s" % hashtags
            print "\n==========================================="

    except Exception :
        pass
