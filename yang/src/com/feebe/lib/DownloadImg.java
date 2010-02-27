package com.feebe.lib;

import java.io.FileInputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.feebe.rings.Const;

public class DownloadImg extends AsyncTask<String, Void, Bitmap> {
  private ImageView image;
  public DownloadImg(ImageView im) {
    image = im;
  }
    @Override
    protected Bitmap doInBackground(String... params) {
      try {
        return BitmapFactory.decodeStream(new FileInputStream(
          Util.downloadFile(params[0], Const.OneYear)));
      } catch (Exception e) {
        
      }
      return null;
    }
      
    @Override
    protected void onPostExecute(Bitmap bmp) {
      if (bmp != null)
        image.setImageBitmap(bmp);
    }
  }
