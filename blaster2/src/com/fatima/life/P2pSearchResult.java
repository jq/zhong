package com.fatima.life;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.util.ApproximateMatcher;
import com.limegroup.gnutella.util.I18NConvert;

/**
 * A single SearchResult.
 *
 * (A collection of RemoteFileDesc, HostData, and Set of alternate locations.)
 */
public final class P2pSearchResult extends SearchResult implements Comparable<P2pSearchResult> {
    private static final int MAX_RFD = 16;
	private ArrayList<RemoteFileDesc> RFD = new ArrayList<RemoteFileDesc>();

	/** The processed version of the filename used for approximate matching.
	 *  Not allocated until a match must be done.  The assumption here is that
	 *  all matches will use the same ApproximateMatcher.  TODO3: when we move
	 *  to Java 1.3, this should be a weak reference so the memory is reclaimed
	 *  after GC. */
	private String processedFilename = null;    
	private final byte[] mGuid;
	private Set<Endpoint> alt;
	
	/**
	 * Constructs a new SearchResult with the given data.
	 */
	public P2pSearchResult(byte[] guid, RemoteFileDesc rfd, HostData data, Set<Endpoint> alts) {
		mGuid = guid;
		if (alts.size() > 0) {
		    if (alt == null) {
		        alt = alts;
		    } else {
		        alt.addAll(alts);
		    }
		}
		RFD.add(rfd);
	}
	
    public Set<Endpoint> getAlt () {
        if (alt == null) {
            return Collections.emptySet();
        } else {
            return alt;
        }
    }
    
	public void add(RemoteFileDesc rfd) {
        if (RFD.size() >= MAX_RFD)
            return;
		RFD.add(rfd);
	}
	
	public byte[] getGuid() {
		return mGuid;
	}
	
	/**
	 * Gets the size of this SearchResult.
	 */
	public int getSize() {
	    int size = RFD.size();
	    if (alt != null) {
	        size += alt.size();
	    }
		return size;
	}
	
	public RemoteFileDesc[] getRFDArray() {
		RemoteFileDesc[] rfds = new RemoteFileDesc[RFD.size()];
		return (RemoteFileDesc[])RFD.toArray(rfds);
	}
	
	public RemoteFileDesc getFirstRFD() {
		return RFD.get(0);
	}
	
	public String getFileName() {
		return RFD.get(0).getFileName();
	}
	
	public long getFileSize() {
		return RFD.get(0).getSize();
	}

	/**
	 * Gets the filename without the extension.
	 */
	private String getFilenameNoExtension() {
		String fullname = RFD.get(0).getFileName();
		int i = fullname.lastIndexOf(".");
		if (i < 0)
			return fullname;
		return I18NConvert.instance().compose(fullname.substring(0,i));
	}

	/**
	 * Returns the extension of this result.
	 */
	private String getExtension() {
		String fullname = RFD.get(0).getFileName();
		int i = fullname.lastIndexOf(".");
		if (i < 0)
			return "";
		return fullname.substring(i+1);
	}    

	/**
	 * Gets the processed filename.
	 */
	private String getProcessedFilename(ApproximateMatcher matcher) {
		if (processedFilename != null)
			return processedFilename;
		processedFilename = matcher.process(getFilenameNoExtension());
		return processedFilename;
	}

	/** 
	 * Compares this against o approximately:
	 * <ul>
	 *  <li> Returns 0 if o is similar to this. 
	 *  <li> Returns 1 if they have non-similar extensions.
	 *  <li> Returns 2 if they have non-similar sizes.
	 *  <li> Returns 3 if they have non-similar names.
	 * <ul>
	 *
	 * Design note: this takes an ApproximateMatcher as an argument so that many
	 * comparisons may be done with the same matcher, greatly reducing the
	 * number of allocations.<b>
	 *
	 * <b>This method is not thread-safe.</b>
	 */
	public int match(P2pSearchResult o, final ApproximateMatcher matcher) {
		//Same file type?
		if (!getExtension().equals(o.getExtension()))
			return 1;

		long thisSize = getSize();
		long thatSize = o.getSize();

		// Sizes same?
		if (thisSize != thatSize)
			return 2;

		//Preprocess the processed fileNames
		getProcessedFilename(matcher);
		o.getProcessedFilename(matcher);

		//Filenames close?  This is the most expensive test, so it should go
		//last.  Allow 5% edit difference in filenames or 4 characters,
		//whichever is smaller.
		int allowedDifferences = Math.round(Math.min(
				0.10f*((float)getFilenameNoExtension().length()),
				0.10f*((float)o.getFilenameNoExtension().length())));
		allowedDifferences = Math.min(allowedDifferences, 4);
		if (!matcher.matches(getProcessedFilename(matcher), 
				o.getProcessedFilename(matcher),
				allowedDifferences))
			return 3;
		return 0;
	}

	@Override
    public int compareTo(P2pSearchResult arg0) {
        return arg0.getSize() -  getSize();
    }
}
