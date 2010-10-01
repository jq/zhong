package com.trans.music.search;


import android.app.Activity;
import android.os.Bundle;

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.view.Window;
import android.view.KeyEvent;
import android.content.Intent;
import android.util.Log;
import android.net.Uri;
import android.graphics.Bitmap;

import com.ringtone.music.search1.R;

public class ArtistList extends Activity {
	
    private WebView mWebView = null;
    String  mRequstUrl = null;
    boolean mShouldShow = true;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        
        setContentView(R.layout.ringtones);
        
 		Bundle extras = getIntent().getExtras();

        if(extras != null){
    		mRequstUrl = extras.getString("url");
        }
        
        mWebView = (WebView) findViewById(R.id.webview);
		mWebView.setWebViewClient(new SimpleWebViewClient(this));		
		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);       
        
        if(mRequstUrl != null && mRequstUrl.length() > 0){
            Log.e("OnlineMusic", "Load page : " + mRequstUrl);
            if(mRequstUrl.indexOf("http://ggapp.appspot.com/mobile/search/") != -1)
                mWebView.loadUrl(mRequstUrl + "&nh=1");
            else
                mWebView.loadUrl(mRequstUrl + "?nh=1");	

        }else{
            mWebView.loadUrl("http://ggapp.appspot.com/mobile/home/");
        }


        
    }
    public void onDestroy(){
        if(mWebView != null){
            Log.e("Ringtone ", "Clear webview : " );
    		mWebView.clearCache(true);	
    		mWebView.destroy();
    		mWebView = null;
        }
        
    	super.onDestroy();
    }

    private class SimpleWebViewClient extends WebViewClient {
		private ArtistList mActivity;
		SimpleWebViewClient(ArtistList activity){
			mActivity = activity;
		}
        
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	Log.e("Ringtones :search ", "shouldOverrideUrlLoading: " + url);
	        //view.loadUrl(url);
	        //super.shouldOverrideUrlLoading();
	        
            if(url.indexOf("http://ggapp.appspot.com/mobile/show/") != -1){
                Intent intent = new Intent();
				Log.e("Ringtones ", "putExtra url : " + url);
				intent.putExtra("url", url);
            	intent.setClass(ArtistList.this, RDetail.class);
				startActivity(intent);	
                //startActivityForResult(intent, 1);
            }else if(url.indexOf("http://ggapp.appspot.com/mobile/") != -1){
                if(url.indexOf("http://ggapp.appspot.com/mobile/search/") != -1)
                    mWebView.loadUrl(url + "&nh=1");
                else
                    mWebView.loadUrl(url + "?nh=1");	
            }else{
    			Intent i = new Intent(Intent.ACTION_VIEW);
    			i.setData(Uri.parse(url));
    			startActivity(i);

            }
			
	        return true;
	    }
        @Override
		public void  onPageFinished(WebView view, String url){
            if(mShouldShow == true){
                findViewById(R.id.center_text).setVisibility(View.GONE);
                mWebView.setVisibility(View.VISIBLE);
            }
            mActivity.getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,Window.PROGRESS_VISIBILITY_OFF );

		}

        @Override
		public void  onPageStarted(WebView view, String url, Bitmap favicon){
			mActivity.getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,Window.PROGRESS_VISIBILITY_ON );

		}

		public void onLoadResource (WebView view, String url){
			super.onLoadResource(view, url);
		}

        
        @Override
        public void  onReceivedError  (WebView view, int errorCode, String description, String failingUrl){
            if(errorCode != 200)
                mShouldShow = false;
                
        }
        
	}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
			Uri uri = data != null ? data.getData() : null;
            setResult(RESULT_OK, new Intent().setData(uri));
            finish();            
        } 
    }

	private void goBackOnePageOrQuit() {
        WebView webView = mWebView;
		if (webView.canGoBack()) {
			webView.goBack();
		} else {
			finish();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		boolean handle = (keyCode == KeyEvent.KEYCODE_BACK);
		return handle || super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			goBackOnePageOrQuit();
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
		
	}

}

