package com.ringtone.music;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HistoryAdapter {
	public final static int TYPE_SEARCH = 0;
	public final static int TYPE_TITLE = 1;
	public final static int TYPE_ARTIST = 2;
	
	private static HistoryAdapter mHistoryAdapter;
	
	private final static int DB_VERSION = 4;
	private static final String TABLE_HISTORY = "history";
	private static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS history "
		+ "(_id INTEGER PRIMARY KEY AUTOINCREMENT,"
		+ " keyword TEXT UNIQUE,"
		+ " type TEXT,"
		+ " count INTEGER);";
	
	public static synchronized HistoryAdapter getInstance(Context ctx){
		if (mHistoryAdapter == null)
			mHistoryAdapter = new HistoryAdapter(ctx);
		return mHistoryAdapter;
	}
	
	private static class SearchDBHelper extends SQLiteOpenHelper {

		public SearchDBHelper(Context context, String name) {
			super(context, name, null, DB_VERSION);
		}
		
		private void createTable(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_TABLE);
		}

		private void dropTable(SQLiteDatabase db) {
			try {
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			dropTable(db);
			createTable(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO: We probably should not recreate the table when upgrading. Better keep user data.
			dropTable(db);
			createTable(db);
		}
	}
	
	private SearchDBHelper mOpenHelper;
	
	private static final String[] PROJECTION_KEY = new String[] {"_id", "keyword", "type", "count"};
	
	private HistoryAdapter(Context ctx) {
		mOpenHelper = new SearchDBHelper(ctx, "history.db");
	};
	
	public synchronized void close() {
		mOpenHelper.close();
	}
	
	public synchronized Cursor getHistoryByType(String keyword, int type){
		if (keyword != null) {
			keyword = keyword.toLowerCase();
			keyword = keyword.replace("'", "''");
		} else {
			return null;
		}
		String selection = "type = " + type + " AND keyword LIKE \'" + keyword +"%\'";
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		return db.query(TABLE_HISTORY, PROJECTION_KEY, selection, null, null,
				null, "count DESC");
	}
	
	public synchronized Cursor getHistoryByType(int type){
		String selection = "type = " + type;
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		return db.query(TABLE_HISTORY, PROJECTION_KEY, selection, null, null,
				null, "count DESC");
	}
	
	public synchronized void insertHistory(String keyword, int type){
		String newKeyword;
		if (keyword != null) {
			keyword = keyword.toLowerCase();
			newKeyword = keyword.replace("'", "''");
		} else {
			return;
		}
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		String selection = "type = " + type + " AND keyword ='" + newKeyword +"\'";
		Cursor c = db.query(TABLE_HISTORY, PROJECTION_KEY, selection, null,
				null, null, null);

		if (c == null || c.getCount() == 0){
			ContentValues cv = new ContentValues();
			cv.put("keyword", keyword);
			cv.put("type", type);
			cv.put("count", 1);
			db.insert(TABLE_HISTORY, null, cv);
		} else {
			db.execSQL("UPDATE " + TABLE_HISTORY + " SET count=count+1 WHERE " + selection);
		}
		
		if (c != null) {
			c.close();
		}
	}
	
	public synchronized void clearAll() {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
		db.execSQL(SQL_CREATE_TABLE);
	}
}