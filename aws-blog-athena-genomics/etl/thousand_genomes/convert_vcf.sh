#!/bin/bash

# You must specify the output path to write the parquet file in your command line

S3VCF=s3://1000genomes/release/20130502/ALL.chr22.phase3_shapeit2_mvncall_integrated_v5a.20130502.genotypes.vcf.gz
S3PARQUET=$1

adam-submit vcf2adam "${S3VCF}" "${S3PARQUET}"