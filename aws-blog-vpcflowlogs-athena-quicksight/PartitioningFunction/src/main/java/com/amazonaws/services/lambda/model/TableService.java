package com.amazonaws.services.lambda.model;

import com.amazonaws.services.lambda.utils.EnvironmentVariableUtils;

import java.sql.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;


public class TableService {

    public Collection<String> getExistingPartitions(String tableName) {

        Collection<String> existingPartitions = new HashSet<>();

        try (Connection conn = AthenaConnection.getConnection(); Statement statement = conn.createStatement()) {

            String sql = String.format("SHOW PARTITIONS %s", tableName);
            try (ResultSet rs = statement.executeQuery(sql)) {
                while (rs.next()) {
                    existingPartitions.add(
                            rs.getString(1).toUpperCase().replace(String.format("%S=", Partition.NAME), ""));
                }
            }

        } catch (Exception ex) {
            throw new IllegalStateException("An error occurred while getting existing partitions.", ex);
        }

        return existingPartitions;
    }

    public void addPartitions(String tableName, Collection<Partition> partitions) {

        try (Connection conn = AthenaConnection.getConnection(); Statement statement = conn.createStatement()) {
            for (Partition partition : partitions) {
                String sql = String.format("ALTER TABLE %s ADD IF NOT EXISTS PARTITION (%s='%s') LOCATION '%s'",
                        tableName, Partition.NAME, partition.spec(), partition.path());
                System.out.printf("SQL: %s%n", sql);
                statement.execute(sql);
                System.out.printf("New partition [Spec: %s, Path: %s]%n", partition.spec(), partition.path());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("An error occurred while adding partitions.", ex);
        }
    }

    public void removePartitions(String tableName, Collection<String> partitionSpecs) {

        try (Connection conn = AthenaConnection.getConnection(); Statement statement = conn.createStatement()) {
            for (String partitionSpec : partitionSpecs) {
                String sql = String.format("ALTER TABLE %s DROP IF EXISTS PARTITION (%s='%s')",
                        tableName, Partition.NAME, partitionSpec);
                System.out.printf("SQL: %s%n", sql);
                statement.execute(sql);
                System.out.printf("Removed partition [Spec: %s]%n", partitionSpec);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("An error occurred while removing partitions.", ex);
        }
    }

    private static class AthenaConnection {

        private static final String JDBC_DRIVER = "com.amazonaws.athena.jdbc.AthenaDriver";
        private static final String DATABASE_URL_TEMPLATE = "jdbc:awsathena://athena.%s.amazonaws.com:443";
        private static final String CREDENTIALS_PROVIDER = "com.amazonaws.auth.DefaultAWSCredentialsProviderChain";

        static Connection getConnection() throws ClassNotFoundException, SQLException {

            Class.forName(JDBC_DRIVER);

            String s3StagingDir = EnvironmentVariableUtils.getMandatoryEnv("S3_STAGING_DIR");
            String region = EnvironmentVariableUtils.getOptionalEnv("ATHENA_REGION", EnvironmentVariableUtils.getMandatoryEnv(("AWS_DEFAULT_REGION")));

            String databaseUrl = String.format(DATABASE_URL_TEMPLATE, region);

            Properties properties = new Properties();
            properties.put("s3_staging_dir", s3StagingDir);
            properties.put("aws_credentials_provider_class", CREDENTIALS_PROVIDER);

            return DriverManager.getConnection(databaseUrl, properties);
        }
    }
}
