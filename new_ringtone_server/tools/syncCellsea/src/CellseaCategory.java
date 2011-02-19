import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.PooledConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.parser.HtmlParser;
import org.lobobrowser.html.test.SimpleUserAgentContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CellseaCategory {
	public final static String HOST_PREFIX = "http://www.cellsea.com/ringtone/lists/";
	public final static String HOST_POSTFIX = "/recent/7/";
	public final static int ITEM_PER_PAGE = 24;
	
	private static int timeThreshold;
	private static int fileIndex;
	private static String itemXpath = "//div[@id=\"basicpanel\"][1]//div";
	private static int numThreadAlive;
	private static ExecutorService pool;
	
	private String mCategory;
	private int page;
	private String mURL;	// for a category
	
	public CellseaCategory(String category) {
		mCategory = category;
		page = 0;
		mURL = HOST_PREFIX + mCategory + HOST_POSTFIX; 
	}
	
	public void parser() {
			Reader reader = null;
			try {
				UserAgentContext uacontext = new SimpleUserAgentContext();
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		        DocumentBuilder builder = factory.newDocumentBuilder();
		        String url = mURL+page; 

		        URL httpLink = new URL(url);
		        InputStream in = httpLink.openConnection().getInputStream();
		        
		        reader = new InputStreamReader(in);
	            Document document = builder.newDocument();
	            // Here is where we use Cobra's HTML parser.            
	            HtmlParser parser = new HtmlParser(uacontext, document);
	            parser.parse(reader);
	            
	            
	            // Now we use XPath to locate "a" elements that are
	            // descendents of any "html" element.
	            XPath xpath = XPathFactory.newInstance().newXPath();
	            
	            int i;
	            NodeList nodeList;
	            nodeList = (NodeList) xpath.evaluate(itemXpath, document, XPathConstants.NODESET);
	            for(i = 0; i < nodeList.getLength(); i++) {
	            	// for each music
	            	String info=null,date=null,size=null,pageURL=null;
	            	Element item = (Element) nodeList.item(i);
	                NodeList propList = item.getElementsByTagName("div");
	                for(int j=0; j<propList.getLength(); j++) {
	                	// for music's each property
	                	Element elem = (Element)propList.item(j);
	                	if(j == 1) {
	                		NodeList urlList = (NodeList)elem.getElementsByTagName("a");
	                		pageURL = ((Element)urlList.item(0)).getAttribute("href");
		                	//System.out.println(url);
	                	}
	                	String val = elem.getTextContent().trim();
	                	switch(j) {
	                		case 1:	info = val; break;
	                		case 2: date = val; break;
	                		case 3: size = val; break;
	                	}
	                }
	                if(pageURL!=null && info!=null && date!=null && size!=null) {
	                	// judge date here
	                	if(!newEnough(date))	return ;
	                	CellseaThread thread = CellseaThread.createCellseaThread(
	                			mCategory, fileIndex++, pageURL, info, size);
	                	if(thread != null) {
	                		startThread(thread);
	                	}
	                }
	            }
	            // render to next page
	            if(nodeList.getLength() == ITEM_PER_PAGE) {
		            page += ITEM_PER_PAGE;
		            parser();
	            }
			}catch (Exception e) {
				System.out.println("xpath parse err");
				e.printStackTrace();
			}finally {
				if(reader != null) {
					try {
						reader.close();
					} catch (Exception e2) {	}
				}
			}
	}
	
	private void startThread(CellseaThread thread) {
		numThreadAlive ++;
		pool.execute(thread);
	}
	
	public static void threadFinish() {
		numThreadAlive --;
	}
	
	//	Submitter:?Misterz Belieberz
    //  posted:?14 days ago
	private boolean newEnough(String str) {
		int idx1 = str.indexOf(':');
		int idx2 = str.indexOf(':', idx1+1);
		if(idx1==-1 || idx2==-1) {
			System.out.println("getDate() err");
			return false;
		}
		String[] split = str.substring(idx2+2).split(" ");
		String unit = split[1];
		int num = Integer.parseInt(split[0]);
		int days = 1000000;
		
		if(unit.equals("hours") || unit.equals("hour") || 
				unit.equals("minute") || unit.equals("minutes")) {
			days = 0;
		}else if(unit.equals("days") || unit.equals("day")) {
			days = num;
		}else if(unit.equals("months") || unit.equals("month")) {
			days = 30*num;
		}else {
			System.out.println("time unit:"+unit);
		}
		//System.out.println(days);
		return days < timeThreshold;
	}
	
	//public final static String[] CATEGORY = new String[]{"Comedy"};
	public final static String[] CATEGORY = new String[]{"Acoustic", "Alternative", "Anime", 
		"Blues", "Classical", "Comedy", "Country", "Dance", "Electronic", "Funk", "Game", 
		"Hard Rock", "Hip-Hop", "Humour", "Indie", "Instrumental", "Jazz", "Latin", "Musical", 
		"Noise", "Oldies", "Opera", "Pop", "R_B", "Rap", "Rock", "Soundtrack", "Symphony", 
		"Techno", "Trailer", "Vocal"};
	public static void sync(Date lastSyncDate) {
		timeThreshold = (int)((new Date().getTime() - lastSyncDate.getTime())/(24*60*60*1000));
		System.out.println("time threshold:"+timeThreshold);
		fileIndex = 1;
		numThreadAlive = 0;
		
		pool = Executors.newFixedThreadPool(3);
		System.out.println("\n");
		for(int i=0; i<CATEGORY.length; i++) {
			System.out.println("Category:"+CATEGORY[i]);
			new CellseaCategory(CATEGORY[i]).parser();
		}
		pool.shutdown();
		
		while(numThreadAlive > 0) {
			try {
				Thread.sleep(10000);
			} catch (Exception e) {
			}
		}
	}
}
