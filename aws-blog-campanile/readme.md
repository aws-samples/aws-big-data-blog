# Campanile; Turning EMR into a Massive S3 Processing Engine

## Introduction 

Have you ever had to copy S3 data from one account to another? Or list all objects greater then a certain size? How about map a function over a large number of objects? In a classical sense, this use to require only simple shell commands. i.e `aws s3 ls s3://bucket | awk '{ if($3 > 1024000) print $0 }'`. But now with object counts in the billions, these patterns no longer apply. For example, how long would it take to list 1 billion objects, with a process listing 1000 objects/sec? **11 days!** EMR to the rescue. 

This article will transform an EMR cluster into a highly scalable, highly parallel, S3 processing engine. It examines the Campanile framework, which uses standard building blocks like Hadoop, Streaming, HIVE, and Python Boto to process S3 data at speeds in excess of 100Gbps and/or 10k transactions per second. All examples make use of the [AWS CLI](http://aws.amazon.com/cli), shell commands, and will require at basic understanding of EMR, S3, and IAM. 

**NOTE:** Through the code there are hard-coded values, against the API driven nature of AWS. This is done by design. When processing billions of objects, a single extra API request can quickly become a performance and cost inhibitor. 

## Streaming Overview 

EMR's [Hadoop Streaming](http://docs.aws.amazon.com/ElasticMapReduce/latest/ReleaseGuide/UseCase_Streaming.html) is the core component of Campanile. Mapper and reducer functions, operate entirely on [standard streams](https://en.wikipedia.org/wiki/Standard_streams), making it extremely easy to integrate scripting languages like python. STDOUT is used for passing data between mappers, reducers, and HDFS/S3, while STDERR is used as control messaging. Below you will fine two examples of how Campanile uses control messaging. Each streaming step creates a _sandboxed_ environment for the mapper and reducers tasks, setting environment variables and copying files (passed in with the -files option) into an temporary location. Campanile provides a number of functions and patterns to support the sandbox.  

### Counters 

For each STEP, mappers can use the following format to create global counter(s).
 
    reporter:counter:<group>,<counter>,<amount>

BucketList uses a counter to count the size of all objects in bytes 

    ## campanile.py
    def counter(group, counter, amount):
        stderr.write("reporter:counter:%s,%s,%s\n" % (group, counter, amount))

    ## bucketlist.py 
    campanile.counter(args.bucket, "Bytes", key.size)
  
### Status Messages

A task will be terminated if it does not get input, write an output, or receive a status string within a configurable amount of time. The following format can be used to message status to the task manager. 

    reporter:status:<message>

GETing or PUTing a large objects can easily exceed default timeouts. Using boto's canned get and set *contents_from_file* functions, one can use the a callback function to report a periodic status. 

    ## campanile.py
    class FileProgress:
	    def __init__(self, name, verbose=0):
	        self.fp = 0
	        self.total = None
	        self.verbose = verbose
	        self.name = name

	    def progress(self, fp, total):
	        self.total = total
	        self.fp = fp
	        if self.verbose == 1:
	            stderr.write("reporter:status:%s:%s out of %s complete\n" % \
	                    (self.name, fp, total))


    ## objectcopy.py
    p = campanile.FileProgress(report_name, verbose=1)
    srckey.get_contents_to_file(fp, headers=headers,
        version_id=version_id, cb=p.progress)


### Sandbox

While shared libraries and configuration files are a perfect use case for [bootstrap actions](http://docs.aws.amazon.com/ElasticMapReduce/latest/ManagementGuide/emr-plan-bootstrap.html), during development it can be much easier to update functions on each invocation. Streaming steps use the -files option, to load files from S3/HDFS into the sandbox environment. In the mapper code, you see the _sandbox_ is added to *PATH*, and the local directory is configuration file search paths.

    ## Support for Streaming sandbox env
    sys.path.append(os.environ.get('PWD'))
    os.environ["BOTO_PATH"] = '/etc/boto.cfg:~/.boto:./.boto'
    import campanile
    import boto

    cfgfiles = [
        "/etc/campanile.cfg",
        "./campanile.cfg"
    ]
    c = ConfigParser.SafeConfigParser()
    c.read(cfgfiles)


### Input Split 

Campanile relies heavily on the [NLineInputFormat](https://hadoop.apache.org/docs/r2.4.1/api/org/apache/hadoop/mapred/lib/NLineInputFormat.html) property, which controls how many lines of input are sent to each mapper task. For example, an objectcopy step is only successful if ALL objects passed to it are copied successfully. Therefore, limiting the number of objects for a single process can drastically increase probability of success. To support this "split", while also supporting campanile functioning in a normal shell, one is needs a "Streaming Environment" check. 

    ## campanile.py provides and index function  
    def stream_index():
    try:
        if os.environ['mapred_input_format_class'] == \
                'org.apache.hadoop.mapred.lib.NLineInputFormat':
            return 1
    except:
        return 0
    
    ## objectcopy.py
    start_index = campanile.stream_index()
    for line in fileinput.input("-"):
        record = line.rstrip('\n').split('\t')[start_index:]
   

## Setup

All code and CloudFormation examples can be found in the [aws-blog git repo](https://github.com/awslabs/aws-big-data-blog), and should be cloned locally. The following definitions will be reference throughout the article.

Name | Description
--- | --- 
account1 | Source AWS account owner
account2 | Destination AWS account owner   
srcbucket | Bucket containing source data, owned by account1
codebucket | Bucket containing source code, also owned by account1
dstbucket | Empty bucket, owned by account2
jeff | Boto profile of IAM User of source account
jassy | Boto profile of IAM User of destination account

To simply command line examples, set the following env variables. All actions will take place in AWS_DEFAULT_REGION. A empty file has been provided [here](etc/blog-cmds). Fill out he file accordingly, and `source etc/blog-cmds`. 

	## Set default region
	export AWS_DEFAULT_REGION=us-west-2

	##  Admin profiles
    $ ADMIN_ACCOUNT1=account1 
    $ ADMIN_ACCOUNT2=account2

    ## Buckets created below
    $ SRC_BUCKET=srcbucket
    $ DST_BUCKET=dstbucket
    $ CODE_BUCKET=codebucket

    ## Region of buckets created below 
    $ SRC_ENDPOINT=s3-us-west-2.amazonaws.com
    $ DST_ENDPOINT=s3-us-west-2.amazonaws.com

    ## Source and destination profiles created in section IAM Users
    $ SRC_PROFILE=jeff
    $ DST_PROFILE=jassy

**NOTE:** You can remove all "--profile $ADMIN_ACCOUNT1", if the ADMIN_ACCOUNT1 is set as the default credentials. 

### S3 Buckets 

This process requires three buckets. A source, destination, and a third bucket that contains code, configuration, and input/output files. If one does not have access to multiple AWS accounts, one can just create a destination bucket in the source account. Step 1 is to create these buckets.  

	## Create test bucket. 
	## Assumes profiles are available in ~/.aws/credentials or ~/.boto
    $ aws s3 mb $SRC_BUCKET --profile $ADMIN_ACCOUNT1
    $ aws s3 mb $CODE_BUCKET --profile $ADMIN_ACCOUNT1
    $ aws s3 mb $DST_BUCKET --profile $ADMIN_ACCOUNT2

**NOTE:** _codebucket_ should be in the same region and account as the _srcbucket_.   

### Test Files

Untar and copy testfiles into source bucket. 

    $ cd aws-big-data-blog/aws-blog-campanile 
    $ tar -C /tmp -xzf var/testfiles.tgz
    $ aws s3 sync /tmp/testfiles s3://$SRC_BUCKET --profile $ADMIN_ACCOUNT1
    
    ## List test objects using aws cli 
    $ aws s3api list-objects --bucket $SRC_BUCKET --query 'Contents[*].[Key,ETag,Size,LastModified]' --output text --profile $ADMIN_ACCOUNT1
    00	"be78ac18165af5cf456453eb3f37ec9a"	10240	2015-12-22T22:02:04.000Z
	000/00	"e5f8e641007332dda1c6cae118f7749c-3"	20971520	2015-12-22T22:02:04.000Z
	01	"bcacdca78c906ade2580a04599d5997d-3"	20971520	2015-12-22T22:04:13.000Z
	02	"c04cab2ac8b2ece0befc39b9cce82f9e"	10240	2015-12-22T22:04:14.000Z
	03	"9596d4ba6e653985470fdec50d7cdbbe"	10240	2015-12-22T22:04:14.000Z
	04	"572a28822adc99c0250b1479ac974b92"	10240	2015-12-22T22:04:14.000Z
	....

**NOTE:** Files 00 and 000/00 have been include to provide an example of why a delimiter is required in nested buckets.  

### IAM Users 

The CloudFormation template [s3_migration_user](cloudformation/s3_migration_user.json) has been provided to create IAM users for the migration, which follows best practice ["least privileged access"](http://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html#grant-least-privilege). One might ask, why not use the EMR instance role that has access to s3:*? Concurrent running mapper tasks quickly overload the local http server, causing permissions errors when the SDK attempts to get temp keys. Therefore, as a part of Hadoop Streaming process, a .boto file containing credentials is loaded into sandbox environment. See creation of this file below. 

    ## Create Source ReadOnly User 
    $ aws cloudformation create-stack --stack-name s3-read-test --template-body file://s3_migration_user.json --parameters ParameterKey=Bucket,ParameterValue=$SRC_BUCKET ParameterKey=Operation,ParameterValue=Read --capabilities CAPABILITY_IAM --profile $ADMIN_ACCOUNT1

    ## Upon completion, get key creation command. Update [profile <name>] accordingly
    $ aws cloudformation describe-stacks --stack-name s3-read-test --query 'Stacks[].Outputs[?OutputKey==`UserKeyCreateCmd`].OutputValue[]' --output text --profile $ADMIN_ACCOUNT1

    $ aws iam create-access-key --user-name s3-read-test-User-1VDS5E5QRC4E3 --query '{aws_access_key_id:AccessKey.AccessKeyId,aws_secret_access_key:AccessKey.SecretAccessKey}' --output text --profile $ADMIN_ACCOUNT1 | awk '{print "[profile jeff]\naws_access_key_id="$1"\naws_secret_access_key="$2"\n"}' >> .boto

    ## Create Write User
    $ aws cloudformation create-stack --stack-name s3-write-test --template-body file://s3_migration_user.json --parameters ParameterKey=Bucket,ParameterValue=$DST_BUCKET ParameterKey=Operation,ParameterValue=Write --capabilities CAPABILITY_IAM --profile $ADMIN_ACCOUNT2
    
    $ aws cloudformation describe-stacks --stack-name s3-write-test --query 'Stacks[].Outputs[?OutputKey==`UserKeyCreateCmd`].OutputValue[]' --output text --profile $ADMIN_ACCOUNT2
    
    ## Update [profile <name>] accordingly
    $ aws iam create-access-key --user-name s3-write-test-User-1G7JRF0PYYA5T --query '{aws_access_key_id:AccessKey.AccessKeyId,aws_secret_access_key:AccessKey.SecretAccessKey}' --output text --profile $ADMIN_ACCOUNT2 | awk '{print "[profile jassy]\naws_access_key_id="$1"\naws_secret_access_key="$2"\n"}' >> ../bin/.boto

    ## See boto file 
	$ cat .boto 
	[profile jeff]
	aws_access_key_id=<access_key>
	aws_secret_access_key=<secret_key>

	[profile jassy]
	aws_access_key_id=<access_key>
	aws_secret_access_key=<secret_key>

	## Move .boto file into bin directory for testing
	$ mv .boto ../bin

### Bucket Part File

A _PartFile_ in delimiter,prefix format, describes the S3 bucket layout with no overlap. An example file has been provided [here](var/input/srcbucket/part.all), that maps to the set of testfiles. In this case, only *one* object maps to each `<delimeter>,<prefix>` entry, but in the real world it could be millions per line. One could also image splitting the part file up further, part.000, part.001, etc..., and processing across multiple clusters concurrently. 

The testfile 000/00 was included to clarify why delimiters are useful. Using the _s3api_, lets go over an example. 

	$ aws s3api list-objects --prefix 00 --bucket $SRC_BUCKET --profile $SRC_PROFILE --query 'Contents[*].[Key,ETag,Size,LastModified]' --output text
	00	"be78ac18165af5cf456453eb3f37ec9a"	10240	2015-12-24T05:26:19.000Z
	000/00	"e5f8e641007332dda1c6cae118f7749c-3"	20971520	2015-12-24T05:35:21.000Z

	## How could break this into two list calls?

	## List 00* in the root, using delimiter 
	$ aws s3api list-objects --delimiter / --prefix 00 --bucket $SRC_BUCKET --profile $SRC_PROFILE --query 'Contents[*].[Key,ETag,Size,LastModified]' --output text
	00	"be78ac18165af5cf456453eb3f37ec9a"	10240	2015-12-24T05:26:19.000Z

	## List 000/*
	aws s3api list-objects --prefix 000/ --bucket $SRC_BUCKET --profile $SRC_PROFILE --query 'Contents[*].[Key,ETag,Size,LastModified]' --output text
	000/00	"e5f8e641007332dda1c6cae118f7749c-3"	20971520	2015-12-24T05:35:21.000Z

### Source Files

Finally, it is time to copy all files into the code bucket! 

	## Change dir into blog root
	$ cd ~/aws-big-data-blog/aws-blog-campanile

	## Upload bootstrap actions 
	$ aws s3 sync bootstrap s3://$CODE_BUCKET/bootstrap --profile $ADMIN_ACCOUNT1
	upload: bootstrap/campanile.sh to s3://codebucket/bootstrap/campanile.sh
	upload: bootstrap/syslog-setup.sh to s3://ccodebucket/bootstrap/syslog-setup.sh
	upload: bootstrap/salt-setup.sh to s3://codebucket/bootstrap/salt-setup.sh

	## Upload mappers and reducers
	$ aws s3 sync --exclude "*.cfg" --exclude "*.pyc" bin s3://$CODE_BUCKET/ --profile $ADMIN_ACCOUNT1
	upload: bin/.boto to s3://codebucket/.boto
	upload: bin/bucketlist.py to s3://codebucket/bucketlist.py
	upload: bin/multipartlist.py to s3://codebucket/multipartlist.py
	upload: bin/multipartcomplete.py to s3://codebucket/multipartcomplete.py
	upload: bin/campanile.py to s3://codebucket/campanile.py
	upload: bin/objectcopy.py to s3://codebucket/objectcopy.py

    ## Upload Partfile 
    $ aws s3 cp var/input/srcbucket/part.all s3://$CODE_BUCKET/input/$SRC_BUCKET/ --profile $ADMIN_ACCOUNT1
    upload: var/input/srcbucket/part.all to s3://codebucket/input/srcbucket/part.all

## Steps 

To determine the correct S3 endpoint, refer to [http://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region](). The following tests are executed locally, and use PIPEs to simulate streaming IO. Once commands are verified, we can launch in EMR. 

**NOTE:** Send _stderr_ to _/dev/null_ to hide status messages: i.e. `2>/dev/null`

![](img/workflow.png)

### BucketList 

The first step is a distributed list, which is only possible because of S3's lexicographically property, and the ability to [list keys hierarchically](http://docs.aws.amazon.com/AmazonS3/latest/dev/ListingKeysHierarchy.html). A part file is piped into _bucketlist.py_, which sets prefix and delimiter parameters and calls [ListObjects](http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketGET.html).   

    ## Control message counters are prefixed with reporter:counter: 
    $ head -n 1 ../var/input/srcbucket/part.all | ./bucketlist.py --bucket $SRC_BUCKET --endpoint $SRC_ENDPOINT --profile $SRC_PROFILE
    00	be78ac18165af5cf456453eb3f37ec9a	10240	2015-12-24 05:26:19
	reporter:counter:srcbucket,Bytes,10240

![](img/bucketlist.png)

### Hive Diff

In some occasions, it is useful to diff two buckets before starting a copy. This will require running a bucketlist on the destination, along with the source, and using the following [HIVE query](hive/diff.q). The output will become the new input to _multipartlist_. 

    DROP TABLE IF EXISTS src;
	DROP TABLE IF EXISTS dst;
	DROP TABLE IF EXISTS diff;
	CREATE EXTERNAL TABLE src(key STRING, etag STRING, size BIGINT, mtime STRING) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t' LINES TERMINATED BY '\n' LOCATION '${SRC}';
	CREATE EXTERNAL TABLE dst(key STRING, etag STRING, size BIGINT, mtime STRING) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t' LINES TERMINATED BY '\n' LOCATION '${DST}';
	CREATE EXTERNAL TABLE diff(key STRING, etag STRING, size BIGINT, mtime STRING) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t' LINES TERMINATED BY '\n' LOCATION '${DIFF}';
	INSERT OVERWRITE TABLE diff SELECT src.key,src.etag,src.size,src.mtime FROM src LEFT OUTER JOIN dst ON (src.key = dst.key) WHERE (dst.key IS NULL) OR (src.etag != dst.etag);


### MultipartList

MultiPartList corresponds to the API request [UploadInitiate](http://docs.aws.amazon.com/AmazonS3/latest/API/mpUploadInitiate.html). Although, it's most difficult task is calculating the part maps of source objects that were uploaded with MultiPart. Review the details of multipart uploads [here](http://docs.aws.amazon.com/AmazonS3/latest/dev/mpuoverview.html), and understand how etags are calculated based on part size(s). In these examples, Campanile uses a function similar to that of the AWS CLI to calculate part size. And since this article assumes the testfiles were uploaded using the cli, multipartlist will be able to determine the correct partsize. 

**NOTE:** The only way to copy an object and keep the same etag, is to know the original partsize(s) of the initial upload. 

**NOTE:** Because UploadInitiate happens here, this is where one can add or modify metadata, enable SSE, and/or make other changes on the new object. 

    ## campanile.py
    def cli_chunksize(size, current_chunksize=DEFAULTS['multipart_chunksize']):
	    chunksize = current_chunksize
	    num_parts = int(math.ceil(size / float(chunksize)))
	    while num_parts > MAX_PARTS:
	        chunksize *= 2
	        num_parts = int(math.ceil(size / float(chunksize)))
	    if chunksize > MAX_SINGLE_UPLOAD_SIZE:
	        return MAX_SINGLE_UPLOAD_SIZE
	    else:
	        return chunksize

	## multipartlist.py
    partsize = campanile.cli_chunksize(int(size))
    if partcount != int(math.ceil(float(size)/partsize)):
    	campanile.status("Can't calculate partsize for %s/%s" %
        		(args.src, name))
        ## Send an alert?
        continue


    ## For testing purposes, use the --dry-run option. 
	$ head -n 2 ../var/input/srcbucket/part.all | ./bucketlist.py --bucket $SRC_BUCKET --endpoint $SRC_ENDPOINT --profile $SRC_PROFILE | ./multipartlist.py --src-bucket $SRC_BUCKET --src-endpoint $SRC_ENDPOINT --src-profile $SRC_PROFILE --dst-bucket $DST_BUCKET --dst-endpoint $DST_ENDPOINT --dst-profile $DST_PROFILE --dry-run
	reporter:counter:srcbucket,Bytes,10240
	reporter:counter:srcbucket,Bytes,20971520
	00	be78ac18165af5cf456453eb3f37ec9a	10240	2015-12-24 05:26:19	\N	\N	\N	\N	\N
	000/00	e5f8e641007332dda1c6cae118f7749c-3	20971520	2015-12-24 05:35:21	381e5a11-aa65-11e5-806d-600308952818	1	3	0	8388607
	000/00	e5f8e641007332dda1c6cae118f7749c-3	20971520	2015-12-24 05:35:21	381e5a11-aa65-11e5-806d-600308952818	2	3	8388608	16777215
	000/00	e5f8e641007332dda1c6cae118f7749c-3	20971520	2015-12-24 05:35:21	381e5a11-aa65-11e5-806d-600308952818	3	3	16777216	20971519

From the output, you see that the 20M object 000/00 has been broken into 3 parts. 

**NOTE:** NULL or None is represented as _\N_ for better HIVE integration

![](img/multipartlist.png)

### ObjectCopy 

Corresponding to S3 API [GET](http://docs.aws.amazon.com/AmazonS3/latest/API/RESTObjectGET.html) and [PUT](http://docs.aws.amazon.com/AmazonS3/latest/API/RESTObjectPUT.html), _objectcopy.py_ does most of the work. It requires two important parameters that are set by the Campanile bootstrap action. To distribute load across disks, the ephemeral parameter is a list of all mount points, and then one is randomly selected for location of the downloaded object. But in many cases, a downloaded object and part will never reach disk. The second setting _maxtmpsize_, tells the _tempfile_ class to flush bytes to disk only when this size is reached. 

    $ head -n3 /etc/capanile.cfg 
	[DEFAULT]
	maxtmpsize=134217728
	ephemeral=/mnt,/mnt1

    ## objectcopy.py
    tmpdir = random.choice(c.get('DEFAULT',"ephemeral").split(','))
    with tempfile.SpooledTemporaryFile(max_size=c.getint('DEFAULT','maxtmpsize'),dir=tmpdir) as fp:
    	....

    ## See objectcopy example in MultipartComplete

For non-multipart objects, this is the step where one can add or modify metadata, enable SSE, and/or make other changes on the new object. It only outputs part information for multipart uploads, for the reducer can sort and complete the upload.   
 

### MultipartComplete 

The only reducer of the bunch, is responsible for sorting parts, and issuing the [MultiPartComplete](http://docs.aws.amazon.com/AmazonS3/latest/API/mpUploadComplete.html) command. The command below copies the the first 2 objects in the source bucket.  

    $ head -n 2 ../var/input/srcbucket/part.all | ./bucketlist.py --bucket $SRC_BUCKET --endpoint $SRC_ENDPOINT --profile $SRC_PROFILE | ./multipartlist.py --src-bucket $SRC_BUCKET --src-endpoint $SRC_ENDPOINT --src-profile $SRC_PROFILE --dst-bucket $DST_BUCKET --dst-endpoint $DST_ENDPOINT --dst-profile $DST_PROFILE | ./objectcopy.py --src-bucket $SRC_BUCKET --src-endpoint $SRC_ENDPOINT --src-profile $SRC_PROFILE --dst-bucket $DST_BUCKET --dst-endpoint $DST_ENDPOINT --dst-profile $DST_PROFILE | ./multipartcomplete.py --bucket $DST_BUCKET --endpoint $DST_ENDPOINT --profile $DST_PROFILE
    ...
    dstbucket/000/00	e5f8e641007332dda1c6cae118f7749c-3	7TiS9GhL59.Ej19ACMh.HDIvu4bbTvnsQNbA.mo4GfiR4vGjiZ2u7RvhTxLokXSBf3c5xREkFHorhgz9E6jluQ.B3msqQSyoKh3Ynar8QlMW8EmbosIf0oWgIRvTmr.J	96995b58d4cbf6aaa9041b4f00c7f6ae	1	0	8388607
	dstbucket/000/00	e5f8e641007332dda1c6cae118f7749c-3	7TiS9GhL59.Ej19ACMh.HDIvu4bbTvnsQNbA.mo4GfiR4vGjiZ2u7RvhTxLokXSBf3c5xREkFHorhgz9E6jluQ.B3msqQSyoKh3Ynar8QlMW8EmbosIf0oWgIRvTmr.J	96995b58d4cbf6aaa9041b4f00c7f6ae	2	8388608	16777215
	dstbucket/000/00	e5f8e641007332dda1c6cae118f7749c-3	7TiS9GhL59.Ej19ACMh.HDIvu4bbTvnsQNbA.mo4GfiR4vGjiZ2u7RvhTxLokXSBf3c5xREkFHorhgz9E6jluQ.B3msqQSyoKh3Ynar8QlMW8EmbosIf0oWgIRvTmr.J	a0448946c0d68b0268940e5f519cba18	3	16777216	20971519


	## Compare source and destination objects
	$ head -n 2 ../var/input/srcbucket/part.all | ./bucketlist.py --bucket $SRC_BUCKET --endpoint $SRC_ENDPOINT --profile $SRC_PROFILE 2>/dev/null
	00	be78ac18165af5cf456453eb3f37ec9a	10240	2015-12-24 05:26:19
	000/00	e5f8e641007332dda1c6cae118f7749c-3	20971520	2015-12-24 05:35:21
	$ head -n 2 ../var/input/srcbucket/part.all | ./bucketlist.py --bucket $DST_BUCKET --endpoint $DST_ENDPOINT --profile $DST_ENDPOINT
	2>/dev/null
	00	be78ac18165af5cf456453eb3f37ec9a	10240	2015-12-24 17:55:07
	000/00	e5f8e641007332dda1c6cae118f7749c-3	20971520	2015-12-24 17:55:06

![](img/objectcopy.png)

## Launch EMR

We've tested the permissions and uploaded all source file into the _codebucket_. This command launches the Processing Cluster, using default EMR Roles, Subnets, and Security Groups. 

	## Update <keyname> accordingly
	$ aws emr create-cluster --release-label emr-4.2.0 --name S3ProcessingEngine --tags Name=Campanile --ec2-attributes KeyName=<keyname> --instance-groups InstanceGroupType=MASTER,InstanceCount=1,InstanceType=m3.2xlarge InstanceGroupType=CORE,InstanceCount=3,InstanceType=m3.xlarge --use-default-roles --enable-debugging --applications Name=HIVE --bootstrap-actions Path=s3://$CODE_BUCKET/bootstrap/campanile.sh,Name=campanile --log-uri s3://$CODE_BUCKET/log/jobflow/
    {
    	"ClusterId": "j-Z3TU7ESI7LPA"
	}






