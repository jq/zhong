package com.macrohard.musicbug;

import com.macrohard.musicbug.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class ListFooterView extends LinearLayout implements View.OnClickListener {
    private View mLoading;
    private View mNetworkError;
    private View mRetry;

    enum Status {
    	LOADED,
    	LOADING,
    	ERROR
    };
    
    public static interface RetryNetworkInterface {
    	void retryNetwork();
    }
    
    public ListFooterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mLoading = findViewById(R.id.loading);
        mNetworkError = findViewById(R.id.network_error);
        mRetry = findViewById(R.id.retry_button);
        mRetry.setOnClickListener(this);
    }

    public void onClick(View v) {
        ((RetryNetworkInterface) v.getTag()).retryNetwork();
    }

    public void bind(Status status, RetryNetworkInterface retry) {
        mRetry.setTag(retry);

        switch (status) {
            case LOADED:
                throw new IllegalStateException();
            case LOADING:
                mLoading.setVisibility(View.VISIBLE);
                mNetworkError.setVisibility(View.GONE);
                break;
            case ERROR:
                mNetworkError.setVisibility(View.VISIBLE);
                mLoading.setVisibility(View.GONE);
                break;
        }
    }
}
