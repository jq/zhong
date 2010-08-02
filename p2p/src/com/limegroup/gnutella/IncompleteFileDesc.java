package com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.downloader.Interval;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.http.HTTPHeaderValue;

/**
 * This class extends FileDesc and wraps an incomplete File, 
 * so it can be used for partial file sharing.
 */

public class IncompleteFileDesc extends FileDesc implements HTTPHeaderValue {
    /**
     * Ranges smalles than this will never be offered to other servents
     */
    private final static int MIN_CHUNK_SIZE = 102400; // 100K
    
    /**
     * Needed to find out what ranges are available
     */
	private VerifyingFile _verifyingFile;

	/**
	 * The name of the file, as returned by IncompleteFileManager
     *     .getCompletedName(FILE).
	 */
    private final String _name;

	/**
	 * The size of the file, casted to an <tt>int</tt>.
	 */
    private final int _size;

    /**
     * Constructor for the IncompleteFileDesc object.
     */
    public IncompleteFileDesc(File file, Set urns, int index, 
                              String completedName, int completedSize,
                              VerifyingFile vf) {
        super(file, urns, index);
        _name = completedName;
        _size = completedSize;
        _verifyingFile = vf;
    }

	/**
	 * Returns the completed size of the file on disk, in bytes.
	 *
	 * @return the size of the file on disk, in bytes
	 */
	public long getSize() {
		return _size;
	}

	/**
	 * Returns the completed name of this file.
	 * 
	 * @return the name of this file
	 */
	public String getName() {
		return _name;
	}
    
    /**
     * Opens an input stream to the <tt>File</tt> instance for this
	 * <tt>FileDesc</tt>.
	 *
	 * @return an <tt>InputStream</tt> to the <tt>File</tt> instance
	 * @throws <tt>FileNotFoundException</tt> if the file represented
	 *  by the <tt>File</tt> instance could not be found
     */
    public InputStream createInputStream() throws FileNotFoundException {
        // if we don't have any available ranges, we should never
        // have entered the download mesh in the first place!!!
        if (getFile().length() == 0)
            throw new FileNotFoundException("nothing downloaded");
                
        return new BufferedInputStream(new FileInputStream(getFile()));
    }
    
    /**
     * Adds the alternate location to this FileDesc and also notifies
     * the ManagedDownloader of a new location for this.
     */
    public boolean add(AlternateLocation al) {
        boolean ret = super.add(al);
        if (ret) {
            ManagedDownloader md = getMyDownloader();
            if( md != null )
                md.addDownload(al.createRemoteFileDesc((int)getSize()),false);
        }
        return ret;
    }
    
    /**
     * Adds a verified location to this FileDesc, not notifying the
     * ManagedDownloader of the location.
     */
    public boolean addVerified(AlternateLocation al) {
        return super.add(al);
    }
    

	/**
     * Adds the alternate locations to this FileDesc and also notifies the
     * ManagedDownloader of new locations for this.
     */
	public int addAll(AlternateLocationCollection alc) {
	    ManagedDownloader md = getMyDownloader();
	    
        // if no downloader, just add the collection.
	    if( md == null )
	        return super.addAll(alc);
	    
        // otherwise, iterate through and individually add them, to make
        // sure they get added to the downloader.
        int added = 0;
        synchronized(alc) {
        for(Iterator iter = alc.iterator(); iter.hasNext(); ) {
            AlternateLocation al = (AlternateLocation)iter.next();
            if( super.add(al) ) {
                md.addDownload(al.createRemoteFileDesc((int)getSize()),false);
                added++;
            }
        } //end of for
        } //end of synchronized block
        return added;
	}
	
    private ManagedDownloader getMyDownloader() {
        return RouterService.getDownloadManager().getDownloaderForURN(getSHA1Urn());
    }
    
	/**
	 * Returns whether or not we are actively downloading this file.
	 */
	public boolean isActivelyDownloading() {
        
        ManagedDownloader md = getMyDownloader();
	    
	    if(md == null)
	        return false;
	        
        switch(md.getState()) {
        case Downloader.QUEUED:
        case Downloader.WAITING_FOR_RETRY:
        case Downloader.ABORTED:
        case Downloader.GAVE_UP:
        case Downloader.DISK_PROBLEM:
        case Downloader.CORRUPT_FILE:
        case Downloader.REMOTE_QUEUED:
        case Downloader.WAITING_FOR_USER:
            return false;
        default:
            return true;
        }
    }    
    
    // implements HTTPHeaderValue
    public String httpStringValue() {
        return "";
    }

	// overrides Object.toString to provide a more useful description
	public String toString() {
		return ("IncompleteFileDesc:\r\n"+
				"name:     "+_name+"\r\n"+
				"index:    "+getIndex()+"\r\n");
	}
}



