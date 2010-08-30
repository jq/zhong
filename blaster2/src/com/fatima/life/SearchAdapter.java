package com.fatima.life;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import android.util.Log;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.search.HostData;

// This class is thread safe.
public class SearchAdapter {
	private HashMap<URN, P2pSearchResult> mResult = new HashMap<URN, P2pSearchResult>();
	private int mSougouResultCount = 0;
	
	// Provide a list view for representation.
	private ArrayList<SearchResultBuffer> mResultBuffers = new ArrayList<SearchResultBuffer>();
	private final byte[] mGuid;
	
	// For displaying.
	private int mCurrentBatch;
	
	public SearchAdapter(byte[] guid) {
		mGuid = guid;
		mCurrentBatch = 0;
	}
	
	public byte[] getGuid() {
		return mGuid;
	}

	public boolean sameGuid(byte[] guid) {
		int size = guid.length;
		for (int i = 0; i < size; i++)
			if (this.mGuid[i] != guid[i]) {
				return false;
			}
		return true;
	}
	
	public synchronized SearchResult get(int i) {
		int bufferIndex = i / SearchResultBuffer.BUFFER_SIZE;
		int bufferOffset = i % SearchResultBuffer.BUFFER_SIZE;
		return mResultBuffers.get(bufferIndex).get(bufferOffset);
	}
	
	public synchronized int size() {
		return mResult.size() + mSougouResultCount;
	}
	
	// Set loc is Endpoint.
	//
	// TODO: we can create RFD from it for now, we just ignore it
	//
	// Returns true if we need to refresh display.
	public synchronized boolean add(RemoteFileDesc rfd, HostData data, Set<Endpoint> loc) {
		if (rfd.getFileName() != null &&
			rfd.getFileName().indexOf("LAWSUIT WARNING") != -1) {
			return false;
		}
		
		URN sha1 = rfd.getSHA1Urn();
		P2pSearchResult rs = mResult.get(sha1);

		SearchResultBuffer lastBuffer = null;
		if (!mResultBuffers.isEmpty())
			lastBuffer = mResultBuffers.get(mResultBuffers.size() - 1);
		
		boolean shouldRefresh = false;
		
		if (rs == null) {
			// A new search result.
			rs = new P2pSearchResult(mGuid, rfd, data, loc);
			mResult.put(sha1, rs);
			
			if (lastBuffer != null && !lastBuffer.isFull()) {
				lastBuffer.add(rs);
			} else {
				lastBuffer = new SearchResultBuffer();
				lastBuffer.add(rs);
				mResultBuffers.add(lastBuffer);
			}
			if (mCurrentBatch < mResultBuffers.size() && mResultBuffers.get(mCurrentBatch) == lastBuffer) {
				shouldRefresh = true;
			}
		} else {
			shouldRefresh = true;
			// TODO: We may need to find the correponding buffer and resort it.
		}
		
		
		rs.add(rfd);
		return shouldRefresh;
	}
	
	public synchronized boolean add(SogouSearchResult result) {
	  if (result == null)
	    return false;
	  
	  SearchResultBuffer lastBuffer = null;
      if (!mResultBuffers.isEmpty())
          lastBuffer = mResultBuffers.get(mResultBuffers.size() - 1);
      
      boolean shouldRefresh = false;
      mSougouResultCount++;
      
      if (lastBuffer != null && !lastBuffer.isFull()) {
        lastBuffer.add(result);
      } else {
        lastBuffer = new SearchResultBuffer();
        lastBuffer.add(result);
        mResultBuffers.add(lastBuffer);
      }
      if (mCurrentBatch < mResultBuffers.size() && mResultBuffers.get(mCurrentBatch) == lastBuffer) {
        shouldRefresh = true;
      }
      return shouldRefresh;
	}
	
	// Method for display..
	public synchronized int displaySize() {
		if (mResultBuffers.isEmpty())
			return 0;
		int lastDisplay = Math.min(mCurrentBatch, mResultBuffers.size() - 1);
		return SearchResultBuffer.BUFFER_SIZE * lastDisplay + mResultBuffers.get(lastDisplay).size();
	}
	
	public synchronized void nextBatch() {
		++mCurrentBatch;
	}
	
	public synchronized boolean isBatchFull() {
		if (mResultBuffers.isEmpty())
			return false;

        if (mCurrentBatch >= mResultBuffers.size())
            return false;
		
		return mResultBuffers.get(mCurrentBatch).isFull();
	}
}
