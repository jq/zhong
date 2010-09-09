package com.ringtone.music;

import java.util.ArrayList;

import com.ringtone.music.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ControlBarView extends LinearLayout {
	View v;
  
    public ControlBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOrientation(HORIZONTAL);
    }
    
    public void setText(String txt){
    	TextView mtv = (TextView) v.findViewById(R.id.text);
    	mtv.setText(txt);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        v=((Activity)getContext()).getLayoutInflater().inflate(R.layout.control_bar, this);
		ImageButton btn_pre = (ImageButton) v.findViewById(R.id.pre);
		ImageButton btn_next = (ImageButton) v.findViewById(R.id.next);
		ImageButton btn_refresh = (ImageButton) v.findViewById(R.id.refresh);
		ImageButton btn_download = (ImageButton) v.findViewById(R.id.download);
		ImageButton btn_head = (ImageButton) v.findViewById(R.id.head);
		btn_pre.setImageDrawable(getResources().getDrawable(R.drawable.button_pre));
		btn_next.setImageDrawable(getResources().getDrawable(R.drawable.button_next));
		btn_refresh.setImageDrawable(getResources().getDrawable(R.drawable.button_refresh));
		btn_download.setImageDrawable(getResources().getDrawable(R.drawable.button_download));
		btn_head.setImageDrawable(getResources().getDrawable(R.drawable.superstar));
		btn_pre.setBackgroundColor(Color.BLACK);
		btn_next.setBackgroundColor(Color.BLACK);
		btn_refresh.setBackgroundColor(Color.BLACK);
		btn_download.setBackgroundColor(Color.BLACK);
		btn_head.setBackgroundColor(Color.BLACK);
		
		btn_download.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if (event.getAction()==MotionEvent.ACTION_DOWN){
					((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.button_download_touch));
				}if(event.getAction() == MotionEvent.ACTION_UP){
					((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.button_download));
				}
				return false;
			}
		});
		
		btn_pre.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if (event.getAction()==MotionEvent.ACTION_DOWN){
					((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.button_pre_touch));
				}if(event.getAction() == MotionEvent.ACTION_UP){
					((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.button_pre));
				}
				return false;
			}
		});
		
		btn_next.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if (event.getAction()==MotionEvent.ACTION_DOWN){
					((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.button_next_touch));
				}if(event.getAction() == MotionEvent.ACTION_UP){
					((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.button_next));
				}
				return false;
			}
		});
		
		btn_refresh.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if (event.getAction()==MotionEvent.ACTION_DOWN){
					((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.button_refresh_touch));
				}if(event.getAction() == MotionEvent.ACTION_UP){
					((ImageButton) v).setImageDrawable(getResources().getDrawable(R.drawable.button_refresh));
				}
				return false;
			}
		});
    }
}