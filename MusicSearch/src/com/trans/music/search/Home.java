package com.trans.music.search;

import com.feebe.lib.TabCreator;
import com.feebe.lib.Util;
import com.ringdroid.RingdroidSelectActivity;

import android.app.Activity;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

public class Home extends TabActivity implements OnTabChangeListener{
  @Override
  public void onCreate(Bundle savedInstanceState) {
    Const.main = this;
    super.onCreate(savedInstanceState);
    Const.init(this);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.tabmain);
    getTabHost().setOnTabChangedListener(this);
    TabHost tabHost = getTabHost();
    Intent hot = new Intent(this, StringList.class);
    TabCreator.createTab(this, tabHost, hot, getString(R.string.tab_hot), android.R.drawable.ic_input_get);
    hot = new Intent(this, SearchTab.class);
    TabCreator.createTab(this, tabHost, hot, getString(R.string.tab_find), android.R.drawable.ic_menu_search);
    
    hot = new Intent(this, RingdroidSelectActivity.class);
    TabCreator.createTab(this, tabHost, hot, getString(R.string.tab_lib), android.R.drawable.ic_menu_gallery);
    hot = new Intent(this, MusicSearch.class);
    TabCreator.createTab(this, tabHost, hot, "will be remove", android.R.drawable.ic_menu_search);

    setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
    Util.runFeed(4, this, R.raw.feed);
  }
  @Override
  protected Dialog onCreateDialog(int id) {
    if (id == Util.DOWNLOAD_APP_DIG) {
      return Util.createDownloadDialog(this); 
    }
    return null;
  }    

  @Override
  public void onTabChanged(String tabId) {
    Activity activity = getLocalActivityManager().getActivity(tabId);
    if (activity != null) {
        activity.onWindowFocusChanged(true);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    // Inflate the currently selected menu XML resource.
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.m, menu);      
      
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.menu_search:
      startSearch(this.getString(R.string.search_hint), true, null, false);
      return true;
    case R.id.menu_local:
      startActivity(new Intent(this, local.class));
      return true;
    case R.id.menu_help:
      startActivity(new Intent(this, help.class));
      return true;

    /*
      case R.string.clear_cache:
        Const.trimCache();
        return true;
        */
    }
    return super.onOptionsItemSelected(item);
    }

}
