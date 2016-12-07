from __future__ import print_function
from argparse import ArgumentParser
from pyspark.sql import SparkSession
from pyspark.conf import SparkConf
from pyspark import SparkContext


def convert_alleles_to_genotypes(record):
    '''
    Removes phasing information currently
    :param record: input record
    :return: reduced record that will be used for analysis
    '''
    allele_to_genotype = {'Ref': '0', 'Alt': '1', 'OtherAlt': 2}
    alleles = record.alleles
    genotypes = sorted([allele_to_genotype[_] for _ in alleles if _ in allele_to_genotype])
    # Ignore when len(genotypes is 0) or all are reference calls
    if not(len(genotypes) == 0 or (len(genotypes) == 1 and genotypes[0] == '0') or
            (len(genotypes) == 2 and genotypes[0] == genotypes[1] and genotypes[0] == '0')):
        if len(genotypes) == 1:
            gt0 = genotypes[0]
            gt1 = None
        else:
            gt0 = genotypes[0]
            gt1 = genotypes[1]
        yield {
            'chromosome': record['chromosome'],
            'startposition': record['start'],
            'endposition': record['end'],
            'referenceallele': record['ref'],
            'alternateallele': record['alt'],
            'sampleid': record['sampleId'],
            'genotype0': gt0,
            'genotype1': gt1
        }

if __name__ == "__main__":
    sc = SparkContext()

    try:
        spark = SparkSession.builder.config(conf=SparkConf()).getOrCreate()

        argparser = ArgumentParser()

        argparser.add_argument('--input_s3_path', type=str, help='Input Parquet Path', required=True)
        argparser.add_argument('--output_s3_path', type=str, help='Output Parquet Path', required=True)

        args = argparser.parse_args()

        df = spark.read.parquet(args.input_s3_path)
        df = df.repartition(20)

        # Select only the fields that we want to put into Athena
        athena_df = df.select(df.contigName.alias('chromosome'),
                              'start',
                              'end',
                              df.variant.referenceAllele.alias('ref'),
                              df.variant.alternateAllele.alias('alt'),
                              'sampleId',
                              'alleles')

        athena_df = spark.createDataFrame(athena_df.rdd.flatMap(lambda x: convert_alleles_to_genotypes(x)))

        athena_df.write.mode('overwrite').parquet(args.output_s3_path)
    finally:
        sc.stop()

