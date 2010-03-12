package com.feebe.lib;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbAdapter {
	private static final String Key = "key";
  private static final String TYPE = "type";

	private static final String TableHistory = "histories";
	private static final String SQLCreateTable = "create table histories (_id integer primary key autoincrement," 
		+ "key text, type integer);";
	
  public final static int TYPE_SEARCH = 0;
  public final static int TYPE_TITLE = 1;
  public final static int TYPE_ARTIST = 2;

	private SQLiteDatabase db;
	private final static int DB_VERSION = 2;
	
	private static class SearchDBHelper extends SQLiteOpenHelper{

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
	
	
	public DbAdapter(Context ctx) {
	  SearchDBHelper dbHelp = new SearchDBHelper(ctx, "db");
    db = dbHelp.getWritableDatabase();
	};

	public long intsertHistory(String key, int type) {
		ContentValues cv = new ContentValues();
		cv.put(Key, key);
		cv.put(TYPE, type);
		return db.insert(TableHistory, null, cv);
	}
  private static final String[] projection_key = new String[] {Key};
	
	public Cursor getHistoryByType(String key, int type){
		String selection = "type = " + type + " and title like \'" + key +"%\'";
    return db.query(TableHistory, projection_key, selection, null, null, null, null);
	}
  public Cursor getHistoryByType(int type){
	  String selection = "type = " + type;
	  return db.query(TableHistory, projection_key, selection, null, null, null, null);
	}

}
