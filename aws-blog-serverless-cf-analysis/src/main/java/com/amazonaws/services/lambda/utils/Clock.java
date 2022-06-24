package com.amazonaws.services.lambda.utils;

import org.joda.time.DateTime;

public interface Clock {

    public static Clock SystemClock = new Clock() {
        @Override
        public DateTime now() {
            return DateTime.now();
        }
    };

    DateTime now();
}
