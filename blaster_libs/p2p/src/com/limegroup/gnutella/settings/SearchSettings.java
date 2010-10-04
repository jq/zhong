package com.limegroup.gnutella.settings;


/**
 * Settings for searches.
 */
public final class SearchSettings {
    /**
     * Constant for the characters that are banned from search
     * strings.
     */
    public static final char[] ILLEGAL_CHARS = {
        '_', '#', '!', '|', '?', '<', '>', '^', '(', ')', 
        ':', ';', '/', '\\', '[', ']', 
        '\t', '\n', '\r', '\f', // these cannot be last or first 'cause they're trimmed
        '{', '}',
    };


	/**
	 * Setting for whether or not OOB searching is enabled.
	 */
	public static final boolean OOB_ENABLED = true;

    /**
     * Setting for the maximum number of bytes to allow in queries.
     */
    public static final int MAX_QUERY_LENGTH = 4096;  // Original number is 30 which is too small. (zyu)

    /**
     * Setting for the maximum number of bytes to allow in XML queries.
     */
    public static final int MAX_XML_QUERY_LENGTH = 500;
    
    /**
	 * The minimum quality (number of stars) for search results to
	 * display.
	 */
    public static final int MINIMUM_SEARCH_QUALITY =0;
    
    /**
	 * The minimum speed for search results to display.
	 */
    public static final int MINIMUM_SEARCH_SPEED =0;
    
    /**
	 * The maximum number of simultaneous searches to allow.
	 */    
    public static final int PARALLEL_SEARCH = 5;
}
