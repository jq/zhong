package com.happy.life;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;

import com.limegroup.gnutella.util.StringUtils;
import com.util.SearchBar;

public class MusicSearchBar extends SearchBar {

  public MusicSearchBar(Activity activity) {
    super(activity);
    mGo.setOnClickListener(new OnClickListener() {   
      @Override
      public void onClick(View v) {
        doSearch();
      }
    });
  }
  
  private void doSearch() {
    String query = mQuery.getText().toString();
    query = StringUtils.removeIllegalChars(query);
    com.util.Utils.D("start search");

    if (!TextUtils.isEmpty(query)) {
        //Debug.startMethodTracing("blaster");
        MusicSearchResultActivity.handleMp3ListSimpleIntent(mActivity, query);
    }
  }

}
