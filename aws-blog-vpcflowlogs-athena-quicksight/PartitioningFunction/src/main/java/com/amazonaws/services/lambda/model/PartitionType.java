package com.amazonaws.services.lambda.model;

import org.joda.time.DateTime;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public enum PartitionType {
    Month {
        @Override
        public String createSpec(DateTime dateTime) {
            return String.format("%04d-%02d-01-00", dateTime.getYear(), dateTime.getMonthOfYear());
        }

        @Override
        public String createPath(S3Object s3Object) {
            return createPartitionRoot(3, s3Object);
        }

        @Override
        public DateTime roundDownTimeUnit(DateTime value) {
            return value.withDayOfMonth(1)
                    .withHourOfDay(0)
                    .withMinuteOfHour(0)
                    .withSecondOfMinute(0)
                    .withMillisOfSecond(0);
        }
    },
    Day {
        @Override
        public String createSpec(DateTime dateTime) {
            return String.format("%04d-%02d-%02d-00", dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth());
        }

        @Override
        public String createPath(S3Object s3Object) {
            return createPartitionRoot(2, s3Object);
        }

        @Override
        public DateTime roundDownTimeUnit(DateTime value) {
            return value.withHourOfDay(0)
                    .withMinuteOfHour(0)
                    .withSecondOfMinute(0)
                    .withMillisOfSecond(0);
        }
    },
    Hour {
        @Override
        public String createSpec(DateTime dateTime) {
            return String.format("%04d-%02d-%02d-%02d", dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(), dateTime.getHourOfDay());
        }

        @Override
        public String createPath(S3Object s3Object) {
            return createPartitionRoot(1, s3Object);
        }

        @Override
        public DateTime roundDownTimeUnit(DateTime value) {
            return value.withMinuteOfHour(0)
                    .withSecondOfMinute(0)
                    .withMillisOfSecond(0);
        }
    };

    public static PartitionType parse(String value) {
        for (PartitionType partitionType : PartitionType.values()) {
            if (value.equalsIgnoreCase(partitionType.name())) {
                return partitionType;
            }
        }

        throw new IllegalArgumentException("Unrecognized PartitionType value: " + value);
    }

    public abstract String createSpec(org.joda.time.DateTime dateTime);

    public abstract String createPath(S3Object s3Object);

    public abstract DateTime roundDownTimeUnit(DateTime value);

    private static String createPartitionRoot(int endIndex, S3Object s3Object) {
        String newKey = IntStream.range(0, s3Object.keyLength() - endIndex)
                .mapToObj(s3Object::keyPart)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("/"));
        return String.format("s3://%s/%s/", s3Object.bucket(), newKey);
    }
}
