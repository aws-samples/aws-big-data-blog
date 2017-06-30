tweets-streaming.py
-------------------

This script pulls random tweets from the Twitter API and stores them in Amazon DynamoDB. There are two modules needed to execute the script:

- boto3: https://aws.amazon.com/sdk-for-python/
- twitter: https://pypi.python.org/pypi/twitter/

A Twitter account is needed to access Twitter API. Go to https://www.twitter.com/ and sign up for a free account, if you don't already have one. Once your account is up, go to https://apps.twitter.com/ and on the main landing page, click the grey "Create New App" button. After you give it a name, you can go to the "Keys and Access Tokens" to get your credentials to use the Twitter API. You will need to generate Customer Tokens/Secret and Access Token/Secret. All four keys will be used to authenticate your request.

In the script, update the following lines with the real security credentials:

    # Twitter security credentials 
    ACCESS_TOKEN    = "...01234..."
    ACCESS_SECRET   = "...i7RkW..."
    CONSUMER_KEY    = "...be4Ma..."
    CONSUMER_SECRET = "...btcar..."

This section can be customized according to your preference. Use your own table name and TTL value as desired:

    # Global variables.
    dynamodb_table     = "TwitterAnalysis"
    expires_after_days = 30



tweets-simulated.py
-------------------

This script generates simulated tweets and stores them in Amazon DynamoDB. There are three modules needed to execute the script:

- boto3: https://aws.amazon.com/sdk-for-python/
- names: https://pypi.python.org/pypi/names/
- loremipsum: https://pypi.python.org/pypi/loremipsum/

In the script, update the following lines with the real security credentials:

    # Twitter security credentials 
    ACCESS_TOKEN    = "...01234..."
    ACCESS_SECRET   = "...i7RkW..."
    CONSUMER_KEY    = "...be4Ma..."
    CONSUMER_SECRET = "...btcar..."

This section can be customized according to your preference. Use your own table name and TTL value as desired:

    # Global variables
    dynamodb_table  = "TwitterAnalysis"
    provisioned_wcu = 1

