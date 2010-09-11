package com.ringtone.music;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class DbAdapter {
	private final static int DB_VERSION = 4;
	
	private final static int MAX_RECORD_NUMBER = 2;
	
	private static final String TableHistory = "search_history";
	private static final String TableInfo = "infotable";
	private static final String SQLCreateTable = "create table if not exists search_history (_id integer primary key autoincrement,"
		+ MusicInfo.TYPT_PAGE + " integer,"
		+ MusicInfo.TYPE_TITLE + " text,"
		+ MusicInfo.TYPE_ARTIST + " text,"
		+ MusicInfo.TYPE_ALBUM + " text,"
		+ MusicInfo.TYPE_TYPE + " text,"
		+ MusicInfo.TYPE_DISPLAYSIZE + " text,"
		+ MusicInfo.TYPE_URL + " text);";
	private static final String SQLCreateInfo = "create table if not exists infotable (maxpage integer, minpage integer);";
	
	private static int mMaxPageNum;		// 最大页码
	private static int mMinPageNum;		// 最小页码

	private static class SearchDBHelper extends SQLiteOpenHelper {

		public SearchDBHelper(Context context, String name) {
			super(context, name, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			dropTable(db);
			createTable(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			dropTable(db);
			createTable(db);
		}
		
		private void createTable(SQLiteDatabase db) {
			db.execSQL(SQLCreateTable);
			db.execSQL(SQLCreateInfo);
		}

		private void dropTable(SQLiteDatabase db) {
			try {
				db.execSQL("DROP TABLE IF EXISTS " + TableHistory);
				db.execSQL("DROP TABLE IF EXISTS " + TableInfo);
			} catch (SQLException ex) {
			}
		}
	}

    SearchDBHelper mOpenHelper;

	public DbAdapter(Context ctx) {
		mOpenHelper = new SearchDBHelper(ctx, "music.db");
	};

	private static final String[] projection_key = new String[] {"_id", MusicInfo.TYPT_PAGE, MusicInfo.TYPE_TITLE, 
		MusicInfo.TYPE_ARTIST,MusicInfo.TYPE_ALBUM,MusicInfo.TYPE_DISPLAYSIZE,
		MusicInfo.TYPE_TYPE,MusicInfo.TYPE_URL
	};
	
	public void insertHistory(ArrayList<MusicInfo> mp3List){
		if (mp3List == null)
			return;
		
		try{
			Iterator it=mp3List.iterator();
			SQLiteDatabase db = mOpenHelper.getWritableDatabase();
			int maxpage=getMaxPageNum();
			
			if (maxpage==-1){
				db.execSQL("delete from infotable;");
				db.execSQL("insert into infotable values(1,0);");
				maxpage++;
			} else {
				db.execSQL("update infotable set maxpage = maxpage +1;");
			}
			
			maxpage++;

			while(it.hasNext()){
				MusicInfo mi=(MusicInfo) it.next();
				ContentValues cv = new ContentValues();
				cv.put(MusicInfo.TYPE_TITLE, mi.getTitle());
				cv.put(MusicInfo.TYPE_ARTIST, mi.getArtist());
				cv.put(MusicInfo.TYPE_ALBUM, mi.getAlbum());
				cv.put(MusicInfo.TYPE_DISPLAYSIZE, mi.getDisplayFileSize());
				cv.put(MusicInfo.TYPE_TYPE, mi.getType());
				cv.put(MusicInfo.TYPE_URL, mi.getUrl());
				cv.put(MusicInfo.TYPT_PAGE, maxpage);
				db.insert(TableHistory, null, cv);
			}

			
//			暂不删除下载下来的记录
//			if (getMaxPageNum()-getMinPageNum()>=MAX_RECORD_NUMBER){
//				String deleteSQL="delete from "+TableHistory+" where "+MusicInfo.TYPT_PAGE+ " <="+Integer.toString(getMaxPageNum()-MAX_RECORD_NUMBER);
//				db.execSQL(deleteSQL);
//				String updateSQL="update infotable set mixpage ="+Integer.toString(getMaxPageNum()-MAX_RECORD_NUMBER+1);
//				db.execSQL(updateSQL);
//			}
		} catch (SQLiteException e) {
				e.printStackTrace();
		}
	}
	
//	public Cursor getHistory(){
//		try{
//			int MaxPageNum = getMaxPageNum();
//			return getHistoryByPage(MaxPageNum);
//		} catch(Exception e) {
//			return null;
//		}
//	}
//	
	public void dropall() {
        try {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();   
            db.execSQL("DROP TABLE IF EXISTS " + TableHistory);
            db.execSQL("DROP TABLE IF EXISTS " + TableInfo);
            db.execSQL(SQLCreateTable);
            db.execSQL(SQLCreateInfo);
        } catch (SQLException e) {   
			e.printStackTrace();
        }
    }
	
	public Cursor getHistoryByPage(int pagenum){
		try{
            SQLiteDatabase db = mOpenHelper.getReadableDatabase();
            String selection = "("+ MusicInfo.TYPT_PAGE+ " = ? )";
            ArrayList<String> args = new ArrayList<String>();
            args.add(Integer.toString(pagenum));
            String[] argsArray = args.toArray(new String[args.size()]);
            return db.query(TableHistory, projection_key, selection, argsArray, null, null, null);
		}catch(Exception e) {
			return null;
		}
	}
	
	public int getMaxPageNum(){
		try{
            SQLiteDatabase db = mOpenHelper.getReadableDatabase();
            Cursor c;
            c=db.query(TableInfo,new String[] {"maxpage"},null,null,null,null,null);
            if (c!= null && 0 != c.getCount()){
                c.moveToFirst();
            	int i=c.getColumnIndex("maxpage");
            	return c.getInt(i);
            } else {
            	return -1;
            }
		}catch(Exception e) {
			return -1;
		}
	}

	public void initCache(){
		int maxpage=getMaxPageNum();
		if (maxpage>0){
			SQLiteDatabase db = mOpenHelper.getWritableDatabase();
			String deleteSQL="delete from "
				+ TableHistory
				+ " where " + MusicInfo.TYPT_PAGE+ " < "+Integer.toString(maxpage);
			db.execSQL(deleteSQL);
			db.execSQL("update "+TableHistory+ " set "+MusicInfo.TYPT_PAGE+"=1");
			db.execSQL("update "+TableInfo+ " set maxpage = 1");
		}
	}
}