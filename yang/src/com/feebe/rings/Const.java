package com.feebe.rings;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.feebe.lib.EndlessUrlArrayAdapter;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

public class Const extends com.feebe.lib.Const {

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

  public static final int NO_FILE_KIND = -1;
  public static final int FILE_KIND_MUSIC = 0;
  public static final int FILE_KIND_ALARM = 1;
  public static final int FILE_KIND_NOTIFICATION = 2;
  public static final int FILE_KIND_RINGTONE = 3;

  public static final int DEFAULT_RESULT = 15;

  public static final String SearchQuery = "http://ggapp.appspot.com/ringtone/search/?json=1&q=";
  public static final String RatingBase = "http://ggapp.appspot.com/ringtone/rate/";
  public static final String SearchBase = "http://ggapp.appspot.com/ringtone/search/?json=1&";

  public static void init(Context c) {
    pkg = "com.feebe.rings";
    com.feebe.lib.Const.init(c);
  	QWName = "Ringtone-g56rajjb";
  	QWID = "34d153f75db441cdbb776ffb70c569c5";
  	AdsViewID = R.id.AdsView;
  	LAYOUT_LIST = R.layout.list;
    tab_image = R.id.tab_image;
    tabheader = R.layout.tabheader;
  	tab_label = R.id.tab_label;
  	no_result = R.string.no_result;
  	no_sd = R.string.no_sd;
  	EndlessUrlArrayAdapter.ThrobberViewRes = R.layout.pending_view;
  	EndlessUrlArrayAdapter.Throbber = R.id.throbber;
  	
  	com.feebe.lib.download.DownloadNotification.status_bar_ongoing_event_progress_bar = R.layout.status_bar_ongoing_event_progress_bar;
  }

  
  private static void addFileName(StringBuilder f, String str) {
    for (int i = 0; i < str.length(); i++) {
      char a = str.charAt(i);
      if (Character.isLetterOrDigit(a) || a == ' ') {
          f.append(a);
      }
    }
  }
  
  public static String getMp3FilePath(String artist, String title, String extension) {
    // Turn the title into a filename
    StringBuilder filebuf = new StringBuilder(artist.length()+ title.length() + contentDir.length() + 1);
    filebuf.append(contentDir);
    addFileName(filebuf, artist);
    filebuf.append(' ');
    addFileName(filebuf, title);

    // Try to make the filename unique
    String path = null;
    String filename = filebuf.toString();
    for (int i = 0; i < 100; i++) {
        String testPath;
        if (i > 0) {
            testPath = filename + i + extension;
        } else {
            testPath = filename + extension;
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
