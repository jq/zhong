package com.feebe.RingTone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.database.MatrixCursor;

import com.feebe.lib.SearchProvider;
import com.feebe.lib.Util;

public class RingSearch extends SearchProvider {
  
  public RingSearch() {
    buildUriMatcher("rings");
  }
	private Object[] columnValuesOfWord(JSONArray array, int i) {
  	try{
  		JSONObject entry = array.getJSONObject(i);
      return new String[] {
      		String.valueOf(i),
              entry.getString("artist"),           // text1
              entry.getString("title"),     // text2
              entry.getString("artist")+" "+entry.getString("title"),           // intent_data (included when clicking on item)
      };
  	}
  	catch(JSONException e){
  		return null;
  	}
  }

  @Override
  protected Cursor getSuggestions(String query, MatrixCursor cursor) {
    
    String queryUrl = Const.SearchBase + "count=8&q="+query;
    JSONArray entries = Util.getJsonArrayFromUrl(queryUrl, Const.OneWeek);
    if (entries != null) {
      for(int i = 0; i < entries.length(); i++)
      {
        Object[] row = columnValuesOfWord(entries, i);
        if (row != null)
          cursor.addRow(row);
      }      
    }
    return cursor;
  }

}
