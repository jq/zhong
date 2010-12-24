import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.Node;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class UpdateDB extends Thread{
	private static String Ring_Buckt_Name;
	private static String Image_Bucket_Name;
	private static String S3_Base_URL;
	private static Map<String,String> PropMap;
	private static String Logger_File_Name;
	private static String URL_Prefix;
	
	static {
		S3_Base_URL = "http://s3.amazonaws.com/";
		Ring_Buckt_Name = "ringtone_ring/";
		Image_Bucket_Name = "ringtone_image/";
		Logger_File_Name = "upload_log_file";
		URL_Prefix = "http://bingliu630.appspot.com/ringtoneserver/insertsong?";
		//URL_Prefix = "http://172.16.166.160:8888/ringtoneserver/insertsong?";
			
		PropMap = new HashMap<String,String>();
		PropMap.put("UUID", "uuid");
		PropMap.put("Title", "title");
		PropMap.put("Artist", "artist");
		PropMap.put("Category", "category");
		PropMap.put("Downloads", "download_count");
		PropMap.put("Mark", "avg_rate");
		PropMap.put("Size", "size");
		PropMap.put("Ring", "file_name");
		PropMap.put("Image", "image");
	}
	
	// xml in this directory need to be upload
	private String dirName;  	
	private Logger logger;
	private DocumentBuilder docBuilder;
	private int count = 0;
	
	public UpdateDB(String dirName) {
		if(dirName.endsWith("/"))
			this.dirName = dirName;
		else 
			this.dirName = dirName+"/";
	}
	
	public boolean createLoggerAndBuilder() {
		try {
			logger = Logger.getLogger("log");
			FileHandler fileHandler = new FileHandler(dirName+Logger_File_Name);
			fileHandler.setLevel(Level.FINE);
			fileHandler.setFormatter(new MyLogFormat());
			logger.addHandler(fileHandler);
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			docBuilder = factory.newDocumentBuilder();
			
			return true;
		} catch (Exception e) {
			System.out.println("log or builder create err");
			e.printStackTrace();
			return false;
		}
	}
	
	public ArrayList<File> getSortedXmlFiles() {
		try {
			File[] files = new File(dirName).listFiles();
			Arrays.sort(files, new Comparator<File>() {
				public int compare(File f1, File f2) {
					String s1 = f1.getName();
					String s2 = f2.getName();
					int idx1 = s1.indexOf(".xml");
					int idx2 = s2.indexOf(".xml");
					if(idx1!=-1 && idx2!=-1) {
						int num1 = Integer.parseInt(s1.substring(s1.indexOf('d')+1, idx1));
						int num2 = Integer.parseInt(s2.substring(s2.indexOf('d')+1, idx2));
						return num1 - num2;
					}
					else if(idx1 == -1)	return -1;
					else 	return 1;
				}
			});
			int i = 0;
			while(i<files.length && !files[i].getName().endsWith("xml")) i++;
			ArrayList<File> list = new ArrayList<File>();
			while(i<files.length) {
				list.add(files[i]);
				i ++;
			}
			return list;
			
		} catch (Exception e) {
			System.out.println("get sorted xml files err");
			e.printStackTrace();
		}
		return null;
	}
	
	public void run() {
		if(!createLoggerAndBuilder())  return ;
		ArrayList<File> files= getSortedXmlFiles();
		if(files == null) return ;
		
		logger.info("update start!");
		for(File file: files) {
			System.out.println("process "+file.getName());
			resolveXML(file);
		}
		logger.info("update finish!");
		logger.info("success:"+count);
 	}
	

	// resolve xml file and generate uploaded data
	public void resolveXML(File file) {
		if(file==null || !file.exists())  return ;
		StringBuffer buffer = new StringBuffer(URL_Prefix);
		try {
			Document doc = docBuilder.parse(file);
			Element root = doc.getDocumentElement();
			String attr,val,uuid="",ringName="";
			org.w3c.dom.Node curNode; 
			NodeList childen = root.getChildNodes();
			
			for(int i=0; i<childen.getLength(); i++) {
				curNode = childen.item(i);
				if(curNode.getNodeType() == Node.ELEMENT_NODE) {
					attr = curNode.getNodeName();
					val = curNode.getFirstChild().getNodeValue();
					//System.out.println(attr+":"+val);
					
					// "date" no need to upload
					if(!attr.equals("Date")) { 
						if(attr.equals("Size") || attr.equals("Downloads")) {
							// remove postfix "kb" and dot in string
							val = String.valueOf(Consts.String2Int(val));
						}
						else if(attr.equals("Image")) {
							// fill to complete url
							val = S3_Base_URL+Image_Bucket_Name+uuid+val;
						}
						else if(attr.equals("UUID")) {
							uuid = val;
						}
						else if(attr.equals("Ring")) {
							ringName = val;
						}
						buffer.append(PropMap.get(attr));
						buffer.append("=");
						buffer.append(URLEncoder.encode(val));
						buffer.append("&");
					}
				}
			}
			if(uuid.equals("") || ringName.equals("")) {
				logger.info(file.getName()+" uuid or ring miss err");
				return ;
			}
			buffer.append("s3url="+URLEncoder.encode(S3_Base_URL+Ring_Buckt_Name+uuid+ringName));
			buffer.append("&record="+file.getName().split("\\.")[0]);
			httpGet(buffer.toString());
			logger.info(file.getName()+" success");
			count ++;
		} catch (SAXException e) {
			logger.info(file.getName()+ " parser err");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println(buffer.toString());
			logger.info(file.getName()+" http get err");
			e.printStackTrace();
		}
	}
	
	public void httpGet(String urlString) throws Exception{
		URL url = new URL(urlString);
		url.openStream().close();
	}
	
	
	// my logger file format 
	class MyLogFormat extends Formatter {
		public String format(LogRecord record) {
			return record.getMessage()+"\n";
		}
	}
	/*
	public static void main(String[] args) {
		new UpdateDB("/home/liutao/mabilo/2010-12-21/").start();
	}*/
}
