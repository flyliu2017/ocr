import json

predict_path='/root/ticket_info/total.json'  #预测结果文件路径
label_path='/root/eclipse-workspace/record.json'  #正确结果文件路径
info=open(predict_path , 'r')
dic=json.load(info)

label=json.load(open(label_path,'r'))

count={}  #用于统计的字典
count=count.fromkeys(label['1.jpg'].keys(),0)

for name in dic.keys():
    for key in count.keys():
        if key!='检票口' and dic[name][key]==label[name][key]:
            count[key]+=1

for key in count.keys():
    count[key]/=len(label)


json.dump(count,open('/root/eclipse-workspace/accuracy4.json','w'),ensure_ascii=False)
print(count)

