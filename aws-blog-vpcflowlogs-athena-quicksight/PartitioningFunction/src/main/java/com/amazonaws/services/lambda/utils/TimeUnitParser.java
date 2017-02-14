package com.amazonaws.services.lambda.utils;

import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.concurrent.TimeUnit;


public class TimeUnitParser {
    private static final PeriodFormatter PARSER = new PeriodFormatterBuilder()
            .appendMillis().appendSuffix("ms").appendSeparatorIfFieldsBefore(" ")
            .appendMinutes().appendSuffix("min").appendSeparatorIfFieldsBefore(" ")
            .appendSeconds().appendSuffix("s").appendSeparatorIfFieldsBefore(" ")
            .appendHours().appendSuffix("h", "hr").appendSeparatorIfFieldsBefore(" ")
            .appendDays().appendSuffix("d").appendSeparatorIfFieldsBefore(" ")
            .toFormatter();

    public static long parse(String value, TimeUnit timeUnit) {
        try {
            return timeUnit.convert(Long.parseLong(value), TimeUnit.MILLISECONDS);
        } catch (NumberFormatException e) {
            long millis = PARSER.parsePeriod(value.replace(" ", "")).toStandardDuration().getMillis();
            return timeUnit.convert(millis, TimeUnit.MILLISECONDS);
        }
    }
}

