#Pattern 2#

Sensor data is generated randomally using the Kinesis Producer created using KPL
	-> Data in ingested into Kinesis stream 
			-> Lambda wakes up to Kinesis events and converts the incoming csv to JSON
					-> Pushed to Elastic Search				
						-> Kibana Dashboard
