package com.ringtone.music;

import java.util.ArrayList;

import com.ringtone.music.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class ControlBarView extends LinearLayout implements View.OnClickListener {
    private View mLoading;
    private View mNetworkError;
    private View mRetry;
    
    private int mFetchPage;
    private int mFocusPage;
    
    private OnClickListener mRetryListener;

    enum Status {
    	LOADED,
    	LOADING,
    	ERROR,
    	CACHE
    };
    
    public ControlBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFetchPage=0;
        mFocusPage=1;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mLoading = findViewById(R.id.loading);
        mNetworkError = findViewById(R.id.network_error);
        mRetry = findViewById(R.id.retry);
        mRetry.setOnClickListener(this);
    }

    public void onClick(View v) {
        mRetryListener.onClick(v);
    }
    
    public void setLoadingStatus() {
    	mLoading.setVisibility(View.VISIBLE);
    	mNetworkError.setVisibility(View.GONE);
    }
    
    public void setErrorStatus(OnClickListener retryListener) {
    	mRetryListener = retryListener;
        mNetworkError.setVisibility(View.VISIBLE);
        mLoading.setVisibility(View.GONE);
    }
    }
