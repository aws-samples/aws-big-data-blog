#hl7-webapp Use and Install

####Installation:
 * Create a working directory for the HL7 conversion service.
 * Ensure that node.js and npm are installed
```
npm install xml2js util http querystring
node service-xml-to-json.js &
```

The service will now be listening on TCP port 8001 on localhost by default.

As a note, this is a simple example implementation and should be expanded to include error checking, redundancy, etc...

####Ideal Implementation:
 * Behind an internal AWS ELB/ALB
 * Load balancer behind an application firewall such as AWS WAF or mod_security
 * Use of AWS Autoscaling to meet demands of processing
 * Implementation is easily containerized and can be deployed with AWS ECS or other container management strategies
