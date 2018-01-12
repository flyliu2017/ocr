package baidu_ocr;


//需要先安装外部包
import java.io.*;
import java.util.*;
import java.util.regex.*;

import baidu_ocr.BaiDuOCRBean.Words_result;
import com.alibaba.fastjson.*;
import com.baidu.aip.ocr.AipOcr;

public class TestOcr {
	//从百度云帐号获取id和key
	public static final String APP_ID = "10613937";
    public static final String API_KEY = "yb028l9ryKL196ywF1F8opKj";
    public static final String SECRET_KEY = "bQXMSOm4Hq1T3pIKdaHkfcgkI4c9GA4U";
    
    //利用正则表达式从识别结果中提取有用的信息
    public static String getImportantInfo(String text) {
		StringBuilder builder=new StringBuilder();
		Pattern station=Pattern.compile("(?m)([\\u4E00-\\u9FA5\\uF900-\\uFA2D]*站)[\\u4E00-\\u9FA5\\uF900-\\uFA2D]*\\n?([\\w\\p{Punct}]+)\\n?(.+)$");
		Pattern timeAndSeat=Pattern.compile("(?m)(\\d{4}.\\d{2}.\\d{2}.\\d{2}.\\d{2}.?)\\n?[\\D]*(.*)");
		Pattern IDnumberAndName=Pattern.compile("(?m)(\\d{8,10}\\*+\\d+)([\\u4E00-\\u9FA5\\uF900-\\uFA2D]*)");
		Pattern price=Pattern.compile("(?m)(^.*元$)");
		
		Matcher matcher=station.matcher(text);
		if (matcher.find()) {
			builder.append("始发站："+matcher.group(1)+"\n");
			builder.append("终点站："+matcher.group(3)+"\n");
			builder.append("车次："+matcher.group(2)+"\n");
		}
		else {
			builder.append("始发站：\n");
			builder.append("终点站：\n");
			builder.append("车次：\n");
		}
		
		matcher=timeAndSeat.matcher(text);
		if (matcher.find()) {
			builder.append("始发时间："+matcher.group(1)+"\n");
			builder.append("车位："+matcher.group(2)+"\n");
		}
		else {
			builder.append("始发时间：\n");
			builder.append("车位：\n");
		}
		
		matcher=IDnumberAndName.matcher(text);
		if (matcher.find()) {
			builder.append("身份证号："+matcher.group(1)+"\n");
			builder.append("名字："+matcher.group(2)+"\n");
		}
		else {
			builder.append("身份证号：\n");
			builder.append("名字：\n");
		}
		
		matcher=price.matcher(text);
		if (matcher.find()) {
			builder.append("票价："+matcher.group(1));
		}
		else {
			builder.append("票价：");
		}
		
		return builder.toString();
	}
	
	//输入图片所在文件夹路径，返回文件夹中所有文件名
	public static String[] getNames(String path) {
		File dir=new File(path);
		return dir.list();
	}
	
	/*输入图片所在文件夹路径，将所有图片调用api识别，保存识别json格式结果、文本结果、
	  提取后的重要信息*/
	public static void createJSON(String path, String savePath) throws FileNotFoundException {
		String[] fileNames=getNames(path);
		HashMap<String, HashMap<String, String>> total=new HashMap<>();
		for (String picName:fileNames) {
			//三个正则表达式
			PrintWriter jsonOut = new PrintWriter(savePath+"/json_" + picName.split("\\.")[0] + ".json");
			PrintWriter txtOut = new PrintWriter(savePath+"/txt_" + picName.split("\\.")[0] + ".txt");
			PrintWriter infoOut = new PrintWriter(savePath+"/info_" + picName.split("\\.")[0] + ".json");
			StringBuilder builder = new StringBuilder();
			String words = new String();

			AipOcr aipOcr = new AipOcr(APP_ID, API_KEY, SECRET_KEY);
			String imgData = path +"/"+ picName;
			HashMap<String, String> options = new HashMap<String, String>();
			
			//调用api获取json结果
			org.json.JSONObject jsonObject = aipOcr.receipt(imgData, options);
			jsonOut.println(jsonObject);
			System.out.println(jsonObject);
			BaiDuOCRBean baiDuOCRBean = com.alibaba.fastjson.JSONObject
					.toJavaObject(JSON.parseObject(jsonObject.toString()), BaiDuOCRBean.class);
			List<Words_result> list = baiDuOCRBean.getWords_result();
			
			//生成识别的文本结果
			for (int i = 0; i < list.size(); i++) {
				words = list.get(i).getWords();
				txtOut.println(words);
				builder.append(words + "\n");
				System.out.println(words);
			}
			
			//提取重要信息，生成字典格式结果
			String info = getImportantInfo(builder.toString());
			String[] line = info.split("\n");
			HashMap<String, String> map = new HashMap<>();
			for (String string : line) {
				String[] kAndV = string.split("：");
				if (kAndV.length == 2) {
					map.put(kAndV[0], kAndV[1]);
				} else {
					map.put(kAndV[0], "");
				}
			}
			
			//字典转换为json
			HashMap<String, HashMap<String, String>> hashMap = new HashMap<>();
			hashMap.put(picName, map);
			total.putAll(hashMap);
			JSONObject result = JSON.parseObject(JSON.toJSONString(hashMap));
			System.out.println(info);
			infoOut.print(result);
			infoOut.close();
			jsonOut.close();
			txtOut.close();
		}
		
		//所有图片结果汇总
		PrintWriter totalOut = new PrintWriter(savePath+"/total.json");
		totalOut.print(JSON.toJSONString(total));
		totalOut.close();
	}
	
	
	//直接使用识别后的文本提取重要信息，无需调用百度云api
	public static void createJSONWithoutAPI(String path,String savePath) throws Exception {
		String[] fileNames=getNames(path);
		HashMap<String, HashMap<String, String>> total=new HashMap<>();
		BufferedReader reader;
		PrintWriter infoOut;
		StringBuilder builder;
		for (String picName:fileNames) {
			reader=new BufferedReader(new FileReader(savePath+"/txt_" + picName.split("\\.")[0] + ".txt"));
			infoOut = new PrintWriter(savePath+"/info_" + picName.split("\\.")[0] + ".json");
			builder = new StringBuilder();
			
			String line;
			while ((line=reader.readLine())!=null) {
				builder.append(line+"\n");
			}
			String info = getImportantInfo(builder.toString());
			String[] lines = info.split("\n");
			HashMap<String, String> map = new HashMap<>();
			for (String string : lines) {
				String[] kAndV = string.split("：");
				if (kAndV.length == 2) {
					map.put(kAndV[0], kAndV[1]);
				} else {
					map.put(kAndV[0], "");
				}
			}
			
			HashMap<String, HashMap<String, String>> hashMap = new HashMap<>();
			hashMap.put(picName, map);
			total.putAll(hashMap);
			JSONObject result = JSON.parseObject(JSON.toJSONString(hashMap));
			System.out.println(info);
			infoOut.print(result);
			infoOut.close();
		}
		PrintWriter totalOut = new PrintWriter(savePath+"/total.json");
		totalOut.print(JSON.toJSONString(total));
		totalOut.close();
	}
	
	public static void main(String[] args) throws Exception {
		String path="/root/eclipse-workspace/火车票１";
		String savePath="/root/ticket_info";
//		createJSON(path,savePath);
		createJSONWithoutAPI(path,savePath);
	}
}

