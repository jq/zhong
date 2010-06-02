package com.feebe.lib;

import com.admob.android.ads.AdManager;
import com.admob.android.ads.AdView;
import com.qwapi.adclient.android.view.QWAdView;

import android.R;
import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public abstract class BaseList extends ListActivity implements OnItemClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(Const.LAYOUT_LIST); 
        AdManager.setTestDevices( new String[] {                 
        	     AdManager.TEST_EMULATOR,             // Android emulator
        	     "E83D20734F72FB3108F104ABC0FFC738",  // My T-Mobile G1 Test Phone
        	     } ); 
        AdView admob = (AdView)findViewById(com.feebe.rings.R.id.adMob);
        if (admob != null){
            admob.setGoneWithoutAd(true);
        }      
        QWAdView qwAdView = (QWAdView)findViewById(com.feebe.rings.R.id.QWAd);
        AdListener adListener = new AdListener(this);
        qwAdView.setAdEventsListener(adListener,
            false);
        final ListView list = getListView();
        setListAdapter(getAdapter());
        list.setDividerHeight(1);
        list.setFocusable(true);
        // list.setOnCreateContextMenuListener(this);
        list.setTextFilterEnabled(true);
        list.setOnItemClickListener(this);
    }
    
    public abstract void onItemClick(AdapterView<?> parent, View v, int pos, long id);
    public abstract ListAdapter getAdapter();

 /*   
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
      boolean handled = keyCode == KeyEvent.KEYCODE_BACK;
      return handled || super.onKeyDown(keyCode, event);
    }
       
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
      boolean handled = keyCode == KeyEvent.KEYCODE_BACK;
      if (handled) {
        moveTaskToBack(true);
      }
      return handled || super.onKeyUp(keyCode, event);
    }
*/
}
