/**
 * 
 */
package com.feebe.lib;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.feebe.rings.Const;

/**
 * @author hy
 *
 */
public class SearchDBAdapter {


	public static final String KeyId = "_id";
	public static final String KeyTitle = "title";
	public static final String KeyArtist = "artist";
	public static final String KeyKey = "key";
	public static final String KeyImage = "image";
	public static final String KeyRating = "rating";
	
	public static final String TableHistory = "histories";
	public static final String SQLCreateTable = "create table histories (_id integer primary key autoincrement," 
		+ "title text, artist text, "
		+ "key text, image text, rating text);";
	
	private SQLiteDatabase db;
	private SearchDBHelper dbHelp;
	private String dbName;
	private Context context;
	
	
	private static class SearchDBHelper extends SQLiteOpenHelper{

		public SearchDBHelper(Context context, String name) {
			// TODO Auto-generated constructor stub
			super(context, name, null, 1);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			db.execSQL(SQLCreateTable);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public SearchDBAdapter(Context ctx, String name) {
		dbName = name;
		dbHelp = new SearchDBHelper(ctx, name);
		
	};
	
	public SearchDBAdapter open() {
		db = dbHelp.getWritableDatabase();
		return this;
	}
	
	public void close() {
		db.close();
	}
	
	public long intsertHistory(String title, String artist, String key, String image, String rating) {
		ContentValues cv = new ContentValues();
		cv.put(KeyTitle, title);
		cv.put(KeyArtist, artist);
		cv.put(KeyKey, key);
		cv.put(KeyImage, image);
		cv.put(KeyRating, rating);
		return db.insert(TableHistory, null, cv);
	}

	public Cursor getAllHistories() {
		return db.query(TableHistory, new String[] {KeyTitle, KeyArtist, KeyKey, KeyImage, KeyRating}, null, null, null, null, null);
	}
}
