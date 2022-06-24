package com.amazonaws.services.lambda.model;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


public class S3ObjectTest {
    @Test
    public void shouldParseDateFromKey() {
        String key = "/prefix/2016/12/15/03/some-file.txt";

        S3Object s3Object = new S3Object("my-bucket", key);
        assertEquals(new DateTime(2016, 12, 15, 3, 0), s3Object.parseDateTimeFromKey());
    }

    @Test
    public void shouldIndicateWhetherKeyContainsDateTimePart() {
        String key = "/prefix/a/b/some-file.txt";

        S3Object s3Object = new S3Object("my-bucket", key);
        assertFalse(s3Object.hasDateTimeKey());
    }
}