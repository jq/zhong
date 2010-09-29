package com.feebe.rings;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Browser;

import com.feebe.lib.Util;

public class RingUtil {
  // TODO since it is all run on UI thread, we can use a buffer List
  public static List getJsonArrayFromUrl(String url, long expire) {
    JSONArray entries = Util.getJsonArrayFromUrl(url, expire);
    if (entries != null && entries.length() > 0) {
      int len = entries.length();
      List list = new ArrayList(len);
      for (int i = 0; i < len; i++) {
        try {
          JSONObject o = entries.getJSONObject(i);
          if (o != null) list.add(o);
        } catch (JSONException e) {
        }
      }
      return list;
    }
    return null;
  }
  
  public static void startShare(final Activity act) {
    // TODO Auto-generated method stub
    new AlertDialog.Builder(act)
    .setTitle(R.string.alertdialog_share)
    .setItems(R.array.select_share_methods, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch(which) {
            case 0:
              Intent sms = new Intent(Intent.ACTION_VIEW);
              sms.putExtra("sms_body",
                  act.getString(R.string.share_sms1) + " " +
                  act.getString(R.string.share_sms3)
                  ); 
              sms.setType("vnd.android-dir/mms-sms");
              act.startActivity(sms);
              break;
            case 1:
              Intent mEmailIntent = new Intent(android.content.Intent.ACTION_SEND);
              mEmailIntent.setType("plain/text");
              mEmailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, 
                  act.getString(R.string.app_name));
              mEmailIntent.putExtra(android.content.Intent.EXTRA_TEXT, 
                  act.getString(R.string.share_sms1) + " " +
                  act.getString(R.string.share_sms3));
              act.startActivity(Intent.createChooser(mEmailIntent, act.getString(R.string.app_name)));
              break;
            case 2:
              Browser.sendString(act, act.getString(R.string.share_sms1) + " " + act.getString(R.string.share_sms3));
            }
        }
    }).create().show();
    
  }
}
