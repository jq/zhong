package com.trans.music.search;

import java.util.List;

import org.json.JSONArray;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.Log;


public class Search extends SearchProvider {

  public Search() {
    buildUriMatcher("feebemusicsearch");
  }
  @Override
  protected Cursor getSuggestions(String query, MatrixCursor cursor) {
   // String queryUrl = Const.SearchBase + "count=8&q="+query;
   // String data = Util.download(queryUrl);
    List<MP3Info> entries = MusicUtil.getSogoMp3(MusicUtil.getSogouLinks(query), 6);
    if (entries != null) {
      for(int i = 0; i < entries.size(); i++)
      {
        
        MP3Info entry = entries.get(i);
        Object[] row = new String[] {
            String.valueOf(i),
            entry.getArtist(),           // text1
            entry.getName(),     // text2
            entry.getArtist()+" "+entry.getName(),           // intent_data (included when clicking on item)
        };
        cursor.addRow(row);
      }
    }
    Log.e("getSuggestions", " " + cursor.getCount());
    return cursor;

  }

}
