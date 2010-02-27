package com.feebe.lib;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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


public abstract class UrlArrayAdapter<T, W> extends BaseAdapter
    implements Filterable {
  private List<T> mObjects;

  /**
   * The resource indicating what views to inflate to display the content of this
   * array adapter.
   */
  protected int mResource;

  protected Context mContext;    

  private List<T> mOriginalValues;
  private ArrayFilter mFilter;

  protected LayoutInflater mInflater;

  /**
   * Constructor
   *
   * @param context The current context.
   * @param resource The resource ID for a layout file containing 
   *        a layout to use when instantiating views.
   */
  public UrlArrayAdapter(Context context, int resource) {
      init(context, resource, new ArrayList<T>());
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
      init(context, resource, Arrays.asList(objects));
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
    init(context, resource, objects);
  }

  /**
   * Adds the specified object at the end of the array.
   *
   * @param object The object to add at the end of the array.
   */
  public void add(T object) {
    mOriginalValues.add(object);
  }
  
  public abstract T getT(JSONObject obj);

  public abstract W getWrapper(View v);
  
  public abstract void applyWrapper(T item, W wp, boolean newView);

  @SuppressWarnings("unchecked")
  @Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		W w;
		if (view == null) {
      view = mInflater.inflate(mResource, parent, false);
      w = getWrapper(view);
		} else {
			w = (W) view.getTag();
		}
    T item = getItem(position);
    applyWrapper(item, w, convertView == null);
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

  class AppendTask extends AsyncTask<String, Void, JSONArray> {
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
        notifyDataSetChanged();
      }else {
        onNoResult();
      }
    }
  }

  
  public void add(T[] objects) {
    mOriginalValues.addAll(Arrays.asList(objects));
  }
  /**
   * Inserts the specified object at the specified index in the array.
   *
   * @param object The object to insert into the array.
   * @param index The index at which the object must be inserted.
   */
  public void insert(T object, int index) {
    mOriginalValues.add(index, object);
  }

  /**
   * Removes the specified object from the array.
   *
   * @param object The object to remove.
   */
  public void remove(T object) {
    mOriginalValues.remove(object);
  }

  /**
   * Remove all elements from the list.
   */
  public void clear() {
    mOriginalValues.clear();
  }

  /**
   * Sorts the content of this adapter using the specified comparator.
   *
   * @param comparator The comparator used to sort the objects contained
   *        in this adapter.
   */
  public void sort(Comparator<? super T> comparator) {
    Collections.sort(mObjects, comparator);
  }


  private void init(
      Context context, int resource, List<T> objects) {
    mContext = context;
    mInflater = (LayoutInflater)context.getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
    mResource = resource;
    mObjects = mOriginalValues = objects;
  }

  /**
   * Returns the context associated with this array adapter. The context is used
   * to create views from the resource passed to the constructor.
   *
   * @return The Context associated with this adapter.
   */
  public Context getContext() {
    return mContext;
  }

  /**
   * {@inheritDoc}
   */
  public int getCount() {
    return mObjects.size();
  }

  /**
   * {@inheritDoc}
   */
  public T getItem(int position) {
    return mObjects.get(position);
  }

  /**
   * Returns the position of the specified item in the array.
   *
   * @param item The item to retrieve the position of.
   *
   * @return The position of the specified item.
   */
  public int getPosition(T item) {
    return mObjects.indexOf(item);
  }

  /**
   * {@inheritDoc}
   */
  public long getItemId(int position) {
    return position;
  }

  /**
   * {@inheritDoc}
   */
  public Filter getFilter() {
    if (mFilter == null) {
      mFilter = new ArrayFilter();
    }
    return mFilter;
  }

  /**
   * <p>An array filter constrains the content of the array adapter with
   * a prefix. Each item that does not start with the supplied prefix
   * is removed from the list.</p>
   */
  private class ArrayFilter extends Filter {
    @Override
    protected FilterResults performFiltering(CharSequence prefix) {
      FilterResults results = new FilterResults();
      if (prefix == null || prefix.length() == 0) {
        results.values = mOriginalValues;
        results.count = mOriginalValues.size();
      } else {
        String prefixString = prefix.toString().toLowerCase();

        final List<T> values = mOriginalValues;
        final int count = values.size();

        final ArrayList<T> newValues = new ArrayList<T>(count);

        for (int i = 0; i < count; i++) {
          final T value = values.get(i);
          final String valueText = value.toString().toLowerCase();

          // First match against the whole, non-splitted value
          if (valueText.startsWith(prefixString)) {
            newValues.add(value);
          } else {
            final String[] words = valueText.split(" ");
            final int wordCount = words.length;

            for (int k = 0; k < wordCount; k++) {
              if (words[k].startsWith(prefixString)) {
                newValues.add(value);
                break;
              }
            }
          }
        }

        results.values = newValues;
        results.count = newValues.size();
      }

      return results;
    }

    @Override
    protected void publishResults(
        CharSequence constraint, FilterResults results) {
      if (results.count == mObjects.size()) {
        return;
      }
      mObjects = (List<T>) results.values;
      
      if (results.count > 0) {
        notifyDataSetChanged();
      } else {
        notifyDataSetInvalidated();
      }
    }
  }
}
