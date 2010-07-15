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
import java.util.Hashtable;
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
	  Toast.makeText(Const.main, Const.no_result,Toast.LENGTH_SHORT).show();
  }
    
  private boolean runList(List entries) {
  	//combine the same
  	List newList = new ArrayList(entries.size());
  	Hashtable newListHt= new Hashtable();
  	ArrayList<String> artistTitleArray = new ArrayList<String>();
  	for(int i = 0; i < entries.size(); i++) {
  		JSONObject first = (JSONObject) (entries.get(i));
  		String firstTitle = null;
  		String firstArtist = null;
  		int firstRating = -1;
  		try {
  			if(first.has("song"))
  			  firstTitle = first.getString("song");
  			if(first.has("title"))
  			  firstTitle = first.getString("title");
  			if(first.has("artist"))
  			  firstArtist = first.getString("artist");
  			if(first.has("rating"))
  			  firstRating = Integer.parseInt(first.getString("rating"));
			} catch (JSONException e) {
			  e.printStackTrace();
			}
			artistTitleArray.add(i, firstArtist+"@"+firstTitle);
			if(newListHt.size() == 0) {
				newListHt.put(firstArtist+"@"+firstTitle, i+"@"+firstRating);
			}
			else {
				boolean in = false;
				if(newListHt.containsKey(firstArtist+"@"+firstTitle)) {
				  in = true;
				  String value = (String) newListHt.get(firstArtist+"@"+firstTitle);
				  if(firstRating > Integer.parseInt(value.substring(value.indexOf("@")+1))) {
				    //remove the one with low rating
				    newListHt.remove(firstArtist+"@"+firstTitle);
				    in = false;
				  }
				}
				if(!in)
				  newListHt.put(firstArtist+"@"+firstTitle, i+"@"+firstRating);
			}
  	}
  	//add elements in hash table to newList
  	for(int i = 0; i < entries.size(); i++) {
  	  String artistTitle = artistTitleArray.get(i);
  	  if(newListHt.containsKey(artistTitle)) {
  	    String value = (String) newListHt.get(artistTitle);
  	    if(i == Integer.parseInt(value.substring(0,value.indexOf("@")))) {
  	      newList.add(entries.get(i));
  	    }
  	  }
  	}
  	//list size no less than 10
  	if(newList.size() < 10) {
  		for(int i = 0; i < entries.size(); i++) {
  			JSONObject o = (JSONObject) (entries.get(i));
  			if(!newList.contains(o))
  				newList.add(o);
  			if(newList.size() > 9)
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
