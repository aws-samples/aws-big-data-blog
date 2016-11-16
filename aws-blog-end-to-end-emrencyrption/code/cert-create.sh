openssl req -newkey rsa:2048 -nodes -keyout privateKey.pem -x509 -days -30 -out certificateChain.pem
cp certificateChain.pem trustedCertificates.pem
zip -r -X certs.zip privateKey.pem certificateChain.pem trustedCertificates.pem
