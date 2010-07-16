package com.feebe.lib;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

public class ImgLoader {
  String bmpUrl;
  String url;
  public void setUrl(String u) {
  	url = u;
  }
  
  public void setBmp(Bitmap bmp) {
    image.setImageBitmap(bmp);
  }
  
  public void download() {
    bmpUrl = url;
	  new DownloadImg(image).execute(url);
  }
  protected ImageView image;
}
