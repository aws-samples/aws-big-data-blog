/*
 * Copyright 2013-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.hbase.kinesis.utils;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.rest.client.Client;
import org.apache.hadoop.hbase.rest.client.Cluster;
import org.apache.hadoop.hbase.rest.client.RemoteAdmin;
import org.apache.hadoop.hbase.rest.client.RemoteHTable;
import org.apache.hadoop.hbase.util.Bytes;


public class HBaseUtils {

	private static Log LOG = LogFactory.getLog(HBaseUtils.class);
	private static boolean isTableAvailable = false;
	
	
	/**
	 * Helper method to create an HBase table in an Amazon EMR cluster with HBase installed
	 * 
	 * @param tableName - name for table to create
	 * @param dnsId - Amazon EMR master node public DNS
	 * @param hbaseRestPort - HBase Rest port
	 */
	public static void createTable(String tableName, String dnsId, int hbaseRestPort) {
		Configuration config = HBaseConfiguration.create();
		RemoteAdmin admin = new RemoteAdmin(new Client(new Cluster().add(dnsId, hbaseRestPort)), config);
		String [] families = {"user", "address", "contact", "likes"};
		try {
			if (admin.isTableAvailable(tableName)) {
				LOG.info("table already exists!");
				return;
			} else {
				HTableDescriptor tableDesc = new HTableDescriptor(tableName);
				for (int i = 0; i < families.length; i++) {
					tableDesc.addFamily(new HColumnDescriptor(families[i]));
				}
				admin.createTable(tableDesc);
				isTableAvailable = true;
				LOG.info("create table " + tableName + " ok.");
			} 

		} catch (IOException e) {
			LOG.error(e, e.getCause()); 
		}

	}
	
	
	/**
	 * Helper method that checks if a table exists in HBase
	 * 
	 * @param tableName - table to check
	 * @param dnsId - Amazon EMR master node public DNS
	 * @param hbaseRestPort - HBase Rest port
	 * @return
	 */
	public static boolean tablesExists(String tableName, String dnsId, int hbaseRestPort) {
		Configuration config = HBaseConfiguration.create();
		RemoteAdmin admin = new RemoteAdmin(new Client(new Cluster().add(dnsId, hbaseRestPort)), config);
		try {
			if (admin.isTableAvailable(tableName)) {
				LOG.info("table already exists!");
				System.out.println("table already exists!");
				return true;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			LOG.error(e, e.getCause()); 
		}
		return false;
	}
	
	
	/**
	 * Helper method to insert a Record into HBase
	 * 
	 * @param tableName - table to insert records
	 * @param dnsId - Amazon EMR master node public DNS
	 * @param hbaseRestPort -  HBase Rest port
	 * @param rowKey - row unique identifier
	 * @param family - column family name
	 * @param qualifier - column qualifier
	 * @param value - value for this key pair
	 */
	public static void addRecord(String tableName, String dnsId, int hbaseRestPort, String rowKey, String family, String qualifier, String value) {		 
		RemoteHTable table = null;
		try {
			table = new RemoteHTable(new Client(new Cluster().add(dnsId, hbaseRestPort)), tableName);
			if (isTableAvailable) {
				Put put = new Put(Bytes.toBytes(rowKey));
				put.add(Bytes.toBytes(family), Bytes.toBytes(qualifier), Bytes.toBytes(value));
				table.put(put);
				table.flushCommits();
				LOG.info("insert recored " + rowKey + " to table "+ tableName + " ok.");
			}
			else createTable(tableName, dnsId, hbaseRestPort);
		} catch (IOException e) {
			LOG.error(e, e.getCause()); 
			if (table != null) {
				try {
					table.close();
				} catch (IOException e1) {
					LOG.error(e, e.getCause()); 
				}
			}
		}
	}
	
	
	/**
	 * Helper method to insert a list of records
	 * 
	 * @param tableName - table to insert
	  * @param dnsId - Amazon EMR master node public DNS
	 * @param hbaseRestPort -  HBase Rest port
	 * @param batch - list of records to insert into HBase
	 */
	public static void addRecords(String tableName, String dnsId, int hbaseRestPort, List<Put>batch) {
		RemoteHTable table = null;
		try {
			table = new RemoteHTable(new Client(new Cluster().add(dnsId, hbaseRestPort)), tableName);
			table.put(batch);
			LOG.info("inserted " + batch.size() + " records to table "+ tableName + " ok.");
			table.flushCommits();
		} catch (IOException e) {
			LOG.error(e, e.getCause()); 
			if (table != null)
				try {
					table.close();
				} catch (IOException e1) {
					LOG.error(e, e.getCause()); 
				}
		}
	}

}
