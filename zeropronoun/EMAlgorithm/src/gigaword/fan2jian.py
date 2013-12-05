import os
import sys
reload(sys)
sys.setdefaultencoding('utf8')

def convert(str, map):
    newStr = ''
    for i in range(0, len(str)):
        if str[i] in map:
            newStr += map[str[i]]
        else:
            newStr += str[i]
    return newStr
    

fanF = open('dict/fan', 'r')
jianF = open('dict/jian', 'r')

fans = fanF.readline().strip('\n').strip()
jians = jianF.readline().strip('\n').strip()

print len(fans)
print len(jians)

map = {}

for i in range(0, len(fans)) :
    f = fans[i].decode('utf-8')
    j = jians[i].decode('utf-8')
    print f +' '+ j
    map[f] = j

print len(map)

fanF.close()
jianF.close()

folder = '/users/yzcchen/chen3/zeroEM/xxx-separate/cna/'


for file in os.listdir(folder):
    f = open(folder + file, 'r')
    lines = f.readlines()
    for line in lines:
        str = convert(line, map)
        #print str,
    
