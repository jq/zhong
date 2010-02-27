package com.feebe.rings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.feebe.lib.SearchProvider;

public class RingSearch extends SearchProvider {
	protected Object[] columnValuesOfWord(JSONArray array, int i) {
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

}
