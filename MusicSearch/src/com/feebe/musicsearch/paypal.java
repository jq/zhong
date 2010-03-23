package com.feebe.musicsearch;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.util.Log;
import android.widget.Toast;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;




public class paypal extends Activity {

	// Paypal donate
	private static String mToken;
	WebView mPPWebview;
	final String UrlSucess = "http://127.0.0.1/musicsearch/sucess/";
	final String UrlFailed = "http://127.0.0.1/musicsearch/failed/";

	static final int DONATE_INIT = 1;
	static final int DONATE_SECUSS = 2;
	static final int DONATE_FAILED = 3;

	static final int TEST_PAYPAL_SANDBOX = 0;
	
	AlertDialog mDialog;
	ProgressDialog   mProgressDialog;


	
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        setContentView(R.layout.paypal);
        
        final String mimeType = "text/html";
        final String encoding = "utf-8";
        
        mPPWebview = (WebView) findViewById(R.id.webview_paypal);;

		showDialog(DONATE_INIT);
		
		(new Thread() {
			public void run() {
			    mToken = PaypalDonate_SetMobileCheckout(); 
			    String uriString;
			    
				if(TEST_PAYPAL_SANDBOX == 1)
					uriString = "https://www.sandbox.paypal.com/wc?t=" + mToken;
				else
					uriString = "https://mobile.paypal.com/wc?t=" + mToken;
				
				mPPWebview.loadUrl(uriString); 
				
				mPPWebview.setWebViewClient(mPPClient);

			}
		}).start();

		
		
		
    }

	WebViewClient mPPClient = new WebViewClient(){
		public void  onPageFinished(WebView view, String url){
			mPPWebview.requestFocus();
			Log.e("MusicSearch Paypal", "finished url: " + url);

			mProgressDialog.dismiss(); 
			paypal.this.setTitle(url);
			
			if(url.startsWith(UrlSucess)){
				Log.e("MusicSearch Paypal", "finished url: " + url);

			}

		}
		public boolean  shouldOverrideUrlLoading(WebView view, String url){
			if(url.startsWith(UrlSucess)){
				Log.e("MusicSearch Paypal", "OverrideUrlLoading url: " + url);


				String token = url.substring(url.indexOf("?token=") + new String("?token=").length());
				Log.e("MusicSearch Paypal", "OverrideUrlLoading token: " + token + "  mToken: " +mToken);
				if(token.equals(mToken) == true){
					showDialog(DONATE_INIT);
					(new Thread() {
						public void run() {
							String response = PaypalDonate_DoMobileCheckoutPayment(mToken); 
							
							if(response.length() > 0){
								// save donate flag to 1		
						    	try{
									byte[] b = new byte[1];
									FileOutputStream fcontrol =   openFileOutput("control2.list", 0);
									b[0] = 1;
									fcontrol.write(b);
									fcontrol.close();
						    	}catch(IOException e) {
									e.printStackTrace();
								} 
								ShowProgressDialog();
								
							}else
								ShowProgressDialog();
						 }
					 }).start();
				}else
					showDialog(DONATE_FAILED);
					
				return true;
			}
			else
				return false;
		}
		
		/*
		public void  onLoadResource(WebView view, String url){
		
			Log.e("MusicSearch Paypal", "finished url 2: " + url);
			if(url.startsWith(UrlSucess)){
				Log.e("MusicSearch Paypal", "finished url 2: " + url);
				PaypalDonate_DoMobileCheckoutPayment();
				Toast.makeText(paypal.this, url, Toast.LENGTH_SHORT).show();

				String token = url.substring(url.indexOf("&TOKEN=") + new String("&TOKEN=").length());
			}
		}
		*/
	};


	private void ShowProgressDialog() {
		this.runOnUiThread(new Runnable() {
			public void run() {
				mProgressDialog.dismiss(); 
				showDialog(DONATE_SECUSS);
			}
		});
	}
	
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DONATE_INIT: {
            	mProgressDialog = new ProgressDialog(this);
            	mProgressDialog.setMessage("Please wait while connect https://mobile.paypal.com/ ...");
            	mProgressDialog.setIndeterminate(true);
            	mProgressDialog.setCancelable(true);

                return mProgressDialog;
            }
            
            case DONATE_SECUSS: {
                mDialog = new AlertDialog.Builder(this)
                .setTitle("Donate sucess ")
                .setMessage("Thank you, donate sucess, you can search unlimited now !")
                .setCancelable(true)				
				.setPositiveButton("Close",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
									
                                    finish();

                            }
                        }).create();	
                
                return mDialog;
            }
			
            case DONATE_FAILED: {
                mDialog = new AlertDialog.Builder(this)
                .setTitle("Donate failed ")
                .setMessage("Sorry to paypap failed , please restart MusicSearch to try again, thanks !")
                .setCancelable(true)
				
				.setPositiveButton("Close",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                    finish();

                            }
                        }).create();
                
                return mDialog;
            }
        }
        return null;
    }


	
	// paypal donate
    private String PaypalDonate_SetMobileCheckout(){	
		String urlString;
		String token;
		
		if(TEST_PAYPAL_SANDBOX == 1)
			urlString = "https://api-3t.sandbox.paypal.com/nvp";
		else
			urlString = "https://api-3t.paypal.com/nvp";
		
        Map map = new HashMap();
		map.put("METHOD", "SetMobileCheckout");
		map.put("VERSION", "51.0");

		if(TEST_PAYPAL_SANDBOX == 1){
	        map.put("USER", "waf.ya_1243923059_biz_api1.gmail.com");
	        map.put("PWD", "1243923076");
			map.put("SIGNATURE", "AKLmkqPFFE7vwEEbGPrfwjOAZSf-A2GE2onAxvbQd69o8ao.h1oGoxAS");
		}else{
	        map.put("USER", "info_api1.irobotsoft.com");
	        map.put("PWD", "3E964KGDFHS84X6J");
			map.put("SIGNATURE", "A89ietZHH8rcYuKCskd3qnsfSktCA2zSupG.FCOCs20Gt1AhSd1BPhPU");

		}
		
		
		map.put("AMT", "7.99");
		map.put("CURRENCYCODE", "USD");
		map.put("DESC", "MusicSearch Donate");
		map.put("RETURNURL", UrlSucess);
		map.put("CANCELURL", UrlFailed);
		
        String temp = doPost(urlString, map, null);
        Log.e("MusicSearch Paypal", "response: " + temp);
        int pos = temp.indexOf("&TOKEN=");
		if(pos != -1)
			token = temp.substring(temp.indexOf("&TOKEN=") + new String("&TOKEN=").length());
		else
			token = null;
		
        return token;
		
    }

    private String PaypalDonate_DoMobileCheckoutPayment(String token){	
		String urlString;
		
        if(TEST_PAYPAL_SANDBOX == 1)
			urlString = "https://api-3t.sandbox.paypal.com/nvp";
		else
			urlString = "https://api-3t.paypal.com/nvp";
		
        Map map = new HashMap();
		map.put("METHOD", "DoMobileCheckoutPayment");
		map.put("VERSION", "51.0");

		if(TEST_PAYPAL_SANDBOX == 1){
	        map.put("USER", "waf.ya_1243923059_biz_api1.gmail.com");
	        map.put("PWD", "1243923076");
			map.put("SIGNATURE", "AKLmkqPFFE7vwEEbGPrfwjOAZSf-A2GE2onAxvbQd69o8ao.h1oGoxAS");
		}else{
	        map.put("USER", "info_api1.irobotsoft.com");
	        map.put("PWD", "3E964KGDFHS84X6J");
			map.put("SIGNATURE", "A89ietZHH8rcYuKCskd3qnsfSktCA2zSupG.FCOCs20Gt1AhSd1BPhPU");

		}

        map.put("TOKEN", token);
		
        String temp = doPost(urlString, map, null);
        Log.e("MusicSearch Paypal", "DoMobileCheckoutPayment response: " + temp);
        return temp;
		
    }
    public String doPost(String reqUrl, Map parameters,
            String recvEncoding)
    {
        HttpURLConnection url_con = null;
        //URLConnection url_con = null;
        String responseContent = null;
		InputStream stream = null;
		InputStreamReader is = null;
		String httpresponse = null;

		Log.e("MusicSearch Paypal", "doPost reqUrl: " + reqUrl);
		
        try
        {
            StringBuffer params = new StringBuffer();
            for (Iterator iter = parameters.entrySet().iterator(); iter
                    .hasNext();)
            {
                Entry element = (Entry) iter.next();
                params.append(element.getKey().toString());
                params.append("=");
				if(recvEncoding == null)
	                params.append(URLEncoder.encode(element.getValue().toString()));
				else
					params.append(URLEncoder.encode(element.getValue().toString(), recvEncoding));
                params.append("&");
            }

            if (params.length() > 0)
            {
                params = params.deleteCharAt(params.length() - 1);
            }

			Log.e("MusicSearch Paypal", "doPost params: " + params.toString());
			
	        HttpsURLConnection.setDefaultHostnameVerifier(hv);
			
            URL url = new URL(reqUrl);
            url_con = (HttpURLConnection) url.openConnection();
            //url_con = (URLConnection) url.openConnection();
            url_con.setRequestMethod("POST");
        	url_con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");
			

            url_con.setDoOutput(true);
            byte[] b = params.toString().getBytes();
            url_con.getOutputStream().write(b, 0, b.length);
            url_con.getOutputStream().flush();
            url_con.getOutputStream().close();			

			url_con.connect();
			
        	stream = url_con.getInputStream();
			
        	StringBuilder builder = new StringBuilder(4096);
			
        	char[] buff = new char[4096];
			is = new InputStreamReader(stream);
			int len;
			while ((len = is.read(buff)) > 0) {
				builder.append(buff, 0, len);
			}
			httpresponse = builder.toString();
			
			Log.e("MusicSearch Paypal", "doPost response: " + httpresponse);

		
			url_con = (HttpURLConnection)url.openConnection();
            url_con.setDoOutput(true);			
			
            url_con.getOutputStream().flush();
            url_con.getOutputStream().close();		

			
			
        }
        catch (IOException e)
        {
			Log.e("MusicSearch", "error: " + e.getMessage(), e);
        }
        finally
        {
            if (url_con != null)
            {
                url_con.disconnect();
            }
        }
        return httpresponse;
    }
	

    static HostnameVerifier hv = new HostnameVerifier() {
        public boolean verify(String urlHostName, SSLSession session) {
            System.out.println("Warning: URL Host: " + urlHostName + " vs. "
                               + session.getPeerHost());
            return true;
        }
    };


	
}
