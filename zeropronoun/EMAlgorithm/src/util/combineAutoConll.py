import os

out = open('train_auto_conll', 'w')
fns = open('chinese_list_all_train', 'r')
filenames = fns.readlines()

for fn in filenames:
    f = open(fn.strip('\n').replace("gold", "auto"), 'r')
    lines = f.readlines()
    out.writelines(lines)
    f.close()
    
fns.close()
out.close()