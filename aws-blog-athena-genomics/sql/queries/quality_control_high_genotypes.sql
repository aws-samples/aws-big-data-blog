SELECT
    count(*)/cast(numsamples AS DOUBLE) AS genotypefrequency
    ,cv.clinicalsignificance
    ,cv.phenotypelist
    ,sv.chromosome
    ,sv.startposition
    ,sv.endposition
    ,sv.referenceallele
    ,sv.alternateallele
    ,sv.genotype0
    ,sv.genotype1
FROM demo.samplevariants sv
CROSS JOIN
    (SELECT count(1) AS numsamples
    FROM
        (SELECT DISTINCT sampleid
        FROM demo.samplevariants))
    JOIN demo.clinvar cv
    ON sv.chromosome = cv.chromosome
        AND sv.startposition = cv.startposition - 1
        AND sv.endposition = cv.endposition
        AND sv.referenceallele = cv.referenceallele
        AND sv.alternateallele = cv.alternateallele
WHERE assembly='GRCh37'
        AND cv.clinsigsimple='1'
GROUP BY  sv.chromosome ,
          sv.startposition
          ,sv.endposition
          ,sv.referenceallele
          ,sv.alternateallele
          ,sv.genotype0
          ,sv.genotype1
          ,cv.clinicalsignificance
          ,cv.phenotypelist
          ,numsamples
ORDER BY  genotypefrequency DESC LIMIT 50