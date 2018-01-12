import json
import os
info=open('/root/ticket_info/total.json' , 'r')
dic=json.load(info)
label=json.load(open('/root/eclipse-workspace/record.json','r'))

file_names=os.listdir('/root/eclipse-workspace/火车票１')

# key='始发站'
key='车位'
for name in file_names:
    print(name + ": "+dic[name][key]+" , "+label[name][key])