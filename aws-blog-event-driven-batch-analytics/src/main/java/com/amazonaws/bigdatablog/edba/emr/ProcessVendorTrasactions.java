package com.amazonaws.bigdatablog.edba.emr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.google.common.collect.ImmutableMap;

import scala.collection.Seq;

public class ProcessVendorTrasactions {

	final static String redshiftJDBCURL="jdbc:redshift://<<YourClusterEndPoint>>:5439/test?user=<<YourUser>>&password=<<YourPassword>>";
	public static void main(String[] args){
		
		    	SparkConf conf = new SparkConf().setAppName("Spark Redshift No Access-Keys");
		    	List<StructField> schemaFields = new ArrayList<StructField>();
		    	schemaFields.add(DataTypes.createStructField("vendor_id", DataTypes.StringType, true));
		    	schemaFields.add(DataTypes.createStructField("trans_amount", DataTypes.StringType, true));
		    	schemaFields.add(DataTypes.createStructField("trans_type", DataTypes.StringType, true));
		    	schemaFields.add(DataTypes.createStructField("item_id", DataTypes.StringType, true));
		    	schemaFields.add(DataTypes.createStructField("trans_date", DataTypes.StringType, true));
		    	StructType schema = DataTypes.createStructType(schemaFields);
		    	
				JavaSparkContext sc = new JavaSparkContext(conf);
				
				JavaRDD<Row> salesRDD = sc.textFile(args[0]).
						map(new Function<String,Row>(){public Row call(String saleRec){ String[] fields = saleRec.split(",");
					      return RowFactory.create(fields[0], fields[1],fields[2],fields[3],fields[4]);}});
				SQLContext sqlc = new SQLContext(sc);
				DataFrame salesDF = sqlc.createDataFrame(salesRDD,schema);
				DataFrame vendorItemSaleAmountDF = salesDF.filter(salesDF.col("trans_type").equalTo("4")).groupBy(salesDF.col("vendor_id"),salesDF.col("item_id"),salesDF.col("trans_date")).agg(ImmutableMap.of("trans_amount", "sum"));
				DataFrame vendorItemTaxAmountDF = salesDF.filter(salesDF.col("trans_type").equalTo("5")).groupBy(salesDF.col("vendor_id"),salesDF.col("item_id"),salesDF.col("trans_date")).agg(ImmutableMap.of("trans_amount", "sum"));
				DataFrame vendorItemDiscountAmountDF = salesDF.filter(salesDF.col("trans_type").equalTo("6")).groupBy(salesDF.col("vendor_id"),salesDF.col("item_id"),salesDF.col("trans_date")).agg(ImmutableMap.of("trans_amount", "sum"));
				String[] joinColArray = {"vendor_id","item_id","trans_date"};
				vendorItemSaleAmountDF.printSchema();
				Seq<String> commonJoinColumns = scala.collection.JavaConversions.asScalaBuffer(Arrays.asList(joinColArray)).seq();

				DataFrame vendorAggregatedDF = vendorItemSaleAmountDF.join(vendorItemTaxAmountDF,commonJoinColumns,"left_outer")
										 .join(vendorItemDiscountAmountDF,commonJoinColumns,"left_outer")
										 .toDF("vendor_id","item_id","trans_date","sale_amount","tax_amount","discount_amount");
				vendorAggregatedDF.printSchema();
				AWSSessionCredentials creds  = (AWSSessionCredentials) new InstanceProfileCredentialsProvider().getCredentials();
				
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
			    .option("tempdir", "s3n://event-driven-batch-analytics/temp/emr/redshift/").mode(SaveMode.Overwrite).save();
			

	}
}
