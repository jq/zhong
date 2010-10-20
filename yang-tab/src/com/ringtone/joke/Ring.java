package com.ringtone.joke;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

import com.feebe.lib.ImgThread;
import com.feebe.lib.Util;
import com.feebe.rings.Const;
import com.feebe.rings.HotList;
import com.feebe.rings.R;
import com.feebe.rings.RingUtil;
import com.feebe.rings.SearchTab;
import com.feebe.rings.StringList;
import com.feebe.rings.local;
import com.lib.RingSelect;

public class Ring extends TabActivity implements OnTabChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      ImgThread.noImg = BitmapFactory.decodeResource(getResources(), R.drawable.ring);
      Const.init(this);
      //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      setContentView(R.layout.main);
      getTabHost().setOnTabChangedListener(this);
      TabHost tabHost = getTabHost();
      Intent hot = new Intent(this, HotList.class);
      TabCreator.createTab(this, tabHost, hot, getString(R.string.tab_hot), R.drawable.tab_hot);
      Intent me = new Intent(this, StringList.class);
      TabCreator.createTab(this, tabHost, me, getString(R.string.tab_browse), R.drawable.tab_browse);
      Intent dl = new Intent(this, RingSelect.class);
      TabCreator.createTab(this, tabHost, dl, getString(R.string.tab_download), R.drawable.tab_download);
      Intent search = new Intent(this, SearchTab.class);
      TabCreator.createTab(this, tabHost, search, getString(R.string.tab_search), R.drawable.tab_search);
      setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
      Util.runFeed(4, this, R.raw.feed);     
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
      if (id == Util.DOWNLOAD_APP_DIG) {
        return Util.createDownloadDialog(this); 
      }
      return null;
    }    
    // Copied from DialTacts Activity
    /** {@inheritDoc} */
    public void onTabChanged(String tabId) {
        Activity activity = getLocalActivityManager().getActivity(tabId);
        if (activity != null) {
            activity.onWindowFocusChanged(true);
        }
    }
    
    @Override
    protected void onDestroy() {
      super.onDestroy();
      if (Const.dbAdapter != null)
        Const.dbAdapter.close();
    }
        
  	private int choice = 0;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);
      menu.add(0, R.string.menu_search, 0, R.string.menu_search).setIcon(
          android.R.drawable.ic_menu_search);
      menu.add(0, R.string.menu_downloaded, 0, R.string.menu_downloaded).setIcon(
          android.R.drawable.ic_menu_gallery);
      if (!Util.hasRate()) {
        menu.add(0, R.string.menu_rate, 0, R.string.menu_rate).setIcon(
          android.R.drawable.ic_menu_help);
      }
      menu.add(0, R.string.menu_unset_ringtone, 0, R.string.menu_unset_ringtone).setIcon(
          android.R.drawable.ic_lock_silent_mode);
      menu.add(0, R.string.clear_cache, 0, R.string.clear_cache).setIcon(
          android.R.drawable.ic_delete);
      
      menu.add(0, R.string.alertdialog_share, 0, R.string.alertdialog_share).setIcon(
          android.R.drawable.ic_menu_share);
      if (Const.ver > 4) {
        menu.add(0, R.string.upload, 0, R.string.upload);
      }
      return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
        case R.string.menu_rate:
          Util.startRate(this);
          return true;
        case R.string.alertdialog_share:
          RingUtil.startShare(this);
          return true;
        case R.string.clear_cache:
          Const.trimCache();
          return true;
        case R.string.upload:
          com.feebe.lib.util5.loadBrowser(this, "http://ggapp.appspot.com/mobile/upload/");
          return true;
        case R.string.menu_search:
           startSearch(this.getString(R.string.search_hint), true, null, false);
          return true; 
        case R.string.menu_downloaded:
          Intent intent = new Intent();
          intent.setClass(Ring.this, local.class);
          startActivity(intent);
          return true;
        case R.string.menu_unset_ringtone:
        	AlertDialog dialog2 = new AlertDialog.Builder(this)
        	.setIcon(android.R.drawable.ic_dialog_alert)
        	.setTitle(R.string.alertdialog_select)
        	.setSingleChoiceItems(R.array.select_unset_ringtone, 0, new DialogInterface.OnClickListener() {					
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO remember chioce
							choice = which;
						}
					})
					.setPositiveButton(R.string.alertdialog_ok, new DialogInterface.OnClickListener() {						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
							switch(choice){
							case 0:							
								audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
								break;
							case 1:
								audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
								break;
							}
							
						}
					})
					.setNegativeButton(R.string.alertdialog_cancel, new DialogInterface.OnClickListener() {					
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub							
						}
					})
					.create();
        	dialog2.show();
        	return true;
      }
      return super.onOptionsItemSelected(item);
    }
}