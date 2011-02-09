package com.ringtone.music;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class SearchAdapter extends CursorAdapter {
	private Context mContext;
	
	public SearchAdapter(Context context, Cursor c){
		super(context,c);
		mContext = context;
	}
	
	@Override
	public void bindView(View arg0, Context arg1, Cursor arg2) {
		if (arg2 == null) return;
	    ((TextView) arg0).setText(arg2.getString(1));
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		if (cursor == null)
			return null;
		final LayoutInflater inflater = LayoutInflater.from(context);
		final TextView view = (TextView)inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
		view.setText(cursor.getString(1));
		return view;
	}
	
	@Override
	public CharSequence convertToString(Cursor cursor) {
		if (cursor == null)
			return "";
		return cursor.getString(1);
	}
	
	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
		 if (getFilterQueryProvider() != null)
		      return getFilterQueryProvider().runQuery(constraint);
		 
		 if (constraint != null){
			 HistoryAdapter adapter = HistoryAdapter.getInstance(mContext);
			 return adapter.getHistoryByType((String) constraint, HistoryAdapter.TYPE_SEARCH);
		 }
		 return null;
	}
}
