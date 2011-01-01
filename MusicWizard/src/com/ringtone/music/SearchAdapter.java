package com.ringtone.music;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class SearchAdapter extends CursorAdapter {
	private ContentResolver mContent;
	
	
	public SearchAdapter(Context context, Cursor c){
		super(context,c);
		mContent = context.getContentResolver();
	}
	
	@Override
	public void bindView(View arg0, Context arg1, Cursor arg2) {
		// TODO Auto-generated method stub
	    ((TextView) arg0).setText(arg2.getString(1));
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		// TODO Auto-generated method stub
		final LayoutInflater inflater = LayoutInflater.from(context);
		final TextView view = (TextView)inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
		view.setText(cursor.getString(1));
		return view;
	}
	
	@Override
	public CharSequence convertToString(Cursor cursor) {
		return cursor.getString(1);
	}
	
	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
		 if (getFilterQueryProvider() != null)
		      return getFilterQueryProvider().runQuery(constraint);
		 
		 StringBuilder buffer = null;
		 String[] args = null;
		 if (constraint != null){
			 return Constants.dbadapter.getHistoryByType((String) constraint, DBAdapter.TYPE_SEARCH);
		 }
		 return null;
	}
}
