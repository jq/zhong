

package com.feebe.musicsearch;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;

import android.view.LayoutInflater;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.util.Log;

import android.net.Uri;
import android.content.Intent;

public class Lyric extends Activity {

	private static final String urlQueryString = "http://www.heiguge.com/mp3/getlyric/?";
	WebView mWebPageView;
	String mArtist, mSong;
	Dialog mFindLyricDia;	
	 
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        setContentView(R.layout.webview);
        
        final String mimeType = "text/html";
        final String encoding = "utf-8";

		mArtist = new String();
		mSong = new String();
		Bundle extras = getIntent().getExtras();

		if(extras != null){
			mArtist = extras.getString("artist");
			mSong = extras.getString("song");

		}
		Log.e("MusicSearch :Lyric ", "artist: " + mArtist + "  song: " + mSong);
		
		mWebPageView = (WebView) findViewById(R.id.webview);
		mWebPageView.setWebViewClient(new SimpleWebViewClient());
		
		WebSettings webSettings = mWebPageView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		//webSettings.setSupportMultipleWindows(true);
		//webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
	
		showDialog(INPUT_ARTIST_SONG); 
		/*
		if(mArtist.length() > 0 && mSong.length() > 0){
			String url = urlQueryString + "a=" + mArtist + "&"  + "s=" + mSong;
			mWebPageView.loadUrl(url); 
		}else{
			showDialog(INPUT_ARTIST_SONG); 
		}
		*/

    }

	static final int INPUT_ARTIST_SONG = 1;
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {

            case INPUT_ARTIST_SONG: {
            LayoutInflater factory = LayoutInflater.from(this);
            final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
			EditText mEditArtist, mEditSong;
			mEditArtist = (EditText) textEntryView.findViewById(R.id.artistname_edit);
			mEditSong = (EditText) textEntryView.findViewById(R.id.songtitle_edit);
			mEditArtist.setText(mArtist);
			mEditSong.setText(mSong);

			mFindLyricDia = new AlertDialog.Builder(Lyric.this)
                //.setIcon(R.drawable.alert_dialog_icon)
                .setTitle("Find Lyric")
                .setView(textEntryView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
    					
                        /* User clicked OK so do some stuff */
						EditText mEditArtist, mEditSong;
						mEditArtist = (EditText) textEntryView.findViewById(R.id.artistname_edit);
						mEditSong = (EditText) textEntryView.findViewById(R.id.songtitle_edit);

						Log.e("MusicSearch :Lyric ", "mEditArtist: " + mEditArtist);

						
						if(mEditArtist != null && mEditSong != null){
							String artist = mEditArtist.getText().toString().replaceAll(" ", "+");
							String song = mEditSong.getText().toString().replaceAll(" ", "+");
							String url = urlQueryString + "a=" + artist + "&"  + "s=" + song;
							//String url = "http://www.cyrket.com/";
							//mWebPageView.loadUrl(url); 
							
							
							Intent i = new Intent(Intent.ACTION_VIEW);
							i.setFlags(0);
							i.setData(Uri.parse(url));
							startActivity(i);	
							
							finish();
							
						}
						

						
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        /* User clicked cancel so do some stuff */
						finish();
                    }
                })
                .create();

				return mFindLyricDia;
            }


        }
        return null;
    }
		
	private class SimpleWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	Log.e("MusicSearch :Lyric ", "shouldOverrideUrlLoading: " + url);
	        //view.loadUrl(url);
	        //super.shouldOverrideUrlLoading();
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(url));
			startActivity(i);
			
	        return true;
	    }
		/*
		@Override
		public void  onLoadResource  (WebView view, String url){
			Log.e("MusicSearch :Lyric ", "onLoadResource: " + url);
			
			if(url.startsWith("http://googleads.g.doubleclick.net")){
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(url));
				startActivity(i);
			}
			
		}
		*/
	}
	
	protected void onDestroy() {

		mFindLyricDia.dismiss();
		
    	super.onDestroy();
	}
	
}


