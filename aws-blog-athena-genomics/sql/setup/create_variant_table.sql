CREATE EXTERNAL TABLE demo.samplevariants
(
  alternateallele STRING
  ,chromosome STRING
  ,endposition BIGINT
  ,genotype0 STRING
  ,genotype1 STRING
  ,referenceallele STRING
  ,sampleid STRING
  ,startposition BIGINT
)
STORED AS PARQUET
LOCATION 's3://<mytestbucket>/thousand_genomes/grch37_trimmed/chr22.parquet/';
