package com.trans.music.search;

import com.feebe.lib.TabCreator;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

public class Home extends TabActivity implements OnTabChangeListener{
  @Override
  public void onCreate(Bundle savedInstanceState) {
    Const.main = this;
    super.onCreate(savedInstanceState);
    Const.init();
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.tabmain);
    getTabHost().setOnTabChangedListener(this);
    TabHost tabHost = getTabHost();
    Intent hot = new Intent(this, StringList.class);
    TabCreator.createTab(this, tabHost, hot, getString(R.string.tab_hot), android.R.drawable.ic_input_get);
    hot = new Intent(this, MusicSearchHome.class);
    TabCreator.createTab(this, tabHost, hot, getString(R.string.tab_hot), android.R.drawable.ic_media_play);
    
    
    setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

  }
  @Override
  public void onTabChanged(String tabId) {
    Activity activity = getLocalActivityManager().getActivity(tabId);
    if (activity != null) {
        activity.onWindowFocusChanged(true);
    }
  }
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
   // menu.add(0, R.string.menu_search, 0, R.string.menu_search).setIcon(
   //     android.R.drawable.ic_menu_search);
    return true;
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    /*
    switch (item.getItemId()) {
      case R.string.clear_cache:
        Const.trimCache();
        return true;
    }*/
    return super.onOptionsItemSelected(item);
    }

}
