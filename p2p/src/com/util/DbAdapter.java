package com.util;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class DbAdapter {
	private static final String TYPE = "type";

	private static final String TableHistory = "histories";
	private static final String SQLCreateTable = "create table histories (_id integer primary key autoincrement," 
		+ SearchManager.SUGGEST_COLUMN_TEXT_1 + " text, type integer, UNIQUE(" + SearchManager.SUGGEST_COLUMN_TEXT_1 +"));";

	public final static int TYPE_SEARCH = 0;
	public final static int TYPE_TITLE = 1;
	public final static int TYPE_ARTIST = 2;
	public final static int TYPE_ALBUM = 3;

	private final static int DB_VERSION = 4;

	private static class SearchDBHelper extends SQLiteOpenHelper {

		public SearchDBHelper(Context context, String name) {
			super(context, name, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			createTable(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			dropTable(db);
			createTable(db);
		}
		private void createTable(SQLiteDatabase db) {
			db.execSQL(SQLCreateTable);
		}

		private void dropTable(SQLiteDatabase db) {
			try {
				db.execSQL("DROP TABLE IF EXISTS " + TableHistory);
			} catch (SQLException ex) {
			}
		}
	}

    SearchDBHelper mOpenHelper;

	public DbAdapter(Context ctx) {
		mOpenHelper = new SearchDBHelper(ctx, "db");
	};

	public void insertHistory(String key, int type) {
        try {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(SearchManager.SUGGEST_COLUMN_TEXT_1, key);
            cv.put(TYPE, type);
            db.insert(TableHistory, null, cv);
        } catch (SQLiteException e) {
        	e.printStackTrace();
        }
	}

	private static final String[] projection_key = new String[] {"_id", SearchManager.SUGGEST_COLUMN_TEXT_1, 
		SearchManager.SUGGEST_COLUMN_TEXT_1 + " AS \"" + SearchManager.SUGGEST_COLUMN_INTENT_DATA + "\""
	};

	public Cursor getHistoryByType(String key, int type){
		String selection = "type = " + type + " and " + SearchManager.SUGGEST_COLUMN_TEXT_1 + " like \'" + key +"%\'";
		try {
            SQLiteDatabase db = mOpenHelper.getReadableDatabase();
			return db.query(TableHistory, projection_key, selection, null, null, null, null);
		} catch (Exception e) {
			return null;
		}

	}

	public Cursor getHistoryByType(int type){
		String selection = "type = " + type;
		try {
            SQLiteDatabase db = mOpenHelper.getReadableDatabase();
			return db.query(TableHistory, projection_key, selection, null, null, null, null);
		} catch(Exception e) {
			return null;
		}
	}
}
