package com.cinla.ringtone;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.cinla.imageloader.ImageLoader;
import com.connect.facebook.Login;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.ringdroid.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MusicPageActivity extends Activity {

	public static final int DLG_RING_PICKER = 0;
	
	private static PreviewMusic sPreviewMusic;
	
	private MusicInfo mMusicInfo;
	private DownloadMusicTask mDownloadTask;
	
	private Button mPreviewButton;
	private Button mDownloadButton;
	private Button mSetButton;
	private Button mAssignButton;
	private Button mShareButton;
	private Button mEditButton;
	private ImageButton mFacebookButton;
	private RatingBar mRatingBar;
	
	private Facebook mFacebook;
	private AsyncFacebookRunner mAsyncRunner;
	
	private ListView mSearchMoreList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.music_page);
		AdListener.createAds(this);
		
		mMusicInfo = (MusicInfo) getIntent().getSerializableExtra(Constant.MUSIC_INFO);
		
		ImageView imageView = (ImageView) findViewById(R.id.image);
		
		ImageLoader.initialize(MusicPageActivity.this);
		ImageLoader.start(mMusicInfo.getmImageUrl(), imageView);
		
		((TextView)findViewById(R.id.title)).setText(mMusicInfo.getmTitle());
		((TextView)findViewById(R.id.artist)).setText(mMusicInfo.getmArtist());
		((TextView)findViewById(R.id.download_count)).setText(getString(R.string.download_count)+" "+Integer.toString(mMusicInfo.getmDownloadCount()));
		((RatingBar)findViewById(R.id.avg_ratingbar)).setRating((int)mMusicInfo.getmRate()/20);
		
		mSearchMoreList = (ListView)findViewById(R.id.search_more_list);
		fillSearchMoreList();
		
		mPreviewButton = ((Button)findViewById(R.id.preview_button));
		mPreviewButton.setOnClickListener(new PreviewClickListener());
		
		mDownloadButton = ((Button)findViewById(R.id.download_button));
		mDownloadButton.setOnClickListener(new DownloadClickListener());
		
		mSetButton = ((Button)findViewById(R.id.set_button));
		mSetButton.setOnClickListener(new SetClickListener());
		
		mShareButton = ((Button)findViewById(R.id.share_button));
		mShareButton.setOnClickListener(new ShareClickListener());
		
		mAssignButton = (Button)findViewById(R.id.assign_button);
		mAssignButton.setOnClickListener(new AssignClickListener());
		
		mEditButton = (Button)findViewById(R.id.edit_button);
		mEditButton.setOnClickListener(new EditClickListener());
		
		mRatingBar = (RatingBar)findViewById(R.id.user_ratingbar);
		mRatingBar.setOnRatingBarChangeListener(new UserRatingBarChangeListener());
		
		mFacebookButton = (ImageButton) findViewById(R.id.shear_facebook_button);
		mFacebookButton.setOnClickListener(new OnClickListener() {
		      @Override
		      public void onClick(View v) {
		        mFacebook = new Facebook();
		        mAsyncRunner = new AsyncFacebookRunner(mFacebook);
		        com.connect.facebook.SessionStore.restore(mFacebook, MusicPageActivity.this);
		        if (!mFacebook.isSessionValid()) {
		          final String[] PERMISSIONS = new String[] {"publish_stream", "read_stream", "offline_access"};                   
		          Login loginFacbook = new Login(mFacebook, PERMISSIONS, MusicPageActivity.this);
		          loginFacbook.LoginFacebook();
		        } else {
		        new AlertDialog.Builder(MusicPageActivity.this)
		          .setMessage("Post ringtone to your Facebook?")
		          .setCancelable(false)
		          .setPositiveButton("Yes", new DialogInterface.OnClickListener() {         
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		              Bundle params = new Bundle();
		              params.putString("method", "links.post");
		              params.putString("comment", "share a ringtone app for Android with you:");
		              //String realKey = key.substring(key.lastIndexOf("/")+1, key.indexOf("?"));
		              params.putString("url", "http://www.android.com/market/#app=" + "com.cinla.ringtone");
		              //params.putString("link", "http://ringtonepromote.appspot.com/?key=" + realKey);
		              mAsyncRunner.request(null, params, "POST", new WallPostRequestListener());
		            }
		          })
		          .setNeutralButton("No", new DialogInterface.OnClickListener() {
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		              dialog.dismiss();
		            }
		          }).show();
		        
		        }
		          
		      }
		    });
		
		updateUI();
	}

	private void fillSearchMoreList() {
		ArrayList<HashMap<String, String>> moreListItems = new ArrayList<HashMap<String,String>>();
		HashMap<String, String> map1 = new HashMap<String, String>();
		HashMap<String, String> map2 = new HashMap<String, String>();
		map1.put(Constant.ITEM_TITLE, getString(R.string.search_more)+" "+mMusicInfo.getmArtist());
		map2.put(Constant.ITEM_TITLE, getString(R.string.search_more_in)+" "+mMusicInfo.getmCategory());
		moreListItems.add(map1);
		moreListItems.add(map2);
		SimpleAdapter simpleAdapter = new SimpleAdapter(this, moreListItems, R.layout.search_more_list_item, new String[] {Constant.ITEM_TITLE}, new int[] {R.id.search_more_item});
		mSearchMoreList.setAdapter(simpleAdapter);
		simpleAdapter.notifyDataSetChanged();
		mSearchMoreList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				switch (arg2) {
				case 0:
					SearchListActivity.startQueryByArtist(MusicPageActivity.this, mMusicInfo.getmArtist());
					break;
				case 1:
					SearchListActivity.startQueryByCategory(MusicPageActivity.this, mMusicInfo.getmCategory());
					break;
				default:
					break;
				}
			}
		});
	}
	
	public static void startMusicPageActivity(Activity activity, MusicInfo musicInfo) {
		Intent intent = new Intent(activity, MusicPageActivity.class);
		intent.putExtra(Constant.MUSIC_INFO, musicInfo);
		activity.startActivity(intent);
	}
	
	private class DownloadClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mDownloadTask == null) {
				mDownloadTask = new DownloadMusicTask(MusicPageActivity.this, mMusicInfo);
				mDownloadTask.execute();
			} else {
				mDownloadTask.showProgressDialog();
			}
		}
	}
	
	private class PreviewClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (sPreviewMusic != null) {
				sPreviewMusic.stopPlay();
				sPreviewMusic = null;
				sPreviewMusic = new PreviewMusic(MusicPageActivity.this, mMusicInfo, mMusicInfo.getmDownloadedPath());
			} else {
				if (sPreviewMusic==null) sPreviewMusic = new PreviewMusic(MusicPageActivity.this, mMusicInfo, mMusicInfo.getmDownloadedPath());
			}
			Utils.D("Downloaded path: "+mMusicInfo.getmDownloadedPath());
			sPreviewMusic.startPlay();
		}
	}
	
	private class SetClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			showDialog(DLG_RING_PICKER);
		}
	}
	
	private class ShareClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			new AlertDialog.Builder(MusicPageActivity.this)
	          .setTitle(R.string.share)
	          .setItems(R.array.select_share_methods, new DialogInterface.OnClickListener() {
	              public void onClick(DialogInterface dialog, int which) {
	                  switch(which) {
	                  case 0:
	                    Intent sms = new Intent(Intent.ACTION_VIEW);
	                    sms.putExtra("sms_body",
	                        MusicPageActivity.this.getString(R.string.share_sms1) + " " +
	                        mMusicInfo.getmTitle()+ " "  + MusicPageActivity.this.getString(R.string.share_sms2)+ "\n" +
	                        mMusicInfo.getmMp3Url()+ "\n" +
	                        MusicPageActivity.this.getString(R.string.share_sms3)
	                        ); 
	                    sms.setType("vnd.android-dir/mms-sms");
	                    startActivity(sms);
	                    break;
	                  case 1:
	                    Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
	                    emailIntent.setType("plain/text");
	                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, 
	                    	mMusicInfo.getmTitle());
	                    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, 
	                        MusicPageActivity.this.getString(R.string.share_sms1) + " " + mMusicInfo.getmTitle() + " " + 
	                        MusicPageActivity.this.getString(R.string.share_sms2) + "\n" + 
	                        mMusicInfo.getmMp3Url() + "\n" +
	                        MusicPageActivity.this.getString(R.string.share_sms3));
	                    startActivity(Intent.createChooser(emailIntent, mMusicInfo.getmArtist()));
	                    break;
	                  case 2:
	                    Browser.sendString(MusicPageActivity.this, MusicPageActivity.this.getString(R.string.share_sms1) + mMusicInfo.getmTitle() + MusicPageActivity.this.getString(R.string.share_sms2) + mMusicInfo.getmArtist());
	                  }
	              }
	          }).create().show();
		}
	}
	
	private class AssignClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(MusicPageActivity.this, com.ringdroid.ChooseContactActivity.class);
			intent.setData(Uri.parse(mMusicInfo.getmDownloadedUri()));
			startActivity(intent);
		}
	}
	
	private class EditClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			String filePath = mMusicInfo.getmDownloadedPath();
			Intent intent = new Intent(Intent.ACTION_EDIT, Uri.parse(filePath));
			intent.putExtra("was_get_content_intent",false);
			intent.setClassName(MusicPageActivity.this, "com.ringdroidlib.RingEditorActivity");
			startActivity(intent);
		}
	}
	
	private class UserRatingBarChangeListener implements OnRatingBarChangeListener {
		@Override
		public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
			Utils.D("rate success?: "+Utils.rateSong(mMusicInfo.getmUuid(), rating));
		}
	}
	
	public void onDownloadFinish(File downloadedFile, Uri downloadedUri) {
		if (mDownloadTask != null) {
			mDownloadTask.cancel(true);
			mDownloadTask = null;
		}
		if (downloadedFile != null) {
			mMusicInfo.setmDownloadedPath(downloadedFile.getAbsolutePath());
			mMusicInfo.setmDownloadedUri(downloadedUri.toString());
			mDownloadButton.setText(this.getString(R.string.edit));
		} else {
			
		}
		updateUI();
	}
	
	public void onHideProgressDialog() {
		mDownloadButton.setText(R.string.show);
	}
	
	private int ring_button_type;
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DLG_RING_PICKER:
			return new AlertDialog.Builder(this)
		      .setIcon(android.R.drawable.ic_dialog_alert)
		      .setTitle(R.string.ring_picker_title)
		      .setSingleChoiceItems(R.array.ring_types, 0, new DialogInterface.OnClickListener() {
		          public void onClick(DialogInterface dialog, int whichButton) {
		            ring_button_type = whichButton;
		          }
		      })
		      .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
		          public void onClick(DialogInterface dialog, int whichButton) {
		            final int ring_type;
		            if (ring_button_type == 0) {
		              ring_type = RingtoneManager.TYPE_RINGTONE;
		            } else if (ring_button_type == 1) {
		              ring_type = RingtoneManager.TYPE_NOTIFICATION;
		            } else {
		              ring_type = RingtoneManager.TYPE_ALARM;
		            }
		            //String u;
		            try {
		              if(Utils.isCupcakeOrBefore()) {
		                new Thread() {
		                  public void run() {
		                    //copy mp3 to ringtone directory
		                    File file = new File(mMusicInfo.getmDownloadedPath());
		                    String newFileName;
		                    if(ring_type == RingtoneManager.TYPE_ALARM)
		                      newFileName = "/sdcard/media/audio/alarms/" + file.getName();
		                    else if(ring_type == RingtoneManager.TYPE_NOTIFICATION)
		                      newFileName = "/sdcard/media/audio/notifications/" + file.getName();
		                    else
		                      newFileName = "/sdcard/media/audio/ringtones/" + file.getName();;
		                    File newFile = new File(newFileName);
		                    try {
		                      java.io.FileInputStream in = new java.io.FileInputStream(file);
		                      java.io.FileOutputStream out = new java.io.FileOutputStream(newFile);
		                      byte bt[] = new byte[1024];
		                      int c;
		                      while ( (c = in.read(bt)) > 0) {
		                        out.write(bt, 0, c);
		                      }
		                      in.close();
		                      out.close();
		                    }catch (Exception e) {
		                      
		                    }
		                    //set ringtone
		                    long fileSize = newFile.length();
		                    String mimeType = "audio/mpeg";
		                    
		                    String artist = "Ringtone";
		                    
		                    ContentValues values = new ContentValues();
		                    values.put(MediaStore.MediaColumns.DATA, newFile.getAbsolutePath());
		                    values.put(MediaStore.MediaColumns.TITLE, mMusicInfo.getmTitle().toString());
		                    values.put(MediaStore.MediaColumns.SIZE, fileSize);
		                    values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
		                    
		                    values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
		                    
		                    // Insert it into the database
		                    Uri uri = MediaStore.Audio.Media.getContentUriForPath(newFile.getAbsolutePath());
		                    final Uri newUri = getContentResolver().insert(uri, values);
		                    RingtoneManager.setActualDefaultRingtoneUri(MusicPageActivity.this, ring_type, newUri);
		                  }
		                  
		                }.start();
		              } else {
		            	//Uri currentFileUri = MediaStore.Audio.Media.getContentUriForPath(mDownloadedPath);
		            	RingtoneManager.setActualDefaultRingtoneUri(MusicPageActivity.this, ring_type, Uri.parse(mMusicInfo.getmDownloadedUri()));
		                //add to system library
		                if(ring_type == RingtoneManager.TYPE_RINGTONE) {
		                  Settings.System.putString(getContentResolver(), Settings.System.RINGTONE, mMusicInfo.getmDownloadedUri());
		                  try {
		                    ContentValues values = new ContentValues(2);
		                    values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
		                    values.put(MediaStore.Audio.Media.IS_ALARM, true);
		                    getContentResolver().update(Uri.parse(mMusicInfo.getmDownloadedUri()), values, null, null);
		                  } catch (UnsupportedOperationException ex) {
		                    // most likely the card just got unmounted
		                    return;
		                  } catch (IllegalArgumentException e) {
		                    Toast.makeText(MusicPageActivity.this, R.string.notification_text_failed, Toast.LENGTH_SHORT);
		                    return;
		                  }
		                }
		                if(ring_type == RingtoneManager.TYPE_NOTIFICATION) {
		                  Settings.System.putString(getContentResolver(), Settings.System.NOTIFICATION_SOUND, mMusicInfo.getmDownloadedUri());
		                  try {
		                    ContentValues values = new ContentValues(2);
		                    values.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
		                    values.put(MediaStore.Audio.Media.IS_ALARM, true);
		                    getContentResolver().update(Uri.parse(mMusicInfo.getmDownloadedUri()), values, null, null);
		                  } catch (UnsupportedOperationException ex) {
		                    // most likely the card just got unmounted
		                    return;
		                  } catch (IllegalArgumentException e) {
		                    Toast.makeText(MusicPageActivity.this, R.string.notification_text_failed, Toast.LENGTH_SHORT);
		                    return;
		                  }
		                }
		                if(ring_type == RingtoneManager.TYPE_ALARM) {
		                  Settings.System.putString(getContentResolver(), Settings.System.ALARM_ALERT, mMusicInfo.getmDownloadedUri());
		                  try {
		                    ContentValues values = new ContentValues(2);
		                    values.put(MediaStore.Audio.Media.IS_ALARM, true);
		                    getContentResolver().update(Uri.parse(mMusicInfo.getmDownloadedUri()), values, null, null);
		                  } catch (UnsupportedOperationException ex) {
		                    // most likely the card just got unmounted
		                    return;
		                  } catch (IllegalArgumentException e) {
		                    Toast.makeText(MusicPageActivity.this, R.string.notification_text_failed, Toast.LENGTH_SHORT);
		                    return;
		                  }
		                }
		              }
		            } catch (Exception e) {
		            	Utils.D("exception in set");
		            	Utils.D(e.getMessage());
		            	e.printStackTrace();
		            }
		          }
		      })
		      .setNegativeButton(R.string.alertdialog_cancel, new DialogInterface.OnClickListener() {
		          public void onClick(DialogInterface dialog, int whichButton) {
		          }
		      })
		     .create(); 

		default:
			break;
		}
		return super.onCreateDialog(id);
	}
	
	private void updateUI() {
		if (mMusicInfo.getmDownloadedPath()!=null && mMusicInfo.getmDownloadedPath().length()>0) {
			mPreviewButton.setText(R.string.play);
			mDownloadButton.setVisibility(View.GONE);
			mEditButton.setVisibility(View.VISIBLE);
			mSetButton.setVisibility(View.VISIBLE);
			mAssignButton.setVisibility(View.VISIBLE);
			mFacebookButton.setVisibility(View.VISIBLE);
		} else {
			mPreviewButton.setText(R.string.preview);
			mDownloadButton.setVisibility(View.VISIBLE);
			mEditButton.setVisibility(View.GONE);
			mSetButton.setVisibility(View.GONE);
			mAssignButton.setVisibility(View.GONE);
			mFacebookButton.setVisibility(View.GONE);
		}
	}
	
	//facebook
	  public class WallPostRequestListener extends com.connect.facebook.BaseRequestListener {
		    
		    public void onComplete(final String response) {
		        Utils.D("Facebook-Example"+" Got response: " + response);
		        String message = "<empty>";
		        try {
		            JSONObject json = com.facebook.android.Util.parseJson(response);
		            message = json.getString("message");
		        } catch (JSONException e) {
		            Utils.D("Facebook-Example "+" JSON Error in response");
		        } catch (FacebookError e) {
		            Utils.D("Facebook-Example" +" Facebook Error: " + e.getMessage());
		        }
		        final String text = "Your Wall Post: " + message;
		        Utils.D(text);
		    }
		  }
	
}
