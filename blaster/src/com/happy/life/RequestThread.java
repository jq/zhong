


package com.happy.life;

import android.util.Log;

import java.io.*;
import java.net.*;
import java.util.*;

import com.limegroup.gnutella.settings.SharingSettings;

/**
 * Copyright Paul Mutton
 * http://www.jibble.org/
 *
 */
public class RequestThread extends Thread {
	
    public RequestThread(Socket socket, File rootDir) {
        _socket = socket;
        _rootDir = rootDir;
    }
    
    private static void sendHeader(
    		BufferedOutputStream out,
    		int code,
    		String contentType,
    		long contentLength, long lastModified) throws IOException {
        out.write(("HTTP/1.0 " + code + " OK\r\n" + 
                   "Date: " + new Date().toString() + "\r\n" +
                   "Server: JibbleWebServer/1.0\r\n" +
                   "Content-Type: " + contentType + "\r\n" +
                   "Expires: Thu, 01 Dec 1994 16:00:00 GMT\r\n" +
                   ((contentLength != -1) ? "Content-Length: " + contentLength + "\r\n" : "") +
                   "Last-modified: " + new Date(lastModified).toString() + "\r\n" +
                   "\r\n").getBytes());
    }
    
    private static void sendError(BufferedOutputStream out, int code, String message) throws IOException {
        message = message + "<hr>" + SimpleWebServer.VERSION;
        sendHeader(out, code, "text/html", message.length(), System.currentTimeMillis());
        out.write(message.getBytes());
        out.flush();
        out.close();
    }
    
    public static Map<String, String> getQueryMap(String query) {  
    	String[] params = query.split("&");  
    	Map<String, String> map = new HashMap<String, String>();  
    	for (String param : params) {
    	    String[] pair = param.split("=");
    	    if (pair.length > 1) {
        		String name = URLDecoder.decode(param.split("=")[0]);  
        		String value = URLDecoder.decode(param.split("=")[1]);  
        		map.put(name, value);  
    	    }
    	}  
    	return map;  
    }  
    
    private void processRequest(String path, BufferedOutputStream out) throws IOException {
    	if (path.startsWith("/")) {
    		path = path.substring(1, path.length()).trim();
    	}
		Map<String, String> map = getQueryMap(path);
		
    	com.util.Utils.D("+++++++++++++++++++++");
    	com.util.Utils.D("path = " + path);
		
    	sendHeader(out, 200, "audio/mpeg", -1, System.currentTimeMillis());
    	
    	String command = map.get("cmd");
    	path = map.get("file");
    	
    	com.util.Utils.D("path = " + path);
    	
    	if (command != null) {
    		if (command.equals("ready")) {
	    		File file = new File(_rootDir, path);
	    		if (file.exists() && file.length() >= 512) {
	    			out.write("true".getBytes());
	    			out.close();
	    		} else {
	    			out.write("false".getBytes());
	    			out.close();
	    		}
	    		return;
    		} else {
    			System.out.println("Invalid command: " + command);
    			return;
    		}
    	}
    	
    	String dwFile = map.get("dwfile");
    	
    	try {
    		File file = new File(_rootDir, path);
    		
    		RandomAccessFile mFile = new RandomAccessFile(file, "r");
    		
    		byte[] buffer = new byte[4096];
    		int totalBytes = 0;
    		boolean finished = false;
    		while (true) {
    			int len = mFile.read(buffer);
    			if (len > 0) {
    				totalBytes += len;
    				out.write(buffer, 0, len);
    				out.flush();
    			} else {
    				if (finished)
    					break;
    				com.util.Utils.D("Total bytes written: " + totalBytes);
    				
    				File tmp = new File(_rootDir, path);
    				if (!tmp.exists()) {
    					finished = true;
    					if (dwFile != null) {
    						File tmp2 = new File(SharingSettings.DEFAULT_SAVE_DIR, dwFile);
    						if (!tmp2.exists())
    							break;
	    					mFile = new RandomAccessFile(tmp2 , "r");
	    					mFile.seek((long)totalBytes);
	    					com.util.Utils.D("Switched to downloaded file.");
    					} else {
    						break;
    					}
    				}
    				
    				Thread.sleep(1000);
    			}
    		}
    	} catch (InterruptedException e) {
    		out.close();
    		e.printStackTrace();
    	}
    }
    
    public void run() {
        InputStream reader = null;
        try {
            _socket.setSoTimeout(30000);
            BufferedReader in = new BufferedReader(new InputStreamReader(_socket.getInputStream()), 8192);
            BufferedOutputStream out = new BufferedOutputStream(_socket.getOutputStream(), 8192);
            
            String request = in.readLine();
            if (request == null || !request.startsWith("GET ") ||
            		!(request.endsWith(" HTTP/1.0") || request.endsWith("HTTP/1.1"))) {
                sendError(out, 500, "Invalid Method.");
                return;
            }
            String path = request.substring(4, request.length() - 9);
            processRequest(path, out);
        } catch (IOException e) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                	ex.printStackTrace();
                }
            }
        }
    }
    
    
    private File _rootDir;
    private Socket _socket;
}