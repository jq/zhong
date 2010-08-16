package com.feebe.rings;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.net.Uri;

import com.feebe.lib.DefaultDownloadListener;
import com.feebe.lib.DownloadFile;
import com.feebe.lib.Util;

public class RingDownloadFile extends DownloadFile {
  private JSONObject ring;

  
  public RingDownloadFile(DefaultDownloadListener listerner, int minSize,
      int filesize, String category, String artist, String title,
      ContentResolver cr, int[] fileKinds, JSONObject ring) {
    super(listerner, minSize, filesize, category, artist, title, cr, fileKinds);
    this.ring = ring;
  }

  protected Uri downloadFinish(File file) {
    Uri u = super.downloadFinish(file);
    try {
      ring.put("filePath", file.getAbsolutePath());
      ring.put(Const.mp3, u.toString());
      Util.saveFile(ring.toString(), Const.jsondir + file.getName());
    } catch (Exception e) {
    }
    return u;
  }
}
