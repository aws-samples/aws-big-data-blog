import requests
import random
import sys
import argparse

def getClicked(rate):
	if random.random() <= rate:
		return True
	else:
		return False

def httpGetImpression():
	url = args.target + '?browseraction=Impression' 
	r = requests.get(url)

def httpGetClick():
	url = args.target + '?browseraction=Click' 
	r = requests.get(url)
	sys.stdout.write('+')

parser = argparse.ArgumentParser()
parser.add_argument("target",help="<http...> the http(s) location to send the GET request")
args = parser.parse_args()
i = 0
while (i < 2500):
	httpGetImpression()
	if(i<1950 or i>=2000):
		clicked = getClicked(.1)
		sys.stdout.write('_')
	else:
		clicked = getClicked(.5)
		sys.stdout.write('-')
	if(clicked):
		httpGetClick()
	i = i + 1
	sys.stdout.flush()





