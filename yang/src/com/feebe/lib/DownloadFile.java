package com.feebe.lib;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;
import android.util.Log;

public class DownloadFile extends AsyncTask<String, Integer, File> {
  private int fileSize;
  
  public interface DownloadListerner {
    void onDownloadFinish(File file);
    void onDownloadProgress(int percentage);
    void onDownloadFail();
  }
  DownloadListerner dlhander;
  private int min_size;
  public DownloadFile(DownloadListerner listerner, int minSize) {
    dlhander = listerner;
    min_size = minSize;
  }
  
  public void setFileSize(int size){
	  fileSize = size;
  }
  /*
   * p0 is url p1 is file
   * @see android.os.AsyncTask#doInBackground(Params[])
   */
  @Override
  protected File doInBackground(String... params) {
	  long count = 0;
	  URL url = null;
	  HttpURLConnection urlConn = null;
	  InputStream stream = null;
	  DataInputStream is = null;
	  try {
		  url = new URL(params[0]);
		  urlConn = (HttpURLConnection)url.openConnection();
		  urlConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");
		  urlConn.setConnectTimeout(4000);
		  urlConn.connect();
      
		  stream = urlConn.getInputStream();
		  byte[] buff = new byte[4096];
		  is = new DataInputStream(stream);
		  int len;
		  File f = new File(params[1]);
		  FileOutputStream file =  new FileOutputStream(f);
		  while ((len = is.read(buff)) > 0) {
			  file.write(buff, 0, len);
			  count = count + len;
			  if (fileSize != 0)
				  publishProgress((int) (count*100)/fileSize);
		  }
		  urlConn.disconnect();
		  return f;
	  } catch (IOException e) {
		  Log.e("download", e.getMessage());
	  }
	  return null;
  }
  
  @Override
  protected void onProgressUpdate(Integer... progress) {         
      dlhander.onDownloadProgress(progress[0]);     
}  
  @Override
  protected void onPostExecute(File file) {
  	if (file != null) {
			long length = file.length();
			if (length <= min_size) {
				file.delete();
				dlhander.onDownloadFail();
			} else {
	      dlhander.onDownloadFinish(file);
			}
  	} else {
  		dlhander.onDownloadFail();
  	}
  }
}


