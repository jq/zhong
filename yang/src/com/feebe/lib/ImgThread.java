package com.feebe.lib;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;

public class ImgThread implements android.widget.AbsListView.OnScrollListener {
  public ImgThread(AbsListView v) {
  	v.setOnScrollListener(this);
  }
  public static Bitmap noImg;
	@Override
  public void onScroll(AbsListView view, int firstVisibleItem,
      int visibleItemCount, int totalItemCount) {
   }

	@Override
  public void onScrollStateChanged(AbsListView view, int scrollState) {
    if (scrollState != SCROLL_STATE_IDLE) {
      return;
    }
    int itemCnt = view.getChildCount();
    //// // Log.e("scrollState", "" + scrollState + " " + itemCnt);
    for (int i = 0; i < itemCnt; ++i) {
      View v = view.getChildAt(i);
      if (v == null) {
        //// // Log.e("view", "null " + i);
        continue;
      }
      ImgLoader w = (ImgLoader) v.getTag();
      
      if (w != null) {
        if (w.url == null) {
        	if (noImg != null) w.setBmp(noImg);
          w.bmpUrl = null;
        } else {
          if (w.url != w.bmpUrl) {
            w.download();
          } 
        }
      } else {
        //// // Log.e("w","null");
      }
    }
   }
}


