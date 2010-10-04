package com.limegroup.gnutella.settings;

/**
 * Settings for Gnutella TCP connections.
 */
public final class ConnectionSettings {
    
    private ConnectionSettings() {}
            
	/**
	 * Settings for whether or not an incoming connection has ever been
	 * accepted.
	 */
	public static Setting<Boolean> EVER_ACCEPTED_INCOMING = new Setting<Boolean>("", false);
	
	/**
	 * Setting for whether we have ever determined that we are not able to
	 * do Firewall-to-firewall transfers in the past based on information
	 * received in pongs.
	 */
	public static Setting<Boolean> LAST_FWT_STATE = new Setting<Boolean>("", false);
			
    /**
     * Setting for the last time (in msecs since epoch) that we
     * connected to retrieve more gWebCache bootstrap servers
     */
    public static Setting<Long> LAST_GWEBCACHE_FETCH_TIME = new Setting<Long>("", 0l);
		                		        
    /**
	 * The time to live.
	 */
    public static final byte TTL = 4;
        
    /**
	 * The port to connect on
	 */
    public static int PORT = 6346;
    
    
    public static final String CONNECT_STRING_FIRST_WORD = "GNUTELLA";
    
    public static final String CONNECT_STRING = "GNUTELLA CONNECT/0.4";
        
    public static final String CONNECT_OK_STRING ="GNUTELLA OK";
    
    public static final boolean USE_LOCALE_PREF = true;

    /**
     * number of slots to reserve for those connections that
     * match the local locale
     */
    public static final int NUM_LOCALE_PREF = 1;
    
    /**
     * how many attempts to connect to a remote host must elapse
     * before we start accepting non-LW vendors as UPs
     */
    public static final int LIME_ATTEMPTS =5;
    
    /**
     * how long we believe firewalls will let us send solicited udp
     * traffic.  Field tests show at least a minute with most firewalls, so lets
     * try 55 seconds.
     */
    public static final int SOLICITED_GRACE_PERIOD =85000;
    
    /**
     * How many pongs to send back for each ping.
     */
    public static final int NUM_RETURN_PONGS = 1;
        
    /**
     * Setting for whether or not firewalled checking is done from any
     * incoming connection or just connectbacks.
     */
    public static final boolean UNSET_FIREWALLED_FROM_CONNECTBACK = false;
                                             
    /**
     * Time in milliseconds to delay prior to flushing data on peer -> peer connections
     */
    public static final int FLUSH_DELAY_TIME = 300;
                                            
    
    /**
     * Lowercase hosts that are evil.
     */
    public static final String[] EVIL_HOSTS = new String[0];
    
    
    /**
     * Helper method left from Settings Manager
     *
	 * Returns the maximum number of connections for the given connection
     * speed.
	 */
    public static final int getMaxConnections() {
    	return 6;
    }
}

