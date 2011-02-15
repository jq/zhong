package com.cinla.ringtone;

import java.io.IOException;

import com.latest.ringtone.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;

public class PreviewMusic {
	
	private MediaPlayer mMediaPlayer;
	
	private MusicInfo mMusicInfo;
	private ProgressDialog mProgressDialog;
	private Activity mActivity;
	private String mDownloadedMusicPath;
	
	public PreviewMusic(Activity activity, MusicInfo musicInfo, String downloadedMusicPath) {
		mActivity = activity;
		mMusicInfo = musicInfo;
		mDownloadedMusicPath = downloadedMusicPath;
	}
	
	public void startPlay() {
		mProgressDialog = new ProgressDialog(mActivity);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setOnCancelListener(new ProgressDialogCancelListener());
		mProgressDialog.setTitle(R.string.streaming_wait);
		mProgressDialog.setButton(mActivity.getString(R.string.stop), new StopClickListener());
		mProgressDialog.show();
		new Thread(new Runnable() {
			@Override
			public void run() {
				MediaPlayer mediaPlayer = mMediaPlayer;
				if (mediaPlayer != null) {
					mediaPlayer.release();
				}
				try {
					mMediaPlayer = new MediaPlayer();
					mMediaPlayer.reset();
					if (mDownloadedMusicPath!=null && mDownloadedMusicPath.length()>0) {
						mMediaPlayer.setDataSource(mDownloadedMusicPath);
						mMediaPlayer.setOnCompletionListener(new MediaPlayerCompletionListener());
					} else {
						mMediaPlayer.setDataSource(mMusicInfo.getmMp3Url());
					}
					mMediaPlayer.prepare();
					mMediaPlayer.start();
				} catch (Exception e) {
					mActivity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(mActivity, R.string.streaming_failed, Toast.LENGTH_SHORT).show();
						}
					});
					previewFailed();
				}
			}
		}).start();
	}
	
	public void stopPlay() {
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mProgressDialog.cancel();
			}
		});
		if (mMediaPlayer !=null) {
			mMediaPlayer.stop();
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				MediaPlayer mediaPlayer = mMediaPlayer;
				if (mediaPlayer != null) {
					mediaPlayer.stop();
					mediaPlayer.release();
				}
			}
		}).start();
		mMediaPlayer = null;
	}
	
	private void previewFailed() {
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (mProgressDialog != null) {
					mProgressDialog.cancel();
				}
			}
		});
	}
	
	private class ProgressDialogCancelListener implements DialogInterface.OnCancelListener {
		@Override
		public void onCancel(DialogInterface dialog) {
			stopPlay();
		}
	}
	
	private class MediaPlayerCompletionListener implements OnCompletionListener {
		@Override
		public void onCompletion(MediaPlayer mp) {
			stopPlay();
		}
	}
	
	private class StopClickListener implements DialogInterface.OnClickListener{
		@Override
		public void onClick(DialogInterface dialog, int which) {
			stopPlay();
		}
	}

}
