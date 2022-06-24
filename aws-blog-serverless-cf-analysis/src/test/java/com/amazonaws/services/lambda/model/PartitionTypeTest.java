package com.amazonaws.services.lambda.model;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PartitionTypeTest {

    @Test
    public void shouldFormatPartitionRoot() {
        assertEquals("s3://my-root/prefix/2016/12/",
                PartitionType.Month.createPath(new S3Object("my-root", "/prefix/2016/12/15/03/some-file.txt")));

        assertEquals("s3://my-root/prefix/2016/12/15/",
                PartitionType.Day.createPath(new S3Object("my-root", "/prefix/2016/12/15/03/some-file.txt")));

        assertEquals("s3://my-root/prefix/2016/12/15/03/",
                PartitionType.Hour.createPath(new S3Object("my-root", "/prefix/2016/12/15/03/some-file.txt")));
    }

    @Test
    public void shouldRoundDownDateTime() {
        DateTime initialValue = new DateTime(2016, 11, 28, 16, 53, 15, 555);

        assertEquals(new DateTime(2016, 11, 1, 0, 0), PartitionType.Month.roundDownTimeUnit(initialValue));
        assertEquals(new DateTime(2016, 11, 28, 0, 0), PartitionType.Day.roundDownTimeUnit(initialValue));
        assertEquals(new DateTime(2016, 11, 28, 16, 0), PartitionType.Hour.roundDownTimeUnit(initialValue));
    }
}