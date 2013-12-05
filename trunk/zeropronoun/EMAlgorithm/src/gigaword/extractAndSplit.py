import os
from langconv import *

def process(path, fn, outputPath):
    f = open(path + fn, 'r')
    lines = f.readlines()
    
    i = 0
    
    out = open(outputPath + '/' + fn + "-" + str(i) + '.txt', 'w')
    switch = False
    outputLine = ''
    for line in lines:
        line = line.strip('\n').strip()
        if line=='<P>':
            switch = True
        elif line=='</P>':
            #outputLine = Converter('zh-hans').convert(outputLine.decode('utf-8'))
            #outputLine = outputLine.encode('utf-8')
            out.write(outputLine + '\n')
            switch = False
            outputLine = ''
        elif switch==True:
            outputLine = outputLine + line
        elif line=='</DOC>':
            out.close()
            i += 1
            out = open(outputPath + '/' + fn + "-" + str(i) + '.txt', 'w')
    f.close()
    out.close()
    
folder = '/users/yzcchen/chen3/zeroEM/LDC2009T27/data/'

outputFolder = '/users/yzcchen/chen3/zeroEM/docs/'
i = 0
for subFolder in os.listdir(folder):
    wholeSubF = outputFolder + subFolder
    if not os.path.exists(wholeSubF):
        os.mkdir(wholeSubF)
    for fn in os.listdir(folder + subFolder + '/'):
        print subFolder + ' ' + fn + "#" + str(i)
        i += 1
        process(folder + subFolder + '/', fn, wholeSubF)
