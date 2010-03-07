package com.feebe.lib.download;

import android.net.Uri;

public final class Downloads {
    private Downloads() {}

    public static final String _ID = "_id";
    /**
     * The name of the column containing the URI of the data being downloaded.
     * <P>Type: TEXT</P>
     * <P>Owner can Init/Read</P>
     */
    public static final String COLUMN_URI = "uri";
    public static final String COLUMN_SAVE_PATH = "path";
    /**
     * the details only calling app knows
     * <P>Type: TEXT</P>
     * <P>Owner can Read</P>
     */
    public static final String _DATA = "_data";

    /**
     * The name of the column containing the total size of the file being
     * downloaded.
     * <P>Type: INTEGER</P>
     * <P>Owner can Read</P>
     */
    public static final String COLUMN_TOTAL_BYTES = "total_bytes";

    /**
     * The name of the column containing the size of the part of the file that
     * has been downloaded so far.
     * <P>Type: INTEGER</P>
     * <P>Owner can Read</P>
     */
    public static final String COLUMN_CURRENT_BYTES = "current_bytes";

    public static final String COLUMN_TITLE = "title";

    /**
     * The name of the column where the initiating application can provide the
     * description of this download. The description will be displayed to the
     * user in the list of downloads.
     * <P>Type: TEXT</P>
     * <P>Owner can Init/Read/Write</P>
     */
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_ERROR = "error";
    /*
     * Lists the states that the download manager can set on a download
     * to notify applications of the download progress.
     * The codes follow the HTTP families:<br>
     * 1xx: informational<br>
     * 2xx: success<br>
     * 3xx: redirects (not used by the download manager)<br>
     * 4xx: client errors<br>
     * 5xx: server errors
     */

    /**
     * Returns whether the status is informational (i.e. 1xx).
     */
    public static boolean isStatusInformational(int status) {
        return (status >= 100 && status < 200);
    }

    /**
     * Returns whether the download is suspended. (i.e. whether the download
     * won't complete without some action from outside the download
     * manager).
     */
    public static boolean isStatusSuspended(int status) {
        return (status == STATUS_PENDING_PAUSED || status == STATUS_RUNNING_PAUSED);
    }

    /**
     * Returns whether the status is a success (i.e. 2xx).
     */
    public static boolean isStatusSuccess(int status) {
        return (status >= 200 && status < 300);
    }

    /**
     * Returns whether the status is an error (i.e. 4xx or 5xx).
     */
    public static boolean isStatusError(int status) {
        return (status >= 400 && status < 600);
    }

    /**
     * Returns whether the status is a client error (i.e. 4xx).
     */
    public static boolean isStatusClientError(int status) {
        return (status >= 400 && status < 500);
    }

    /**
     * Returns whether the status is a server error (i.e. 5xx).
     */
    public static boolean isStatusServerError(int status) {
        return (status >= 500 && status < 600);
    }

    /**
     * Returns whether the download has completed (either with success or
     * error).
     */
    public static boolean isStatusCompleted(int status) {
        return (status >= 200 && status < 300) || (status >= 400 && status < 600);
    }

    /**
     * This download hasn't stated yet
     */
    public static final int STATUS_PENDING = 190;

    /**
     * This download hasn't stated yet and is paused
     */
    public static final int STATUS_PENDING_PAUSED = 191;

    /**
     * This download has started
     */
    public static final int STATUS_RUNNING = 192;

    /**
     * This download has started and is paused
     */
    public static final int STATUS_RUNNING_PAUSED = 193;

    /**
     * This download has successfully completed.
     * Warning: there might be other status values that indicate success
     * in the future.
     * Use isSucccess() to capture the entire category.
     */
    public static final int STATUS_SUCCESS = 200;

    /**
     * This request couldn't be parsed. This is also used when processing
     * requests with unknown/unsupported URI schemes.
     */
    public static final int STATUS_BAD_REQUEST = 400;

    /**
     * This download can't be performed because the content type cannot be
     * handled.
     */
    public static final int STATUS_NOT_ACCEPTABLE = 406;

    /**
     * This download cannot be performed because the length cannot be
     * determined accurately. This is the code for the HTTP error "Length
     * Required", which is typically used when making requests that require
     * a content length but don't have one, and it is also used in the
     * client when a response is received whose length cannot be determined
     * accurately (therefore making it impossible to know when a download
     * completes).
     */
    public static final int STATUS_LENGTH_REQUIRED = 411;

    /**
     * This download was interrupted and cannot be resumed.
     * This is the code for the HTTP error "Precondition Failed", and it is
     * also used in situations where the client doesn't have an ETag at all.
     */
    public static final int STATUS_PRECONDITION_FAILED = 412;

    /**
     * This download was canceled
     */
    public static final int STATUS_CANCELED = 490;

    /**
     * This download has completed with an error.
     * Warning: there will be other status values that indicate errors in
     * the future. Use isStatusError() to capture the entire category.
     */
    public static final int STATUS_UNKNOWN_ERROR = 491;

    /**
     * This download couldn't be completed because of a storage issue.
     * Typically, that's because the filesystem is missing or full.
     */
    public static final int STATUS_FILE_ERROR = 492;

    /**
     * This download couldn't be completed because of an HTTP
     * redirect response that the download manager couldn't
     * handle.
     */
    public static final int STATUS_UNHANDLED_REDIRECT = 493;

    /**
     * This download couldn't be completed because of an
     * unspecified unhandled HTTP code.
     */
    public static final int STATUS_UNHANDLED_HTTP_CODE = 494;

    /**
     * This download couldn't be completed because of an
     * error receiving or processing data at the HTTP level.
     */
    public static final int STATUS_HTTP_DATA_ERROR = 495;

    /**
     * This download couldn't be completed because of an
     * HttpException while setting up the request.
     */
    public static final int STATUS_HTTP_EXCEPTION = 496;

    /**
     * This download couldn't be completed because there were
     * too many redirects.
     */
    public static final int STATUS_TOO_MANY_REDIRECTS = 497;
}
