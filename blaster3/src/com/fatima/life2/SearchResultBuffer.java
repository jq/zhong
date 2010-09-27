package com.fatima.life2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import android.util.Log;

import com.limegroup.gnutella.RemoteFileDesc;

// Result for presentation in one batch.
class SearchResultBuffer { 
	public static final int BUFFER_SIZE = 20;
	private ArrayList<SearchResult> mResults;
    private long mTimeLastSorted;

	public SearchResultBuffer() {
		mResults = new ArrayList<SearchResult>(BUFFER_SIZE);
	}

	public boolean isFull() {
		return mResults.size() == BUFFER_SIZE;
	}

	public int size() {
		return mResults.size();
	}
	
	public SearchResult get(int i) {
		return mResults.get(i);
	}

	public void add(SearchResult result) {
		if (mResults.size() == BUFFER_SIZE) {
			throw new IllegalStateException("Adding result while it is full.");
		}
//		//remove the same
//		boolean isDumplicated = false;
//		SearchResult dum = null;
//		boolean removeOld = false;
//		for(Iterator<SearchResult> it = mResults.iterator();it.hasNext();){
//		  SearchResult inBuffer = it.next();
//		  if(inBuffer.getFileName().equals(result.getFileName())
//		      && Utils.getSizeInM(inBuffer.getFileSize()) == Utils.getSizeInM(result.getFileSize())) {
//		    if(inBuffer.getAlt().size() > result.getAlt().size()) {
//		      isDumplicated = true;
//		    }
//		    dum = inBuffer;
//		    removeOld = true;
//		  }
//		}
//		if(removeOld)
//		  mResults.remove(dum);
//		if(!isDumplicated)
//		  mResults.add(result);
		
		mResults.add(result);
//		resort();
	}
	
//	public void resort() {
//		long t = System.currentTimeMillis();
//		if (t - mTimeLastSorted > 1000) {
//			Collections.sort(mResults);
//			mTimeLastSorted = t;
//		}
//	}
}
