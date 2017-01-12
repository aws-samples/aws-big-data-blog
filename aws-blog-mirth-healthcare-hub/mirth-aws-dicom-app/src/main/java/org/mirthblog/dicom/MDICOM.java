package org.mirthblog.dicom;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.json.JSONWriter;
import org.dcm4che3.tool.dcm2jpg.Dcm2Jpg;

/**
 * Mirth DICOM processing to JSON
 *
 */
public class MDICOM {

  public static void main(String[] args) throws IOException {
    System.out.println("Starting Test DICOM Processing...");

    //Place a DICOM file in /tmp/ for testing
    //set the dicomFileName string to the name of the file.
    String dicomFilePath = "/tmp/";
    String dicomFilePathSrc = "/tmp/";
    String dicomFilePathDst = "/var/tmp/";
    String dicomFileName = "image.dcm";

    System.out.println("Processing File: " + dicomFilePath + dicomFileName);

    System.out.println("Saving DICOM Image Attachment to: /var/tmp/" + dicomFileName + ".jpg");
    String attachRetVal = dicomAttachProcess(dicomFileName, dicomFilePathSrc, dicomFilePathDst);
    System.out.println("Processing Complete with: " + attachRetVal);

    String jsonDicomDoc = dicomFileProcess(dicomFileName, dicomFilePath);
    System.out.println(jsonDicomDoc);
  }

  public static String dicomAttachProcess(String filename, String directoryNameSrc, String directoryNameDst) {
    String returnVal = directoryNameDst + filename + ".jpg";
    String[] arguments = {
      directoryNameSrc + filename,
      directoryNameDst + filename + ".jpg"
    };

    Dcm2Jpg.main(arguments);

    return returnVal;
  }

  /**
   * Entry point for the Mirth Connect destination/channel
   * @param fileName string with the name of the file to be opened
   * @param directoryName string with the full path to the file defined above
   * @return
   */
  public static String dicomFileProcess(String fileName, String directoryName) {
    String fullPath = directoryName + fileName;
    System.out.println(fullPath);
    String jsonDicomDoc = "EMPTY";

    try {
      File dicomFile = new File(fullPath);
      jsonDicomDoc = dicomJsonContent(dicomFile);
      dicomFile.delete();
    } catch (Exception e) {
      System.out.println("error opening file: " + fullPath + " error message: " + e.getMessage());
    }

    return jsonDicomDoc;
  }

  public static String dicomJsonContent(File dicomFile) {
    String dicomJsonString = "";

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DicomInputStream dis = new DicomInputStream(dicomFile)) {
      IncludeBulkData includeBulkData = IncludeBulkData.URI;
      dis.setIncludeBulkData(includeBulkData);

      JsonGenerator jsonGen = Json.createGenerator(baos);
      JSONWriter jsonWriter = new JSONWriter(jsonGen);
      dis.setDicomInputHandler(jsonWriter);
      dis.readDataset(-1, -1);
      jsonGen.flush();
      dicomJsonString = new String(baos.toByteArray(), "UTF-8");
    } catch (Exception e) {
      System.out.println("error processing dicom: " + e.getMessage());
    }

    return dicomJsonString;
  }
}
