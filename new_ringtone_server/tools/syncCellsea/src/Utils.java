import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;


public class Utils {
	// download file 
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
	
	public static String filterString(String in) {
		char ch;
		int i;
		for(i=0; i<in.length(); i++) {
			ch = in.charAt(i);
			if(!Character.isLetterOrDigit(ch) && ch!=' '
					&& ch!='-' && ch!='_' && ch!='(' && ch!=')' && ch!='.' )
					break;
		}
		return in.substring(0, i).trim();
	}
}
