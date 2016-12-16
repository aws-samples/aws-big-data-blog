#!/bin/bash

PARQUETPREFIX=$1

for i in chr22 chr1 chr2 chr3 chr4 chr5 chr6 chr7 chr8 chr9 chr10 chr11 chr12 chr13 chr14 chr15 chr16 chr17 chr18 chr19 chr20 chr21
do
    echo $i
    S3VCF=s3://1000genomes/release/20130502/ALL.${i}.phase3_shapeit2_mvncall_integrated_v5a.20130502.genotypes.vcf.gz
    S3PARQUET=$PARQUETPREFIX/${i}.parquet/

    adam-submit vcf2adam "${S3VCF}" "${S3PARQUET}"
done