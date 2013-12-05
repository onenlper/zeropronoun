import os
from langconv import *

def process(path, fn, outputPath):
    f = open(path + fn, 'r')
    lines = f.readlines()
    i = 0
    out = open(outputPath + '/' + fn, 'w')
    for line in lines:
        line = line.strip('\n').strip()
        if line=='':
            out.write('xxxx\n')
        else:
            out.write(line + '\n')
    f.close()
    out.close()
    
folder = '/users/yzcchen/chen3/zeroEM/rawText/'

outputFolder = '/users/yzcchen/chen3/zeroEM/xxx-separate/'
i = 0
for subFolder in os.listdir(folder):
    if os.path.isfile(folder + subFolder):
        continue
    wholeSubF = outputFolder + subFolder
    if not os.path.exists(wholeSubF):
        os.mkdir(wholeSubF)
    for fn in os.listdir(folder + subFolder + '/'):
        print subFolder + ' ' + fn + "#" + str(i)
        i += 1
        process(folder + subFolder + '/', fn, wholeSubF)
