package com.feebe.lib;

import java.io.File;
import android.os.AsyncTask;

public class DownloadFile extends AsyncTask<String, Void, File> {
  public interface DownloadListerner {
    void onDownloadFinish(File file);
  }
  DownloadListerner dlhander;
  public DownloadFile(DownloadListerner listerner) {
    dlhander = listerner;
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
      dlhander.onDownloadFinish(file);
  }
}


