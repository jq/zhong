import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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

public class UpdateDB {
	private static HashMap<String, String> map;
	
	static {
		map = new HashMap<String, String>();
		map = new HashMap<String,String>();
		map.put("UUID", "uuid");
		map.put("Title", "title");
		map.put("Artist", "artist");
		map.put("Category", "category");
		map.put("Downloads", "download_count");
		map.put("Mark", "avg_rate");
		map.put("Size", "size");
		map.put("Ring", "file_name");
		map.put("Image", "image");
	}
	
	// xml in this directory need to be upload
	private Logger logger;
	private DocumentBuilder docBuilder;
	private int count = 0;
	
	
	private boolean createLoggerAndBuilder() {
		try {
			logger = Logger.getLogger("log");
			FileHandler fileHandler = new FileHandler(Consts.LOG_FILE);
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
			File[] files = new File(Consts.NEW_DOWNLOAD_DIR).listFiles();
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
	
	public static void update() {
		new UpdateDB().startUpdate();
 	}
	
	private void startUpdate() {
		if(!createLoggerAndBuilder())  return ;
		ArrayList<File> files= getSortedXmlFiles();
		if(files == null) return ;
		
		logger.info("update database start!");
		for(File file: files) {
			System.out.println("process "+file.getName());
			resolveFile(file);
		}
		logger.info("update database finish!");
		logger.info("success count:"+count);
	}

	private void resolveFile(File file) {
		BufferedReader reader = null;
		String uuid="", attr, val, ringname="";
		StringBuffer buffer = new StringBuffer(Consts.GAE_URL_PREFIX);
		
		try {
			reader = new BufferedReader(
					new FileReader(file));
			String line;
			while((line=reader.readLine()) != null) {
				int idx = line.indexOf(":");
				attr = line.substring(0, idx);
				val = line.substring(idx+1);
				
				if(attr.equals("UUID")) {
					uuid = val;
				}else if(attr.equals("Image")) {
					val = Consts.AMAZON_S3_URL+Consts.AMAZON_IMAGE_BUCKET+"/"
							+ uuid + val;
				}else if(attr.equals("Ring")) {
					ringname = val;
				}
				buffer.append(map.get(attr));
				buffer.append("=");
				buffer.append(URLEncoder.encode(val));
				buffer.append("&");
			}
			if(uuid.equals("") || ringname.equals("")) {
				logger.info(file.getName()+" uuid or ring miss err when parser xml");
				return ;
			}
			buffer.append("s3url="+URLEncoder.encode(
					Consts.AMAZON_S3_URL+Consts.AMAZON_RING_BUCKET+"/"+uuid+ringname));
			buffer.append("&record="+file.getName().split("\\.")[0]);
			httpGet(buffer.toString());
			logger.info(file.getName()+" success");
			count ++;
			
		} catch (FileNotFoundException e) {
			System.out.println("resolve file err");
			e.printStackTrace();
		} catch (Exception e) {
			logger.info(file.getName()+" http get err");
			e.printStackTrace();
		} finally {
			if(reader != null) 
				try {
					reader.close();
				} catch (Exception e2) {}
		}
	}
	
	/*
	// resolve xml file and generate uploaded data
	public void resolveXML(File file) {
		if(file==null || !file.exists())  return ;
		StringBuffer buffer = new StringBuffer(Consts.GAE_URL_PREFIX);
		try {
			Document doc = docBuilder.parse(file);
			Element root = doc.getDocumentElement();
			String uuid="", attr, val, ringname="";
			org.w3c.dom.Node curNode; 
			NodeList childen = root.getChildNodes();
			for(int i=0; i<childen.getLength(); i++) {
				curNode = childen.item(i);
				if(curNode.getNodeType() == Node.ELEMENT_NODE) {
					attr = curNode.getNodeName();
					val = curNode.getFirstChild().getNodeValue();
					//System.out.println(attr+":"+val);
					if(attr.equals("UUID")) {
						uuid = val;
					}else if(attr.equals("Image")) {
						val = Consts.AMAZON_S3_URL+Consts.AMAZON_IMAGE_BUCKET+"/"
								+ uuid + val;
					}else if(attr.equals("Ring")) {
						ringname = val;
					}
					buffer.append(map.get(attr));
					buffer.append("=");
					buffer.append(URLEncoder.encode(val));
					buffer.append("&");
				}
			}
			if(uuid.equals("") || ringname.equals("")) {
				logger.info(file.getName()+" uuid or ring miss err when parser xml");
				return ;
			}
			buffer.append("s3url="+URLEncoder.encode(
					Consts.AMAZON_S3_URL+Consts.AMAZON_RING_BUCKET+"/"+uuid+ringname));
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
	}*/
	
	public void httpGet(String urlString) throws Exception{
		URL url = new URL(urlString);
		System.out.println(url);
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
