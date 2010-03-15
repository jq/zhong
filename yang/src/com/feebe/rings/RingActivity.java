package com.feebe.rings;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.feebe.lib.AdsView;
import com.feebe.lib.DownloadImg;
import com.feebe.lib.DownloadFile;
import com.feebe.lib.Util;
import com.feebe.lib.DownloadFile.DownloadListerner;
import com.ringdroid.RingdroidSelectActivity;

import entagged.audioformats.AudioFile;
import entagged.audioformats.AudioFileIO;
import entagged.audioformats.exceptions.CannotReadException;
import entagged.audioformats.exceptions.CannotWriteException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Browser;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RatingBar.OnRatingBarChangeListener;

public class RingActivity extends Activity implements DownloadFile.DownloadListerner{
  private final static String TAG = "RingActivity";

  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      // Log.e(TAG,"onCreate");

      requestWindowFeature(Window.FEATURE_NO_TITLE);
      setContentView(R.layout.ring);
      AdsView.createQWAd(this);

      ringActivityIntent = getIntent();			
      
      iconImageView = (ImageView) findViewById(R.id.row_icon);
      titleTextView = (TextView) findViewById(R.id.row_title);
      artistTextView = (TextView) findViewById(R.id.row_artist);
      ratingBar = (RatingBar) findViewById(R.id.row_small_ratingbar);
      largeRatingBar = (RatingBar) findViewById(R.id.ratingBar);
      
      detailInfo = (TextView) findViewById(R.id.info_text);
      listSearchOthers = (ListView) findViewById(R.id.list_searchOthers);
     

      dl = (Button)this.findViewById(R.id.download);
      mPreview = (Button)this.findViewById(R.id.preview);
      dl.setOnClickListener(dlClick);
      mPreview.setOnClickListener(previewClick);
      
      largeRatingBar.setIsIndicator(true);
      largeRatingBar.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
        @Override
        public void onRatingChanged(RatingBar largeRatingBar, float rating,
      		  boolean fromUser) {	  		
      		if(key!="" && jsonLocation != null) {
      			String ratingUrl = Const.RatingBase + key + "?score=" + rating*20;
      			try {
      				URL url = new URL(ratingUrl);
      				HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
      				urlConn.setConnectTimeout(4000);
      			    urlConn.connect();
      			    urlConn.disconnect();
      			    // Log.d(TAG, ratingUrl);
      			} catch (MalformedURLException e) {
      				e.printStackTrace();
      			} catch (IOException e) {
      				e.printStackTrace();
      			}
      			if (ring.has(myRating)) {
      				ring.remove(myRating);
      			}
            try {
      					ring.put(Const.myRating, rating);
      					Util.saveFile(ring.toString(), jsonLocation);
            } catch (JSONException e) {
      					// Log.e(TAG, "put myRating "+ rating);
      			}
      		} 			
      	}
      });
      String json = getIntent().getStringExtra(Const.searchurl);
      if (!json.startsWith("http")) {
        jsonLocation = json;
      }
      ring = Search.getRingJson(json);
      if (ring == null) {
        // TODO: tell user this file is broken
        finish();
        return;
      }

      try {
  	  	category = ring.getString(Const.category);
  	  	download = ring.getString(Const.download);
  	  	author = ring.getString(Const.author);
  	  	artist = ring.getString(Const.artist);
  	  	rating = ring.getString(Const.rating);
  	  	title = ring.getString(Const.title);
  	  	key = ring.getString(Const.key);
        mp3Location = ring.getString(Const.mp3);
        mp3Size = ring.getInt(Const.size);
        titleTextView.setText(title);
        artistTextView.setText(artist);
        detailInfo.append(download + this.getString(R.string.info_download_count));
//        detailInfo.append("Added by: " + author + "\n");
//        detailInfo.append("Category: " + category);
        
        if(rating.length()>0) {
	        int ratingNum = Integer.parseInt(rating);
		  	if (ratingNum < 60){
		  		ratingBar.setRating(1);
		  	}else if (ratingNum < 70){
		  		ratingBar.setRating(2);
		  	}else if (ratingNum < 80){
		  		ratingBar.setRating(3);
		  	}else if (ratingNum < 90){
		  		ratingBar.setRating(4);
		  	}else
		  		ratingBar.setRating(5);  
        }
        String imgUrl = ring.getString(Const.image);
        if (imgUrl != null && imgUrl.length() > 0)
          new DownloadImg(iconImageView).execute(imgUrl);
      } catch (JSONException e) {
        // Log.e(TAG, e.getMessage());
        return;
      }
      
      if(ring.has(Const.myRating)){
    	  try {
			myRating = ring.getString(Const.myRating);
			largeRatingBar.setRating(Float.parseFloat(myRating));
		} catch (JSONException e) {
			e.printStackTrace();
		}
      }
      
      ArrayList<HashMap<String, String>> ringlist = new ArrayList<HashMap<String, String>>(); 
      HashMap<String, String> map1 = new HashMap<String, String>();
      HashMap<String, String> map2 = new HashMap<String, String>();
      HashMap<String, String> map3 = new HashMap<String, String>();
      HashMap<String, String> map4 = new HashMap<String, String>();
      map1.put("ItemTitle", this.getString(R.string.search_more) + " " + artist);
      map2.put("ItemTitle", this.getString(R.string.search_more_by) + " " +  author);
      map3.put("ItemTitle", this.getString(R.string.search_more_in) +  " " + category);
      map4.put("ItemTitle",  this.getString(R.string.search_more) +  " " + title);

      ringlist.add(map1);
      ringlist.add(map2);
      ringlist.add(map3);
      ringlist.add(map4);
      SimpleAdapter mSearchOthers = new SimpleAdapter(this, 
                                                  ringlist,   
                                                  R.layout.ring_list_item,       
                                                  new String[] {"ItemTitle"},                                             
                                                  new int[] {R.id.ringListItem1});   
      listSearchOthers.setAdapter(mSearchOthers);
      listSearchOthers.setOnItemClickListener(new OnItemClickListener() {
		
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position,
						long id) {
					// TODO Auto-generated method stub
					switch(position){
					case 0:	Search.getArtistRing(artist);
					  return;
					case 1:	Search.getAuthorRing(author);
					  return;
					case 2: Search.getCate(category);
				    return;
					case 3: Search.getTitleRing(title);
			    return;
					}
				}

      });
      
      queue = (Button)this.findViewById(R.id.queue);

      if (!mp3Location.startsWith("http:")) {
        initFinishDownloadButton();
      } else {
      	queue.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
          	// save json file
            Toast.makeText(
                RingActivity.this, R.string.queue, Toast.LENGTH_SHORT).show();
            download(null);
            finish();
            //Const.downloadDb.insert(values);
            // TODO: start new intent to launch download queue view
            // pass json file to the download view, so that when download finish, it will get it
          }  
        });
      }
  }
  
  private static final int RING_PICKER = 1;

  private void initFinishDownloadButton() {
  	queue.setVisibility(View.GONE);
    dl.setText(R.string.play);
    mSet = (Button)this.findViewById(R.id.set);
    mSet.setVisibility(View.VISIBLE);
    mSet.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        showDialog(RING_PICKER);
      }  
    });
    
    largeRatingBar.setIsIndicator(false);
    
    mAssign = (Button)this.findViewById(R.id.assign);
    mAssign.setVisibility(View.VISIBLE);
    mAssign.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent();
        try {
        	
            if (mCurrentFileUri == null) {
    	          mCurrentFileUri = Uri.parse(ring.getString(Const.mp3));
            }
            Log.e("u", " uri " + mCurrentFileUri.toString() + " mp3 " + ring.getString(Const.mp3));
	          intent.setData(mCurrentFileUri);
	          intent.setClass(RingActivity.this, com.ringdroid.ChooseContactActivity.class);
	          RingActivity.this.startActivity(intent);
          } catch (JSONException e) {
            // Log.e("assign", e.getMessage());
          }
        }
    });
    
    mEdit = (Button) findViewById(R.id.edit);
    mEdit.setVisibility(View.VISIBLE);
    mEdit.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				// TODO edit ringtone
				try {
					String filePath = ring.getString("filePath");
					Intent intent = new Intent(Intent.ACTION_EDIT, Uri.parse(filePath));
					intent.putExtra("was_get_content_intent",false);
					intent.setClassName("com.feebe.rings","com.ringdroid.RingdroidEditActivity");
					startActivity(intent);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
			}
		});
    
    mShare = (Button) findViewById(R.id.share);
    mShare.setVisibility(View.VISIBLE);
    mShare.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				new AlertDialog.Builder(RingActivity.this)
        .setTitle(R.string.alertdialog_share)
        .setItems(R.array.select_share_methods, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch(which) {
                case 0:
                	Intent sms = new Intent(Intent.ACTION_VIEW);
                	sms.putExtra("sms_body",
                	    RingActivity.this.getString(R.string.share_sms1) + " " +
                	    title+ " "  + RingActivity.this.getString(R.string.share_sms2)+ "\n" +
                	    key.substring(0, key.length() - 7)+ "\n" +
                	    RingActivity.this.getString(R.string.share_sms3)
                	    ); 
                	sms.setType("vnd.android-dir/mms-sms");
                	startActivity(sms);
                	break;
                case 1:
                	Intent mEmailIntent = new Intent(android.content.Intent.ACTION_SEND);
                	mEmailIntent.setType("plain/text");
                	mEmailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, 
                	    title);
                	mEmailIntent.putExtra(android.content.Intent.EXTRA_TEXT, 
                	    RingActivity.this.getString(R.string.share_sms1) + " " + title + " " + 
                	    RingActivity.this.getString(R.string.share_sms2) + "\n" + 
                	    key.substring(0, key.length() - 7) + "\n" +
                      RingActivity.this.getString(R.string.share_sms3));
                	startActivity(Intent.createChooser(mEmailIntent, artist));
                	break;
                case 2:
                	Browser.sendString(RingActivity.this, RingActivity.this.getString(R.string.share_sms1) + title + RingActivity.this.getString(R.string.share_sms2) + artist);
                }
            }
        }).create().show();
				
			}
		});
    
    mPreview.setVisibility(View.GONE);
    
    layoutMyReview = (LinearLayout) findViewById(R.id.layoutMyReview);
    layoutMyReview.setVisibility(View.VISIBLE);
  }
  
  OnClickListener dlClick = new OnClickListener() {
    @Override
    public void onClick(View v) {
    	if(mPlayer.isPlaying())	{
    		mPlayer.pause();
    		isPaused = true;
    		dl.setText(R.string.play);
    		return;
    	}
      if (mp3Location.startsWith("http:")) {
      	dlProgress = new ProgressDialog(RingActivity.this);
      	dlProgress.setTitle(R.string.dlprogress_title);
      	dlProgress.setMessage(RingActivity.this.getString(R.string.dlprogress_message));
      	dlProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      	dlProgress.setIndeterminate(false);
      	dlProgress.setMax(100);
      	dlProgress.setProgress(0);
      	dlProgress.setCancelable(false);
      	dlProgress.show();
      	
      	download(RingActivity.this);
        saveArtist();
      } else if(isPaused) {
      	isPaused = false;
      	dl.setText(R.string.pause);
      	mPlayer.start();
      } else{
        // TODO: play
      	try {
					filePath = ring.getString("filePath");
				} catch (JSONException e1) {
					// Log.e(TAG, "error read mp3 file");
				}
      	try {
					mPlayer.setDataSource(filePath);
					mPlayer.prepare();
					mPlayer.start();
					mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {					
						@Override
						public void onCompletion(MediaPlayer mp) {
							isPaused = false;
							dl.setText(R.string.play);
							mPlayer.reset();
						}
					});
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				dl.setText(R.string.pause);
				
        // Log.e("mp3", mp3Location);
      }
    }
  };
  ProgressDialog mStreaming;
  OnClickListener previewClick = new OnClickListener() {	
		@Override
		public void onClick(View v) {
			// TODO play online
			if(mp3Location.startsWith("http:")) {
			  if (mStreaming == null) {
			    mStreaming  = new ProgressDialog(RingActivity.this);
  			  mStreaming.setTitle(R.string.mStreaming_title);
  				mStreaming.setMessage(RingActivity.this.getString(R.string.mStreaming_message));
  				mStreaming.setIndeterminate(true);
  				mStreaming.setCancelable(true);
  				mStreaming.setButton(RingActivity.this.getString(R.string.stop), new DialogInterface.OnClickListener() {			
  					@Override
  					public void onClick(DialogInterface dialog, int which) {
  						mediaPreview.stop();
  					}
  				});
			  }
				mStreaming.show();
				if (mediaPreview == null) {
				  mediaPreview = new MediaPlayer();
				}
				
				new Thread(new Runnable() {

          @Override
          public void run() {
            try {
              mediaPreview.setDataSource(mp3Location);
              mediaPreview.prepare();
              mediaPreview.start();
              mediaPreview.setOnCompletionListener(new OnCompletionListener () {
                @Override
                public void onCompletion(MediaPlayer mp) {
                  mStreaming.dismiss();
                }
              });
            } catch (IllegalArgumentException e) {
            } catch (IllegalStateException e) {
            } catch (IOException e) {
            }       
            
          }
				  
				}).start();
				
			}		
		}
	};
  
  private void saveArtist() {
  	if (this.artist.length() > 0) {
      // TODO: save artist 
      SharedPreferences s = getSharedPreferences(Const.artist, 0);
      Editor e = s.edit();
  		e.putBoolean(artist, true);
  		e.commit();
  	}
  }
  
  private int ring_button_type;
//  private String[] ringTypes = {"Ringtone","Notification","Alarm"};
  
  @Override
  protected Dialog onCreateDialog(int id) {
    if (id == RING_PICKER) {
      return new AlertDialog.Builder(this)
      .setIcon(android.R.drawable.ic_dialog_alert)
      .setTitle(R.string.ring_picker_title)
      .setSingleChoiceItems(R.array.ring_types, 0, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            ring_button_type = whichButton;
          }
      })
      .setPositiveButton(R.string.alertdialog_ok, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            int ring_type;
            if (ring_button_type == 0) {
              ring_type = RingtoneManager.TYPE_RINGTONE;
            } else if (ring_button_type == 1) {
              ring_type = RingtoneManager.TYPE_NOTIFICATION;
            } else {
              ring_type = RingtoneManager.TYPE_ALARM;
            }
            String u;
            try {
              //u = Const.mp3dir + ring.getString(Const.mp3);
              if (mCurrentFileUri == null) {
                mCurrentFileUri = Uri.parse(ring.getString(Const.mp3));
              }
              RingtoneManager.setActualDefaultRingtoneUri(RingActivity.this, ring_type, mCurrentFileUri);
            } catch (JSONException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }
      })
      .setNegativeButton(R.string.alertdialog_cancel, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
          }
      })
     .create();           

    }
    return null;
  }
  
  private static int[] fileKinds = 
    new int[]{Const.FILE_KIND_RINGTONE,Const.FILE_KIND_NOTIFICATION,Const.FILE_KIND_ALARM};

  private void download(DownloadListerner listerner) {
    int lastPos = mp3Location.lastIndexOf('.');
    String extension = mp3Location.substring(lastPos);
    // Log.e("path", fullpathame);

    DownloadFile df = new RingDownloadFile(
        listerner, 512,mp3Size, category, artist, title, this.getContentResolver(), fileKinds, ring);
    df.execute(mp3Location, Const.getMp3FilePath(artist, title, extension));
  }

  @Override
  public void onDownloadProgress(int percentage) {
  	dlProgress.setProgress(percentage);
  }
  
  
  @Override
  public void onDownloadFail() {
	int icon = R.drawable.ring;
	String tickerText = "\"" + title + "\"" + getString(R.string.notification_text_failed);
	long when = System.currentTimeMillis();
	Notification notification = new Notification(icon, tickerText, when);
	Context context = getApplicationContext();
	String expandedText =  "\"" + title + "\"" + getString(R.string.notification_text_failed);
	String expandedTitle = getString(R.string.notification_title);
	//Intent intent = new Intent(RingActivity.this, RingdroidSelectActivity.class);
	PendingIntent launchIntent = PendingIntent.getActivity(context, 0, ringActivityIntent, 0);
    notification.setLatestEventInfo(context, expandedTitle, expandedText, launchIntent);
    notification.flags |= Notification.FLAG_AUTO_CANCEL;
    NotificationManager notificationManager;
    notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
      
    int notificationRef = 1;
    notificationManager.notify(notificationRef, notification);
    
    dlProgress.dismiss();
    Toast.makeText(
        RingActivity.this, R.string.download_failed, Toast.LENGTH_SHORT).show();
    return;
  }

  
  @Override
  public void onDownloadFinish(File file, Uri u) {
    //Log.e("onDownloadFinish", file.getAbsolutePath());
    mp3Location = file.getAbsolutePath();
    mCurrentFileUri = u;
    jsonLocation = Const.jsondir + file.getName();
    // TODO: reload download ring page 
    RingActivity.this.runOnUiThread(new Runnable() {
      @Override
      public void run() {
    	  int icon = R.drawable.ring;
    	  String tickerText = "\"" + title + "\"" + getString(R.string.notification_text_finish);
    	  long when = System.currentTimeMillis();
    	  Notification notification = new Notification(icon, tickerText, when);
    	  Context context = getApplicationContext();
    	  String expandedText =  "\"" + title + "\"" + getString(R.string.notification_text_finish);
    	  String expandedTitle = getString(R.string.notification_title);
    	  Intent intent = new Intent(RingActivity.this, RingdroidSelectActivity.class);
    	  PendingIntent launchIntent = PendingIntent.getActivity(context, 0, intent, 0);
          notification.setLatestEventInfo(context, expandedTitle, expandedText, launchIntent);
          notification.flags |= Notification.FLAG_AUTO_CANCEL;
          NotificationManager notificationManager;
          notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
          
          int notificationRef = 1;
          notificationManager.notify(notificationRef, notification);
          
    	  initFinishDownloadButton();
          dlProgress.dismiss();
      }
    });
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    // TODO: cancel download 
  }
  
  private Uri mCurrentFileUri;
  private JSONObject ring;
  private String mp3Location;
  private String jsonLocation;

  private Button mSet;
  private Button mAssign;
  private Button dl;
  private Button queue;
  private Button mEdit;
  private Button mShare;
  private Button mPreview;
  private ImageView iconImageView;
  private TextView titleTextView;
  private TextView artistTextView;
  private TextView detailInfo;
  private ListView listSearchOthers;
  private RatingBar ratingBar;
  private RatingBar largeRatingBar;
  private LinearLayout layoutMyReview; 
  private ProgressDialog dlProgress;
  
  MediaPlayer mPlayer = new MediaPlayer();;
  MediaPlayer mediaPreview;
  
  boolean isPaused = false;
  
  String category = "";
  String download = "";
  String artist = "";
  String author = "";
  String size = "";
  String rating = "";
  String title = "";
  String key = "";
  String myRating = "";
  String filePath = "";
  int mp3Size;
  
  private Intent ringActivityIntent;		
}
