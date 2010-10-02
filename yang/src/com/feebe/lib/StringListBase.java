package com.feebe.lib;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.feebe.rings.RingUtil;
import com.feebe.rings.StringList.StringAdapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.TextView;

public abstract class StringListBase extends BaseList {    

  public static class StringAdapter extends UrlArrayAdapter<String, TextView> {
    private String key_;
    public StringAdapter(Context context, int resource, String url, long expire, String key) {
      super(context, resource);
      useDedup_ = false;
      key_ = key;
      if (url != null)
      runAsyn(url, expire);
    }

    @Override
    public String getT(Object o) {
      try {
        JSONObject obj = (JSONObject) o;
        String name = obj.getString(key_);
        // Log.e("or", name);
        return name;
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return null;
    }

		@Override
    public TextView getWrapper(View v) {
		  TextView t = (TextView)v.findViewById(android.R.id.text1);
		  v.setTag(t);
	    return t;
    }

		@Override
    public void applyWrapper(String item, TextView wp, boolean newView) {
      wp.setText(item);
    }
    @Override
    protected List getListFromUrl(String url, long expire) {
      return RingUtil.getJsonArrayFromUrl(url, expire);
    }
  }
	protected StringAdapter mAdapter;

}
