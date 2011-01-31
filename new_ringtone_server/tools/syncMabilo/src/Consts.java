import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Locale;


public class Consts {
	public static final String SYNC_DIR = "d:/mabilo/";
	public static final String NEW_DOWNLOAD_DIR = SYNC_DIR+"download/";
	public static final String M4R_DIR = SYNC_DIR + "m4r/";
	public static final String LAST_SYNC_TIME_FILENAME = "lasttime";
	public static final String MABILO_BASE = "http://www.mabilo.com/";
	public static final String AMAZON_RING_BUCKET = "ringtone_ring";
	public static final String AMAZON_IMAGE_BUCKET = "ringtone_image";
	public static final String AMAZON_M4R_BUCKET = "ringtone_m4r";
	
	public static final SimpleDateFormat SDF=new SimpleDateFormat("MMM dd yyyy", Locale.US);
	
	public static String fetchHtmlPage(String url) throws IOException {
		return fetchHtmlPage(url, "utf-8");
	}
	
	// get webpage content 
	public static String fetchHtmlPage(String link, String coding) throws IOException { 
		URL url = new URL(link);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.3) Gecko/20100401 Firefox/3.6.3");
		connection.setRequestProperty("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
		connection.setRequestProperty("Accept-Language", "en-us");
		connection.setRequestProperty("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
		connection.setRequestProperty("Keep-Alive", "300");
		connection.setRequestProperty("Connection", "keep-alive");
		connection.setConnectTimeout(20000);
		connection.connect();
		//String cookie = connection.getHeaderField("Set-Cookie");
		
		StringBuilder builder = new StringBuilder(1024);
		InputStreamReader is = coding != null ? new InputStreamReader(connection.getInputStream(), coding) :
			new InputStreamReader(connection.getInputStream());
		
		BufferedReader reader = new BufferedReader(is);
		String line;
		while ((line = reader.readLine()) != null) {
			builder.append(line + "\n");
		}
	
		try {
			is.close();
		}catch (Exception e)  {
			e.printStackTrace();
		}
		
		return builder.toString();
	}
	
	public static boolean downloadTryMulTimes(String url, String filename) {
		for(int i=0; i<3; i++) {
			if(download(url, filename)) return true;
			try {
				Thread.sleep(1000);
			} catch (Exception e) { }
		}
		// delete fail file 
		File delfile = new File(filename);
		if(delfile.exists()) {
			delfile.delete();
		}
		return false;
	}
	
	// download a file
	public static boolean download(String link, String filename)  {
		int byteread=0,bytesum=0;
		InputStream inStream=null;
		FileOutputStream fs=null;
		
		try {
			URL url = new URL(link);
			URLConnection conn;
			conn = url.openConnection();
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(60000);
	        inStream = conn.getInputStream();
	        fs = new FileOutputStream(filename);
	        byte[] buffer = new byte[1024];
	    
	        while ((byteread = inStream.read(buffer)) != -1) {
	        	bytesum += byteread;
                fs.write(buffer, 0, byteread);
            }
	        fs.flush();
	        return true;
		}catch (Exception e) {
			e.printStackTrace();
			return false;
		}finally {
			try  {
				if(inStream != null)  inStream.close();
				if(fs != null) 		fs.close();
			}
			catch (Exception e2) {
				System.out.println("close error in download");
				e2.printStackTrace();
			}
		}
	}
	
	public static int String2Int(String value) {
		int result = 0;
		for(int i=0; i<value.length(); i++) 
			if(Character.isDigit(value.charAt(i)))
				result = result*10 + value.charAt(i) - '0';
		return result;
	}
}
