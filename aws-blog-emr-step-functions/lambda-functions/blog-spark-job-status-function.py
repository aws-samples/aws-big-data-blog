from botocore.vendored import requests
import json
def lambda_handler(event, context):
  jobid = event.get('jobId')
  url = 'http://xxxxx:8998/batches/' + str(jobid)
  res = requests.get(url)
  json_data = json.loads(res.text)
  return json_data.get('state')