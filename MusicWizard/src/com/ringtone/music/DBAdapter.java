package com.ringtone.music;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBAdapter {
	public final static int TYPE_SEARCH = 0;
	public final static int TYPE_TITLE = 1;
	public final static int TYPE_ARTIST = 2;
	
	private final static int DB_VERSION = 4;
	private static final String TableHistory = "history";
	private static final String SQLCreateTable = "create table if not exists history "
		+ "(_id integer primary key autoincrement,"
		+ " keyword text,"
		+ " type text,"
		+ " count integer,"
		+ " favorite text);";
	
	private static class SearchDBHelper extends SQLiteOpenHelper {

		public SearchDBHelper(Context context, String name) {
			super(context, name, null, DB_VERSION);
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

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			dropTable(db);
			createTable(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			dropTable(db);
			createTable(db);
		}
	}
	
	SearchDBHelper mOpenHelper;
	
	private static final String[] projection_key = new String[] {"_id","keyword", "type", "count", "favorite"};
	
	public DBAdapter(Context ctx) {
		mOpenHelper = new SearchDBHelper(ctx, "history.db");
	};
	
	public void close() {
		mOpenHelper.close();
	}
	
	public Cursor getHistoryByType(String keyword, int type){
		if (keyword!=null) keyword=keyword.toLowerCase();
		String selection = "type = " + type + " and keyword like \'" + keyword +"%\'";
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		return db.query(TableHistory,projection_key,selection,null,null,null,null);
	}
	
	public Cursor getHistoryByType(int type){
		String selection = "type = " + type;
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		return db.query(TableHistory,projection_key,selection,null,null,null,null);
	}
	
	public void insertHistory(String keyword,int type){
		if (keyword!=null) keyword=keyword.toLowerCase();
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		String selection = "type = " + type + " and keyword ='" + keyword +"\'";
		Cursor c=db.query(TableHistory,projection_key,selection,null,null,null,null);

		if (c==null || c.getCount()==0){
			ContentValues cv = new ContentValues();
			cv.put("keyword", keyword);
			cv.put("type", type);
			cv.put("count", 1);
			db.insert(TableHistory, null, cv);
		} else {
			db.execSQL("update "+TableHistory+ " set count=count+1 where " + selection);
		}
	}
	
	public void clearAll(){
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TableHistory);
		db.execSQL(SQLCreateTable);
	}
}