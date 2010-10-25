package com.test;

import java.beans.Encoder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.spec.EncodedKeySpec;
import java.sql.Time;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.Node;
import javax.xml.transform.Templates;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
// 处理记录文件->读取xml->上传记录
public class Upload 
{
	private int count=0;			
	private String dir; 		 	// 目录名
	private String s3base;			// s3根目录
	private String ringBucket;		// ring目录
	private String imageBucket;		// image目录
	
	private DocumentBuilderFactory factory;
	private DocumentBuilder docBuilder;
	private Map<String,String>map;
	
	public void init()
	{
		dir = "/home/liutao/workspace/python/1-fetch/download_Classical/";
		factory = DocumentBuilderFactory.newInstance();
		try
		{
			docBuilder = factory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e)
		{
			System.out.println("ParserConfigurationException");
			e.printStackTrace();
		}
		s3base = "http://s3.amazonaws.com/";
		ringBucket = "ringtone_ring/";
		imageBucket = "ringtone_image/";
		
		map = new HashMap<String,String>();
		map.put("Title", "title");
		map.put("Artist", "artist");
		map.put("Category", "category");
		map.put("Downloads", "download_count");
		map.put("Mark", "avg_rate");
		map.put("Size", "size");
		map.put("Ring", "file_name");
		map.put("Image", "image");
	}
	
	public Upload()
	{
		init();
		
		System.out.println("beging...");
	    //upload("http://172.16.166.17:8888/ringtoneserver/insertsong?uuid=ffffffffff&title=t1&artist=a1&category=g3&download_count=1&avg_rate=100&size=32322&file_name=asdfadf.mp3&image=wwwww&s3url=s3url1");
	    processLog();
		System.out.println("end.");
		System.out.println("count:"+count);
	}
	
	
	public static void main(String []args)
	{
		Upload abc = new Upload(); 
	}
	
	public void parseXML(String filename, String uuid)
	{ 
		System.out.println(filename);
		File recordFile = new File(dir+filename);
		if(!recordFile.exists())
		{
			System.out.println(filename+" does not exist!");
			return ;	
		}
		
		int j;
		String content,typeStr,valStr,str="";
		content = "uuid="+uuid+"&";
		
		try
		{
			InputStream in = new FileInputStream(recordFile);
			Document doc;
			doc = docBuilder.parse(in);
			org.w3c.dom.Element  root = doc.getDocumentElement();
			
			org.w3c.dom.Node curNode;
			NodeList childen = root.getChildNodes();
			for(j=0; j<childen.getLength(); j++)
			{
				curNode = childen.item(j);
				if(curNode.getNodeType() == Node.ELEMENT_NODE)
				{
					typeStr = curNode.getNodeName();
					valStr = curNode.getFirstChild().getNodeValue();
					//System.out.println(typeStr+":"+valStr);
					
					if(!typeStr.equals("Date"))
					{
						if(typeStr.equals("Size"))	// 把后缀kb去掉	
							valStr=valStr.substring(0, valStr.indexOf('k'));
						else if(typeStr.equals("Downloads"))
						{// 处理数字中有逗号
							int temp = 0;
							for(int i=0; i<valStr.length(); i++)
								if(valStr.charAt(i)>='0' && valStr.charAt(i)<='9')
									temp = temp*10 + valStr.charAt(i)-'0';
							valStr = temp+"";
						}
						else if(typeStr.equals("Image"))		// 补全url
							valStr = s3base+imageBucket+uuid+valStr;
						else if(typeStr.equals("Ring"))
							str = valStr;
						
						content += map.get(typeStr)+"="+URLEncoder.encode(valStr)+"&";
					}
				}
			}
			content += "s3url="+s3base+ringBucket+uuid+str;
			//System.out.println(content);
			upload(content);
		}
		catch (IOException e)
		{
			System.out.println("IOException");
			e.printStackTrace();
		}
		catch (SAXException e)
		{
			System.out.println("SAXException");
			e.printStackTrace();
		}
	}
	
	public void processLog()
	{
		File logFile = new File(dir+"log");
		if(!logFile.exists())	
		{
			System.out.println("log file does not exist!");
			return ;
		}
		
		BufferedReader reader = null;
		try
		{
			reader=new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));
		}
		catch (FileNotFoundException e)
		{
			System.out.println("FileNotFoundException");
			e.printStackTrace();
		} 
		
		String line;
		String uuid;
		int temp;
		try
		{
			while((line=reader.readLine()) != null)
			{
				//System.out.println(line);
				temp = line.indexOf(':');
				if(line.substring(temp+1).startsWith("uuid:"))
				{ // only those without error
					parseXML(line.substring(0,temp), line.substring(temp+6));
				   /*
					try
					{
						Thread.sleep(300);	
					}
					catch (InterruptedException e)
					{
						System.out.println("sleep error!");
					}*/
				}
			}	
		}
		catch (Exception e)
		{
			System.out.println("file read error");
		}
		
	}
	

	
	public void upload(String content)
	{
		// HttpURLConnection  httpConn = null;

		String prefix = "http://172.16.166.17:8888/ringtoneserver/insertsong?";
		URL url;
		HttpURLConnection urlConn=null;
		//System.out.println(prefix+content);
		try
		{
			//content = content.replace(' ', '+');
			url = new URL(prefix+content);
			urlConn = (HttpURLConnection)url.openConnection();
			urlConn.setConnectTimeout(4000);
			urlConn.setDoInput(true);
			urlConn.setRequestMethod("GET");
			urlConn.connect();
			urlConn.getInputStream();
			count ++;
		}
		catch (MalformedURLException e)
		{
			System.out.println("MalformedURLException");
			e.printStackTrace();
		}
		catch (IOException e)
		{
			System.out.println("IOException");
			e.printStackTrace();
		}
		finally
		{
			try{
				urlConn.disconnect();
			}
			catch (Exception e) 
			{
				System.out.println("close error");
			}
			
		}
	 
		
		/*
		final String ratingUrl = Const.RatingBase + realKey + "?score=" + (int) rating*20;
        new Thread(new Runnable() {
@Override
public void run() {
try {
URL url = new URL(ratingUrl);
HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
urlConn.setConnectTimeout(4000);
urlConn.connect();
urlConn.getInputStream();
urlConn.disconnect();
// Log.d(TAG, ratingUrl);
} catch (MalformedURLException e) {
  e.printStackTrace();
} catch (IOException e) {
  e.printStackTrace();
}
}
}).start();
		
		*/
		
			/*
			 * DataInputStream dis = new DataInputStream(is); String line;
			 * while((line=dis.readLine()) != null) System.out.println(line);
			 * dis.close();
			 */
			/*
			 * httpConn = (HttpURLConnection)url.openConnection();
			 * HttpURLConnection.setFollowRedirects(true);
			 * httpConn.setRequestMethod("GET");
			 * httpConn.setRequestProperty("User-Agent"
			 * ,"Mozilla/4.0 (compatible; MSIE 6.0; Windows 2000)");
			 */	
	}
}
