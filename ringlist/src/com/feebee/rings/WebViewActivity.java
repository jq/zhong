package com.feebee.rings;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.feebee.rings.R;

public class WebViewActivity extends Activity {
  private WebView mView;
  private String mUrl;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.webview);
    mView = (WebView) findViewById(R.id.web_view);
    WebSettings ws = mView.getSettings();
    ws.setJavaScriptEnabled(true);
    ws.setPluginsEnabled(true);
    ws.setLoadsImagesAutomatically(true);
    ws.setSupportZoom(true);
    ws.setBuiltInZoomControls(true);

    mUrl = getIntent().getStringExtra("url");
    Log.e("url", mUrl);
    mView.loadUrl(mUrl);

  }

}
