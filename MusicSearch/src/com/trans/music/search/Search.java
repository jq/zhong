package com.trans.music.search;

import org.json.JSONArray;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.Log;

import com.feebe.lib.SearchProvider;
import com.feebe.lib.Util;

public class Search extends SearchProvider {

  public Search() {
    buildUriMatcher("musicsearch");
  }
  @Override
  protected Cursor getSuggestions(String query, MatrixCursor cursor) {
    String queryUrl = Const.SearchBase + "count=8&q="+query;
    String data = Util.download(queryUrl);
    
    /*
    if (entries != null) {
      for(int i = 0; i < entries.length(); i++)
      {
        Object[] row = columnValuesOfWord(entries, i);
        if (row != null)
          cursor.addRow(row);
      }      
    }
    */
    Log.e("getSuggestions", " " + cursor.getCount());
    return cursor;

  }

}
