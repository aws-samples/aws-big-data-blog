__author__ = 'Amo Abeyaratne'

import string
import random
import time
from datetime import datetime
from boto import kinesis


def random_generator(size=6, chars=string.ascii_lowercase + string.digits):
    return ''.join(random.choice(chars) for x in range(size))


 #connecting to Kinesis stream

#region = 'us-east-1'
region = 'ap-southeast-1'
kinesisStreamName = 'js-development-stream'

kinesis = kinesis.connect_to_region(region)

# generating data and feeding kinesis.

while True:


    y = random_generator(10,"techsummit2015")

    urls = ['foo.com','amazon.com','testing.com','google.com','sydney.com']
    x = random.randint(0,4)
    userid = random.randint(25,35)+1200

    now = datetime.now()
    timeformatted = str(now.month) + "/" + str(now.day) + "/" + str(now.year) + " " + str(now.hour) + ":" +str(now.minute) + ":" + str(now.second)


    #building the pay load for kinesis puts.

    putString = str(userid)+','+'www.'+urls[x]+'/'+y+','+timeformatted
    patitionKey = random.choice('abcdefghij')

    # schema of the imput string now userid,url,timestamp

    print putString

    result = kinesis.put_record(kinesisStreamName,putString,patitionKey)

    print result







