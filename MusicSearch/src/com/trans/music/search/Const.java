package com.trans.music.search;

import android.content.Context;

import com.feebe.lib.EndlessUrlArrayAdapter;

public class Const extends com.feebe.lib.Const{
  public static final String Key = "key";
  public static void init(Context c) {
    no_sd = R.string.no_sd;
    com.feebe.lib.Const.init(c);
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
