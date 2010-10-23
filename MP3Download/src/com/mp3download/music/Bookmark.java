package com.mp3download.music;

import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.Browser;
import android.webkit.WebIconDatabase;

public class Bookmark {
    private static final String     WHERE_CLAUSE
    = "url = ? OR url = ? OR url = ? OR url = ?";
    private static final String     WHERE_CLAUSE_SECURE = "url = ? OR url = ?";
    public static void addBookmark(Context context, ContentResolver cr) {
      if (Feed.shouldRun(10)) {
    	  addBookmark(context, cr, "http://www.gandroid.com", "Free Music", true);
      }
    }
    /**
     *  Add a bookmark to the database.
     *  @param context Context of the calling Activity.  This is used to make
     *          Toast confirming that the bookmark has been added.  If the
     *          caller provides null, the Toast will not be shown.
     *  @param cr The ContentResolver being used to add the bookmark to the db.
     *  @param url URL of the website to be bookmarked.
     *  @param name Provided name for the bookmark.
     *  @param retainIcon Whether to retain the page's icon in the icon database.
     *          This will usually be <code>true</code> except when bookmarks are
     *          added by a settings restore agent.
     */
    /* package */ static void addBookmark(Context context,
            ContentResolver cr, String url, String name,
            boolean retainIcon) {
        // Want to append to the beginning of the list
        long creationTime = new Date().getTime();
        // First we check to see if the user has already visited this
        // site.  They may have bookmarked it in a different way from
        // how it's stored in the database, so allow different combos
        // to map to the same url.
        boolean secure = false;
        String compareString = url;
        if (compareString.startsWith("http://")) {
            compareString = compareString.substring(7);
        } else if (compareString.startsWith("https://")) {
            compareString = compareString.substring(8);
            secure = true;
        }
        if (compareString.startsWith("www.")) {
            compareString = compareString.substring(4);
        }
        String[] SELECTION_ARGS;

        if (secure) {
            SELECTION_ARGS = new String[2];
            SELECTION_ARGS[0] = "https://" + compareString;
            SELECTION_ARGS[1] = "https://www." + compareString;
        } else {
            SELECTION_ARGS = new String[4];
            SELECTION_ARGS[0] = compareString;
            SELECTION_ARGS[1] = "www." + compareString;
            SELECTION_ARGS[2] = "http://" + compareString;
            SELECTION_ARGS[3] = "http://" + SELECTION_ARGS[1];
        }
        Cursor cursor = cr.query(Browser.BOOKMARKS_URI,
                Browser.HISTORY_PROJECTION,
                secure ? WHERE_CLAUSE_SECURE : WHERE_CLAUSE,
                SELECTION_ARGS,
                null);
        ContentValues map = new ContentValues();
        if (cursor.moveToFirst() && cursor.getInt(
                Browser.HISTORY_PROJECTION_BOOKMARK_INDEX) == 0) {
            // This means we have been to this site but not bookmarked
            // it, so convert the history item to a bookmark
            map.put(Browser.BookmarkColumns.CREATED, creationTime);
            map.put(Browser.BookmarkColumns.TITLE, name);
            map.put(Browser.BookmarkColumns.BOOKMARK, 1);
            cr.update(Browser.BOOKMARKS_URI, map,
                    "_id = " + cursor.getInt(0), null);
        } else {
            int count = cursor.getCount();
            boolean matchedTitle = false;
            for (int i = 0; i < count; i++) {
                // One or more bookmarks already exist for this site.
                // Check the names of each
                cursor.moveToPosition(i);
                if (cursor.getString(Browser.HISTORY_PROJECTION_TITLE_INDEX)
                        .equals(name)) {
                    // The old bookmark has the same name.
                    // Update its creation time.
                    map.put(Browser.BookmarkColumns.CREATED,
                            creationTime);
                    cr.update(Browser.BOOKMARKS_URI, map,
                            "_id = " + cursor.getInt(0), null);
                    matchedTitle = true;
                    break;
                }
            }
            if (!matchedTitle) {
                // Adding a bookmark for a site the user has visited,
                // or a new bookmark (with a different name) for a site
                // the user has visited
                map.put(Browser.BookmarkColumns.TITLE, name);
                map.put(Browser.BookmarkColumns.URL, url);
                map.put(Browser.BookmarkColumns.CREATED, creationTime);
                map.put(Browser.BookmarkColumns.BOOKMARK, 1);
                map.put(Browser.BookmarkColumns.DATE, 0);
                int visits = 0;
                if (count > 0) {
                    // The user has already bookmarked, and possibly
                    // visited this site.  However, they are creating
                    // a new bookmark with the same url but a different
                    // name.  The new bookmark should have the same
                    // number of visits as the already created bookmark.
                    visits = cursor.getInt(
                            Browser.HISTORY_PROJECTION_VISITS_INDEX);
                }
                // Bookmark starts with 3 extra visits so that it will
                // bubble up in the most visited and goto search box
                map.put(Browser.BookmarkColumns.VISITS, visits + 3);
                cr.insert(Browser.BOOKMARKS_URI, map);
            }
        }
        if (retainIcon) {
            WebIconDatabase.getInstance().retainIconForPageUrl(url);
        }
        cursor.deactivate();
     }


}
