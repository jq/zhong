import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
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
	
	public static void copy(String srcPath, String destPath) {
		if(!new File(srcPath).exists()) {
			System.out.println("source file not exist");
			return ;
		}
//		String[] split = srcPath.split("\\.");
//		if(split[split.length-1].equals("lck")) return ;
		
		InputStream reader = null;
		FileOutputStream writer = null;
		try {
			reader = new FileInputStream(srcPath);
			writer = new FileOutputStream(destPath);
			byte[] buf = new byte[1024];
			while(reader.read(buf) != -1) {
				writer.write(buf);
			}
			writer.flush();
		} catch (Exception e) {
			System.out.println("copy file err:"+srcPath);
		}finally {
			if(reader != null) {
				try {
					reader.close();
				} catch (Exception e2) {}
			}
			if(writer != null) {
				try {
					writer.close();
				} catch (Exception e2) {}
			}
		}
	}
	
	
	public static void moveAllFiles(String srcFolder, String destFolder) {
		if(!new File(srcFolder).exists()) {
			System.out.println("source folder not exist");
			return ;
		}
		
		File[] list = new File(srcFolder).listFiles();
		for(File file: list) {
			copy(srcFolder+file.getName(), destFolder+file.getName());
		}
	}
	
	// process folder of depth 1
	public static void delAllFiles(String folder) {
		File dir = new File(folder);
		if(!dir.exists()) {
			System.out.println("folder to del not exist");
			return ;
		}
		File[] list = dir.listFiles(); 
		for(File file: list)
			file.delete();
		dir.delete();
	}
}
