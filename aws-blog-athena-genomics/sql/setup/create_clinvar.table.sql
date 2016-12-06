CREATE EXTERNAL TABLE demo.clinvar
(
    alleleId STRING
    ,variantType STRING
    ,hgvsName STRING
    ,geneID STRING
    ,geneSymbol STRING
    ,hgncId STRING
    ,clinicalSignificance STRING
    ,clinSigSimple STRING
    ,lastEvaluated STRING
    ,rsId STRING
    ,dbVarId STRING
    ,rcvAccession STRING
    ,phenotypeIds STRING
    ,phenotypeList STRING
    ,origin STRING
    ,originSimple STRING
    ,assembly STRING
    ,chromosomeAccession STRING
    ,chromosome STRING
    ,startPosition INT
    ,endPosition INT
    ,referenceAllele STRING
    ,alternateAllele STRING
    ,cytogenetic STRING
    ,reviewStatus STRING
    ,numberSubmitters STRING
    ,guidelines STRING
    ,testedInGtr STRING
    ,otherIds STRING
    ,submitterCategories STRING
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
STORED AS TEXTFILE
LOCATION 's3://<mytestbucket>/clinvar/';