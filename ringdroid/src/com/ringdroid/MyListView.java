package com.ringdroid;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ListAdapter;
import android.widget.ListView;

public class MyListView 
	extends ListView{

	public MyListView(Context context, AttributeSet attrs) {
		super(context,attrs);
	}
	
	@Override
	protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {   
	    super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);   
	    if (gainFocus && previouslyFocusedRect != null) {   
	        final ListAdapter adapter = getAdapter();   
	        final int count = adapter.getCount();   
	        switch (direction) {   
	            case FOCUS_DOWN:   
	                for (int i = 0; i < count; i++) {   
	                    if (!adapter.isEnabled(i)) {   
	                        continue;   
	                    }   
	                    setSelection(i);   
	                    break;   
	                }   
	                break;   
	            case FOCUS_UP:   
	                for (int i = count-1; i>=0; i--) {   
	                    if (!adapter.isEnabled(i)) {   
	                        continue;   
	                    }   
	                    setSelection(i);   
	                    break;   
	                }   
	                break;   
	            default:   
	                break;   
	        }   
	    }   
	}  
}
