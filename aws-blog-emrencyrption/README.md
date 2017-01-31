#Secure EMR with Encryption

##Create SSL Certs
Use the cert-create.sh file for creating your certificates & push them to S3
```
openssl req -x509 -newkey rsa:2048 -keyout privateKey.pem -out certificateChain.pem -days 365 -nodes -subj '/C=US/S=Washington/L=Seattle/O=MyOrg/OU=MyDept/CN=*.ec2.internal'
cp certificateChain.pem trustedCertificates.pem
zip -r -X certs.zip privateKey.pem certificateChain.pem trustedCertificates.pem
```

##SSH Commands
Use these SSH commands in the ssh-commands.txt file to hop on to bastion & then to the EMR Hadoop Master instance
```
ssh-add -K ~/your-key-pair.pem #add private key pair to keychain 
ssh-add -L #verify the keys available to ssh-agent
ssh -A ec2-user@bastion-instance-public-dns #login on bastion
ssh hadoop@hadoop-master-private-ip #from bastion, jump onto master node of EMR
```

##Hive Script
Use the test.q hive script to test encryption
```
hive -hiveconf bucketName='your-bucket-name' -f test.q
```

##PySpark Script
Use the test.py pyspark script to test the encryption
```
spark-submit --master yarn --deploy-mode client ~/test.py --bucketName your-bucket-name
```
