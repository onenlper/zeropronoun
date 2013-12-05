import os

pros = set(["你", "我", "他", "她", "它", "你们", "我们", "他们", "她们", "它们"])
ngramFn = '/users/yzcchen/chen3/5-gram/5-gram/'
i = 0
pattern = []
for fn in os.listdir(ngramFn):
    print fn + "  " + str(i)
    i += 1
    file = open(ngramFn + fn)
    line = ''
    while not line:
        line = file.read()
        tks = line.split()
        print line
    last
        
    file.close()