package com.feebe.rings;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.feebe.lib.EndlessUrlArrayAdapter;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.webkit.WebIconDatabase.IconListener;
import android.widget.Toast;

public class Const extends com.feebe.lib.Const {
  public static final String USEDEDUP = "dedup";
  public static final String AUTH = "auth";

  public static final String key = "key";
  public static final String mp3 = "mp3";
  public static final String song = "song";
  public static final String artist = "artist";
  public static final String title = "title";
  public static final String image = "image";
  public static final String rating = "rating";
  public static final String category = "category";
  public static final String download = "download";
  public static final String size = "size";
  public static final String author = "author";
 // public static final String jsonfile_extention = ".d";
  
  public static final String jsonfile_key = "json";
  
  public static final String myRating = "myRating";
  // TODO: no need 
 // public static final String jsonLocation = "jsonLocation";
  
  public static final String searchurl = "url";

  public static final int DEFAULT_RESULT = 15;

  public static final String RatingBase = "http://ggapp.appspot.com/ringtone/rate/";
  public static final String SearchBase = "http://ggapp.appspot.com/ringtone/searchau/?json=1&uid=218&q=";
  public static final String CommentBase = "http://ggapp.appspot.com/ringtone/addcm/";

  public static final String TableHistory = "histories";
  
  public static void init(Activity c) {
	if (main != null) return;

    appname = "FeebeRings";
    no_sd = R.string.no_sd;
    com.feebe.lib.Const.init(c);
  }

  
  private static void addFileName(StringBuilder f, String str) {
	if (str == null) return;
    for (int i = 0; i < str.length(); i++) {
      char a = str.charAt(i);
      if (Character.isLetterOrDigit(a) || a == ' ') {
          f.append(a);
      }
    }
  }
  
  public static String getMp3FilePath(String artist, String title, String extension) {
    StringBuilder filebuf = new StringBuilder(256);
    filebuf.append(contentDir);

    addFileName(filebuf, artist);
    filebuf.append(' ');
    addFileName(filebuf, title);

    // Try to make the filename unique
    String path = null;
    String filename = filebuf.toString();
    // Log.e("file", filename);
    for (int i = 0; i < 100; i++) {
        String testPath;
        if (i > 0) {
            testPath = filename + i;
        } else {
            testPath = filename;
        }
        if (extension != null) {
          testPath += extension;
        }
        try {
            RandomAccessFile f = new RandomAccessFile(
                new File(testPath), "r");
        } catch (Exception e) {
            // Good, the file didn't exist
            path = testPath;
            break;
        }
    }
    return path;
  }


}
