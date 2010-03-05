package com.trans.music.search;

import com.feebe.lib.EndlessUrlArrayAdapter;

public class Const extends com.feebe.lib.Const{
  public static final String SearchBase = "http://mp3.sogou.com/music.so?pf=mp3&ac=1&class=1&query=";
  public static final String Key = "key"; 
  
  public static void init() {
    no_sd = R.string.no_sd;
    com.feebe.lib.Const.init();
    QWName = "Ringtone-g56rajjb";
    QWID = "34d153f75db441cdbb776ffb70c569c5";
    AdsViewID = R.id.AdsView;
    LAYOUT_LIST = R.layout.list;
    tab_image = R.id.tab_image;
    tabheader = R.layout.tabheader;
    tab_label = R.id.tab_label;
    no_result = R.string.no_result;
    // in res/layout/searchlist_row.xml
    EndlessUrlArrayAdapter.Throbber = R.id.throbber;
    EndlessUrlArrayAdapter.ThrobberViewRes = R.layout.pending_view;
  }

}
