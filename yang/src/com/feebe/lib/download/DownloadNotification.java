package com.feebe.lib.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;

import java.util.HashMap;

import com.feebe.lib.Const;

/**
 * This class handles the updating of the Notification Manager for the 
 * cases where there is an ongoing download. Once the download is complete
 * (be it successful or unsuccessful) it is no longer the responsibility 
 * of this component to show the download in the notification manager.
 *
 */
public class DownloadNotification {
    public static int status_bar_ongoing_event_progress_bar;
    Context mContext;
    public NotificationManager mNotificationMgr;
    HashMap <String, NotificationItem> mNotifications;
    
    static final String LOGTAG = "DownloadNotification";
    static final String WHERE_RUNNING = 
        "(" + Downloads.COLUMN_STATUS + " >= '100') AND (" +
        Downloads.COLUMN_STATUS + " <= '199') AND (";
    static final String WHERE_COMPLETED =
        Downloads.COLUMN_STATUS + " >= '200' AND ";
    
    
    /**
     * This inner class is used to collate downloads that are owned by
     * the same application. This is so that only one notification line
     * item is used for all downloads of a given application.
     *
     */
    static class NotificationItem {
        int mId;  // This first db _id for the download for the app
        int mTotalCurrent = 0;
        int mTotalTotal = 0;
        int mTitleCount = 0;
        String mPackageName;  // App package name
        String mDescription;
        String[] mTitles = new String[2]; // download titles.
        
        /*
         * Add a second download to this notification item.
         */
        void addItem(String title, int currentBytes, int totalBytes) {
            mTotalCurrent += currentBytes;
            if (totalBytes <= 0 || mTotalTotal == -1) {
                mTotalTotal = -1;
            } else {
                mTotalTotal += totalBytes;
            }
            if (mTitleCount < 2) {
                mTitles[mTitleCount] = title;
            }
            mTitleCount++;
        }
    }
        
    
    /**
     * Constructor
     * @param ctx The context to use to obtain access to the 
     *            Notification Service
     */
    DownloadNotification(Context ctx) {
        mContext = ctx;
        mNotificationMgr = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifications = new HashMap<String, NotificationItem>();
    }
    
    /*
     * Update the notification ui. 
     */
    public void updateNotification(Cursor active, Cursor complete) {
        updateActiveNotification(active);
        updateCompletedNotification(complete);
    }

    private void updateActiveNotification(Cursor c) {
        if (c == null) {
            return;
        }
        
        // Columns match projection in query above
        final int idColumn = 0;
        final int titleColumn = 1;
        final int descColumn = 2;
        final int ownerColumn = 3;
        final int classOwnerColumn = 4;
        final int currentBytesColumn = 5;
        final int totalBytesColumn = 6;
        final int statusColumn = 7;

        // Collate the notifications
        mNotifications.clear();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String packageName = c.getString(ownerColumn);
            int max = c.getInt(totalBytesColumn);
            int progress = c.getInt(currentBytesColumn);
            long id = c.getLong(idColumn);
            String title = c.getString(titleColumn);
            /*
            if (title == null || title.length() == 0) {
                title = mContext.getResources().getString(
                        R.string.download_unknown_title);
            }*/
            if (mNotifications.containsKey(packageName)) {
                mNotifications.get(packageName).addItem(title, progress, max);
            } else {
                NotificationItem item = new NotificationItem();
                item.mId = (int) id;
                item.mPackageName = packageName;
                item.mDescription = c.getString(descColumn);
                String className = c.getString(classOwnerColumn);
                item.addItem(title, progress, max);
                mNotifications.put(packageName, item);
            }
            
        }
        c.close();
        
        // Add the notifications
        for (NotificationItem item : mNotifications.values()) {
            // Build the notification object
            Notification n = new Notification();
            n.icon = android.R.drawable.stat_sys_download;

            n.flags |= Notification.FLAG_ONGOING_EVENT;
            
            // Build the RemoteView object
            RemoteViews expandedView = new RemoteViews(
                    Const.pkg,
            		status_bar_ongoing_event_progress_bar
                    );
            StringBuilder title = new StringBuilder(item.mTitles[0]);
            /*
            if (item.mTitleCount > 1) {
                title.append(mContext.getString(R.string.notification_filename_separator));
                title.append(item.mTitles[1]);
                n.number = item.mTitleCount;
                if (item.mTitleCount > 2) {
                    title.append(mContext.getString(R.string.notification_filename_extras,
                            new Object[] { Integer.valueOf(item.mTitleCount - 2) }));
                }
            } else {
                expandedView.setTextViewText(R.id.description, 
                        item.mDescription);
            }
            expandedView.setTextViewText(R.id.title, title);
            expandedView.setProgressBar(R.id.progress_bar, 
                    item.mTotalTotal,
                    item.mTotalCurrent,
                    item.mTotalTotal == -1);
            expandedView.setTextViewText(R.id.progress_text, 
                    getDownloadingText(item.mTotalTotal, item.mTotalCurrent));
            expandedView.setImageViewResource(R.id.appIcon,
                    android.R.drawable.stat_sys_download);
            n.contentView = expandedView;

            Intent intent = new Intent(Constants.ACTION_LIST);
            intent.setClassName("com.android.providers.downloads",
                    DownloadReceiver.class.getName());
            intent.setData(Uri.parse(Downloads.CONTENT_URI + "/" + item.mId));
            intent.putExtra("multiple", item.mTitleCount > 1);

            n.contentIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
*/
            mNotificationMgr.notify(item.mId, n);
            
        }
    }

    private void updateCompletedNotification(Cursor c) {
        if (c == null) {
            return;
        }
        
        // Columns match projection in query above
        final int idColumn = 0;
        final int titleColumn = 1;
        final int descColumn = 2;
        final int ownerColumn = 3;
        final int classOwnerColumn = 4;
        final int currentBytesColumn = 5;
        final int totalBytesColumn = 6;
        final int statusColumn = 7;
        final int lastModColumnId = 8;
        final int destinationColumnId = 9;

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            // Add the notifications
            Notification n = new Notification();
            n.icon = android.R.drawable.stat_sys_download_done;

            long id = c.getLong(idColumn);
            String title = c.getString(titleColumn);
            /*
            if (title == null || title.length() == 0) {
                title = mContext.getResources().getString(
                        R.string.download_unknown_title);
            }
            Uri contentUri = Uri.parse(Downloads.CONTENT_URI + "/" + id);
            String caption;
            Intent intent;
            if (Downloads.isStatusError(c.getInt(statusColumn))) {
                caption = mContext.getResources()
                        .getString(R.string.notification_download_failed);
                intent = new Intent(Constants.ACTION_LIST);
            } else {
                caption = mContext.getResources()
                        .getString(R.string.notification_download_complete);
                if (c.getInt(destinationColumnId) == Downloads.DESTINATION_EXTERNAL) {
                    intent = new Intent(Constants.ACTION_OPEN);
                } else {
                    intent = new Intent(Constants.ACTION_LIST);
                }
            }
            
            intent.setClassName("com.android.providers.downloads",
                    DownloadReceiver.class.getName());
            intent.setData(contentUri);

            n.when = c.getLong(lastModColumnId);
            n.setLatestEventInfo(mContext, title, caption,
                    PendingIntent.getBroadcast(mContext, 0, intent, 0));

            intent = new Intent(Constants.ACTION_HIDE);
            intent.setClassName("com.android.providers.downloads",
                    DownloadReceiver.class.getName());
            intent.setData(contentUri);
            n.deleteIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
*/
            mNotificationMgr.notify(c.getInt(idColumn), n);
        }
        c.close();
    }

    /*
     * Helper function to build the downloading text.
     */
    private String getDownloadingText(long totalBytes, long currentBytes) {
        if (totalBytes <= 0) {
            return "";
        }
        long progress = currentBytes * 100 / totalBytes;
        StringBuilder sb = new StringBuilder();
        sb.append(progress);
        sb.append('%');
        return sb.toString();
    }
    
}
