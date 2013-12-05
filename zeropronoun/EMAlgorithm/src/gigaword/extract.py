import os
from langconv import *

def process(path, fn, outputPath):
    f = open(path + fn, 'r')
    lines = f.readlines()
    
    out = open(outputPath + fn + '.text', 'w')
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
            out.write('\n')
    f.close()
    out.close()
    
folder = '/users/yzcchen/chen3/zeroEM/LDC2009T27/data/'

outputFolder = '/users/yzcchen/chen3/zeroEM/rawText/'
i = 0
for subFolder in os.listdir(folder):
    print subFolder
    if subFolder == 'pda_cmn':
        for fn in os.listdir(folder + subFolder + '/'):
            print subFolder + ' ' + fn + "#" + str(i)
            i += 1
            process(folder + subFolder + '/', fn, outputFolder)
