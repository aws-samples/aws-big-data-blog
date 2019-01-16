package com.amazonaws.services.lambda.utils;

import org.junit.Test;

import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;


public class TimeUnitParserTest {

    @Test
    public void shouldParseMillisFromString() throws ParseException
    {
        assertEquals( 10, TimeUnitParser.parse( "10ms", TimeUnit.MILLISECONDS ) );
        assertEquals( 10000, TimeUnitParser.parse( "10s", TimeUnit.MILLISECONDS ) );
        assertEquals( 60000, TimeUnitParser.parse( "1min", TimeUnit.MILLISECONDS ) );
        assertEquals( 3600000, TimeUnitParser.parse( "1h", TimeUnit.MILLISECONDS ) );
        assertEquals( 3600000, TimeUnitParser.parse( "1hr", TimeUnit.MILLISECONDS ) );
    }

    @Test
    public void shouldInterpretValueWithoutSuffixAsMillis()
    {
        assertEquals( 10, TimeUnitParser.parse( "10", TimeUnit.MILLISECONDS ) );
        assertEquals( 10, TimeUnitParser.parse( "10000", TimeUnit.SECONDS ) );
    }

    @Test
    public void shouldParseSecondsFromString() throws ParseException
    {
        assertEquals( 0, TimeUnitParser.parse( "10ms", TimeUnit.SECONDS ) );
        assertEquals( 10, TimeUnitParser.parse( "10s", TimeUnit.SECONDS ) );
        assertEquals( 60, TimeUnitParser.parse( "1min", TimeUnit.SECONDS ) );
        assertEquals( 3600, TimeUnitParser.parse( "1h", TimeUnit.SECONDS ) );
        assertEquals( 3600, TimeUnitParser.parse( "1hr", TimeUnit.SECONDS ) );
    }
}