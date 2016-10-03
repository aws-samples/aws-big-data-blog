package com.amazonaws.bigdatablog.edba.emr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.collect.ImmutableMap;

import scala.collection.Seq;

public class ProcessVendorTrasactions implements Serializable {
	

	public static void run(String jobInputParam) throws Exception{
		
    	List<StructField> schemaFields = new ArrayList<StructField>();
    	schemaFields.add(DataTypes.createStructField("vendor_id", DataTypes.StringType, true));
    	schemaFields.add(DataTypes.createStructField("trans_amount", DataTypes.StringType, true));
    	schemaFields.add(DataTypes.createStructField("trans_type", DataTypes.StringType, true));
    	schemaFields.add(DataTypes.createStructField("item_id", DataTypes.StringType, true));
    	schemaFields.add(DataTypes.createStructField("trans_date", DataTypes.StringType, true));
    	StructType schema = DataTypes.createStructType(schemaFields);

    	SparkConf conf = new SparkConf().setAppName("Spark Redshift No Access-Keys");
    	SparkSession spark = SparkSession.builder().config(conf).getOrCreate();	
		JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());
				
		String redshiftJDBCURL=props.getProperty("redshift.jdbc.url");
		String s3TempPath = props.getProperty("s3.temp.path");
		System.out.println("props"+props);
		
		JavaRDD<Row> salesRDD = sc.textFile(jobInputParam).
				map(new Function<String,Row>(){public Row call(String saleRec){ String[] fields = saleRec.split(",");
			      return RowFactory.create(fields[0], fields[1],fields[2],fields[3],fields[4]);}});
		Dataset<Row> salesDF = spark.createDataFrame(salesRDD,schema);
		Dataset<Row> vendorItemSaleAmountDF = salesDF.filter(salesDF.col("trans_type").equalTo("4")).groupBy(salesDF.col("vendor_id"),salesDF.col("item_id"),salesDF.col("trans_date")).agg(ImmutableMap.of("trans_amount", "sum"));
		Dataset<Row> vendorItemTaxAmountDF = salesDF.filter(salesDF.col("trans_type").equalTo("5")).groupBy(salesDF.col("vendor_id"),salesDF.col("item_id"),salesDF.col("trans_date")).agg(ImmutableMap.of("trans_amount", "sum"));
		Dataset<Row> vendorItemDiscountAmountDF = salesDF.filter(salesDF.col("trans_type").equalTo("6")).groupBy(salesDF.col("vendor_id"),salesDF.col("item_id"),salesDF.col("trans_date")).agg(ImmutableMap.of("trans_amount", "sum"));
		String[] joinColArray = {"vendor_id","item_id","trans_date"};
		vendorItemSaleAmountDF.printSchema();
		Seq<String> commonJoinColumns = scala.collection.JavaConversions.asScalaBuffer(Arrays.asList(joinColArray)).seq();

		Dataset<Row> vendorAggregatedDF = vendorItemSaleAmountDF.join(vendorItemTaxAmountDF,commonJoinColumns,"left_outer")
								 .join(vendorItemDiscountAmountDF,commonJoinColumns,"left_outer")
								 .toDF("vendor_id","item_id","trans_date","sale_amount","tax_amount","discount_amount");
		
		vendorAggregatedDF.printSchema();
		DefaultAWSCredentialsProviderChain provider = new DefaultAWSCredentialsProviderChain();
		AWSSessionCredentials creds  = (AWSSessionCredentials) provider.getCredentials();
		
		String appendix=new StringBuilder(String.valueOf(System.currentTimeMillis())).append("_").append(String.valueOf(new Random().nextInt(10)+1)).toString();
		String vendorTransSummarySQL = new StringBuilder("begin transaction;delete from vendortranssummary using vendortranssummary_temp")
				 .append(appendix)
				 .append(" where vendortranssummary.vendor_id=vendortranssummary_temp")
				 .append(appendix)
				 .append(".vendor_id and vendortranssummary.item_id=vendortranssummary_temp")
				 .append(appendix)
				 .append(".item_id and vendortranssummary.trans_date = vendortranssummary_temp")
				 .append(appendix)
				 .append(".trans_date;")
				 .append("insert into vendortranssummary select * from vendortranssummary_temp")
				 .append(appendix)
				 .append(";drop table vendortranssummary_temp")
				 .append(appendix)
				 .append(";end transaction;").toString();
		vendorAggregatedDF.write().format("com.databricks.spark.redshift").option("url", redshiftJDBCURL)
	    .option("dbtable", "vendortranssummary_temp"+appendix)
	    .option("usestagingtable","false")
	    .option("postactions",vendorTransSummarySQL)
	    .option("temporary_aws_access_key_id", creds.getAWSAccessKeyId())
	    .option("temporary_aws_secret_access_key",creds.getAWSSecretKey())
	    .option("temporary_aws_session_token", creds.getSessionToken())
	    .option("tempdir", s3TempPath).mode(SaveMode.Overwrite).save();
			
	}

	public static void main(String[] args) throws Exception{
				
		String configBucket = args[0];
		String configPrefix = args[1];
		AmazonS3 s3Client = new AmazonS3Client();
		S3Object s3Object = s3Client.getObject(configBucket,configPrefix);
		props.load(s3Object.getObjectContent());
		ProcessVendorTrasactions.run(args[2]);
	}
    static Properties props = new Properties();
}
