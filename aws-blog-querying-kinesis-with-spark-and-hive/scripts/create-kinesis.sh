#!/bin/bash
function usage_and_exit {
	echo "usage $0 [stream name] [shard count]"
	exit 1
}

if [ "$#" -ne 2 ]; then
	usage_and_exit
fi

STREAM_NAME=$1
SHARD_COUNT=$2

aws kinesis create-stream --stream-name $STREAM_NAME --shard-count $SHARD_COUNT

