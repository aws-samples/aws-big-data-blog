# mirth-dicom-example

This jar is used with the AWS Mirth Examples illustrated in the big data blog post

#### Installation:

Build this jar using Maven.  

Create a directory and copy this jar file to the new directory.  For additional dcm4che3 functionality, you can also copy the dcm4che3 jar files from the Maven repository identified in the pom.xml.  Version 3.3.5 has been used.

#####Add this new directory to your Mirth Connection configuration.
 * From within Mirth Connect Administrative Console, click settings.
 * Click Resources
 * Click Add Resource
 * Enter full directory path and add a description
 * Once complete, click, Reload Resource

#####For each Mirth Channel using these libraries, you must include this library resource in the channel as well as any destinations.
 * For each channel, enter the channel configuration.
 * Click, Set Dependencies
 * Click, Library Resources
 * Select the context the library should be used with.
 * Highlight the check box for the library to include.
 * Save and deploy channel

Example use can be found in the example Mirth Channels provided.
