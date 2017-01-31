#! /bin/bash
openssl req -x509 -newkey rsa:2048 -keyout privateKey.pem -out certificateChain.pem -days 365 -nodes -subj '/C=US/S=Washington/L=Seattle/O=MyOrg/OU=MyDept/CN=*.ec2.internal'
cp certificateChain.pem trustedCertificates.pem
zip -r -X certs.zip privateKey.pem certificateChain.pem trustedCertificates.pem
