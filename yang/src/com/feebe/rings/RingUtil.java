package com.feebe.rings;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.feebe.lib.Util;

public class RingUtil {
  // TODO since it is all run on UI thread, we can use a buffer List
  public static List getJsonArrayFromUrl(String url, long expire) {
    JSONArray entries = Util.getJsonArrayFromUrl(url, expire);
    if (entries != null && entries.length() > 0) {
      int len = entries.length();
      List list = new ArrayList(len);
      for (int i = 0; i < len; i++) {
        try {
          JSONObject o = entries.getJSONObject(i);
          if (o != null) list.add(o);
        } catch (JSONException e) {
        }
      }
      return list;
    }
    return null;
  }
}
