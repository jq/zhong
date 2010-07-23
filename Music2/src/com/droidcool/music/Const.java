package com.droidcool.music;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

public class Const {
	// MediaStore.UNKNOWN_STRING
  public static final String UNKNOWN_STRING = "<unknown>";
  // MediaStore.Audio.Media.IS_PODCAST
  public static final String IS_PODCAST = "is_podcast";
  public static final String BOOKMARK = "bookmark";

  public static final int sdk = Integer.valueOf(Build.VERSION.SDK);
  
  public static final boolean moveItem(ContentResolver res,
          long playlistId, int from, int to) {
      Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external",
              playlistId)
              .buildUpon()
              .appendEncodedPath(String.valueOf(from))
              .appendQueryParameter("move", "true")
              .build();
      ContentValues values = new ContentValues();
      values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, to);
      return res.update(uri, values, null, null) != 0;
  }

}
