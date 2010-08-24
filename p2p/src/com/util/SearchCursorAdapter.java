package com.util;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class SearchCursorAdapter extends CursorAdapter {
	private int searchType;
	
	public SearchCursorAdapter(Context context, Cursor c, int searchtype) {
		super(context, c);
		this.searchType = searchtype;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		((TextView) view).setText(cursor.getString(1));
	}

	@Override
	public CharSequence convertToString(Cursor cursor) {
		return cursor.getString(1);
	}

	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
		if (constraint != null) {
		  return Constants.dbAdapter.getHistoryByType(constraint.toString(), searchType);
    }
    return null;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(context);
        final TextView view = (TextView) inflater.inflate(
                android.R.layout.simple_dropdown_item_1line, parent, false);
        
        view.setText(cursor.getString(1));
        return view;
		
	}

}
