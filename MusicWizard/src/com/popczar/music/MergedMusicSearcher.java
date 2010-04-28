package com.popczar.music;

import java.util.ArrayList;

import android.util.Log;

public class MergedMusicSearcher implements IMusicSearcher {
	
	private boolean mBackupMode = false;
	private IMusicSearcher mSogou;
	private IMusicSearcher mSkreemr;
	private int mPage;
	
	public MergedMusicSearcher() {
		mSogou = MusicSearcherFactory.getInstance(MusicSearcherFactory.ID_SOGOU);
		mSkreemr = MusicSearcherFactory.getInstance(MusicSearcherFactory.ID_SKREEMR);
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
			return mSkreemr.getNextResultList();
		}
		// Will not reach here.
		return new ArrayList<MusicInfo>();
	}

	@Override
	public void setMusicDownloadUrl(MusicInfo info) {
		if (!mBackupMode) {
			mSogou.setMusicDownloadUrl(info);
			return;
		}
		mSkreemr.setMusicDownloadUrl(info);
	}

	@Override
	public void setQuery(String query) {
		mSogou.setQuery(query);
		mSkreemr.setQuery(query);
		mPage = 1;
	}

}
