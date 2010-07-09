package com.ringtone.music;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

public class MergedMusicSearcher implements IMusicSearcher {
	
	private boolean mBackupMode = false;
	private IMusicSearcher mSogou;
	private IMusicSearcher mSecondSearcher;
	private int mPage;
	
	private static int sNumQueries = 0;
	
	public MergedMusicSearcher() {
		mSogou = MusicSearcherFactory.getInstance(MusicSearcherFactory.ID_SOGOU);
		mSecondSearcher = MusicSearcherFactory.getInstance(MusicSearcherFactory.ID_SKREEMR);
		//mSecondSearcher = MusicSearcherFactory.getInstance(MusicSearcherFactory.ID_BAIDU);
	}
	

	@Override
	public ArrayList<MusicInfo> getNextResultList() {
		sNumQueries++;
		if (!mBackupMode) {
			ArrayList<MusicInfo> infos = mSogou.getNextResultList();
			if (infos == null)  // Error
				return null;
			if (infos.size() == 0 && mPage == 1) {
				if (sNumQueries <= 2)  { // Retry 
					Log.i(Utils.TAG, "Retry " + sNumQueries);
					return getNextResultList();
				}
				mBackupMode = true;
				Log.i(Utils.TAG, "Switching to backup mode");
				// fall through
			} else {
				mPage++;
				return infos;
			}
		}
		
		if (mBackupMode) {
			return mSecondSearcher.getNextResultList();
		}
		// Will not reach here.
		return new ArrayList<MusicInfo>();
	}

	@Override
	public void setMusicDownloadUrl(Context context, MusicInfo info) {
		if (!mBackupMode) {
			mSogou.setMusicDownloadUrl(context, info);
			return;
		}
		mSecondSearcher.setMusicDownloadUrl(context, info);
	}

	@Override
	public void setQuery(String query) {
		mSogou.setQuery(query);
		mSecondSearcher.setQuery(query);
		mPage = 1;
	}

}
