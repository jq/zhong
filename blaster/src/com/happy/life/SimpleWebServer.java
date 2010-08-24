
package com.happy.life;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Copyright Paul Mutton
 * http://www.jibble.org/
 *
 */
public class SimpleWebServer extends Thread {

	private static RequestThread sRequestThread;
	
    public static final String VERSION = "SimpleWebServer  http://www.jibble.org/";
    public SimpleWebServer(String rootDir, int port) throws IOException {
        _rootDir = new File(rootDir);
        if (!_rootDir.isDirectory()) {
            throw new IOException("Not a directory.");
        }
        _serverSocket = new ServerSocket(port, 0, InetAddress.getByName("localhost"));
        start();
    }
    
    public void run() {
    	com.util.Utils.D("SimpleWebServer running");
        while (_running) {
            try {
                Socket socket = _serverSocket.accept();
                if (sRequestThread != null)
                	sRequestThread.interrupt();
                sRequestThread = new RequestThread(socket, _rootDir);
                sRequestThread.start();
            } catch (IOException e) {
            	e.printStackTrace();
            }
        }
    }
    
    public static void stopCurrentThread() {
    	if (sRequestThread != null)
    		sRequestThread.interrupt();
    }
    
    
    // Work out the filename extension.  If there isn't one, we keep
    // it as the empty string ("").
    public static String getExtension(java.io.File file) {
        String extension = "";
        String filename = file.getName();
        int dotPos = filename.lastIndexOf(".");
        if (dotPos >= 0) {
            extension = filename.substring(dotPos);
        }
        return extension.toLowerCase();
    }
    
    public static String getSuffix(String path) {
        String string = "";
        int dotPos = path.lastIndexOf(".");
        if (dotPos >= 0) {
            string = path.substring(dotPos);
        }
        return string.toLowerCase();
    }
    
    public void onDestroy() {
        try {
            _serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private File _rootDir;
    private ServerSocket _serverSocket;
    private boolean _running = true;
}