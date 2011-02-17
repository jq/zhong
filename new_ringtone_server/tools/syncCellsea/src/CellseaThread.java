import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLDecoder;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.parser.HtmlParser;
import org.lobobrowser.html.test.SimpleUserAgentContext;
import org.lobobrowser.util.io.RecordedInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CellseaThread extends Thread{
	public final static String MUSIC_HOST_PREFIX = "http://www.mabilo.com/search/";
	public final static String MUSIC_HOST_POSTFIX = "-1-tr.htm";
	public final static String DEFAULT_IMGNAME = "0.jpg";
	private String mUrl;
	private String mRingUrl;
	private MusicInfo mMusicInfo;
	
	private boolean getRingUrl() {
		char[] line = new char[1024];
		int i=0;
		Reader reader = null;
		try {
			URL url = new URL(mUrl);
			reader = new InputStreamReader(
					url.openConnection().getInputStream());
			while(reader.read(line) != -1) {
				String str1 = new String(line);
				int idx1 = str1.indexOf("id=http");
				if(idx1 != -1) {
					idx1 += 3;
					reader.read(line);
					String str2 = new String(line);
					String str = str1 + str2;
					int idx2 = str.indexOf("%26stream", idx1);
					if(idx2 != -1) {
						mRingUrl = URLDecoder.decode(str.substring(idx1, idx2));
						//System.out.println(mRingUrl);
						return true;
					}
 				}
			}
			return false;
		} catch (Exception e) {
			System.out.println("Ringtone Url not Found!");
			e.printStackTrace();
			return false;
		}finally {
			if(reader != null) {
				try {
					reader.close();
				} catch (Exception e2) { }
			}
		}
	}
	
	// get image
	private boolean getImage(String rawUrl) {
		Reader reader = null;
		try {
			UserAgentContext uacontext = new SimpleUserAgentContext();
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        URL url = new URL(rawUrl);
	        
	        InputStream in = url.openConnection().getInputStream();
	        
	        reader = new InputStreamReader(in);
	        Document document = builder.newDocument();
	        // Here is where we use Cobra's HTML parser.            
	        HtmlParser parser = new HtmlParser(uacontext, document);
	        parser.parse(reader);
	        XPath xpath = XPathFactory.newInstance().newXPath();
	        String imgXpath = "//div[@class=\"row2\"][1]//img[1]";
	        NodeList nodeList = (NodeList) xpath.evaluate(imgXpath, document, XPathConstants.NODESET);
	        if(nodeList.getLength() == 0) {
	        	// use default image instead 
	        	mMusicInfo.setImgName(DEFAULT_IMGNAME);
	        }else {
	        	String imgUrl = ((Element)nodeList.item(0)).getAttribute("src");
	        	mMusicInfo.setImgName(mMusicInfo.getTitle() + ".jpg");
	        	Utils.download(imgUrl, Consts.NEW_DOWNLOAD_DIR+mMusicInfo.getImageName());
	        }
	        return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}finally {
			if(reader != null) {
				try {
					reader.close();
				} catch (Exception e2) {	}
			}
		}
	}
	
	private boolean download() {
		// download ringtone
		if(!getRingUrl()) return false;
		mMusicInfo.setRingName(mMusicInfo.getTitle()+mMusicInfo.getFormat());
		if(!Utils.download(mRingUrl, Consts.NEW_DOWNLOAD_DIR+mMusicInfo.getRingName())) return false;
		
		// download image
		// search from a image web, like: mabilo
		String[] split = mMusicInfo.getTitle().split(" ");
		StringBuffer rawUrl = new StringBuffer(MUSIC_HOST_PREFIX);
		for(int i=0; i<split.length-1; i++) {
			rawUrl.append(split[i]);
			rawUrl.append("_");
		}
		rawUrl.append(split[split.length-1]);
		rawUrl.append(MUSIC_HOST_POSTFIX);
		return getImage(rawUrl.toString());
	}
	
	//	Tired of Waiting
	//  Artist:?2PM
    //	Format:?MP3
	private static String[] getTitleAndArtist(String str) {
		int idx1 = str.indexOf(':');
		int idx2 = str.indexOf(':', idx1+1);

		if(idx1==-1 || idx2==-1) {
			System.out.println("getTitle() err");
			return null;
		}
		
		String title = str.substring(0, idx1-6).trim();
		if(title.equals("")) title = UUID.randomUUID().toString();
		String artist = str.substring(idx1+2, idx2-6).trim();
		
		String format = str.substring(idx2+2).trim();
		if(format.equals("MP3")) {
			format = ".mp3";
			return new String[]{title, artist, format};
		}else {
			System.out.println("music format:"+format);
			return null;
		}
	}
	
	// "File size:?454?KB"
	private static String[] getSize(String str) {
		int idx1 = str.indexOf(':');
		int idx2 = str.indexOf("KB", idx1+2);
		if(idx1==-1 || idx2==-1) {
			System.out.println("getSize() err");
			return null;
		}
		String size = str.substring(idx1+2, idx2-1);
		return new String[]{size};
	}
	
	public static CellseaThread createCellseaThread(String category, int index, 
			String pageURL, String info, String size) {
		String[] infoArray = getTitleAndArtist(info);
		if(infoArray==null || infoArray.length!=3)  return null;
		
		String[] sizeArray = getSize(size);
		if(sizeArray == null)  return null;
		
		MusicInfo music = new MusicInfo(index, category, infoArray[0],
		            infoArray[1], infoArray[2], sizeArray[0]);
		return new CellseaThread(music, pageURL);
	}
	
	private CellseaThread(MusicInfo music, String url) {
		mUrl = url;
		mMusicInfo = music;
	}
	
	public void run() {
		System.out.println("start "+mMusicInfo.getTitle()+"!");
		if(download()) {
			mMusicInfo.setUUID(UUID.randomUUID().toString());
			
			// convert music format
			// conver music format here
			
		if(UploadToS3.upload(mMusicInfo) && 
			 	StoreRingInfo.storeMusicInfo(mMusicInfo)) {
				System.out.println(mMusicInfo.getTitle()+" finish!");
			}
		}
		CellseaCategory.threadFinish();
	}
}
