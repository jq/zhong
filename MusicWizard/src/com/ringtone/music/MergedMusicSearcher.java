package com.popczar.music;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

public class MergedMusicSearcher implements IMusicSearcher {
	
	private boolean mBackupMode = false;
	private IMusicSearcher mSogou;
	private IMusicSearcher mSecondSearcher;
	private int mPage;
	
	public MergedMusicSearcher() {
		mSogou = MusicSearcherFactory.getInstance(MusicSearcherFactory.ID_SOGOU);
		mSecondSearcher = MusicSearcherFactory.getInstance(MusicSearcherFactory.ID_SKREEMR);
		//mSecondSearcher = MusicSearcherFactory.getInstance(MusicSearcherFactory.ID_BAIDU);
	}
	

	@Override
	public ArrayList<MusicInfo> getNextResultList() {
		if (!mBackupMode) {
			ArrayList<MusicInfo> infos = mSogou.getNextResultList();
			if (infos == null)
				return null;
			if (infos.size() == 0 && mPage == 1) {
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
