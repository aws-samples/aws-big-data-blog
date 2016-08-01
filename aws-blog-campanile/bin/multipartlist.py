#!/usr/bin/python2.7

import sys
import os
import fileinput
import argparse
import math
import uuid

# -----------------------------------------------------------------------------
#  Support for Hadoop Streaming Sandbox Env
# -----------------------------------------------------------------------------
sys.path.append(os.environ.get('PWD'))
os.environ["BOTO_PATH"] = '/etc/boto.cfg:~/.boto:./.boto'
import campanile
import boto
from boto.s3.connection import S3Connection

# -----------------------------------------------------------------------------
# Functions
# -----------------------------------------------------------------------------
def main():

    ## Args
    parser = argparse.ArgumentParser()
    parser.add_argument('--src-bucket', required=True, dest='src',
            help='Source S3 bucket')
    parser.add_argument('--dst-bucket', required=True, dest='dst',
            help='Destination S3 bucket')
    parser.add_argument('--src-endpoint', 
            default=boto.s3.connection.NoHostProvided,
            help='S3 source endpoint')
    parser.add_argument('--dst-endpoint', 
            default=boto.s3.connection.NoHostProvided,
            help='S3 destination endpoint')
    parser.add_argument('--src-profile', 
            help='Boto profile used for source connection')
    parser.add_argument('--dst-profile', 
            help='Boto profile used for destination connection')
    parser.add_argument('--dry-run',  action="store_true",
            help='Auto generate multipart-uid')
    args = parser.parse_args()

    ## S3 Bucket Connections
    src_bucket = S3Connection(suppress_consec_slashes=False,\
            host=args.src_endpoint,is_secure=True,
            profile_name=args.src_profile).\
            get_bucket(args.src,validate=False)

    dst_bucket = S3Connection(suppress_consec_slashes=False,\
            host=args.dst_endpoint,is_secure=True,
            profile_name=args.dst_profile).\
            get_bucket(args.dst,validate=False) 

    start_index = campanile.stream_index()
    for line in fileinput.input("-"):
        record = line.rstrip('\n').split('\t')[start_index:]
        name, etag, size = record[0:3]

        partcount = campanile.partcount(etag)
        if partcount == 0:
            print '\t'.join(record + [campanile.NULL] * 5)
            continue

        ## Find partsize
        partsize = campanile.cli_chunksize(int(size))
        if partcount != int(math.ceil(float(size)/partsize)):
            campanile.status("Can't calculate partsize for %s/%s\n" %
                    (args.src, name))
            ## Add alert
            continue

        if args.dry_run:
            mid = uuid.uuid1()
        else:
            srckey = src_bucket.get_key(name, validate=True)
            metadata = srckey.metadata
            headers = {}
            
            ## Set Cache and Content Values
            if srckey.cache_control is not None:
                headers['Cache-Control'] = srckey.cache_control
            if srckey.content_type is not None:
                headers['Content-Type'] = srckey.content_type
            if srckey.content_encoding is not None:
                headers['Content-Encoding'] = srckey.content_encoding
            if srckey.content_disposition is not None:
                headers['Content-Disposition'] = srckey.content_disposition
            if srckey.content_language is not None:
                headers['Content-Language'] = srckey.content_language

            ## Initiate Multipart Upload
            mid = dst_bucket.initiate_multipart_upload(name,
                headers = headers,
                metadata = metadata,
                encrypt_key = srckey.encrypted).id

        for i in range(partcount):
            offset = partsize * i
            bytes = min(partsize, int(size) - offset)
            print '\t'.join(record) + "\t%s\t%s\t%s\t%s\t%s" % (mid, 
                    (i+1), partcount, offset, (offset + bytes - 1))


# -----------------------------------------------------------------------------
#  Main
# -----------------------------------------------------------------------------
if __name__ == "__main__":
    main()
