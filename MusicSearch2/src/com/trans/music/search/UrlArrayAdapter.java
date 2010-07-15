package com.trans.music.search;

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
import java.util.Hashtable;
import java.util.Iterator;
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
	  Toast.makeText(Const.main, R.string.no_result,Toast.LENGTH_SHORT).show();
  }
    
  private boolean runList(List entries) {
    //combine
    List newList = new ArrayList(entries.size());
    Hashtable newListHt= new Hashtable();
    for(int i = 0; i < entries.size(); i++) {
      MP3Info mp3 = (MP3Info) entries.get(i);
      String title = mp3.getName();
      String artist = mp3.getArtist();
      int speed = Integer.parseInt(mp3.getSpeed());
      if(newListHt.size() == 0)
        newListHt.put(artist+title, mp3);
      else {
        boolean in = false;
        if(newListHt.containsKey(artist+title)) {
          in = true;
          MP3Info htMp3 = (MP3Info) newListHt.get(artist+title);
          int htSpeed = Integer.parseInt(htMp3.getSpeed());
          if(speed > htSpeed) {
            newListHt.remove(artist+title);
            in = false;
          }
        }
        if(!in)
          newListHt.put(artist+title, mp3);
      }
    }
    //add elements in hash table to newList
    Iterator<MP3Info> it = newListHt.values().iterator();
    while(it.hasNext()) {
      newList.add(it.next());
    }
    //list size no less than 10
    final int MINSIZE = 10;
    if(newList.size()<MINSIZE) {
      for(int i = 0; i < entries.size(); i++) {
        MP3Info mp3 = (MP3Info) entries.get(i);
        if(!newList.contains(mp3))
          newList.add(mp3);
        if(newList.size() > MINSIZE-1)
          break;
      }
    }
      
    int len = newList.size();
    if (len == 0) {
      return false;
    }
    for(int i = 0; i < len; i++){
      T obj = getT(newList.get(i));
      if (obj != null) {
        add(obj);
      }
    }
    return true;
  }
  
  protected abstract List getListFromUrl(final String url, final long expire);
  protected abstract T getT(Object obj);
  
  public boolean runSyn(final String url, final long expire) {
    List list = getListFromUrl(url, expire);
    if (list != null) {
      return runList(list);
    } else {
    	onNoResult();
    }
    return false;
    
  }
    
  public void runAsyn(final String url, final long expire) {
    new AppendTask(expire).execute(url);
  }

  @SuppressWarnings("unchecked")
  public class AppendTask extends AsyncTask<String, Void, List> {
    Long expire;
    AppendTask(long e) {
      expire = e;
    }
    @Override
    protected List doInBackground(String... url) {
      return getListFromUrl(url[0], expire);
    }
    
    @Override
    protected void onPostExecute(List result) {
      onPost(result);
    }
    
    protected void onPost(List result) {
      if (result != null) {
        runList(result);
      }else {
        onNoResult();
      }
    }
    
  }

 }
