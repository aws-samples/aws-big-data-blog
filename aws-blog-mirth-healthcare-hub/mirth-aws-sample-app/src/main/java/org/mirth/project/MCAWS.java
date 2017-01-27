package org.mirth.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.FileInputStream;
import java.util.UUID;
import java.util.Iterator;
import org.apache.commons.io.IOUtils;
import org.json.*;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;



/**
 * Mirth Connect, AWS sample integrations
 *
 */
public class MCAWS 
{

    public static void main( String[] args ) {
	//String bucketName = "mirth-blog-data";
	System.out.println("Mirth Example Exec");
	//listBucketItems(bucketName);

	//Example for testing CCD:
        //try {
        //    FileInputStream inputStream = new FileInputStream("/var/mirth/ccd-bb/output/ccd.xml");
        //    ccdJson = IOUtils.toString(inputStream);
        //} catch (IOException e) { System.out.println("ERROR ON FILE"); e.printStackTrace(); }
	//dynamoInsertJson(ccdJson, "mirthdata", "222333444UUID", "10-26-2016");

	//Example pulling out CCD Details:
	//try {
	//JSONObject obj = new JSONObject(ccdJson);
	//String firstName = obj.getJSONObject("data").getJSONObject("demographics").getJSONObject("name").getString("first");
	//} catch (org.json.JSONException e) { System.out.println("JSON ERROR"); }

	//dynamoInsertJson(ccdJson, "mirthdata", "888222333444UUID", "10-26-2016");
	//System.out.println("Demographics: "+firstName+" "+lastName+" "+DOB);

	//String directory = "/tmp/";
	//String fileName = "test.txt";
	//String fileLocation = directory + fileName;
	//String key = "filename" + UUID.randomUUID() + ".txt";
	//String fileContents = "asdfhapfuapf-98rfiosafj\nap9sdfpa9udsfa\n98asdp9f8a\n";

	}

    public static void putImageS3(String bucketName, String key, String fileName) {
        AmazonS3 s3 = new AmazonS3Client();
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        //Region usWest2 = Region.getRegion(s3Region);
        s3.setRegion(usWest2);
        try {
            File file = new File(fileName);
            s3.putObject(new PutObjectRequest(bucketName, key, file));
        } catch (Exception e) { System.out.println("ERROR ON IMAGE FILE"); }
    }

    public static void putMirthS3(String bucketName, String key, String fileLocation, String fileContents) {
	AmazonS3 s3 = new AmazonS3Client();
	Region usWest2 = Region.getRegion(Regions.US_WEST_2);
	s3.setRegion(usWest2);
	try {
	s3.putObject(new PutObjectRequest(bucketName, key, createTmpFile(fileContents)));
	} catch (Exception e) { System.out.println("ERROR ON TEXT FILE"); }
    }

    public static void dynamoInsertHl7Json(String hl7Json, String mirthTable, String mirthId, String mirthDate) {
	String firstName = "NONE";
        String lastName = "NONE";
        String dob = "NONE";
        String docType = "hl7";
	String messageType = "NONE";

        AmazonDynamoDBClient client = new AmazonDynamoDBClient();
        client.withRegion(Regions.US_WEST_2);
        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable(mirthTable);
        try {
        JSONObject obj = new JSONObject(hl7Json);
	
	firstName = obj.getJSONObject("HL7Message").getJSONObject("PID").getJSONObject("PID.5").getString("PID.5.2");
	lastName = obj.getJSONObject("HL7Message").getJSONObject("PID").getJSONObject("PID.5").getString("PID.5.1");
	dob = obj.getJSONObject("HL7Message").getJSONObject("PID").getJSONObject("PID.7").getString("PID.7.1");
	messageType = obj.getJSONObject("HL7Message").getJSONObject("MSH").getJSONObject("MSH.9").getString("MSH.9.1");
        } catch (org.json.JSONException e) { System.out.println("HL7 JSON ERROR"); }        

	//replace empyty string with value representing blank
        hl7Json = hl7Json.replaceAll("\"\"","\"NONE\"");

        Item item =
            new Item()
                .withPrimaryKey("mirthid", mirthId)
                .withString("mirthdate", mirthDate)
                .withString("type", docType)
                .withString("FirstName", firstName)
                .withString("LastName", lastName)
                .withString("DOB", dob)
                .withString("HL7Type", messageType)
                .withString("Processed", "N")
                .withJSON("document", hl7Json);

        table.putItem(item);
    }

    public static void dynamoInsertJson(String ccdJson, String mirthTable, String mirthId, String mirthDate) {
        System.out.println( "Performing insert into DynamoDB" );
	String firstName = "NONE";
	String lastName = "NONE";
	String dob = "NONE";
	String docType = "ccda";

        AmazonDynamoDBClient client = new AmazonDynamoDBClient();
        client.withRegion(Regions.US_WEST_2);
        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable(mirthTable);

	//System.out.println(ccdJson);

        try {
        JSONObject obj = new JSONObject(ccdJson);
	
        firstName = obj.getJSONObject("data").getJSONObject("demographics").getJSONObject("name").getString("first");
        lastName = obj.getJSONObject("data").getJSONObject("demographics").getJSONObject("name").getString("last");
        dob = obj.getJSONObject("data").getJSONObject("demographics").getJSONObject("dob").getJSONObject("point").getString("date");

        //System.out.println(firstName);
        } catch (org.json.JSONException e) { System.out.println("JSON ERROR"); }

	ccdJson = ccdJson.replaceAll("\"\"","\"NONE\"");

        Item item = 
            new Item()
                .withPrimaryKey("mirthid", mirthId)
                .withString("mirthdate", mirthDate)
		.withString("type", docType)
                .withString("FirstName", firstName)
                .withString("LastName", lastName)
                .withString("DOB", dob)
                .withString("Processed", "N")
                .withJSON("document", ccdJson);

        table.putItem(item);
    }

    public static void dynamoInsertDicom(String dicomJson, String mirthTable, String mirthId, String mirthDate) {
        System.out.println( "Performing insert into DynamoDB" );
        String firstName = "EMPTY";
        String lastName = "EMPTY";
        String dob = "EMPTY";
        String docType = "dicom";

        AmazonDynamoDBClient client = new AmazonDynamoDBClient();
        client.withRegion(Regions.US_WEST_2);
        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable(mirthTable);

        try {
        JSONObject obj = new JSONObject(dicomJson);

	//DICOM stores patient name in tag 00100010
        if(obj.has("00100010")) {
                if(obj.getJSONObject("00100010").has("Value")) {
                        JSONArray lastNameArray = obj.getJSONObject("00100010").getJSONArray("Value");
                        if(lastNameArray !=null) {
                                JSONObject lastNameObj = lastNameArray.getJSONObject(0);
                                if(lastNameObj.has("Alphabetic")) {
                                        String patientName = lastNameObj.getString("Alphabetic");
                                        String[] patientNameArray = patientName.split("\\^");

					//some sample DICOM files only have one name string rather than 
					//delimited strings like in production messages.
					//in that case, we use that string as the last name
					//the else statement covers when we have a first and last name
                                        if(patientNameArray.length == 1) {
                                                lastName = lastNameObj.getString("Alphabetic");
                                        } else if(patientNameArray.length > 1) {
                                                lastName = patientNameArray[0];
                                                firstName = patientNameArray[1];
                                                }
                                        }
                                }
                        }
                }

	//DICOM stores Date of Birth in tag 00100030
        if(obj.has("00100030")) {
                if(obj.getJSONObject("00100030").has("Value")) {
                        JSONArray dobArray = obj.getJSONObject("00100030").getJSONArray("Value");
                        if(dobArray !=null) {
				dob = dobArray.getString(0);
                                }
                        }
                }

        } catch (org.json.JSONException e) { System.out.println("JSON ERROR"); }

        //ccdJson = ccdJson.replaceAll("\"\"","\"NONE\"");

        Item item =
            new Item()
                .withPrimaryKey("mirthid", mirthId)
                .withString("mirthdate", mirthDate)
                .withString("type", docType)
                .withString("FirstName", firstName)
                .withString("LastName", lastName)
                .withString("DOB", dob)
                .withString("Processed", "N")
                .withJSON("document", dicomJson);

        table.putItem(item);
    }

    public static void listBucketItems(String bucketName) {
	System.out.println( "Connecting to AWS" );
	System.out.println( "Listing files in bucket "+ bucketName );
	AmazonS3 s3 = new AmazonS3Client();
	Region usWest2 = Region.getRegion(Regions.US_WEST_2);
	s3.setRegion(usWest2);
	System.out.println("Listing buckets");
	ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
		.withBucketName(bucketName));
	for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
		System.out.println(" - " + objectSummary.getKey() + "  " +
		"(size = " + objectSummary.getSize() + ")");
		}
	System.out.println();
	}

    private static File createTmpFile(String fileContents) throws IOException {
        File file = File.createTempFile("mirth-ccd-tmp-", ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write(fileContents);
        writer.close();

        return file;
    }

}
