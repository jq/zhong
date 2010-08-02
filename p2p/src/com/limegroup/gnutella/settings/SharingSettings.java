
package com.limegroup.gnutella.settings;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import android.os.Environment;

import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;

/**
 * Settings for sharing
 */
public class SharingSettings {
 	private static final File SD_CARD_ROOT = Environment.getExternalStorageDirectory();
 	public static final File HOME = new File(SD_CARD_ROOT, "musiclife");
 	public static final File SETTINGS_DIRECTORY = new File(HOME, "setting");;


    public static final File DEFAULT_SAVE_DIR = new File(HOME, "Shared");
	private static String CANONICAL_SAVE_DIR;

    /**
     * The directory where incomplete files are stored (downloads in progress).
     */
    public static final File INCOMPLETE_DIRECTORY = new File(HOME, "Incomplete");
    
    /**
	 * A file with a snapshot of current downloading files.
	 */                
    public static final File DOWNLOAD_SNAPSHOT_FILE =new File(INCOMPLETE_DIRECTORY, "downloads.dat");
            
    /**
	 * A file with a snapshot of current downloading files.
	 */                
    public static final File DOWNLOAD_SNAPSHOT_BACKUP_FILE =new File(INCOMPLETE_DIRECTORY, "downloads.bak");
    
    /** The minimum age in days for which incomplete files will be deleted.
     *  This values may be zero or negative; doing so will cause LimeWire to
     *  delete ALL incomplete files on startup. */   
    public static final int INCOMPLETE_PURGE_TIME =7;
    
    public static final void setSaveDirectory() {
        if (!DEFAULT_SAVE_DIR.exists()) {
            HOME.mkdir();
            DEFAULT_SAVE_DIR.mkdir();
            INCOMPLETE_DIRECTORY.mkdir();
            SETTINGS_DIRECTORY.mkdir();
        }
    }
    
    public static String getCanonicalSaveDir() throws IOException {
    	if (CANONICAL_SAVE_DIR == null) {
			CANONICAL_SAVE_DIR = FileUtils.getCanonicalPath(DEFAULT_SAVE_DIR);
    	}
    	return CANONICAL_SAVE_DIR;
    }
    
     /**
	 * The timeout value for persistent HTTP connections in milliseconds.
	 */
    public static final int PERSISTENT_HTTP_CONNECTION_TIMEOUT =15000;
 }
