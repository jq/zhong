package com.ringtone.music;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class DbAdapter {
	private final static int DB_VERSION = 4;
	
	private static final String TableHistory = "search_history";
	private static final String SQLCreateTable = "create table if not exists search_history (_id integer primary key autoincrement," 
		+ MusicInfo.TYPE_TITLE + " text,"
		+ MusicInfo.TYPE_ARTIST + " text,"
		+ MusicInfo.TYPE_ALBUM + " text,"
		+ MusicInfo.TYPE_TYPE + " text,"
		+ MusicInfo.TYPE_DISPLAYSIZE + " text,"
		+ MusicInfo.TYPE_URL + " text);";

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
		mOpenHelper = new SearchDBHelper(ctx, "music.db");
	};

	private static final String[] projection_key = new String[] {"_id", MusicInfo.TYPE_TITLE, 
		MusicInfo.TYPE_ARTIST,MusicInfo.TYPE_ALBUM,MusicInfo.TYPE_DISPLAYSIZE,
		MusicInfo.TYPE_TYPE,MusicInfo.TYPE_URL
	};
	
	public void insertHistory(MusicInfo mMusicInfo) {
		try {
			SQLiteDatabase db = mOpenHelper.getWritableDatabase();
			ContentValues cv = new ContentValues();
			cv.put(MusicInfo.TYPE_TITLE, mMusicInfo.getTitle());
			cv.put(MusicInfo.TYPE_ARTIST, mMusicInfo.getArtist());
			cv.put(MusicInfo.TYPE_ALBUM, mMusicInfo.getAlbum());
			cv.put(MusicInfo.TYPE_DISPLAYSIZE, mMusicInfo.getDisplayFileSize());
			cv.put(MusicInfo.TYPE_TYPE, mMusicInfo.getType());
			cv.put(MusicInfo.TYPE_URL, mMusicInfo.getUrl());
			
			db.insert(TableHistory, null, cv);
		} catch (SQLiteException e) {
				e.printStackTrace();
		}
	}
	
	public Cursor getHistory(){
		try{
            SQLiteDatabase db = mOpenHelper.getReadableDatabase();
            return db.query(TableHistory, projection_key, null, null, null, null, null);
		} catch(Exception e) {
			return null;
		}
	}
	
	public void dropall() {   
        try {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();   
            db.execSQL("DROP TABLE IF EXISTS " + TableHistory);
            db.execSQL(SQLCreateTable);
        } catch (SQLException e) {   
			e.printStackTrace();
        }
    }
}