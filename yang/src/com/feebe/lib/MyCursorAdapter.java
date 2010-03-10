package com.feebe.lib;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class MyCursorAdapter extends CursorAdapter {

	private int columnIndex;
	private SearchDBAdapter searchDBAdapter;
	private String searchType;
	
	public MyCursorAdapter(Context context, Cursor c, int col, SearchDBAdapter s, String searchtype) {
		super(context, c);
		// TODO Auto-generated constructor stub
		this.columnIndex = col;
		this.searchDBAdapter = s;
		this.searchType = searchtype;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// TODO Auto-generated method stub
		((TextView) view).setText(cursor.getString(columnIndex));
	}

	@Override
	public CharSequence convertToString(Cursor cursor) {
		// TODO Auto-generated method stub
		return cursor.getString(columnIndex);
	}

	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
		// TODO Auto-generated method stub
		if (constraint != null) {
            if(searchType.equals(SearchDBAdapter.KeyArtist)){
            	return searchDBAdapter.getHistoryByKeyArtist(constraint.toString());
            }
            else{
            	return searchDBAdapter.getHistoryByKeyTitle(constraint.toString());
            }
        }
        else {
            return null;
        }
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		// TODO Auto-generated method stub
		final LayoutInflater inflater = LayoutInflater.from(context);
        final TextView view = (TextView) inflater.inflate(
                android.R.layout.simple_dropdown_item_1line, parent, false);
        view.setText(cursor.getString(columnIndex));
        return view;
		
	}

}
