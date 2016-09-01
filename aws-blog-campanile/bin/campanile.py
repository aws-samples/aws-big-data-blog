import os
import re
import math
from sys import stderr
from time import sleep
from random import randint

# -----------------------------------------------------------------------------
# Global
# -----------------------------------------------------------------------------
ETAG_PARTCOUNT = re.compile('-(\d+)$')
NULL = '\N'
MAX_PARTS = 10000
MAX_SINGLE_UPLOAD_SIZE = 5 * (1024 ** 3)
DEFAULTS = {
    'multipart_threshold': 8 * (1024 ** 2),
    'multipart_chunksize': 8 * (1024 ** 2),
    's3n_blocksize' : 67108864,
    's3n_blocksizes' : [67108864]
}
CFGFILES = [
    "/etc/campanile.cfg"
]


# -----------------------------------------------------------------------------
# Progress Class - Mapper requires status message every xxx seconds or will 
#                  timeout.      
# -----------------------------------------------------------------------------
class FileProgress:
    def __init__(self, name, verbose=0):
        self.fp = 0
        self.total = None
        self.verbose = verbose
        self.name = name

    def progress(self, fp, total):
        self.total = total
        self.fp = fp
        if self.verbose == 1:
            stderr.write("reporter:status:%s:%s out of %s complete\n" % \
                    (self.name, fp, total))

# -----------------------------------------------------------------------------
# Functions
# -----------------------------------------------------------------------------
def random_sleep(maxsleep=5):
    sleep(randint(0,maxsleep))

def cli_chunksize(size, current_chunksize=DEFAULTS['multipart_chunksize']):
    chunksize = current_chunksize
    num_parts = int(math.ceil(size / float(chunksize)))
    while num_parts > MAX_PARTS:
        chunksize *= 2
        num_parts = int(math.ceil(size / float(chunksize)))
    if chunksize > MAX_SINGLE_UPLOAD_SIZE:
        return MAX_SINGLE_UPLOAD_SIZE
    else:
        return chunksize

def stream_index():
    try:
        if os.environ['mapred_input_format_class'] == \
                'org.apache.hadoop.mapred.lib.NLineInputFormat' and \
                os.environ['mapreduce_task_ismap'] == "true":
            return 1
    except:
        pass
    return 0

def counter(group, counter, amount):
    stderr.write("reporter:counter:%s,%s,%s\n" % (group, counter, amount))

def status(msg):
    stderr.write("reporter:status:%s\n" % msg)

def partcount(etag):
    match = ETAG_PARTCOUNT.search(etag.replace("\"", ""))
    if match:
        return int(match.group(1))
    else:
        return 0

def random_sleep(maxsleep=5):
    sleep(randint(0,maxsleep))

def config_section():
    return os.path.splitext(os.path.basename(sys.argv[0]))[0]

def cfg_file_locations():
    return list(CFGFILES)
