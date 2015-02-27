# Installing IPython Notebook on  Amazon EMR

This is bootstrap action to install [IPython Notebook] on [Amazon EMR]. Here we will discuss on how to install and play around with a simple word count example and later plot results on a bar chart.

## Prerequisites
- Amazon Web Services account
- [AWS Command Line Interface (CLI)]

#### Overview of Bootstrap
Bootstrap install IPython notebook, [requests], [numpy] and [matplotlib] packages. The notebook server is running on Amazon EMR master node on port 8192.


#### Launching cluster
```
aws emr create-cluster --name iPythonNotebookEMR \
--ami-version 3.2.3 --instance-type m3.xlarge --instance-count 3 \
--ec2-attributes KeyName=<<MYKEY>> \
--bootstrap-actions Path=s3://elasticmapreduce.bootstrapactions/ipython-notebook/install-ipython-notebook,Name=Install_iPython_NB \
--termination-protected
```

### Connecting to Notebook
To access the IPython notebook interface, replace master-public-dns-name in the URI with the DNS name of the master node after creating an SSH tunnel. For more information about retrieving the master public DNS name, see [Retrieve the Public DNS Name of the Master Node]. For more information about creating an SSH tunnel, see [Set Up an SSH Tunnel to the Master Node Using Dynamic Port Forwarding.]

##### Creating SSH Tunnel
```
ssh -o ServerAliveInterval=10 -i <<credentials.pem>> -N -L 8192:<<master-public-dns-name>>:8192 hadoop@<<master-public-dns-name>>
```

#####Viewing the Notebook 
In your browser open the below link 
```
http://localhost:8192/
```
#### Overview of Example
This example shows how to use Hadoop Streaming to count the number of times that words occur within a text collection and output the results to HDFS.Hadoop streaming allows one to execute MapReduce programs written in languages such as Python. 

### Running example

The easiest way to call shell from IPython is to prepend an exclamation point to a shell command. Open your iPython notebook and run the below commands. 

1) Run wordcount example
```
!wget https://elasticmapreduce.s3.amazonaws.com/samples/wordcount/wordSplitter.py
!hadoop  jar /home/hadoop/contrib/streaming/hadoop-*streaming*.jar -files wordSplitter.py -mapper wordSplitter.py -reducer aggregate -input s3://elasticmapreduce/samples/wordcount/input -output /output
```

2) Plot Top 10 words on bar chart

Copy paste and run the below code in your notebook.
```
from subprocess import Popen, PIPE

import numpy as np
import matplotlib.pyplot as plt
%matplotlib inline

#Setting the top X number of words to show
TOP_X_WORDS = 10
catCommand = Popen(["hadoop", "fs", "-cat", "/output/*"],
stdout=PIPE, bufsize=-1)
sortCommand = Popen(["sort","-rn","-k","2"],stdin=catCommand.stdout, stdout=PIPE)
headCommand = Popen(["head","-n",str(TOP_X_WORDS)],stdin=sortCommand.stdout,stdout=PIPE)
output = headCommand.communicate()[0]

# initialize  variable to be lists:
xValue = []
yValue = []

# scan the rows of the file stored in lines, and put the values into some variables:
for i in output.splitlines():
	line=i.split("\t")
	xValue.append(line[0])
	yValue.append(int(line[1]))

# Plotting the data
topN = list(range(1,TOP_X_WORDS+1))
plt.xlabel('Word')
plt.ylabel('Count')
plt.title('Top '+ str(TOP_X_WORDS) +  'Words')
plt.bar(topN,yValue)
plt.xticks(rotation=70)
plt.xticks(topN, xValue)

plt.show()
```
[IPython Notebook]:http://ipython.org/notebook.html
[requests]:https://pypi.python.org/pypi/requests
[numpy]:https://pypi.python.org/pypi/numpy
[matplotlib]:https://pypi.python.org/pypi/matplotlib
[Amazon EMR]:http://aws.amazon.com/elasticmapreduce/
[AWS Command Line Interface (CLI)]:http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-welcome.html
[Retrieve the Public DNS Name of the Master Node]:http://docs.aws.amazon.com/ElasticMapReduce/latest/DeveloperGuide/emr-connect-master-node-ssh.html#emr-connect-master-dns
[Set Up an SSH Tunnel to the Master Node Using Dynamic Port Forwarding.]:http://docs.aws.amazon.com/ElasticMapReduce/latest/DeveloperGuide/emr-ssh-tunnel.html

