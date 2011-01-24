package com.cinla.ringtone;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Debug;
import android.provider.MediaStore;
import android.widget.ProgressBar;
import android.widget.Toast;

public class DownloadMusicTask extends AsyncTask<Void, Integer, File> {

	private Context mContext;
	private MusicInfo mMusicInfo;
	private String mMusicPath;
	private ProgressDialog mProgressDialog;
	
	public DownloadMusicTask(Context context, MusicInfo musicInfo) {
		mContext = context;
		mMusicInfo = musicInfo;
	}
	
	@Override
	protected void onPreExecute() {
		mProgressDialog = new ProgressDialog(mContext);
		mProgressDialog.setTitle(R.string.download);
		if (mMusicInfo.getmSize() != 0) {
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(100);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setProgress(0);
		} else {
			mProgressDialog.setIndeterminate(true);	
		}
		mProgressDialog.setCancelable(true);			//should be false, force user to click the 2 buttons above.
		mProgressDialog.setButton(mContext.getString(R.string.hide), new HideButtonClickListener());
		mProgressDialog.show();
		super.onPreExecute();
	}

	@Override
	protected File doInBackground(Void... params) {
		Utils.D("background start:");
		int count = 0;
		URL url = null;
		HttpURLConnection urlConn = null;
		InputStream stream = null;
		DataInputStream is = null;
		File f = null;
		try {
			Utils.D("Download url: "+Utils.encodeUrlTail(mMusicInfo.getmMp3Url()));
			url = new URL(Utils.encodeUrlTail(mMusicInfo.getmMp3Url()));
			urlConn = (HttpURLConnection)url.openConnection();
			urlConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3 -Java");
			urlConn.setConnectTimeout(4000);
			urlConn.connect();
			stream = urlConn.getInputStream();
			byte[] buff = new byte[4096];
			is = new DataInputStream(stream);
			int len;
			f = new File(mMusicInfo.getFilePath());
			mMusicPath = f.getAbsolutePath();
			FileOutputStream file =  new FileOutputStream(f);
			int percent = 0;
			int last_percent = 0;
			while ((len = is.read(buff)) > 0) {
				file.write(buff, 0, len);
				file.flush();
				count = count + len;
				if (mMusicInfo.getmSize() != 0) {
					percent = (int) ((count*100)/(mMusicInfo.getmSize()*1024));
					Utils.D("Percent: "+percent);
					Utils.D("Size: "+mMusicInfo.getmSize());
					if (percent != last_percent) {
						publishProgress(percent);
						last_percent = percent;
					}
				}
			}
			urlConn.disconnect();
			return f;
		} catch (Exception e) {
			if (f!=null) {
				f.delete();
			}
			return null;
		} 
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		mProgressDialog.setProgress(values[0]);
		super.onProgressUpdate(values);
	}

	@Override
	protected void onPostExecute(File result) {
		mProgressDialog.cancel();
		if (result == null) {
			Toast.makeText(mContext, R.string.download_failed, Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(mContext, R.string.download_finished, Toast.LENGTH_SHORT).show();
			Uri uri = Utils.saveToMediaLib(mMusicInfo.getmTitle(), result.getAbsolutePath(), result.length(), mMusicInfo.getmArtist(), mContext.getContentResolver());
			mMusicInfo.setmDownloadedPath(result.getAbsolutePath());
			mMusicInfo.setmDownloadedUri(uri.toString());
			Utils.D("wirteToDisk: "+Utils.writeToDisk(mMusicInfo));
			((MusicPageActivity)mContext).onDownloadFinish(result, uri);
		}
		super.onPostExecute(result);
	}

	@Override
	protected void onCancelled() {
		mProgressDialog.cancel();
		Toast.makeText(mContext, R.string.download_canceled, Toast.LENGTH_SHORT).show();
		super.onCancelled();
	}
	
	private class HideButtonClickListener implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			mProgressDialog.hide();
			((MusicPageActivity)mContext).onHideProgressDialog();
		}
	}
	
	public void showProgressDialog() {
		mProgressDialog.show();
	}
}
