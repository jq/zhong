package com.trans.music.search;

import org.json.JSONArray;

import android.database.Cursor;
import android.database.MatrixCursor;

import com.feebe.lib.SearchProvider;
import com.feebe.lib.Util;

public class Search extends SearchProvider {

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
    return cursor;

  }

}
