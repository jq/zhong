package com.happy.life;

import com.util.SearchResultBuffer;

public class MusicSearchAdapter extends com.util.SearchAdapter {

  private int mSougouResultCount = 0;

  public MusicSearchAdapter(byte[] guid) {
    super(guid);
  }
  
  public synchronized int size() {
    return mResult.size() + mSougouResultCount;
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
}
