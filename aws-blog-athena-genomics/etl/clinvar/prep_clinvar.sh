#!/usr/bin/env bash

ANNOPATH=s3://<mytestbucket>/clinvar/

wget ftp://ftp.ncbi.nlm.nih.gov/pub/clinvar/tab_delimited/variant_summary.txt.gz

# Need to strip out the first line (header)
gzcat variant_summary.txt.gz | sed '1d' > temp ; mv -f temp variant_summary.trim.txt ; gzip variant_summary.trim.txt

aws s3 cp variant_summary.trim.txt.gz $ANNOPATH --sse
