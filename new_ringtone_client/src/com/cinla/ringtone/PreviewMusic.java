package com.cinla.ringtone;

import java.io.IOException;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.AsyncTask;
import android.view.View;

public class PreviewMusic {
	
	private static MediaPlayer sMediaPlayer;
	private static PreviewMusicTask sPreviewMusicTask;
	
	private MusicInfo mMusicInfo;
	private ProgressDialog mProgressDialog;
	private Context mContext;
	private String mDownloadedMusicPath;
	
	public PreviewMusic(Context context, MusicInfo musicInfo, String downloadedMusicPath) {
		mContext = context;
		mMusicInfo = musicInfo;
		mDownloadedMusicPath = downloadedMusicPath;
	}
	
	public void startPlay() {
		if (sPreviewMusicTask != null) {
			sPreviewMusicTask.cancel(true);
		}
		sPreviewMusicTask = new PreviewMusicTask();
		sPreviewMusicTask.execute();
	}
	
	public void stopPlay() {
		mProgressDialog.cancel();
		if (sMediaPlayer != null) {
			sMediaPlayer.pause();
			sMediaPlayer.release();
		}
		sMediaPlayer = null;
	}
	
	private void previewFailed() {
		if (mProgressDialog != null) {
			mProgressDialog.cancel();
		}
	}
	
	private class ProgressDialogCancelListener implements DialogInterface.OnCancelListener {
		@Override
		public void onCancel(DialogInterface dialog) {
			stopPlay();
		}
	}
	
	private class PreviewMusicTask extends AsyncTask<Void, Void, Integer> {
		@Override
		protected void onPreExecute() {
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setCancelable(true);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setOnCancelListener(new ProgressDialogCancelListener());
			mProgressDialog.setButton(mContext.getString(R.string.stop), new StopClickListener());
			mProgressDialog.show();
			super.onPreExecute();
		}
		@Override
		protected Integer doInBackground(Void... params) {

			if (sMediaPlayer != null) {
				stopPlay();
			}
			sMediaPlayer = new MediaPlayer();
			if (mDownloadedMusicPath != null) {
				try {
					sMediaPlayer.setDataSource(mDownloadedMusicPath);
					sMediaPlayer.setOnCompletionListener(new MediaPlayerCompletionListener());
				} catch (Exception e) {
					previewFailed();
					return null;
				} 
			} else {
				try {
					sMediaPlayer.setDataSource(mMusicInfo.getmMp3Url());
				} catch (Exception e) {
					previewFailed();
					return null ;
				}
			}
			try {
				sMediaPlayer.prepare();
			} catch (Exception e) {
				previewFailed();
				return null;
			}
			sMediaPlayer.start();
			return 1;
		}
		@Override
		protected void onPostExecute(Integer result) {
			if (result == null) {
				stopPlay();
			}
			super.onPostExecute(result);
		}
		@Override
		protected void onCancelled() {
			stopPlay();
			super.onCancelled();
		}
		
		private class MediaPlayerCompletionListener implements OnCompletionListener {
			@Override
			public void onCompletion(MediaPlayer mp) {
				stopPlay();
			}
		}
	}
	
	private class StopClickListener implements DialogInterface.OnClickListener{
		@Override
		public void onClick(DialogInterface dialog, int which) {
			stopPlay();
		}
	}

}
