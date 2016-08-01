# Agile Analytics with Amazon Redshift #


## Overview


This repository contains the CloudFormation and code to support the [AWS Big Data Blog post](https://blogs.aws.amazon.com/bigdata/) of the same name.  Detailed steps are provided here to build a Jenkins Build Job together with an AWS CodePipeline with 4 stages.  The first 2 stages are described in detail, the second 2 stages are provided as stubs and left for the reader to implement. 

The code presented here is provide as a sample and not intended for production use.  Tested in US-East-1 region.

## Getting Started

The following steps take you through the process of creating the sample pipleline thats discussed in the blog article.  All resources should be created in the same AWS Region.

### Prerequisites

1. Build the CloudFormation stack found in `./environment/environment.json`.  This will create a VPC with a configured Jenkins server.  If you need want to SSH to your Jenkins server, you will need to provide the name of a set of keys for the region.

1. Using the AWS Console, go to AWS CodeCommit and create a new repository.  Once it has been created, make a note of the SSH URL.

1. Using the AWS Console, go to S3 and create a new bucket for storing build artefacts.  Go to the properties for the bucket and enable versioning.

1. Using the AWS Console, create another S3 bucket for a CodePipeline artefact store.

1. Create a new IAM user with rights to run CloudFormation, create a VPC and create a Redshift cluster.  Download the access keys for the user.

1. Make sure you have access to an environment with Git Client and the AWS CLI installed.



### Create the Jenkins Build Job

1. The output of the CloudFormation that you ran above will be the URL of the ELB created in your environment.  Cut and paste this into your browser to navigate to the Jenkins server.

1. At this point, it is best practice to [secure your Jenkins server](https://wiki.jenkins-ci.org/display/JENKINS/Standard+Security+Setup).

1. Click on **New Item**.

1. In the Item Name box, type **Build**

1. Select **Freestyle Project** and click on **OK**

1. Under **Source Code Management** select **Git**

1. Enter the SSH URL of the CodeCommit repository that you created.  No credentials or authentication is required; this was configured when the instance was created by the CloudFormation script.

1. In the **Build Triggers** section, select **Poll SCM**

1. At the bottom of the **Source Code Management** section, click on **Add** and select **Polling ignores commits in certain paths** from the drop-down.  Add **preprod/preprod.snapshotid** to the **Excluded Regions** box.

1. In the **Schedule** box, enter **\*/1 * * * * **.  You can ignore the warning "Do you really mean every minute..."

1. Under **Build**, in the **Add build step** drop-down list, click **Execute Shell**.

1. In the **Command** box, paste in the following command, replacing `<your S3 bucket>` with the name of the bucket that you created:

```
#!/bin/bash
sleep 5
rm deploy.zip
zip -r deploy .
aws s3 cp deploy.zip s3://<your S3 bucket>/deploy.zip
```
1. Click **Save** and then click **Back to Dashboard**

### Create the Jenkins Automated Test Job

1. From the Jenkins dashboard, click on **New Item**.

1. In the Item Name box, type **AutomatedTest**

1. Select **Freestyle Project** and click on **OK**

1. Under **Source Code Management** select **AWS CodePipeline**


1. In the **Category** box, select **Test** from the drop-down.

1. In the **Provider** box, enter **JenkinsCustomAction**

1. In the **Build Triggers** section, select **Poll SCM**

1. In the **Schedule** box, enter **\*/1 * * * * **.  You can ignore the warning "Do you really mean every minute..."

1. Click the box next to **Create AWS Cloud Formation stack**

1. Enter `./cloudformation/redshiftvpc.json` for the **Cloud Formation recipe file/S3 URL. (.json)**

1. Enter **redshiftautomatedtest** for the **Stack Name**, you can also provide a **Stack Description**

1. Enter the **AWS Access Key** and the **AWS Secret Key** that you created. 

1. Enter the following into the **Cloud Formation parameters** box

```
DatabaseName=db,ClusterType=single-node,NumberOfNodes=1,MasterUsername=aws,MasterUserPassword=AgileAnalytics01,DatabasePort=8192
```

1. Under **Build**, in the **Add build step** drop-down list, click **Execute Shell**.

1. In the **Command** box, paste in the following command:

```
echo $redshiftautomatedtest_ClusterEndpoint:8192:db:aws:AgileAnalytics01 >> ~/.pgpass
chmod 600 ~/.pgpass
exit 0
```

1. 1. Under **Build**, in the **Add build step** drop-down list, click **Execute Shell**.

1. In the **Command** box, paste in the following command:

```
for f in ./ddl/*.sql
do
	psql -h $redshiftautomatedtest_ClusterEndpoint -U aws -d db -p 8192 -w -f $f
done
```

1. Under **Build**, in the **Add build step** drop-down list, click **Execute Shell**.

1. In the **Command** box, paste in the following command:

```
for f in ./testdata/*.sql
do
	psql -h $redshiftautomatedtest_ClusterEndpoint -U aws -d db -p 8192 -w -f $f
done
```

1. Under **Build**, in the **Add build step** drop-down list, click **Execute Shell**.

1. In the **Command** box, paste in the following command:

```
for f in ./tests/*.sql
do
	a=$(psql -h $redshiftautomatedtest_ClusterEndpoint -U aws -d db -p 8192 -w -f $f | tr '\n' ' ' | tr -d '[[:space:]]')
    b=$(cat $f.result | tr '\n' ' ' | tr -d '[[:space:]]')
    if [ "$a" != "$b" ]
    then
    	exit 1
    fi
done
```

1. Under **Post-build Actions**, in the **Add post-build action** drop-down list, click **AWS CodePipeline Publisher**.

1. Click **Save** and then click **Back to Dashboard**

### Create the Jenkins Pre-Production Deploy Job

1. From the Jenkins dashboard, click on **New Item**.

1. In the Item Name box, type **PreProdDeploy**

1. Select **Freestyle Project** and click on **OK**

1. Under **Source Code Management** select **AWS CodePipeline**

1. In the **Category** box, select **Test** from the drop-down.

1. In the **Provider** box, enter **JenkinsCustomAction**

1. In the **Build Triggers** section, select **Poll SCM**

1. In the **Schedule** box, enter **\*/1 * * * * **.  You can ignore the warning "Do you really mean every minute..."


1. 1. Under **Build**, in the **Add build step** drop-down list, click **Execute Shell**.

1. In the **Command** box, paste in the following command:

```
echo "done"
exit 0
```

1. Under **Post-build Actions**, in the **Add post-build action** drop-down list, click **AWS CodePipeline Publisher**.

1. Click **Save** and then click **Back to Dashboard**

### Create the Jenkins Production Deploy Job

1. From the Jenkins dashboard, click on **New Item**.

1. In the Item Name box, type **ProdDeploy**

1. Select **Freestyle Project** and click on **OK**

1. Under **Source Code Management** select **AWS CodePipeline**

1. In the **Category** box, select **Test** from the drop-down.

1. In the **Provider** box, enter **JenkinsCustomAction**

1. In the **Build Triggers** section, select **Poll SCM**

1. In the **Schedule** box, enter **\*/1 * * * * **.  You can ignore the warning "Do you really mean every minute..."


1. 1. Under **Build**, in the **Add build step** drop-down list, click **Execute Shell**.

1. In the **Command** box, paste in the following command:

```
echo "done"
exit 0
```

1. Under **Post-build Actions**, in the **Add post-build action** drop-down list, click **AWS CodePipeline Publisher**.

1. Click **Save** and then click **Back to Dashboard**

###Set up CodePipeline

1. Using a text editor, update `./environment/jenkins-custom-action.json`.  You need to make 2 changes: update the <Jenkins url> to the url of your Jenkins server, returned by the CloudFormation.  The url appears twice in the file.

1. Open a command prompt on your computer, navigate to the `./environment` folder and type the following command:

```
aws codepipeline create-custom-action-type --cli-input-json file://jenkins-custom-action.json --region us-east-1
```

1. Using a text editor, update `./environment/codepipeline.json`.  Make the following changes:
	1. Change the **roleArn** to the ARN of the role that was returned by the CloudFormation (line 3)
	2. Change the **S3 Bucket** in the first **Source** stage to the name of the bucket where the build stage puts your deploy package (line 23).
	3. Change the name of the **location** in the **artifactStore** to the name of the CodePipeline bucket that you created (line 105).

1. From the command prompt on your computer, enter the following statement:

```
aws codepipeline create-pipeline --cli-input-json file://codepipeline.json --region us-east-1
```

1. Go to the AWS Console and check that your CodePipeline has been created.  Click on the arrow between the Pre-Production and Production stage and disable this transition (we don't want to automatically deploy to production)

###Check in Code and Run the Pipeline

 1. Create a new folder on your computer
 
 1. Open a command prompt and navigate to the new folder
 
 1. Initialise the local Git repository:
 
 ```
 git init
 ```
 
 1. Configure the local git repository to push changes to your CodeCommit repo, by replacing the CodeCommit SSH URL in the statement below
 
 ```
 git remote add origin <CodeCommit SSH URL>
 ```
1. Copy the contents of the `sourceControl` folder (from the blog repo) into your local git folder.  The folders `cloudformation`, `ddl`, `tests` etc should all be top level folder in your local git repo.

1. Add the new files to git

```
git add .
```

1. Commit the new files

```
git commit -m "Initial Release"
```

1. Push the files to the origin

```
git push origin master
```























