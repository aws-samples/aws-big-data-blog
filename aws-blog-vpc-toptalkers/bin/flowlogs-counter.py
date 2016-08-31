#!/usr/bin/python2.7

import os
import sys
import fileinput
import shlex

## Index
SRC=3
DST=4
PORT=6

def stream_index():
    try:
        if os.environ['mapred_input_format_class'] == \
                'org.apache.hadoop.mapred.lib.NLineInputFormat' and \
                os.environ['mapreduce_task_ismap'] == "true":
            return 1
    except:
        pass
    return 0

def main(index=0):
    for line in fileinput.input("-"):
        try:
            i = shlex.split(line.rstrip('\r\n'))
            print "LongValueSum:%s:%s:%s\t%s" % (i[SRC+index], 
                    i[DST+index], i[PORT+index], 1)
        except:
            sys.stderr.write("reporter:status:err:%s" % line)
            raise

# -----------------------------------------------------------------------------
#  Main
# -----------------------------------------------------------------------------
if __name__ == "__main__":
    main(index=stream_index())
