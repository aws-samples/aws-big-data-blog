SELECT
    count(*)/cast(numsamples AS DOUBLE) AS genotypefrequency
    ,cv.rsid
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
        FROM demo.samplevariants
        WHERE sampleid LIKE 'NA12%'))
JOIN demo.clinvar cv
ON sv.chromosome = cv.chromosome
    AND sv.startposition = cv.startposition - 1
    AND sv.endposition = cv.endposition
    AND sv.referenceallele = cv.referenceallele
    AND sv.alternateallele = cv.alternateallele
WHERE assembly='GRCh37'
    AND cv.clinicalsignificance LIKE '%response%'
    AND sampleid LIKE 'NA12%'
GROUP BY  sv.chromosome
          ,sv.startposition
          ,sv.endposition
          ,sv.referenceallele
          ,sv.alternateallele
          ,sv.genotype0
          ,sv.genotype1
          ,cv.clinicalsignificance
          ,cv.phenotypelist
          ,cv.rsid
          ,numsamples
ORDER BY  genotypefrequency DESC LIMIT 50
