package com.droidcool.music;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.util.Log;

// Utilities for LastFM.
public class LastFM {
	private static AndroidHttpClient sHttpClient = AndroidHttpClient.newInstance("Android/2.2");
	private static final int BUFFER_SIZE = 4096;
	private static final String TAG = "Music2";
	
	private static byte[] getStreamData(InputStream stream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] readBuffer = new byte[BUFFER_SIZE];
		int nRead;
		while ((nRead = stream.read(readBuffer)) >= 0) {
			baos.write(readBuffer, 0, nRead);
		}
		return baos.toByteArray();
	}
	
    private static InputStream getHttpContent(String url) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        
        HttpResponse response = sHttpClient.execute(httpGet);
        int status = response.getStatusLine().getStatusCode();
        if (status == 200) {
            HttpEntity body = response.getEntity();
            if (body != null) {
                return body.getContent();
            } else {
            	return null;
            }
        } else if (status == 404) {
        	return null;
        } else {
        	throw new IOException("Failed to retrieve [" + url +"] with status " +
                        response.getStatusLine());
        }
    }
	
    private static Bitmap getJpgPicture(String urlStr) throws IOException {
        Bitmap image = null;
        InputStream inputStream = getHttpContent(urlStr);

        if (inputStream == null)
        	return null;
        
        // save downloaded content
        byte[] data = getStreamData(inputStream);

        inputStream = new ByteArrayInputStream(data);


        /*
         * The following code does not compile in 1.5. Disable it temporarily.
         *
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap trialImage = BitmapFactory.decodeStream(inputStream, null, options);
        if (options.outHeight == -1 || options.outWidth == -1) {
            return null;
        }
        // Now downscale to fit bounds
        int hscale = options.outHeight / HEIGHT;
        int wscale = options.outWidth / WIDTH;
        options.inDensity = Math.max(hscale, wscale);
        options.inJustDecodeBounds = false;

        // this time decode for real
        inputStream = new ByteArrayInputStream(data);
        image = BitmapFactory.decodeStream(inputStream, null, options);
        */
    	BitmapFactory.Options options = new BitmapFactory.Options();
    	image = BitmapFactory.decodeStream(inputStream, null, options);
        if (image == null) {
            Log.e(TAG, "Failed to fetch picture: "
                    + "(BitmapFactory.decodeStream null)\n");
        }
        return image;
    }

}
