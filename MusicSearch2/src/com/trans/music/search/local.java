package com.trans.music.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.json.JSONArray;

import com.trans.music.search.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;

import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ImageView;

import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Context;

import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.net.Uri;
import android.content.ContentResolver;
import android.provider.Settings;

import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.content.ContentUris;
import android.database.Cursor;
import android.content.ContentValues;

public class local extends Activity {
    // Local Playlist
	ListView mLocalList;
	JSONArray mLocalMp3s = new JSONArray();
    ArrayAdapter<String> mLocalAdapter; 
    ArrayList<String> mLocalStrings = new ArrayList<String>();

    SeekBar  mSeekBar;
    
	int mLocalMp3index = -1;
	
	Uri mCurrentFileUri ;
	private boolean mChooseItem = false;
	
	long  mSongDuration;
	
	ImageView mPlayStop;

  private MediaPlayer mPlayer = new MediaPlayer();
  
  @Override
  protected void onResume() {
    super.onResume();
    updateDownloadList();
  }
    @Override
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.local);

		
        // Local Playlist UI
        mLocalList = (ListView) findViewById(R.id.local_playlist);
        mLocalAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mLocalStrings);    
        mLocalList.setAdapter(mLocalAdapter);   


        mLocalList.setOnItemClickListener(new OnItemClickListener() {
          public void onItemClick(AdapterView parent, View v, int position, long id) {         
            if (mLocalMp3index == position) {
              if(mPlayer.isPlaying()) {
                Toast.makeText(local.this, "Pause", Toast.LENGTH_SHORT).show();

                mPlayer.pause();
              } else {
                Toast.makeText(local.this, "Resume", Toast.LENGTH_SHORT).show();

                mPlayer.start();
                return;
              }
              return;
            }
  				  mLocalMp3index = position;
            if(mPlayer.isPlaying()) {
              mPlayer.reset();
            }
   				  String fileLocal = Const.homedir + mLocalStrings.get(mLocalMp3index);
    				Toast.makeText(local.this, "Playing:  " + fileLocal, Toast.LENGTH_SHORT).show();
    				try {
              mPlayer.setDataSource(fileLocal);
              mPlayer.prepare();
              mPlayer.start();
            } catch (IllegalArgumentException e) {
              e.printStackTrace();
            } catch (IllegalStateException e) {
              e.printStackTrace();
            } catch (IOException e) {
              e.printStackTrace();
            }
            }
            });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the currently selected menu XML resource.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.local, menu);      
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_search:
				finish();
            	break;
            case R.id.menu_ringtone:
				if(mLocalMp3index >= 0 && mChooseItem == true){
			        try {
						ContentResolver resolver = this.getContentResolver();
						Uri ringUri = mCurrentFileUri;
						
			            ContentValues values = new ContentValues(2);
			            values.put(MediaStore.Audio.Media.IS_RINGTONE, "1");
			            values.put(MediaStore.Audio.Media.IS_ALARM, "1");
			            resolver.update(ringUri, values, null, null);
						
		                Settings.System.putString(resolver, Settings.System.RINGTONE, ringUri.toString());
						Toast.makeText(this, "This playing song has set as phone ringtone.", Toast.LENGTH_SHORT).show();

			        }catch(Exception e) {
						e.printStackTrace();
					} 
				}else{
					Toast.makeText(this, "Please select one music to play.", Toast.LENGTH_SHORT).show();
				}
            	break;
			case R.id.menu_delete:
            	try{
					String fileLocal = Const.homedir + mLocalStrings.get(mLocalMp3index);
					File mp3 = new File(fileLocal);
					if(mp3.exists()){
						mp3.delete();
						updateDownloadList();
						mChooseItem = false;
					}
                }catch(Exception e) {
					e.printStackTrace();
				} 
                return true;
            case R.id.menu_help:
                intent = new Intent(local.this, help.class);
                startActivity(intent);
                return true;
            default:
                break;
        }
        
        return false;
    }
	
	private void updateDownloadList() {
				try{
					File[] file=(new File(Const.homedir)).listFiles();
					mLocalAdapter.clear();
					for(int i = 0; i < file.length; i++){
						if(file[i].isFile()){
							String fname = file[i].getName();
							if(fname.endsWith(".mp3"))
								mLocalAdapter.add(fname);
						}
					}
				}catch(Exception e) {
					e.printStackTrace();
				} 
	}
	
	
 
}
