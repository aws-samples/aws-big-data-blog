'''
Created on Oct 8, 2015

@author: mentzera
'''
from elasticsearch import Elasticsearch
from elasticsearch.helpers import bulk
import config
from elasticsearch.exceptions import ElasticsearchException
from tweet_utils import get_tweet, id_field, tweet_mapping


index_name = 'twitter'
doc_type = 'tweet'
mapping = {doc_type: tweet_mapping
           }
bulk_chunk_size = config.es_bulk_chunk_size


def create_index(es,index_name,mapping):
    print('creating index {}...'.format(index_name))
    es.indices.create(index_name, body = {'mappings': mapping})


def load(tweets):    
    es = Elasticsearch(host = config.es_host, port = config.es_port)

    if es.indices.exists(index_name):
        print ('index {} already exists'.format(index_name))
        try:
            es.indices.put_mapping(doc_type, tweet_mapping, index_name)
        except ElasticsearchException as e:
            print('error putting mapping:\n'+str(e))
            print('deleting index {}...'.format(index_name))
            es.indices.delete(index_name)
            create_index(es, index_name, mapping)
    else:
        print('index {} does not exist'.format(index_name))
        create_index(es, index_name, mapping)
    
    counter = 0
    bulk_data = []
    list_size = len(tweets)
    for doc in tweets:
        tweet = get_tweet(doc)
        bulk_doc = {
            "_index": index_name,
            "_type": doc_type,
            "_id": tweet[id_field],
            "_source": tweet
            }
        bulk_data.append(bulk_doc)
        counter+=1
        
        if counter % bulk_chunk_size == 0 or counter == list_size:
            print "ElasticSearch bulk index (index: {INDEX}, type: {TYPE})...".format(INDEX=index_name, TYPE=doc_type)
            success, _ = bulk(es, bulk_data)
            print 'ElasticSearch indexed %d documents' % success
            bulk_data = []
  
