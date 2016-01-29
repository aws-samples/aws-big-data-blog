#!/usr/bin/python2.7

import sys
import os
import fileinput
import argparse
import random
import tempfile
import ConfigParser

# -----------------------------------------------------------------------------
#  Support for Hadoop Streaming Sandbox Env
# -----------------------------------------------------------------------------
sys.path.append(os.environ.get('PWD'))
os.environ["BOTO_PATH"] = '/etc/boto.cfg:~/.boto:./.boto'
import campanile
import boto
from boto.s3.connection import S3Connection

# -----------------------------------------------------------------------------
# Global 
# -----------------------------------------------------------------------------
# cfgfiles      Config file search path
# -----------------------------------------------------------------------------
cfgfiles = [
    "/etc/campanile.cfg",
    "./campanile.cfg"
]

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
    parser.add_argument('--config', '-c', default="./campanile.cfg",
            help='Path to config file')
    args = parser.parse_args()

    ## Config Object
    cfgfiles = campanile.cfg_file_locations()
    cfgfiles.insert(0, args.config)
    c = ConfigParser.SafeConfigParser({'ephemeral':'/tmp'})
    c.read(cfgfiles)

    ## S3 Bucket Connections
    src_bucket = S3Connection(suppress_consec_slashes=False,\
            host=args.src_endpoint,is_secure=True,
            profile_name=args.src_profile).\
            get_bucket(args.src,validate=False)

    dst_bucket = S3Connection(suppress_consec_slashes=False,\
            host=args.dst_endpoint,is_secure=True,
            profile_name=args.dst_profile).\
            get_bucket(args.dst,validate=False) 

    ## Reporting Counters
    files = 0
    movedbytes = 0

    ## Select random tmpdir to distribute load across disks
    tmpdir = random.choice(c.get('DEFAULT',"ephemeral").split(','))

    start_index = campanile.stream_index()
    for line in fileinput.input("-"):
        name, etag, size, mtime, mid, part, partcount, startbyte, stopbyte \
                = line.rstrip('\n').split('\t')[start_index:]
        
        srckey = src_bucket.get_key(name, validate=False)
        dstkey = dst_bucket.get_key(name, validate=False)

        if mid == campanile.NULL:
                headers={}
                report_name = name
                expected_size = int(size)
        else:
            headers={'Range' : "bytes=%s-%s" % (startbyte, stopbyte)}
            report_name = "%s-%s" % (name, 'part')
            expected_size = int(stopbyte) - int(startbyte) + 1

        with tempfile.SpooledTemporaryFile(max_size=c.getint('DEFAULT',\
                                    'maxtmpsize'),dir=tmpdir) as fp:
            ## Download
            p = campanile.FileProgress(name, verbose=1)
            srckey.get_contents_to_file(fp, headers=headers, cb=p.progress)

            if fp.tell() != expected_size: 
                raise Exception("Something bad happened for %s. \
                        Expecting %s, but got %s" % \
                        (report_name, expected_size, fp.tell()))

            campanile.counter(args.src, "OutputBytes", size)
            fp.flush
            fp.seek(0)

            if mid == campanile.NULL:
                dstkey.cache_control= srckey.cache_control
                dstkey.content_type = srckey.content_type
                dstkey.content_encoding = srckey.content_encoding
                dstkey.content_disposition = srckey.content_disposition
                dstkey.content_language = srckey.content_language
                dstkey.metadata = srckey.metadata
                dstkey.md5 = srckey.md5
                report_name = name
            else:
                mp = boto.s3.multipart.MultiPartUpload(bucket=dst_bucket)
                mp.id = mid
                mp.key_name = name
                report_name = "%s-%s" % (name, part)

            ## Upload
            p = campanile.FileProgress(report_name, verbose=1)
            if mid == campanile.NULL:
                dstkey.set_contents_from_file(fp,
                        encrypt_key=srckey.encrypted, cb=p.progress)
                newetag = dstkey.etag.replace("\"","")
            else:
                mpart = mp.upload_part_from_file(fp,part_num=int(part),
                    cb=p.progress)
                newetag = mpart.etag.replace("\"","")

            if newetag != srckey.md5:
                ## Add alert
                raise Exception("Something bad happened for %s. \
                        Expecting %s md5, but got %s" % \
                        (report_name, srckey.md5, newetag))

            if mid != campanile.NULL:
                print "%s\t%s\t%s\t%s\t%s\t%s\t%s" % \
                        (name, etag, mid, newetag, part, startbyte, stopbyte)
            
            campanile.counter(args.dst, "InputBytes", expected_size)
            campanile.status("%s/%s:OK" % (args.dst,report_name))


# -----------------------------------------------------------------------------
#  Main
# -----------------------------------------------------------------------------
if __name__ == "__main__":
    main()
