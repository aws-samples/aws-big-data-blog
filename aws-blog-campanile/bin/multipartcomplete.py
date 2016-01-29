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

# -----------------------------------------------------------------------------
# Functions
# -----------------------------------------------------------------------------
def parts_to_xml(parts):
    s = '<CompleteMultipartUpload>\n'
    for part in sorted(parts, key=lambda x: x.part_number):
        s += '  <Part>\n'
        s += '    <PartNumber>%d</PartNumber>\n' % part.part_number
        s += '    <ETag>%s</ETag>\n' % part.etag
        s += '  </Part>\n'
    s += '</CompleteMultipartUpload>'
    return s

def main():
    ## Args
    parser = argparse.ArgumentParser()
    parser.add_argument('--bucket', required=True, help='Bucket')
    parser.add_argument('--endpoint', 
            default=boto.s3.connection.NoHostProvided, help='S3 endpoint')
    parser.add_argument('--profile', help='Boto profile used for connection')
    parser.add_argument('--dry-run', action="store_true",
            help='Do everything except complete multipart upload')
    args = parser.parse_args()

    ## S3 Connection
    bucket = S3Connection(suppress_consec_slashes=False,
            host=args.endpoint,is_secure=True,
            profile_name=args.profile).get_bucket(args.bucket)

    current_key  = { 'name' : None }
    mparts = []
    ## Process input
    for line in fileinput.input("-"):
        key = {}
        key['name'], key['etag'], key['mid'], part_etag, part, startbyte, \
                stopbyte = line.rstrip('\n').split('\t')[0:]
        
        ## Print to save partmap 
        print "%s" % line.rstrip('\n')

        ## Part object
        mpart = boto.s3.multipart.Part()
        mpart.part_number = int(part)
        mpart.etag = part_etag
        mpart.size = int(stopbyte) - int(startbyte)

        if key['name'] == current_key['name']:
            mparts.append(mpart)
            current_key = key
            continue

        if mparts:
            if args.dry_run:
                print "Complete %s:%s\n%s" % (current_key['name'], 
                        current_key['mid'],parts_to_xml(mparts))
            else:
                ## Added retry because partlist hard to recreate
                retry = 3
                while True:
                    try:
                        result = bucket.complete_multipart_upload(\
                                current_key['name'], current_key['mid'],
                                parts_to_xml(mparts))
                        if current_key['etag'] != \
                                result.etag.replace("\"", ""):
                            ## Add alert; Maybe wrong partsize
                            pass
                        campanile.status("%s:OK" % current_key['mid'])
                        break
                    except Exception, e:
                        if retry == 0:
                            raise
                        retry -= 1
                        campanile.status("%s:FAIL" % current_key['mid'])
                        campanile.random_sleep()
                        ## Lets try a new bucket connection 
                        bucket = S3Connection(suppress_consec_slashes=False,
                            host=args.endpoint,is_secure=True,
                            profile_name=args.profile).get_bucket(args.bucket)

        mparts = []
        mparts.append(mpart)
        current_key = key

    ## Complete upload
    if mparts:
        if args.dry_run:
            print "Complete %s:%s\n%s" % (current_key['name'], 
                    current_key['mid'],parts_to_xml(mparts))
        else:
            ## Added retry because partlist hard to recreate
            retry = 3 
            while True:
                try:
                    result = bucket.complete_multipart_upload(\
                            current_key['name'], current_key['mid'],
                            parts_to_xml(mparts))
                    if current_key['etag'] != result.etag.replace("\"", ""):
                        ## Add alert; Maybe wrong partsize
                        pass
                    campanile.status("%s:OK" % current_key['mid'])
                    break
                except Exception, e:
                    if retry == 0:
                        raise
                    retry -= 1
                    campanile.status("%s:FAIL" % current_key['mid'])
                    campanile.random_sleep()
                    ## Lets try a new bucket connection 
                    bucket = S3Connection(suppress_consec_slashes=False,
                        host=args.endpoint,is_secure=True,
                        profile_name=args.profile).get_bucket(args.bucket)

# -----------------------------------------------------------------------------
#  Main
# -----------------------------------------------------------------------------
if __name__ == "__main__":
        main()
