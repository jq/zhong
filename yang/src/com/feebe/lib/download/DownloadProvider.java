package com.feebe.lib.download;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.CrossProcessCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.CursorWrapper;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.util.Config;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;


/**
 * Allows application to interact with the download manager.
 */
public class DownloadProvider {
    /** Database filename */
    private static final String DB_NAME = "downloads.db";
    /** Current database version */
    private static final int DB_VERSION = 1;
    /** Name of table in the database */
    private static final String DB_TABLE = "downloads";

    private static final String[] sAppReadableColumnsArray = new String[] {
        Downloads._ID,
        Downloads._DATA,
        Downloads.COLUMN_TOTAL_BYTES,
        Downloads.COLUMN_CURRENT_BYTES,
        Downloads.COLUMN_TITLE,
        Downloads.COLUMN_DESCRIPTION,
        Downloads.COLUMN_ERROR
    };

    private static HashSet<String> sAppReadableColumnsSet;
    static {
        sAppReadableColumnsSet = new HashSet<String>();
        for (int i = 0; i < sAppReadableColumnsArray.length; ++i) {
            sAppReadableColumnsSet.add(sAppReadableColumnsArray[i]);
        }
    }

    /** The database that lies underneath this content provider */
    private SQLiteOpenHelper mOpenHelper = null;


    /**
     * Creates and updated database on demand when opening it.
     * Helper class to create database the first time the provider is
     * initialized and upgrade it when a new version of the provider needs
     * an updated version of the database.
     */
    private final class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(final Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        /**
         * Creates database the first time we try to open it.
         */
        @Override
        public void onCreate(final SQLiteDatabase db) {
            createTable(db);
        }

        /**
         * Updates the database format when a content provider is used
         * with a database that was created with a different format.
         */
        // Note: technically, this could also be a downgrade, so if we want
        //       to gracefully handle upgrades we should be careful about
        //       what to do on downgrades.
        @Override
        public void onUpgrade(final SQLiteDatabase db, int oldV, final int newV) {
            dropTable(db);
            createTable(db);
        }
    }
    private Context mContext;
    public DownloadProvider(final Context context) {
        mOpenHelper = new DatabaseHelper(context);
        mContext = context;
    }

    /**
     * Creates the table that'll hold the download information.
     */
    private void createTable(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE " + DB_TABLE + "(" +
                    Downloads._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    Downloads.COLUMN_URI + " TEXT, " +
                    Constants.RETRY_AFTER_X_REDIRECT_COUNT + " INTEGER, " +
                    Downloads._DATA + " TEXT, " +
                    Constants.FAILED_CONNECTIONS + " INTEGER, " +
                    Downloads.COLUMN_TOTAL_BYTES + " INTEGER, " +
                    Downloads.COLUMN_CURRENT_BYTES + " INTEGER, " +
                    Downloads.COLUMN_TITLE + " TEXT, " +
                    Downloads.COLUMN_DESCRIPTION + " TEXT, " +
                    Downloads.COLUMN_ERROR + " TEXT, " +
                    ");");
        } catch (SQLException ex) {
            Log.e(Constants.TAG, "couldn't create table in downloads database");
            throw ex;
        }
    }

    /**
     * Deletes the table that holds the download information.
     */
    private void dropTable(SQLiteDatabase db) {
        try {
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE);
        } catch (SQLException ex) {
            Log.e(Constants.TAG, "couldn't drop table in downloads database");
            throw ex;
        }
    }

    /**
     * Inserts a row in the database
     */
    public Uri insert(final ContentValues values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        ContentValues filteredValues = new ContentValues();

        copyString(Downloads.COLUMN_URI, values, filteredValues);
        copyString(Downloads.COLUMN_TITLE, values, filteredValues);
        copyString(Downloads.COLUMN_DESCRIPTION, values, filteredValues);

        mContext.startService(new Intent(mContext, DownloadService.class));

        long rowID = db.insert(DB_TABLE, null, filteredValues);

        Uri ret = null;

        if (rowID != -1) {
        	mContext.startService(new Intent(mContext, DownloadService.class));
        } else {
            if (Config.LOGD) {
                Log.d(Constants.TAG, "couldn't insert into downloads database");
            }
        }

        return ret;
    }

    public Cursor query(String[] projection,
             final String selection, final String[] selectionArgs,
             final String sort) {
    	SQLiteDatabase db = mOpenHelper.getReadableDatabase();
 
        Cursor ret = db.query(DB_TABLE, projection, selection, selectionArgs,
                              null, null, sort);
        return ret;
    }

    public int update(final ContentValues values,
            final String where, final String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        int count;
        count = db.update(DB_TABLE, values, where, whereArgs);
        return count;
    }

    public int delete(final String where,
            final String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        count = db.delete(DB_TABLE, where, whereArgs);
        return count;
    }


    private static final void copyInteger(String key, ContentValues from, ContentValues to) {
        Integer i = from.getAsInteger(key);
        if (i != null) {
            to.put(key, i);
        }
    }

    private static final void copyBoolean(String key, ContentValues from, ContentValues to) {
        Boolean b = from.getAsBoolean(key);
        if (b != null) {
            to.put(key, b);
        }
    }

    private static final void copyString(String key, ContentValues from, ContentValues to) {
        String s = from.getAsString(key);
        if (s != null) {
            to.put(key, s);
        }
    }
}
