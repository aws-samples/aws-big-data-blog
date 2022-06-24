from botocore.vendored import requests
import json
def lambda_handler(event, context):
  headers = { "content-type": "application/json" }
  url = 'http://xxxxx:8998/batches'
  payload = {
    'file' : 's3://<<s3-root-path>>/spark-taxi/spark-taxi-job.jar',
    'className' : 'com.example.RateCodeStatus',
    'args' : [event.get('rootPath')]
  }
  res = requests.post(url, data = json.dumps(payload), headers = headers, verify = False)
  json_data = json.loads(res.text)
  return json_data.get('id')