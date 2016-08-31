#!/usr/bin/python2.7

import argparse
import fileinput
import os
import sys

## Support for Streaming sandbox env
sys.path.append(os.environ.get('PWD'))
os.environ["BOTO_PATH"] = '/etc/boto.cfg:~/.boto:./.boto'
import campanile
import boto
from boto.s3.connection import S3Connection
from boto.utils import parse_ts

# -----------------------------------------------------------------------------
# Functions
# -----------------------------------------------------------------------------
def main():

    ## Args
    parser = argparse.ArgumentParser()
    parser.add_argument('--bucket', required=True, help='Bucket')
    parser.add_argument('--endpoint', 
            default=boto.s3.connection.NoHostProvided, help='S3 endpoint')
    parser.add_argument('--profile', help='Boto profile used for connection')
    args = parser.parse_args()

    ## S3 Connection
    bucket = S3Connection(suppress_consec_slashes=False,
            host=args.endpoint,is_secure=True,
            profile_name=args.profile).get_bucket(args.bucket)

    ## Hadoop Counters
    totalsize = 0
    
    ## In a Stream?
    start_index = campanile.stream_index()

    ## Process input
    for line in fileinput.input("-"):
        if line.startswith('#'):
            continue
        
        delim, prefix = line.rstrip('\n').split('\t')[start_index].split(',')
        for key in bucket.list(prefix=prefix,delimiter=delim):
            
            if key.__class__.__name__ == "Prefix":
                continue 

            ## Don't include glacier obejcts 
            if key.storage_class == 'GLACIER':
                continue 

            print "%s\t%s\t%s\t%s" % (key.name.encode('utf-8'), 
                    key.etag.replace("\"", ""), 
                    key.size, parse_ts(key.last_modified))

            ## Log stats
            campanile.counter(args.bucket, "Bytes", key.size)


# -----------------------------------------------------------------------------
#  Main
# -----------------------------------------------------------------------------
if __name__ == "__main__":
        main()
