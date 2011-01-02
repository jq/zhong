package com.cinla.ringtone;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

public class SearchListFooterView extends RelativeLayout{

	private static final String TAG = "SearchListFooterView"; 
	private Context context;
	private ImageButton btn_prev;
	private ImageButton btn_next;

	public SearchListFooterView(Context context) {
		super(context);
		initialize(context);
	}

	public SearchListFooterView(Context context, AttributeSet attrs) {
		super(context,attrs);
		initialize(context);
	}

	private void initialize(Context context) {
		this.context = context;
		View view = LayoutInflater.from(context).inflate(R.layout.list_footer, null);
		addView(view);
		btn_prev = (ImageButton) findViewById(R.id.btn_prev);
		btn_next = (ImageButton) findViewById(R.id.btn_next);
	}
	
	public ImageButton getBtnPre() {
		return btn_prev;
	}
	public ImageButton getBtnNext() {
		return btn_next;
	}
}
