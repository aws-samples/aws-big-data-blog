package org.mirthblog.dicom;

import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.*;
import java.io.Writer;
import java.util.UUID;
import java.util.Iterator;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.xml.transform.TransformerConfigurationException;

import java.text.Format.Field;
import java.util.Iterator;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.io.BulkDataDescriptor;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.json.JSONReader;
import org.dcm4che3.json.JSONWriter;
import org.dcm4che3.tool.common.CLIUtils;
import org.dcm4che3.util.SafeClose;
import org.dcm4che3.tool.dcm2jpg.Dcm2Jpg;
import org.dcm4che3.tool.*;

/**
 * Mirth DICOM processing to JSON
 *
 */
public class MDICOM
{

    public static void main( String[] args ) throws IOException{
    {
        System.out.println( "Starting Test DICOM Processing..." );

        //Place a DICOM file in /tmp/ for testing
        //set the dicomFileName string to the name of the file.
        String dicomFilePath = "/tmp/";
        String dicomFilePathSrc = "/tmp/";
        String dicomFilePathDst = "/var/tmp/";
        String dicomFileName = "image.dcm";

	System.out.println("Processing File: "+dicomFilePath+dicomFileName);

        System.out.println("Saving DICOM Image Attachment to: /var/tmp/"+dicomFileName+".jpg");
        String attachRetVal = dicomAttachProcess(dicomFileName, dicomFilePathSrc, dicomFilePathDst);
        System.out.println("Processing Complete with: "+attachRetVal);

	String jsonDicomDoc = dicomFileProcess(dicomFileName,dicomFilePath);
	System.out.println(jsonDicomDoc);

        }
    }

    public static String dicomAttachProcess(String filename, String directoryNameSrc, String directoryNameDst) {
	String returnVal = directoryNameDst+filename+".jpg";
	Dcm2Jpg main = new Dcm2Jpg();
	String[] arguments = new String[] {directoryNameSrc+filename,directoryNameDst+filename+".jpg"};

	Dcm2Jpg.main(arguments);
	
        return returnVal;
    }

    //Entry point for the Mirth Connect destination/channel
    //fileName - string with the name of the file to be opened
    //directoryName - string with the full path to the file defined above
    public static String dicomFileProcess(String fileName, String directoryName) {
        String fullPath = directoryName+fileName;
        System.out.println(fullPath);
	String jsonDicomDoc = "EMPTY";

        try {
        File dicomFile = new File(fullPath);
        jsonDicomDoc = dicomJsonContent(dicomFile);
	dicomFile.delete();
	} catch (Exception e) { System.out.println("error opening file: "+fullPath); }

        return jsonDicomDoc;
    }

    public static String dicomJsonContent(File dicomFile) {
	String dicomJsonString = "";
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            DicomInputStream dis = new DicomInputStream(dicomFile);
            IncludeBulkData includeBulkData = IncludeBulkData.URI;
            dis.setIncludeBulkData(includeBulkData);

            JsonGenerator jsonGen = createGenerator(baos);
            JSONWriter jsonWriter = new JSONWriter(jsonGen);
            dis.setDicomInputHandler(jsonWriter);
            dis.readDataset(-1, -1);
            jsonGen.flush();
            dicomJsonString = new String( baos.toByteArray(), "UTF-8" );
            dis.close();
            } catch (Exception e) { System.out.println("error processing dicom"); }

        return dicomJsonString;
    }

    private static JsonGenerator createGenerator(OutputStream out) {
	Map<String, ?> conf = new HashMap<String, Object>(2);

        return Json.createGeneratorFactory(conf).createGenerator(out);
    }

}
