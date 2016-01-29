#!/usr/bin/python2.7

import os
import sys
import platform
import re
import argparse
import logging
import pprint
from time import time

os.environ["BOTO_PATH"] = '/etc/boto.cfg:~/.boto:./.boto:~/.aws/credentials'
import boto
from boto import emr
# -----------------------------------------------------------------------------
#  Globals
# -----------------------------------------------------------------------------
FILES = '{0}/campanile.py,' \
    '{0}/bucketlist.py,' \
    '{0}/multipartlist.py,' \
    '{0}/objectcopy.py,' \
    '{0}/multipartcomplete.py,' \
    '{0}/.boto'
LIST_CMD = 'bucketlist.py'\
    ' --bucket {0}' \
    ' --endpoint {1}'
MULTIPARTLIST_CMD = 'multipartlist.py'\
    ' --src-bucket {0}' \
    ' --src-endpoint {1}' \
    ' --dst-bucket {2}' \
    ' --dst-endpoint {3}'
OBJECTCOPY_CMD = 'objectcopy.py'\
    ' --src-bucket {0}' \
    ' --src-endpoint {1}' \
    ' --dst-bucket {2}' \
    ' --dst-endpoint {3}'
MULTIPARTCOMPLETE_CMD = 'multipartcomplete.py'\
    ' --bucket {0}' \
    ' --endpoint {1}'
DEFAULT_BOTO_SEC = 'Credentials'
DEFAULT_AWS_SEC = 'default'

# -----------------------------------------------------------------------------
#  Functions
# -----------------------------------------------------------------------------
def bucketlist(name, files, cmd, input, output, lines=1,
        debug=False, timeout=300000):
    stepargs = [
        'hadoop-streaming',
        '-files', files,
        '-D', 'mapreduce.map.maxattempts=4',
        '-D', 'mapreduce.input.lineinputformat.linespermap=%i' % lines,
        '-D', 'mapreduce.map.speculative=false',
        '-D', 'mapreduce.task.timeout=%i' % timeout,
        '-inputformat', 'org.apache.hadoop.mapred.lib.NLineInputFormat'
    ]

    if debug:
        stepargs.append('-verbose')

    return emr.step.StreamingStep(name,
            cmd,
            action_on_failure='CANCEL_AND_WAIT',
            input=input,
            output=output,
            step_args = stepargs,
            jar="command-runner.jar")

def hive(name, hivefile, src, dst, diff):
    stepargs = [ 
        'hive-script', 
        '--run-hive-script',
        '--args',
        '-f', hivefile, 
        '-d', 'SRC=%s' % src,
        '-d', 'DST=%s' % dst,
        '-d', 'DIFF=%s' % diff
    ]
    return boto.emr.step.JarStep(name, 
        jar="command-runner.jar", 
        action_on_failure='CANCEL_AND_WAIT',
        step_args= stepargs)

def multipartlist(name, files, cmd, input, output, lines=1000,
        debug=False, timeout=300000):
    stepargs = [
        'hadoop-streaming',
        '-files', files,
        '-D', 'mapreduce.map.maxattempts=4',
        '-D', 'mapreduce.input.lineinputformat.linespermap=%i' % lines,
        '-D', 'mapreduce.map.speculative=false',
        '-D', 'mapreduce.task.timeout=%i' % timeout,
        '-inputformat', 'org.apache.hadoop.mapred.lib.NLineInputFormat'
    ]

    if debug:
        stepargs.append('-verbose')

    return emr.step.StreamingStep(name,
            cmd,
            action_on_failure='CANCEL_AND_WAIT',
            input=input,
            output=output,
            step_args = stepargs,
            jar="command-runner.jar"
            )

def objectcopy(name, files, cmd, input, output, rcmd,
        lines=10, debug=False, timeout=600000):
    stepargs = [
        'hadoop-streaming',
        '-files', files,
        '-D', 'mapreduce.map.maxattempts=6',
        '-D', 'mapreduce.task.timeout=%i' % timeout,
        '-D', 'mapreduce.map.speculative=false',
        '-D', 'mapreduce.job.reduces=1',
        '-D', 'mapreduce.output.fileoutputformat.compress=true',
        '-D', 'mapreduce.output.fileoutputformat.compress.codec=org.apache.hadoop.io.compress.GzipCodec',
        '-D', 'mapreduce.input.lineinputformat.linespermap=%i' % lines,
        '-inputformat', 'org.apache.hadoop.mapred.lib.NLineInputFormat'
    ]

    if debug:
        stepargs.append('-verbose')

    return emr.step.StreamingStep(name,
            cmd,
            reducer=rcmd,
            action_on_failure='CANCEL_AND_WAIT',
            input=input,
            output=output,
            step_args=stepargs,
            jar="command-runner.jar")

def bucket_endpoint(conn, name):
    try: 
        location = conn.get_bucket(name).get_location()
    except Exception, e:
        print "Bucket %s - %s" % (name, e)
        raise e
        
    if location is '':
        return 's3.amazonaws.com'
    else:
        return "s3-%s.amazonaws.com" % location

def cluster_id(string):
    if re.match("^j-[A-Z0-9]+$",string):
        return string
    else:
        raise argparse.ArgumentTypeError("Invalid cluster id format")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--job','-j', required=True, type=cluster_id,
            help='Job Id')
    parser.add_argument('--input', required=True,
            help='Input file')
    parser.add_argument('--code', required=True,
            help='Code path')
    parser.add_argument('--output', required=True,
            help='Output path')
    parser.add_argument('--src-bucket', required=True, dest='src',
            help='Source S3 bucket')
    parser.add_argument('--dst-bucket', required=True, dest='dst',
            help='Destination S3 bucket')
    parser.add_argument('--src-profile', 
            help='Boto profile used for source connection')
    parser.add_argument('--dst-profile', 
            help='Boto profile used for destination connection')
    parser.add_argument('--dry-run', action="store_true",
            help='Do everything but execute steps')
    parser.add_argument('--diff', action="store_true",
            help='Run diff before copy')
    parser.add_argument('--no-copy', action="store_true",
            help='Do everything but copy')
    parser.add_argument('--list-only', action="store_true",
            help='Run list step only')
    parser.add_argument('--uuid',
            help='Set uuid instead of generating it')
    parser.add_argument('--region', 
            help='Override default region for emr connection')
    parser.add_argument('--profile', 
            help='Profile to use for emr connection')
    parser.add_argument('--verbose', '-v', action="store_true",
            help='Log to console')
    parser.add_argument('--debug', '-d', action="store_true",
            help='Enable debug logging')
    args = parser.parse_args()

    ## Set region if defined, otherwise fallback on profile
    ## Default to ~/.aws/credentials, then .boto
    if args.region:
        region = args.region
    elif args.profile:
        region = boto.config.get('%s' % args.profile, 'region', \
                boto.config.get('profile %s' % args.profile, 'region', None))
    else:
        region = boto.config.get(DEFAULT_AWS_SEC, 'region', \
                boto.config.get(DEFAULT_BOTO_SEC, 'region', None))
        
    if args.uuid:
        uuid = args.uuid
    else:
        uuid = int(time())

    rundir = "/%s" % uuid
    print "RunDir: hdfs://%s" % rundir

    ## Setup logging 
    logger = logging.getLogger('rsync')
    logger.setLevel(logging.INFO)
    sh = logging.handlers.SysLogHandler("/var/run/syslog" \
            if platform.system() == 'Darwin' else "/dev/log",
            logging.handlers.SysLogHandler.LOG_LOCAL0)
    sh.setFormatter(logging.Formatter('%(module)s[%(process)d]: %(message)s'))
    logger.addHandler(sh)
    
    if args.debug:
        logger.setLevel(logging.DEBUG)
    
    if args.verbose:
        ch = logging.StreamHandler(stream=sys.stdout)
        ch.setFormatter(logging.Formatter('%(asctime)s %(module)s' + \
                '[%(process)d]: %(message)s', datefmt='%b %d %H:%M:%S'))
        logger.addHandler(ch) 

    try:
        src_endpoint = bucket_endpoint(boto.connect_s3(\
            profile_name=args.src_profile),args.src)
        dst_endpoint = bucket_endpoint(boto.connect_s3(\
                profile_name=args.dst_profile),args.dst)
    except:
        sys.exit(1)
   
    ## Create Commands 
    srclist_cmd = LIST_CMD.format(args.src, src_endpoint)
    dstlist_cmd = LIST_CMD.format(args.dst, dst_endpoint)
    multipartlist_cmd = MULTIPARTLIST_CMD.format(args.src, src_endpoint,
            args.dst, dst_endpoint)
    objectcopy_cmd = OBJECTCOPY_CMD.format(args.src, src_endpoint,
            args.dst, dst_endpoint)
    multipartcomplete_cmd = MULTIPARTCOMPLETE_CMD.format(args.dst,
            dst_endpoint)
           
    ## Add Profiles
    if args.src_profile is not None:
        srclist_cmd += " --profile %s" % args.src_profile
        multipartlist_cmd += " --src-profile %s" % args.src_profile
        objectcopy_cmd += " --src-profile %s" % args.src_profile

    if args.dst_profile is not None:
        dstlist_cmd += " --profile %s" % args.dst_profile
        multipartlist_cmd += " --dst-profile %s" % args.dst_profile
        objectcopy_cmd += " --dst-profile %s" % args.dst_profile
        multipartcomplete_cmd += " --profile %s" % args.dst_profile

    ## Inputs 
    partfile = os.path.split(args.input)[1]
    files = FILES.format(args.code.rstrip('\\'))
    hivefile = "%s/diff.q" % args.code.rstrip('\\')

    ## HDFS Outputs
    srclist = "%s/%s.list" % (rundir, args.src)
    dstlist = "%s/%s.list" % (rundir, args.dst)
    hivelist = "%s/diff.list" % (rundir)
    mpartlist = "%s/%s.multipartlist" % (rundir, args.src)
   
    ## HDSF inputs
    multipartlist_input = hivelist if args.diff else srclist

    ## Job output
    output = "%s.%s" % (os.path.join(args.output, partfile), uuid)
    
    ## jobsteps 
    jobsteps = []  

    while True:
        jobsteps.append(bucketlist("list.%s.%s" % (args.src, partfile),
                files,
                srclist_cmd,
                args.input,
                srclist,
                debug = args.debug))

        if args.list_only:
            break

        if args.diff:
            jobsteps.append(bucketlist("list.%s.%s" % (args.dst, partfile),
                        files,
                        dstlist_cmd,
                        args.input,
                        dstlist,
                        debug = args.debug))

            jobsteps.append(hive(\
                "hive.%s.%s" % (args.src, partfile),
                hivefile,
                srclist,
                dstlist,
                hivelist))
       
        if args.no_copy:
            print "CopyInput:%s" % multipartlist_input
            break

        jobsteps.append(multipartlist("mlist.%s.%s" % (args.src, partfile),
            files,
            multipartlist_cmd,
            multipartlist_input,
            mpartlist,
            debug = args.debug))

        jobsteps.append(objectcopy("copy.%s.%s" % (args.src, partfile),
            files,
            objectcopy_cmd,
            mpartlist,
            output,
            multipartcomplete_cmd,
            debug = args.debug))

        print "Output:%s" % output

        ## Exit loop
        break

    if args.debug or args.dry_run:
        pp = pprint.PrettyPrinter(indent=4)
        for i in jobsteps:
            pp.pprint(i.__dict__)

    if args.dry_run:
        sys.exit(0)

    ## Run job
    response = emr.connect_to_region(region,
            profile_name=args.profile).add_jobflow_steps(args.job, jobsteps)

    for i in response.stepids:
        print "Added step %s" % i.value

# -----------------------------------------------------------------------------
#  Main
# -----------------------------------------------------------------------------
if __name__ == "__main__":
    main()
