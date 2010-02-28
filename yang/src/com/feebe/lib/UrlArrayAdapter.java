package com.feebe.lib;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Comparator;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public abstract class UrlArrayAdapter<T, W> extends ArrayAdapter<T> {
  /**
   * Constructor
   *
   * @param context The current context.
   * @param resource The resource ID for a layout file containing 
   *        a layout to use when instantiating views.
   */
  public UrlArrayAdapter(Context context, int resource) {
    super(context, resource);
    mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    mResource = resource;
  }

  /**
   * Constructor
   *
   * @param context The current context.
   * @param resource The resource ID for a layout file
   *        containing a layout to use when instantiating views.
   * @param objects The objects to represent in the ListView.
   */
  public UrlArrayAdapter(Context context, int resource, T[] objects) {
    super(context, resource, objects);
    mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    mResource = resource;
  }

  /**
   * Constructor
   *
   * @param context The current context.
   * @param resource The resource ID for a layout file containing a 
   *        layout to use when instantiating views.
   * @param objects The objects to represent in the ListView.
   */
  public UrlArrayAdapter(
      Context context, int resource, List<T> objects) {
    super(context, resource, objects);
    mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    mResource = resource;
  }
  
  public abstract T getT(JSONObject obj);

  public abstract W getWrapper(View v);
  
  public abstract void applyWrapper(T item, W wp, boolean newView);

  protected LayoutInflater mInflater;
  protected int mResource;

  @SuppressWarnings("unchecked")
  @Override
	public View getView(int position, View convertView, ViewGroup parent) {
    boolean newView;
		View view = convertView;
		W w;
    if (view != null) {
      w = (W) view.getTag();
      if (w == null) {
        view = null;
        
      }
    }
		if (view == null) {
      view = mInflater.inflate(mResource, parent, false);
      w = getWrapper(view);
      newView = true;
		} else {
		  newView = false;
			w = (W) view.getTag();
		}
    T item = getItem(position);
    applyWrapper(item, w, newView);
    return view;
	}

  protected void onNoResult(){
	  Toast.makeText(Const.main, Const.no_result,Toast.LENGTH_SHORT).show();
  }
    
  public boolean runJsonArray(JSONArray entries) {
    try {
      int len = entries.length();
      if (len == 0) {
        return false;
      }
      for(int i = 0; i < len; i++){
        if( entries.isNull(i) )
          break;
        JSONObject mp3 = entries.getJSONObject(i);
        
        T obj = getT(mp3);
        if (obj != null) {
          add(obj);
        }
      }
      return true;
    } catch (JSONException e) {
      return false;
    }

  }
  
  public boolean runSyn(final String url, final long expire) {
    JSONArray entries = Util.getJsonArrayFromUrl(url, expire);
    if (entries != null) {
      return runJsonArray(entries);
    } else {
    	onNoResult();
    }
    return false;
    
  }
    
  public void runAsyn(final String url, final long expire) {
    new AppendTask(expire).execute(url);
  }

  public class AppendTask extends AsyncTask<String, Void, JSONArray> {
    Long expire;
    AppendTask(long e) {
      expire = e;
    }
    @Override
    protected JSONArray doInBackground(String... url) {
      return Util.getJsonArrayFromUrl(url[0], expire);
    }
    
    @Override
    protected void onPostExecute(JSONArray result) {
      if (result != null) {
        runJsonArray(result);
      }else {
        onNoResult();
      }
    }
  }

 }
