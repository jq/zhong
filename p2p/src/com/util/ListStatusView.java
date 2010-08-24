package com.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.limegroup.gnutella.R;

public class ListStatusView extends LinearLayout {
    private View mFooter;
    private ProgressBar mProgress;
    private TextView mMessage;
    
    public enum Status {
    	OFFLINE,
    	LOAD_MORE,
    	SEARCHING,
    	NO_RESULT
    };
    
    public ListStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mFooter = findViewById(R.id.footer);
        mProgress = (ProgressBar) findViewById(R.id.progress);
        mMessage = (TextView) findViewById(R.id.message);
    }

    public void setStatusSearching() {
    	mFooter.setVisibility(View.VISIBLE);
    	mProgress.setVisibility(View.VISIBLE);
    	mMessage.setVisibility(View.VISIBLE);
    	//mMessage.setText("Searching \"" + query + "\"");
    	mMessage.setText("Please wait while we search...");
    }
    
    public void setStatusLoadMore() {
    	mFooter.setVisibility(View.VISIBLE);
    	mProgress.setVisibility(View.GONE);
    	mMessage.setVisibility(View.VISIBLE);
    	mMessage.setText("Tap to load more results");
    }
    
    public void setStatusNoResult() {
    	mFooter.setVisibility(View.VISIBLE);
    	mProgress.setVisibility(View.GONE);
    	mMessage.setVisibility(View.VISIBLE);
    	mMessage.setText("Sorry, we didn't find any result");
    }
}
