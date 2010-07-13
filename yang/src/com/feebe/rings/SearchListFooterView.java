package com.feebe.rings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

public class SearchListFooterView extends RelativeLayout{

	private static final String TAG = "SearchListFooterView"; 
	private Context context;
	private Button btn_pre;
	private Button btn_next;
	
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
		btn_pre = (Button) findViewById(R.id.btn_pre);
		btn_next = (Button) findViewById(R.id.btn_next);
	}
	
	public Button getBtnPre() {
		return btn_pre;
	}
	public Button getBtnNext() {
		return btn_next;
	}
}
