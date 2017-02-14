package com.amazonaws.services.lambda;

import com.amazonaws.services.lambda.model.ExpirationConfig;
import com.amazonaws.services.lambda.model.PartitionConfig;
import com.amazonaws.services.lambda.model.PartitionType;
import com.amazonaws.services.lambda.model.TableService;
import com.amazonaws.services.lambda.utils.Clock;
import com.amazonaws.services.lambda.utils.TimeUnitParser;
import org.joda.time.DateTime;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class RemoveAthenaPartitionsTest {


    @Test
    public void shouldAskTableServiceToRemoveExpiredPartitionsForMonthBasedPartitions() throws IOException {

        String tableName = "vpc_flow_logs";
        PartitionConfig partitionConfig = new PartitionConfig(PartitionType.Month, tableName);
        ExpirationConfig expirationConfig = new ExpirationConfig(TimeUnit.DAYS.toMillis(50));
        TableService tableService = mock(TableService.class);
        Clock clock = () -> new DateTime(2017, 1, 15, 13, 45, 10, 555);

        when (tableService.getExistingPartitions(tableName)).thenReturn(asList(
                "2017-01-01-00",
                "2016-12-01-00",
                "2016-11-01-00",
                "2016-10-01-00",
                "2016-09-01-00",
                "2016-08-01-00",
                "2016-07-01-00",
                "2016-06-01-00"

        ));

        RemoveAthenaPartitions lambda = new RemoveAthenaPartitions(partitionConfig, expirationConfig, tableService, clock);
        lambda.handleRequest(null, null, null);

        verify(tableService).removePartitions(tableName, new HashSet<>( asList(
                "2016-11-01-00",
                "2016-10-01-00",
                "2016-09-01-00",
                "2016-08-01-00",
                "2016-07-01-00",
                "2016-06-01-00")));
    }

    @Test
    public void shouldAskTableServiceToRemoveExpiredPartitionsForDayBasedPartitions() throws IOException {

        String tableName = "vpc_flow_logs";
        PartitionConfig partitionConfig = new PartitionConfig(PartitionType.Day, tableName);
        ExpirationConfig expirationConfig = new ExpirationConfig(TimeUnit.DAYS.toMillis(5));
        TableService tableService = mock(TableService.class);
        Clock clock = () -> new DateTime(2017, 1, 15, 13, 45, 10, 555);

        when (tableService.getExistingPartitions(tableName)).thenReturn(asList(
                "2017-01-15-00",
                "2017-01-14-00",
                "2017-01-13-00",
                "2017-01-12-00",
                "2017-01-11-00",
                "2017-01-10-00",
                "2017-01-09-00",
                "2017-01-08-00"
        ));

        RemoveAthenaPartitions lambda = new RemoveAthenaPartitions(partitionConfig, expirationConfig, tableService, clock);
        lambda.handleRequest(null, null, null);

        verify(tableService).removePartitions(tableName, new HashSet<>( asList(
                "2017-01-10-00",
                "2017-01-09-00",
                "2017-01-08-00")));
    }

    @Test
    public void shouldAskTableServiceToRemoveExpiredPartitionsForHourBasedPartitions() throws IOException {

        String tableName = "vpc_flow_logs";
        PartitionConfig partitionConfig = new PartitionConfig(PartitionType.Hour, tableName);
        ExpirationConfig expirationConfig = new ExpirationConfig(TimeUnit.HOURS.toMillis(5));
        TableService tableService = mock(TableService.class);
        Clock clock = () -> new DateTime(2017, 1, 15, 13, 45, 10, 555);

        when (tableService.getExistingPartitions(tableName)).thenReturn(asList(
                "2017-01-15-13",
                "2017-01-15-12",
                "2017-01-15-12",
                "2017-01-15-10",
                "2017-01-15-09",
                "2017-01-15-08",
                "2017-01-15-07",
                "2017-01-15-06"
        ));

        RemoveAthenaPartitions lambda = new RemoveAthenaPartitions(partitionConfig, expirationConfig, tableService, clock);
        lambda.handleRequest(null, null, null);

        verify(tableService).removePartitions(tableName, new HashSet<>( asList(
                "2017-01-15-08",
                "2017-01-15-07",
                "2017-01-15-06")));
    }
}