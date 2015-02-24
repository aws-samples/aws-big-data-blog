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
