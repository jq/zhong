package com.feebe.lib;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import entagged.audioformats.AudioFile;
import entagged.audioformats.AudioFileIO;
import entagged.audioformats.exceptions.CannotReadException;
import entagged.audioformats.exceptions.CannotWriteException;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class DownloadFile extends AsyncTask<String, Integer, File> {
  private int fileSize;
  
  DefaultDownloadListener dlhander;
  private int min_size;

  private String category;
  private String artist;
  private String title;
  private ContentResolver cr;
  private int[] fileKinds;
  public DownloadFile(DefaultDownloadListener listerner, int minSize, int filesize, 
      String category, String artist, String title, ContentResolver cr, int[] fileKinds) {
    dlhander = listerner;
    min_size = minSize;
    fileSize = filesize;
    this.category = category;
    this.artist = artist;
    this.title = title;
    this.cr = cr;
    this.fileKinds = fileKinds;
    listerner.onStart();
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
    if (dlhander != null)
      dlhander.onDownloadProgress(progress[0]);     
  }
  
  protected Uri downloadFinish(File file) {
    try {
      AudioFile audioFile = AudioFileIO.read(file);
      audioFile.getTag().setTitle(title);
      audioFile.getTag().setArtist(artist);
      audioFile.getTag().setGenre(category);
      audioFile.commit();
    } catch (CannotReadException e1) {
    } catch (CannotWriteException e) {
    }

    Uri u = Util.saveToMediaLib(title, file.getAbsolutePath(), file.length(), 
        artist, fileKinds, cr);
    return u;
  }
  @Override
  protected void onPostExecute(File file) {
  	if (file != null) {
			long length = file.length();
			if (length <= min_size) {
				file.delete();
		    if (dlhander != null)	dlhander.onDownloadFail();
			} else {
			  Uri u = downloadFinish(file);
		    if (dlhander != null) dlhander.onDownloadFinish(file, u);
			}
  	} else if (dlhander != null){
  		dlhander.onDownloadFail();
  	}
  }
}


