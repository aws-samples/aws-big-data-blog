import time
import logging
import json

from sjsclient import client

logging.basicConfig()
logger = logging.getLogger()
logger.setLevel(logging.INFO)

sjs = client.Client("http://jobserver.ml-test-blog.internal:8090")
myApp = sjs.apps.get("ml")
myContext = sjs.contexts.get("ml-context")

def testHandler(event, context):
    logger.info('got event{}'.format(event))
    s3DataLocProtocol = "\"s3://{}\"".format(event['s3DataLoc'])
    s3ModelLocProtocol = "\"s3://{}\"".format(event['s3ModelLoc'])
    conf = '{'+'s3DataLoc:{},s3ModelLoc:{}'.format(s3DataLocProtocol,s3ModelLocProtocol)+'}'
    class_path = "com.amazonaws.proserv.ml.TestParams"
    myJob = sjs.jobs.create(myApp, class_path, conf = conf, ctx = myContext)
    myId = myJob.jobId
    while myJob.status != "FINISHED":
        time.sleep(2)
        myJob = sjs.jobs.get(myId)
    return {
        'result' : sjs.jobs.get(myId).result
    }

def loadHandler(event, context):
    logger.info('got event{}'.format(event))
    s3DataLocProtocol = "\"s3://{}\"".format(event['s3DataLoc'])
    s3ModelLocProtocol = "\"s3://{}\"".format(event['s3ModelLoc'])
    conf = '{'+'s3DataLoc:{},s3ModelLoc:{}'.format(s3DataLocProtocol,s3ModelLocProtocol)+'}'
    class_path = "com.amazonaws.proserv.ml.LoadModelAndData"
    myJob = sjs.jobs.create(myApp, class_path, conf = conf, ctx = myContext)
    myId = myJob.jobId
    return {
        'result' : 'Request Submitted with ID '+myJob.jobId+'.  It should be ready shortly.'
    }


def recommenderHandler(event, context):
    logger.info('got event{}'.format(event))
    userId = event['userId']
    conf = '{'+'userId:{}'.format(userId)+'}'
    class_path = "com.amazonaws.proserv.ml.MoviesRec"
    myJob = sjs.jobs.create(myApp, class_path, conf = conf, ctx = myContext)
    myId = myJob.jobId
    while myJob.status != "FINISHED":
        time.sleep(2)
        myJob = sjs.jobs.get(myId)
    return {
        'result' : sjs.jobs.get(myId).result
    }

def genreHandler(event, context):
    logger.info('got event{}'.format(event))
    userId = event['userId']
    genre = event['genre']
    conf = '{'+'userId:{},genre:{}'.format(userId,genre)+'}'
    class_path = "com.amazonaws.proserv.ml.MoviesRecByGenre"
    myJob = sjs.jobs.create(myApp, class_path, conf = conf, ctx = myContext)
    myId = myJob.jobId
    while myJob.status != "FINISHED":
        time.sleep(2)
        myJob = sjs.jobs.get(myId)
    return {
        'result' : sjs.jobs.get(myId).result,
    }

if __name__ == '__main__':
    event = 'none'
    context = 'none'

    #Uncomment the follwing lines to test...one at a time
    #print(testHandler(event, context))
    #print(loadHandler(event, context))
    #print(recommenderHandler(event, context))
    #print(genreHandler(event, context))
