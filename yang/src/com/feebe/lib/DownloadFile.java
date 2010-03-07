package com.feebe.lib;

import java.io.File;
import android.os.AsyncTask;

public class DownloadFile extends AsyncTask<String, Void, File> {
  public interface DownloadListerner {
    void onDownloadFinish(File file);
    void onDownloadFail();
  }
  DownloadListerner dlhander;
  private int min_size;
  public DownloadFile(DownloadListerner listerner, int minSize) {
    dlhander = listerner;
    min_size = minSize;
  }
  /*
   * p0 is url p1 is file
   * @see android.os.AsyncTask#doInBackground(Params[])
   */
  @Override
  protected File doInBackground(String... params) {
    try {
      return Util.download(params[0], params[1]);
    } catch (Exception e) {
      
    }
    return null;
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


