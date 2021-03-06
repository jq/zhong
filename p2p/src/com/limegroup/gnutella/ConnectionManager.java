package com.limegroup.gnutella;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import com.util.LOG;
import com.limegroup.gnutella.connection.ConnectionChecker;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.handshaking.BadHandshakeException;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.security.Authenticator;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.Sockets;
import com.limegroup.gnutella.messages.ByeRequest;

/**
 * The list of all ManagedConnection's.  Provides a factory method for creating
 * user-requested outgoing connections, accepts incoming connections, and
 * fetches "automatic" outgoing connections as needed.  Creates threads for
 * handling these connections when appropriate.
 *
 * Because this is the only list of all connections, it plays an important role
 * in message broadcasting.  For this reason, the code is highly tuned to avoid
 * locking in the getInitializedConnections() methods.  Adding and removing
 * connections is a slower operation.<p>
 *
 * LimeWire follows the following connection strategy:<br>
 * As a leaf, LimeWire will ONLY connect to 'good' Ultrapeers.  The definition
 * of good is constantly changing.  For a current view of 'good', review
 * HandshakeResponse.isGoodUltrapeer().  LimeWire leaves will NOT deny
 * a connection to an ultrapeer even if they've reached their maximum
 * desired number of connections (currently 4).  This means that if 5
 * connections resolve simultaneously, the leaf will remain connected to all 5.
 * <br>
 * As an Ultrapeer, LimeWire will seek outgoing connections for 5 less than
 * the number of it's desired peer slots.  This is done so that newcomers
 * on the network have a better chance of finding an ultrapeer with a slot
 * open.  LimeWire ultrapeers will allow ANY other ultrapeer to connect to it,
 * and to ensure that the network does not become too LimeWire-centric, it
 * reserves 3 slots for non-LimeWire peers.  LimeWire ultrapeers will allow
 * ANY leaf to connect, so long as there are atleast 15 slots open.  Beyond
 * that number, LimeWire will only allow 'good' leaves.  To see what consitutes
 * a good leave, view HandshakeResponse.isGoodLeaf().  To ensure that the
 * network does not remain too LimeWire-centric, it reserves 3 slots for
 * non-LimeWire leaves.<p>
 *
 * ConnectionManager has methods to get up and downstream bandwidth, but it
 * doesn't quite fit the BandwidthTracker interface.
 */
public class ConnectionManager {

    /**
     * Timestamp for the last time the user selected to disconnect.
     */
    private volatile long _disconnectTime = -1;

    /**
     * Timestamp for the time we began automatically connecting.  We stop
     * trying to automatically connect if the user has disconnected since that
     * time.
     */
    private volatile long _automaticConnectTime = 0;

    /**
     * Flag for whether or not the auto-connection process is in effect.
     */
    private volatile boolean _automaticallyConnecting;

    /**
     * Timestamp of our last successful connection.
     */
    private volatile long _lastSuccessfulConnect = 0;

    /**
     * Timestamp of the last time we checked to verify that the user has a live
     * Internet connection.
     */
    private volatile long _lastConnectionCheck = 0;


    /**
     * Counter for the number of connection attempts we've made.
     */
    private volatile static int _connectionAttempts;

	/**
	 * The number of Ultrapeer connections to ideally maintain as an Ultrapeer.
	 */
	public static final int ULTRAPEER_CONNECTIONS =
        32;

    /**
     * The number of connections leaves should maintain to Ultrapeers.
     */
    public static final int PREFERRED_CONNECTIONS_FOR_LEAF = 3;

    /**
     * To number of connections leaves should maintain when idle.
     */
    public static final int PREFERRED_CONNECTIONS_FOR_LEAF_WHEN_IDLE = 1;

    /**
     * The number of leaf connections reserved for non LimeWire clients.
     * This is done to ensure that the network is not solely LimeWire centric.
     */
    public static final int RESERVED_NON_LIMEWIRE_LEAVES = 2;

    /**
     * The number of ultrapeer connections reserved for non LimeWire clients.
     * This is done to ensure that the network is not solely LimeWire centric.
     */
    public static final int RESERVED_NON_LIMEWIRE_PEERS = 3;

    /**
     * The current number of connections we want to maintain.
     */
    private volatile int _preferredConnections = -1;

    /**
     * Reference to the <tt>HostCatcher</tt> for retrieving host data as well
     * as adding host data.
     */
    private HostCatcher _catcher;

    /** Threads trying to maintain the NUM_CONNECTIONS.
     *  LOCKING: obtain this. */
    private final List /* of ConnectionFetcher */ _fetchers =
        new ArrayList();
    /** Connections that have been fetched but not initialized.  I don't
     *  know the relation between _initializingFetchedConnections and
     *  _connections (see below).  LOCKING: obtain this. */
    private final List /* of ManagedConnection */ _initializingFetchedConnections =
        new ArrayList();

    /**
     * dedicated ConnectionFetcher used by leafs to fetch a
     * locale matching connection
     * NOTE: currently this is only used by leafs which will try
     * to connect to one connection which matches the locale of the
     * client.
     */
    private ConnectionFetcher _dedicatedPrefFetcher;

    /**
     * boolean to check if a locale matching connection is needed.
     */
    private volatile boolean _needPref = true;
    
    /**
     * boolean of whether or not the interruption of the prefFetcher thread
     * has been scheduled.
     */
    private boolean _needPrefInterrupterScheduled = false;

    /**
     * List of all connections.  The core data structures are lists, which allow
     * fast iteration for message broadcast purposes.  Actually we keep a couple
     * of lists: the list of all initialized and uninitialized connections
     * (_connections), the list of all initialized non-leaf connections
     * (_initializedConnections), and the list of all initialized leaf connections
     * (_initializedClientConnections).
     *
     * INVARIANT: neither _connections, _initializedConnections, nor
     *   _initializedClientConnections contains any duplicates.
     * INVARIANT: for all c in _initializedConnections,
     *   c.isSupernodeClientConnection()==false
     * INVARIANT: for all c in _initializedClientConnections,
     *   c.isSupernodeClientConnection()==true
     * COROLLARY: the intersection of _initializedClientConnections
     *   and _initializedConnections is the empty set
     * INVARIANT: _initializedConnections is a subset of _connections
     * INVARIANT: _initializedClientConnections is a subset of _connections
     * INVARIANT: _shieldedConnections is the number of connections
     *   in _initializedConnections for which isClientSupernodeConnection()
     *   is true.
     * INVARIANT: _nonLimeWireLeaves is the number of connections
     *   in _initializedClientConnections for which isLimeWire is false
     * INVARIANT: _nonLimeWirePeers is the number of connections
     *   in _initializedConnections for which isLimeWire is false
     *
     * LOCKING: _connections, _initializedConnections and
     *   _initializedClientConnections MUST NOT BE MUTATED.  Instead they should
     *   be replaced as necessary with new copies.  Before replacing the
     *   structures, obtain this' monitor.  This avoids lock overhead when
     *   message broadcasting, though it makes adding/removing connections
     *   much slower.
     */
    //TODO:: why not use sets here??
    private volatile List /* of ManagedConnection */
        _connections = new ArrayList();
    private volatile List /* of ManagedConnection */
        _initializedConnections = new ArrayList();
    
    private volatile List /* of ManagedConnection */
        _initializedClientConnections = new ArrayList();

    private volatile int _shieldedConnections = 0;
    private volatile int _nonLimeWireLeaves = 0;
    private volatile int _nonLimeWirePeers = 0;
    /** number of peers that matches the local locale pref. */
    private volatile int _localeMatchingPeers = 0;

    /**
     * For authenticating users
     */
    private final Authenticator _authenticator;

    /**
     * The current measured upstream bandwidth.
     */
    private volatile float _measuredUpstreamBandwidth = 0.f;

    /**
     * The current measured downstream bandwidth.
     */
    private volatile float _measuredDownstreamBandwidth = 0.f;

    /**
     * Constructs a ConnectionManager.  Must call initialize before using.
     * @param authenticator Authenticator instance for authenticating users
     */
    public ConnectionManager(Authenticator authenticator) {
        _authenticator = authenticator;
    }

    /**
     * Links the ConnectionManager up with the other back end pieces and
     * launches the ConnectionWatchdog and the initial ConnectionFetchers.
     */
    public void initialize() {
        _catcher = RouterService.getHostCatcher();
    }


    /**
     * Create a new connection, blocking until it's initialized, but launching
     * a new thread to do the message loop.
     */
    public ManagedConnection createConnectionBlocking(String hostname,
        int portnum)
		throws IOException {
        ManagedConnection c =
			new ManagedConnection(hostname, portnum);

        // Initialize synchronously
        initializeExternallyGeneratedConnection(c);
        // Kick off a thread for the message loop.
        Thread conn =
            new ManagedThread(new OutgoingConnector(c, false), "OutgoingConnector");
        conn.setDaemon(true);
        conn.start();
        return c;
    }

    /**
     * Create a new connection, allowing it to initialize and loop for messages
     * on a new thread.
     */
    public void createConnectionAsynchronously(
            String hostname, int portnum) {

		Runnable outgoingRunner =
			new OutgoingConnector(new ManagedConnection(hostname, portnum),
								  true);
        // Initialize and loop for messages on another thread.

		Thread outgoingConnectionRunner =
			new ManagedThread(outgoingRunner, "OutgoingConnectionThread");
		outgoingConnectionRunner.setDaemon(true);
		outgoingConnectionRunner.start();
    }


    /**
     * Create an incoming connection.  This method starts the message loop,
     * so it will block for a long time.  Make sure the thread that calls
     * this method is suitable doing a connection message loop.
     * If there are already too many connections in the manager, this method
     * will launch a RejectConnection to send pongs for other hosts.
     */
     void acceptConnection(Socket socket) {
         //1. Initialize connection.  It's always safe to recommend new headers.
         Thread.currentThread().setName("IncommingConnectionThread");
         ManagedConnection connection = new ManagedConnection(socket);
         try {
             initializeExternallyGeneratedConnection(connection);
         } catch (IOException e) {
			 connection.close();
             return;
         }

         try {
			 startConnection(connection);
         } catch(IOException e) {
             // we could not start the connection for some reason --
             // this can easily happen, for example, if the connection
             // just drops
         }
     }


    /**
     * Removes the specified connection from currently active connections, also
     * removing this connection from routing tables and modifying active
     * connection fetchers accordingly.
     *
     * @param mc the <tt>ManagedConnection</tt> instance to remove
     */
    public synchronized void remove(ManagedConnection mc) {
    		mc.setByeReason(ByeRequest.EXIT_NORMAL,"Normal exit\n");
        removeInternal(mc);

        adjustConnectionFetchers();
    }


    /**
     * Returns whether this (probably) has a connection to the given host.  This
     * method is currently implemented by iterating through all connections and
     * comparing addresses but not ports.  (Incoming connections use ephemeral
     * ports.)  As a result, this test may conservatively return true even if
     * this is not connected to <tt>host</tt>.  Likewise, it may it mistakenly
     * return false if <tt>host</tt> is a multihomed system.  In the future,
     * additional connection headers may make the test more precise.
     *
     * @return true if this is probably connected to <tt>host</tt>
     */
    boolean isConnectedTo(String hostName) {
        //A clone of the list of all connections, both initialized and
        //uninitialized, leaves and unrouted.  If Java could be prevented from
        //making certain code transformations, it would be safe to replace the
        //call to "getConnections()" with "_connections", thus avoiding a clone.
        //(Remember that _connections is never mutated.)
        List connections=getConnections();
        for (Iterator iter=connections.iterator(); iter.hasNext(); ) {
            ManagedConnection mc = (ManagedConnection)iter.next();

            if (mc.getAddress().equals(hostName))
                return true;
        }
        return false;
    }

    /**
     * @return the number of connections, which is greater than or equal
     *  to the number of initialized connections.
     */
    public int getNumConnections() {
        return _connections.size();
    }

    /**
     * @return the number of initialized connections, which is less than or
     *  equals to the number of connections.
     */
    public int getNumInitializedConnections() {
		return _initializedConnections.size();
    }


    /**
     *@return the number of initialized connections for which
     * isClientSupernodeConnection is true.
     */
    public int getNumClientSupernodeConnections() {
        return _shieldedConnections;
    }

    /**
     * @return the number of free non-leaf slots.
     */
    public int getNumFreeNonLeafSlots() {
        return _preferredConnections - getNumInitializedConnections();
    }

    /**
     * @return the number of free non-leaf slots that LimeWires can connect to.
     */
    public int getNumFreeLimeWireNonLeafSlots() {
        return Math.max(0,
                        getNumFreeNonLeafSlots()
                        - Math.max(0, RESERVED_NON_LIMEWIRE_PEERS - _nonLimeWirePeers)
                        - getNumLimeWireLocalePrefSlots()
                        );
    }
    
    /**
     * Returns true if we've made a locale-matching connection (or don't
     * want any at all).
     */
    public boolean isLocaleMatched() {
        return !ConnectionSettings.USE_LOCALE_PREF ||
               _localeMatchingPeers != 0;
    }

    /**
     * @return the number of locale reserved slots to be filled
     *
     * An ultrapeer may not have Free LimeWire Non Leaf Slots but may still
     * have free slots that are reserved for locales
     */
    public int getNumLimeWireLocalePrefSlots() {
        return Math.max(0, ConnectionSettings.NUM_LOCALE_PREF
                        - _localeMatchingPeers);
    }
    
    /**
     * Determines if we've reached our maximum number of preferred connections.
     */
    public boolean isFullyConnected() {
        return _initializedConnections.size() >= _preferredConnections;
    }    

	/**
	 * Returns whether or not the client has an established connection with
	 * another Gnutella client.
	 *
	 * @return <tt>true</tt> if the client is currently connected to
	 *  another Gnutella client, <tt>false</tt> otherwise
	 */
	public boolean isConnected() {
		return ((_initializedClientConnections.size() > 0) ||
				(_initializedConnections.size() > 0));
	}
	
	/**
	 * Returns whether or not we are currently attempting to connect to the
	 * network.
	 */
	public boolean isConnecting() {
	    if(_disconnectTime != 0)
	        return false;
	    if(isConnected())
	        return false;
	    synchronized(this) {
	        return _fetchers.size() != 0 ||
	               _initializingFetchedConnections.size() != 0;
	    }
	}

    /**
     * Takes a snapshot of the upstream and downstream bandwidth since the last
     * call to measureBandwidth.
     * @see BandwidthTracker#measureBandwidth
     */
    public void measureBandwidth() {
        float upstream=0.f;
        float downstream=0.f;
        List connections = getInitializedConnections();
        for (Iterator iter=connections.iterator(); iter.hasNext(); ) {
            ManagedConnection mc=(ManagedConnection)iter.next();
            mc.measureBandwidth();
            upstream+=mc.getMeasuredUpstreamBandwidth();
            downstream+=mc.getMeasuredDownstreamBandwidth();
        }
        _measuredUpstreamBandwidth=upstream;
        _measuredDownstreamBandwidth=downstream;
    }

    /**
     * Returns the upstream bandwidth between the last two calls to
     * measureBandwidth.
     * @see BandwidthTracker#measureBandwidth
     */
    public float getMeasuredUpstreamBandwidth() {
        return _measuredUpstreamBandwidth;
    }

    /**
     * Returns the downstream bandwidth between the last two calls to
     * measureBandwidth.
     * @see BandwidthTracker#measureBandwidth
     */
    public float getMeasuredDownstreamBandwidth() {
        return _measuredDownstreamBandwidth;
    }

    /**
     * Checks if the connection received can be accepted,
     * based upon the type of connection (e.g. client, ultrapeer,
     * temporary etc).
     * @param c The connection we received, for which to
     * test if we have incoming slot.
     * @return true, if we have incoming slot for the connection received,
     * false otherwise
     */
    private boolean allowConnection(ManagedConnection c) {
        if(!c.receivedHeaders()) return false;
		    return allowConnection(c.headers(), false);
    }

    /**
     * Checks if the connection received can be accepted,
     * based upon the type of connection (e.g. client, ultrapeer,
     * temporary etc).
     * @param c The connection we received, for which to
     * test if we have incoming slot.
     * @return true, if we have incoming slot for the connection received,
     * false otherwise
     */
     public boolean allowConnection(HandshakeResponse hr) {
         return allowConnection(hr, !hr.isUltrapeer());
     }

    /**
     * Returns true if this has slots for an incoming connection, <b>without
     * accounting for this' ultrapeer capabilities</b>.  More specifically:
     * <ul>
     * <li>if ultrapeerHeader==null, returns true if this has space for an
     *  unrouted old-style connection.
     * <li>if ultrapeerHeader.equals("true"), returns true if this has slots
     *  for a leaf connection.
     * <li>if ultrapeerHeader.equals("false"), returns true if this has slots
     *  for an ultrapeer connection.
     * </ul>
     *
     * <tt>useragentHeader</tt> is used to prefer LimeWire and certain trusted
     * vendors.  <tt>outgoing</tt> is currently unused, but may be used to
     * prefer incoming or outgoing connections in the forward.
     *
     * @param outgoing true if this is an outgoing connection; true if incoming
     * @param ultrapeerHeader the value of the X-Ultrapeer header, or null
     *  if it was not written
     * @param useragentHeader the value of the User-Agent header, or null if
     *  it was not written
     * @return true if a connection of the given type is allowed
     */
    public boolean allowConnection(HandshakeResponse hr, boolean leaf) {

        //Old versions of LimeWire used to prefer incoming connections over
        //outgoing.  The rationale was that a large number of hosts were
        //firewalled, so those who weren't had to make extra space for them.
        //With the introduction of ultrapeers, this is not an issue; all
        //firewalled hosts become leaf nodes.  Hence we make no distinction
        //between incoming and outgoing.
        //
        //At one point we would actively kill old-fashioned unrouted connections
        //for ultrapeers.  Later, we preferred ultrapeers to old-fashioned
        //connections as follows: if the HostCatcher had marked ultrapeer pongs,
        //we never allowed more than DESIRED_OLD_CONNECTIONS old
        //connections--incoming or outgoing.
        //
        //Now we simply prefer connections by vendor, which has some of the same
        //effect.  We use BearShare's clumping algorithm.  Let N be the
        //keep-alive and K be RESERVED_GOOD_CONNECTIONS.  (In BearShare's
        //implementation, K=1.)  Allow any connections in for the first N-K
        //slots.  But only allow good vendors for the last K slots.  In other
        //words, accept a connection C if there are fewer than N connections and
        //one of the following is true: C is a good vendor or there are fewer
        //than N-K connections.  With time, this converges on all good
        //connections.

		int limeAttempts = ConnectionSettings.LIME_ATTEMPTS;
		
        //Don't allow anything if disconnected.
        if (_preferredConnections <=0 ) {
            return false;
        //If a leaf (shielded or not), check rules as such.
		} else {
		    // require ultrapeer.
		    if(!hr.isUltrapeer())
		        return false;
		    
		    // If it's not good, or it's the first few attempts & not a LimeWire, 
		    // never allow it.
		    if(!hr.isGoodUltrapeer() || 
		      (Sockets.getAttempts() < limeAttempts && !hr.isLimeWire())) {
		        return false;
		    // if we have slots, allow it.
		    } else if (_shieldedConnections < _preferredConnections) {
		        // if it matched our preference, we don't need to preference
		        // anymore.
		        if(checkLocale(hr.getLocalePref()))
		            _needPref = false;
                return true;
            } else {
                // if we were still trying to get a locale connection
                // and this one matches, allow it, 'cause no one else matches.
                // (we would have turned _needPref off if someone matched.)
                if(_needPref && checkLocale(hr.getLocalePref()))
                    return true;

                // don't allow it.
                return false;
            }
				}
    }

    /**
     * Provides handle to the authenticator instance
     * @return Handle to the authenticator
     */
    public Authenticator getAuthenticator(){
        return _authenticator;
    }


    /**
     * @requires returned value not modified
     * @effects returns a list of this' initialized connections.  <b>This
     *  exposes the representation of this, but is needed in some cases
     *  as an optimization.</b>  All lookup values in the returned value
     *  are guaranteed to run in linear time.
     */
    public List getInitializedConnections() {
        return _initializedConnections;
    }

    /**
     * return a list of initialized connection that matches the parameter
     * String loc.
     * create a new linkedlist to return.
     */
    public List getInitializedConnectionsMatchLocale(String loc) {
        List matches = new LinkedList();
        for(Iterator itr= _initializedConnections.iterator();
            itr.hasNext();) {
            Connection conn = (Connection)itr.next();
            if(loc.equals(conn.getLocalePref()))
                matches.add(conn);
        }
        return matches;
    }

    /**
     * @requires returned value not modified
     * @effects returns a list of this' initialized connections.  <b>This
     *  exposes the representation of this, but is needed in some cases
     *  as an optimization.</b>  All lookup values in the returned value
     *  are guaranteed to run in linear time.
     */
    public List getInitializedClientConnections() {
        return _initializedClientConnections;
    }

    /**
     * return a list of initialized client connection that matches the parameter
     * String loc.
     * create a new linkedlist to return.
     */
    public List getInitializedClientConnectionsMatchLocale(String loc) {
    	List matches = new LinkedList();
        for(Iterator itr= _initializedClientConnections.iterator();
            itr.hasNext();) {
            Connection conn = (Connection)itr.next();
            if(loc.equals(conn.getLocalePref()))
                matches.add(conn);
        }
        return matches;
    }

    /**
     * @return all of this' connections.
     */
    public List getConnections() {
        return _connections;
    }

    /**
     * Accessor for the <tt>Set</tt> of push proxies for this node.  If
     * there are no push proxies available, or if this node is an Ultrapeer,
     * this will return an empty <tt>Set</tt>.
     *
     * @return a <tt>Set</tt> of push proxies with a maximum size of 4
     *
     *  TODO: should the set of pushproxy UPs be cached and updated as
     *  connections are killed and created?
     */
    public Set getPushProxies() {
            // this should be fast since leaves don't maintain a lot of
            // connections and the test for proxy support is cached boolean
            // value
            Iterator ultrapeers = getInitializedConnections().iterator();
            Set proxies = new HashSet();
            while (ultrapeers.hasNext() && (proxies.size() < 4)) {
                ManagedConnection currMC = (ManagedConnection)ultrapeers.next();
                if (currMC.getPushProxyPort() >= 0)
                    proxies.add(currMC);
            }
            return proxies;
    }

    /**
     * Sends a TCPConnectBack request to (up to) 2 connected Ultrapeers.
     * @returns false if no requests were sent, otherwise true.
     */
    public boolean sendTCPConnectBackRequests() {
        int sent = 0;
        final Message cb = new TCPConnectBackVendorMessage(RouterService.getPort());
        List peers = new ArrayList(getInitializedConnections());
        Collections.shuffle(peers);
        for(Iterator i = peers.iterator(); i.hasNext() && sent < 5; ) {
            ManagedConnection currMC = (ManagedConnection)i.next();
            if (currMC.remoteHostSupportsTCPRedirect() >= 0) {
                currMC.send(cb);
                sent++;
            }
        }
        return (sent > 0);
    }

    /**
     * Sends a UDPConnectBack request to (up to) 4 (and at least 2)
     * connected Ultrapeers.
     * @returns false if no requests were sent, otherwise true.
     */
    public boolean sendUDPConnectBackRequests(GUID cbGuid) {
        int sent =  0;
        final Message cb =
            new UDPConnectBackVendorMessage(RouterService.getPort(), cbGuid);
        List peers = new ArrayList(getInitializedConnections());
        Collections.shuffle(peers);
        for(Iterator i = peers.iterator(); i.hasNext() && sent < 5; ) {
            ManagedConnection currMC = (ManagedConnection)i.next();
            if (currMC.remoteHostSupportsUDPConnectBack() >= 0) {
                currMC.send(cb);
                sent++;
            }
        }
        return (sent > 0);
    }

    /**
     * Sends a QueryStatusResponse message to as many Ultrapeers as possible.
     *
     * @param
     */
    public void updateQueryStatus(QueryStatusResponse stat) {
            // this should be fast since leaves don't maintain a lot of
            // connections and the test for query status response is a cached
            // value
            Iterator ultrapeers = getInitializedConnections().iterator();
            while (ultrapeers.hasNext()) {
                ManagedConnection currMC = (ManagedConnection)ultrapeers.next();
                if (currMC.remoteHostSupportsLeafGuidance() >= 0)
                    currMC.send(stat);
            }
    }

	/**
	 * Returns the <tt>Endpoint</tt> for an Ultrapeer connected via TCP,
	 * if available.
	 *
	 * @return the <tt>Endpoint</tt> for an Ultrapeer connected via TCP if
	 *  there is one, otherwise returns <tt>null</tt>
	 */
	public Endpoint getConnectedGUESSUltrapeer() {
		for(Iterator iter=_initializedConnections.iterator(); iter.hasNext();) {
			ManagedConnection connection = (ManagedConnection)iter.next();
			if(connection.isSupernodeConnection() &&
			   connection.isGUESSUltrapeer()) {
				return new Endpoint(connection.getInetAddress().getAddress(),
									connection.getPort());
			}
		}
		return null;
	}


    /** Returns a <tt>List<tt> of Ultrapeers connected via TCP that are GUESS
     *  enabled.
     *
     * @return A non-null List of GUESS enabled, TCP connected Ultrapeers.  The
     * are represented as ManagedConnections.
     */
	public List getConnectedGUESSUltrapeers() {
        List retList = new ArrayList();
		for(Iterator iter=_initializedConnections.iterator(); iter.hasNext();) {
			ManagedConnection connection = (ManagedConnection)iter.next();
			if(connection.isSupernodeConnection() &&
               connection.isGUESSUltrapeer())
				retList.add(connection);
		}
		return retList;
	}


    /**
     * Adds an initializing connection.
     * Should only be called from a thread that has this' monitor.
     * This is called from initializeExternallyGeneratedConnection
     * and initializeFetchedConnection, both times from within a
     * synchronized(this) block.
     */
    private void connectionInitializing(Connection c) {
        //REPLACE _connections with the list _connections+[c]
        List newConnections=new ArrayList(_connections);
        newConnections.add(c);
        _connections = Collections.unmodifiableList(newConnections);
    }

    /**
     * Adds an incoming connection to the list of connections. Note that
     * the incoming connection has already been initialized before
     * this method is invoked.
     * Should only be called from a thread that has this' monitor.
     * This is called from initializeExternallyGeneratedConnection, for
     * incoming connections
     */
    private void connectionInitializingIncoming(ManagedConnection c) {
        connectionInitializing(c);
    }

    /**
     * Marks a connection fully initialized, but only if that connection wasn't
     * removed from the list of open connections during its initialization.
     * Should only be called from a thread that has this' monitor.
     */
    private boolean connectionInitialized(ManagedConnection c) {
        LOG.error("enter connectionInitialized");

        if(_connections.contains(c)) {
	        // build the queues and start the output runner.
	        // this MUST be done before _initializedConnections
	        // or _initializedClientConnections has added this
	        // connection to its list.  otherwise, messages may
	        // attempt to be sent to the connection before it has
	        // set up its output queues.
            c.buildAndStartQueues();

            //update the appropriate list of connections
            if(!c.isSupernodeClientConnection()){
                //REPLACE _initializedConnections with the list
                //_initializedConnections+[c]
                List newConnections=new ArrayList(_initializedConnections);
                newConnections.add(c);
                _initializedConnections =
                    Collections.unmodifiableList(newConnections);
                //maintain invariant
                if(c.isClientSupernodeConnection())
                    _shieldedConnections++;
                if(!c.isLimeWire())
                    _nonLimeWirePeers++;
                if(checkLocale(c.getLocalePref()))
                    _localeMatchingPeers++;
            } else {
                //REPLACE _initializedClientConnections with the list
                //_initializedClientConnections+[c]
                List newConnections
                    =new ArrayList(_initializedClientConnections);
                newConnections.add(c);
                _initializedClientConnections =
                    Collections.unmodifiableList(newConnections);
                if(!c.isLimeWire())
                    _nonLimeWireLeaves++;
            }
	        // do any post-connection initialization that may involve sending.
	        c.postInit();
	        // sending the ping request.
    		sendInitialPingRequest(c);
            return true;
        }
        return false;

    }

    /**
     * Disconnects from the network.  Closes all connections and sets
     * the number of connections to zero.
     */
    public synchronized void disconnect() {
        _disconnectTime = System.currentTimeMillis();
        _preferredConnections = 0;
        adjustConnectionFetchers(); // kill them all
        //2. Remove all connections.
        for (Iterator iter=getConnections().iterator();
             iter.hasNext(); ) {
            ManagedConnection c=(ManagedConnection)iter.next();
            remove(c);
            //add the endpoint to hostcatcher
            if (c.isSupernodeConnection()) {
                //add to catcher with the locale info.
                _catcher.add(new Endpoint(c.getInetAddress().getHostAddress(),
                                          c.getPort()), true, c.getLocalePref());
            }
        }
        
        Sockets.clearAttempts();
    }

    /**
     * Connects to the network.  Ensures the number of messaging connections
     * is non-zero and recontacts the pong server as needed.
     */
    public synchronized void connect() {

        // Reset the disconnect time to be a long time ago.
        _disconnectTime = 0;

        // Ignore this call if we're already connected
        // or not initialized yet.
        if(isConnected() || _catcher == null) {
            return;
        }
        
        _connectionAttempts = 0;
        _lastConnectionCheck = 0;
        _lastSuccessfulConnect = 0;


        // Notify HostCatcher that we've connected.
        _catcher.expire();
        
        // Set the number of connections we want to maintain
        setPreferredConnections();
        
        // tell the catcher to start pinging people.
        _catcher.sendUDPPings();
    }

    /**
     * Sends the initial ping request to a newly initialized connection.  The
     * ttl of the PingRequest will be 1 if we don't need any connections.
     * Otherwise, the ttl = max ttl.
     */
    private void sendInitialPingRequest(ManagedConnection connection) {
        if(connection.supportsPongCaching()) return;

        //We need to compare how many connections we have to the keep alive to
        //determine whether to send a broadcast ping or a handshake ping,
        //initially.  However, in this case, we can't check the number of
        //connection fetchers currently operating, as that would always then
        //send a handshake ping, since we're always adjusting the connection
        //fetchers to have the difference between keep alive and num of
        //connections.
        PingRequest pr;
        if (getNumInitializedConnections() >= _preferredConnections)
            pr = new PingRequest((byte)1);
        else
            pr = new PingRequest((byte)4);

        connection.send(pr);
        //Ensure that the initial ping request is written in a timely fashion.
        try {
            connection.flush();
        } catch (IOException e) {
            /* close it later */ 
            //LOG.logSp("flush ping " + e.getMessage());
        }
    }

    /**
     * An unsynchronized version of remove, meant to be used when the monitor
     * is already held.  This version does not kick off ConnectionFetchers;
     * only the externally exposed version of remove does that.
     */
    private void removeInternal(ManagedConnection c) {
        // 1a) Remove from the initialized connections list and clean up the
        // stuff associated with initialized connections.  For efficiency
        // reasons, this must be done before (2) so packets are not forwarded
        // to dead connections (which results in lots of thrown exceptions).
        if(!c.isSupernodeClientConnection()){
            int i=_initializedConnections.indexOf(c);
            if (i != -1) {
                //REPLACE _initializedConnections with the list
                //_initializedConnections-[c]
                List newConnections=new ArrayList();
                newConnections.addAll(_initializedConnections);
                newConnections.remove(c);
                _initializedConnections =
                    Collections.unmodifiableList(newConnections);
                //maintain invariant
                if(c.isClientSupernodeConnection())
                    _shieldedConnections--;
                if(!c.isLimeWire())
                    _nonLimeWirePeers--;
                if(checkLocale(c.getLocalePref()))
                    _localeMatchingPeers--;
            }
        }else{
            //check in _initializedClientConnections
            int i=_initializedClientConnections.indexOf(c);
            if (i != -1) {
                //REPLACE _initializedClientConnections with the list
                //_initializedClientConnections-[c]
                List newConnections=new ArrayList();
                newConnections.addAll(_initializedClientConnections);
                newConnections.remove(c);
                _initializedClientConnections =
                    Collections.unmodifiableList(newConnections);
                if(!c.isLimeWire())
                    _nonLimeWireLeaves--;
            }
        }

        // 1b) Remove from the all connections list and clean up the
        // stuff associated all connections
        int i=_connections.indexOf(c);
        if (i != -1) {
            //REPLACE _connections with the list _connections-[c]
            List newConnections=new ArrayList(_connections);
            newConnections.remove(c);
            _connections = Collections.unmodifiableList(newConnections);
        }

        // 2) Ensure that the connection is closed.  This must be done before
        // step (3) to ensure that dead connections are not added to the route
        // table, resulting in dangling references.        
        c.close();

        // 3) Clean up route tables.
        RouterService.getMessageRouter().removeConnection(c);

        // 4) Notify the listener
        RouterService.getCallback().connectionClosed(c);

     }
    
    /**
     * Stabilizes connections by removing extraneous ones.
     *
     * This will remove the connections that we've been connected to
     * for the shortest amount of time.
     */
    private synchronized void stabilizeConnections() {
        while(getNumInitializedConnections() > _preferredConnections) {
            ManagedConnection newest = null;
            for(Iterator i = _initializedConnections.iterator(); i.hasNext();){
                ManagedConnection c = (ManagedConnection)i.next();
                
                // first see if this is a non-limewire connection and cut it off
                // unless it is our only connection left
                
                if (!c.isLimeWire()) {
                    LOG.error("remove not lime?");             

                    newest = c;
                    break;
                }
                
                if(newest == null || 
                   c.getConnectionTime() > newest.getConnectionTime())
                    newest = c;
            }
            if(newest != null) {
                LOG.error("remove in stabilizeConnections " + newest.getAddress());             

                remove(newest);
            }
        }

        adjustConnectionFetchers();
    }    

    /**
     * Starts or stops connection fetchers to maintain the invariant
     * that numConnections + numFetchers >= _preferredConnections
     *
     * _preferredConnections - numConnections - numFetchers is called the need.
     * This method is called whenever the need changes:
     *   1. setPreferredConnections() -- _preferredConnections changes
     *   2. remove(Connection) -- numConnections drops.
     *   3. initializeExternallyGeneratedConnection() --
     *        numConnections rises.
     *   4. initialization error in initializeFetchedConnection() --
     *        numConnections drops when removeInternal is called.
     *   Note that adjustConnectionFetchers is not called when a connection is
     *   successfully fetched from the host catcher.  numConnections rises,
     *   but numFetchers drops, so need is unchanged.
     *
     * Only call this method when the monitor is held.
     */
    private void adjustConnectionFetchers() {
        if(ConnectionSettings.USE_LOCALE_PREF) {
            //if it's a leaf and locale preferencing is on
            //we will create a dedicated preference fetcher
            //that tries to fetch a connection that matches the
            //clients locale
            if( _needPref
               && !_needPrefInterrupterScheduled
               && _dedicatedPrefFetcher == null) {
                _dedicatedPrefFetcher = new ConnectionFetcher(true);
                Runnable interrupted = new Runnable() {
                        public void run() {
                            synchronized(ConnectionManager.this) {
                                // always finish once this runs.
                                _needPref = false;

                                if (_dedicatedPrefFetcher == null)
                                    return;
                                _dedicatedPrefFetcher.interrupt();
                                _dedicatedPrefFetcher = null;
                            }
                        }
                    };
                _needPrefInterrupterScheduled = true;
                // shut off this guy if he didn't have any luck
                RouterService.schedule(interrupted, 15 * 1000, 0);
            }
        }
        int goodConnections = getNumInitializedConnections();
        int neededConnections = _preferredConnections - goodConnections;
        //Now how many fetchers do we need?  To increase parallelism, we
        //allocate 3 fetchers per connection, but no more than 10 fetchers.
        //(Too much parallelism increases chance of simultaneous connects,
        //resulting in too many connections.)  Note that we assume that all
        //connections being fetched right now will become ultrapeers.
        int multiple;

        // The end result of the following logic, assuming _preferredConnections
        // is 32 for Ultrapeers, is:
        // When we have 22 active peer connections, we fetch
        // (27-current)*1 connections.
        // All other times, for Ultrapeers, we will fetch
        // (32-current)*3, up to a maximum of 20.
        // For leaves, assuming they maintin 4 Ultrapeers,
        // we will fetch (4-current)*2 connections.

        // If we have not accepted incoming, fetch 3 times
        // as many connections as we need.
        // We must also check if we're actively being a Ultrapeer because
        // it's possible we may have turned off acceptedIncoming while
        // being an Ultrapeer.
        if( !RouterService.acceptedIncomingConnection()) {
            multiple = 3;
        }
        // Otherwise, if we're not ultrapeer capable,
        // or have not become an Ultrapeer to anyone,
        // also fetch 3 times as many connections as we need.
        // It is critical that active ultrapeers do not use a multiple of 3
        // without reducing neededConnections, otherwise LimeWire would
        // continue connecting and rejecting connections forever.
        else {
            multiple = 3;
        }
        int need = Math.min(10, multiple*neededConnections)
                 - _fetchers.size()
                 - _initializingFetchedConnections.size();
       // LOG.logSp("need " + need + " fetcher " + _fetchers.size() + " init " + _initializingFetchedConnections.size());

        // Start connection fetchers as necessary
        while(need > 0) {
            // This kicks off the thread for the fetcher
            _fetchers.add(new ConnectionFetcher());
            need--;
        }

        // Stop ConnectionFetchers as necessary, but it's possible there
        // aren't enough fetchers to stop.  In this case, close some of the
        // connections started by ConnectionFetchers.
        int lastFetcherIndex = _fetchers.size();
        while((need < 0) && (lastFetcherIndex > 0)) {
            ConnectionFetcher fetcher = (ConnectionFetcher)
                _fetchers.remove(--lastFetcherIndex);
            fetcher.interrupt();
            need++;
        }
        
        int lastInitializingConnectionIndex =
            _initializingFetchedConnections.size();
        while((need < 0) && (lastInitializingConnectionIndex > 0)) {
            ManagedConnection connection = (ManagedConnection)
                _initializingFetchedConnections.remove(
                    --lastInitializingConnectionIndex);
            LOG.logSp("remove in connectionFether" + connection.getAddress());             

            removeInternal(connection);
            need++;
        }
    }

    /**
     * Initializes an outgoing connection created by a ConnectionFetcher
     * Throws any of the exceptions listed in Connection.initialize on
     * failure; no cleanup is necessary in this case.
     *
     * @exception IOException we were unable to establish a TCP connection
     *  to the host
     * @exception NoGnutellaOkException we were able to establish a
     *  messaging connection but were rejected
     * @exception BadHandshakeException some other problem establishing
     *  the connection, e.g., the server responded with HTTP, closed the
     *  the connection during handshaking, etc.
     * @see com.limegroup.gnutella.Connection#initialize(int)
     */
    private void initializeFetchedConnection(ManagedConnection mc,
                                             ConnectionFetcher fetcher)
            throws NoGnutellaOkException, BadHandshakeException, IOException {
        synchronized(this) {
            if(fetcher.isInterrupted()) {
                // Externally generated interrupt.
                // The interrupting thread has recorded the
                // death of the fetcher, so throw IOException.
                // (This prevents fetcher from continuing!)
                throw new IOException("connection fetcher");
            }

            _initializingFetchedConnections.add(mc);
            if(fetcher == _dedicatedPrefFetcher)
                _dedicatedPrefFetcher = null;
            else
                _fetchers.remove(fetcher);
            connectionInitializing(mc);
            // No need to adjust connection fetchers here.  We haven't changed
            // the need for connections; we've just replaced a ConnectionFetcher
            // with a Connection.
        }
        RouterService.getCallback().connectionInitializing(mc);

        try {
            mc.initialize();
        } catch(IOException e) {
            synchronized(ConnectionManager.this) {
                _initializingFetchedConnections.remove(mc);
                //LOG.logSp("close in initializeFetchedConnection" + mc.getAddress() + e.getMessage());
                removeInternal(mc);
                // We've removed a connection, so the need for connections went
                // up.  We may need to launch a fetcher.
                adjustConnectionFetchers();
            }
            throw e;
        }
        finally {
            //if the connection received headers, process the headers to
            //take steps based on the headers
            processConnectionHeaders(mc);
        }

        completeConnectionInitialization(mc, true);
    }

    /**
     * Processes the headers received during connection handshake and updates
     * itself with any useful information contained in those headers.
     * Also may change its state based upon the headers.
     * @param headers The headers to be processed
     * @param connection The connection on which we received the headers
     */
    private void processConnectionHeaders(Connection connection){
        if(!connection.receivedHeaders()) {
            return;
        }

        //get the connection headers
        Properties headers = connection.headers().props();
        //return if no headers to process
        if(headers == null) return;
        //update the addresses in the host cache (in case we received some
        //in the headers)
        updateHostCache(connection.headers());

        //get remote address.  If the more modern "Listen-IP" header is
        //not included, try the old-fashioned "X-My-Address".
        String remoteAddress
            = headers.getProperty(HeaderNames.LISTEN_IP);
        if (remoteAddress==null)
            remoteAddress
                = headers.getProperty(HeaderNames.X_MY_ADDRESS);

        //set the remote port if not outgoing connection (as for the outgoing
        //connection, we already know the port at which remote host listens)
        if((remoteAddress != null) && (!connection.isOutgoing())) {
            int colonIndex = remoteAddress.indexOf(':');
            if(colonIndex == -1) return;
            colonIndex++;
            if(colonIndex > remoteAddress.length()) return;
            try {
                int port =
                    Integer.parseInt(
                        remoteAddress.substring(colonIndex).trim());
                if(NetworkUtils.isValidPort(port)) {
                	// for incoming connections, set the port based on what it's
                	// connection headers say the listening port is
                    connection.setListeningPort(port);
                }
            } catch(NumberFormatException e){
                // should nothappen though if the other client is well-coded
            }
        }
    }

    /**
     * Returns true if this can safely switch from Ultrapeer to leaf mode.
	 * Typically this means that we are an Ultrapeer and have no leaf
	 * connections.
	 *
	 * @return <tt>true</tt> if we will allow ourselves to become a leaf,
	 *  otherwise <tt>false</tt>
     */
    public boolean allowLeafDemotion() {
			    return true;
    }

    /**
     * Adds the X-Try-Ultrapeer hosts from the connection headers to the
     * host cache.
     *
     * @param headers the connection headers received
     */
    private void updateHostCache(HandshakeResponse headers) {

        if(!headers.hasXTryUltrapeers()) return;

        //get the ultrapeers, and add those to the host cache
        String hostAddresses = headers.getXTryUltrapeers();

        //tokenize to retrieve individual addresses
        StringTokenizer st = new StringTokenizer(hostAddresses,
            Constants.ENTRY_SEPARATOR);

        List hosts = new ArrayList(st.countTokens());
        while(st.hasMoreTokens()){
            String address = st.nextToken().trim();
            try {
                Endpoint e = new Endpoint(address);
                hosts.add(e);
            } catch(IllegalArgumentException iae){
                continue;
            }
        }
        _catcher.add(hosts);        
    }



    /**
     * Initializes an outgoing connection created by createConnection or any
     * incomingConnection.  If this is an incoming connection and there are no
     * slots available, rejects it and throws IOException.
     *
     * @throws IOException on failure.  No cleanup is necessary if this happens.
     */
    private void initializeExternallyGeneratedConnection(ManagedConnection c)
		throws IOException {
        //For outgoing connections add it to the GUI and the fetcher lists now.
        //For incoming, we'll do this below after checking incoming connection
        //slots.  This keeps reject connections from appearing in the GUI, as
        //well as improving performance slightly.
        if (c.isOutgoing()) {
            synchronized(this) {
                connectionInitializing(c);
                // We've added a connection, so the need for connections went
                // down.
                adjustConnectionFetchers();
            }
            RouterService.getCallback().connectionInitializing(c);
        }

        try {
            c.initialize();

        } catch(IOException e) {
            LOG.error("close on initializeExternallyGeneratedConnection" + c.getAddress() + e.getMessage());
            remove(c);
            throw e;
        }
        finally {
            //if the connection received headers, process the headers to
            //take steps based on the headers
            processConnectionHeaders(c);
        }

        //If there's not space for the connection, reject it.  This mechanism
        //works for Gnutella 0.4 connections, as well as some odd cases for 0.6
        //connections.  Sometimes ManagedConnections are handled by headers
        //directly.
        if (!c.isOutgoing() && !allowConnection(c)) {
            c.loopToReject();
            //No need to remove, since it hasn't been added to any lists.
            throw new IOException("No space for connection");
        }

        //For incoming connections, add it to the GUI.  For outgoing connections
        //this was done at the top of the method.  See note there.
        if (! c.isOutgoing()) {
            synchronized(this) {
                connectionInitializingIncoming(c);
                // We've added a connection, so the need for connections went
                // down.
                adjustConnectionFetchers();
            }
            RouterService.getCallback().connectionInitializing(c);
        }

        completeConnectionInitialization(c, false);
    }

    /**
     * Performs the steps necessary to complete connection initialization.
     *
     * @param mc the <tt>ManagedConnection</tt> to finish initializing
     * @param fetched Specifies whether or not this connection is was fetched
     *  by a connection fetcher.  If so, this removes that connection from
     *  the list of fetched connections being initialized, keeping the
     *  connection fetcher data in sync
     */
    private void completeConnectionInitialization(ManagedConnection mc,
                                                  boolean fetched) {
        synchronized(this) {
            if(fetched) {
                _initializingFetchedConnections.remove(mc);
            }
            // If the connection was killed while initializing, we shouldn't
            // announce its initialization
            boolean connectionOpen = connectionInitialized(mc);
            if(connectionOpen) {
                RouterService.getCallback().connectionInitialized(mc);
                RouterService.resendWhenConnect(mc);
                setPreferredConnections();
            }
        }
    }

    /**
     * Gets the number of preferred connections to maintain.
     */
    public int getPreferredConnectionCount() {
        return _preferredConnections;
    }

    /**
     * Determines if we're attempting to maintain the idle connection count.
     */
    public boolean isConnectionIdle() {
        return
         _preferredConnections == PREFERRED_CONNECTIONS_FOR_LEAF_WHEN_IDLE;
    }

    /**
     * Sets the maximum number of connections we'll maintain.
    */
    private void setPreferredConnections() {
        // if we're disconnected, do nothing.
        if(_disconnectTime != 0)
            return;

        int oldPreferred = _preferredConnections;

            _preferredConnections = PREFERRED_CONNECTIONS_FOR_LEAF;

        if(oldPreferred != _preferredConnections)
            stabilizeConnections();
    }


    //
    // End connection list management functions
    //


    //
    // Begin connection launching thread inner classes
    //

    /**
     * This thread does the initialization and the message loop for
     * ManagedConnections created through createConnectionAsynchronously and
     * createConnectionBlocking
     */
    private class OutgoingConnector implements Runnable {
        private final ManagedConnection _connection;
        private final boolean _doInitialization;

        /**
		 * Creates a new <tt>OutgoingConnector</tt> instance that will
		 * attempt to create a connection to the specified host.
		 *
		 * @param connection the host to connect to
         */
        public OutgoingConnector(ManagedConnection connection,
								 boolean initialize) {
            _connection = connection;
            _doInitialization = initialize;
        }

        public void run() {
            try {
				if(_doInitialization) {
					initializeExternallyGeneratedConnection(_connection);
				}
				startConnection(_connection);
            } catch(IOException ignored) {}
        }
    }

	/**
	 * Runs standard calls that should be made whenever a connection is fully
	 * established and should wait for messages.
	 *
	 * @param conn the <tt>ManagedConnection</tt> instance to start
	 * @throws <tt>IOException</tt> if there is an excpetion while looping
	 *  for messages
	 */
	private void startConnection(ManagedConnection conn) throws IOException {
	    Thread.currentThread().setName("MessageLoopingThread");
		// this can throw IOException
		conn.loopForMessages();
	}

    /**
     * Asynchronously fetches a connection from hostcatcher, then does
     * then initialization and message loop.
     *
     * The ConnectionFetcher is responsible for recording its instantiation
     * by adding itself to the fetchers list.  It is responsible  for recording
     * its death by removing itself from the fetchers list only if it
     * "interrupts itself", that is, only if it establishes a connection. If
     * the thread is interrupted externally, the interrupting thread is
     * responsible for recording the death.
     */
    private class ConnectionFetcher extends ManagedThread {
        //set if this connectionfetcher is a preferencing fetcher
        private boolean _pref = false;
        /**
         * Tries to add a connection.  Should only be called from a thread
         * that has the enclosing ConnectionManager's monitor.  This method
         * is only called from adjustConnectionFetcher's, which has the same
         * locking requirement.
         */
        public ConnectionFetcher() {
            this(false);
        }

        public ConnectionFetcher(boolean pref) {
            setName("ConnectionFetcher");
            _pref = pref;
            // Kick off the thread.
            setDaemon(true);
            start();
        }

        // Try a single connection
        @Override
        public void managedRun() {
            try {
                // Wait for an endpoint.
                Endpoint endpoint = null;
                do {
                    endpoint = _catcher.getAnEndpoint();
                } while ( !IPFilter.instance().allow(endpoint.getAddress()) ||
                          isConnectedTo(endpoint.getAddress()) );
                //Assert.that(endpoint != null);
                _connectionAttempts++;
                ManagedConnection connection = new ManagedConnection(
                    endpoint.getAddress(), endpoint.getPort());
                //set preferencing
                connection.setLocalePreferencing(_pref);

                // If we've been trying to connect for awhile, check to make
                // sure the user's internet connection is live.  We only do
                // this if we're not already connected, have not made any
                // successful connections recently, and have not checked the
                // user's connection in the last little while or have very
                // few hosts left to try.
                long curTime = System.currentTimeMillis();
                if(!isConnected() &&
                   _connectionAttempts > 40 &&
                   ((curTime-_lastSuccessfulConnect)>4000) &&
                   ((curTime-_lastConnectionCheck)>60*60*1000)) {
                    _connectionAttempts = 0;
                    _lastConnectionCheck = curTime;
                    //LOG.logSp("checking for live connection");
                    ConnectionChecker.checkForLiveConnection();
                }

                //Try to connect, recording success or failure so HostCatcher
                //can update connection history.  Note that we declare
                //success if we were able to establish the TCP connection
                //but couldn't handshake (NoGnutellaOkException).
                try {
                    initializeFetchedConnection(connection, this);
                    _lastSuccessfulConnect = System.currentTimeMillis();
                    _catcher.doneWithConnect(endpoint, true);
                    if(_pref) // if pref connection succeeded
                        _needPref = false;
                } catch (NoGnutellaOkException e) {
                    _lastSuccessfulConnect = System.currentTimeMillis();
                    if(e.getCode() == HandshakeResponse.LOCALE_NO_MATCH) {
                        //if it failed because of a locale matching issue
                        //readd to hostcatcher??
                        _catcher.add(endpoint, true,
                                     connection.getLocalePref());
                    }
                    else {
                        _catcher.doneWithConnect(endpoint, true);
                        _catcher.putHostOnProbation(endpoint);
                    }
                    throw e;
                } catch (IOException e) {
                    _catcher.doneWithConnect(endpoint, false);
                    _catcher.expireHost(endpoint);
                    throw e;
                }
                LOG.logSp("before startConnection"  + connection.getAddress());
				startConnection(connection);
            } catch(IOException e) {
                LOG.logSp(e.getMessage());
            } catch (InterruptedException e) {
                // Externally generated interrupt.
                // The interrupting thread has recorded the
                // death of the fetcher, so just return.
                return;
            } catch(Throwable e) {
                //Internal error!
                ErrorService.error(e);
            }
        }

        public String toString() {
            return "ConnectionFetcher";
        }
	}

    /**
     * This method notifies the connection manager that the user does not have
     * a live connection to the Internet to the best of our determination.
     * In this case, we notify the user with a message and maintain any
     * Gnutella hosts we have already tried instead of discarding them.
     */
    public void noInternetConnection() {

        if(_automaticallyConnecting) {
            // We've already notified the user about their connection and we're
            // alread retrying automatically, so just return.
            return;
        }


            // Kill all of the ConnectionFetchers.
            disconnect();

            // Try to reconnect in 10 seconds, and then every minute after
            // that.
            RouterService.schedule(new Runnable() {
                public void run() {
                    // If the last time the user disconnected is more recent
                    // than when we started automatically connecting, just
                    // return without trying to connect.  Note that the
                    // disconnect time is reset if the user selects to connect.
                    if(_automaticConnectTime < _disconnectTime) {
                        return;
                    }

                    if(!RouterService.isConnected()) {
                        // Try to re-connect.  Note this call resets the time
                        // for our last check for a live connection, so we may
                        // hit web servers again to check for a live connection.
                        connect();
                    }
                }
            }, 10*1000, 2*60*1000);
            _automaticConnectTime = System.currentTimeMillis();
            _automaticallyConnecting = true;

        recoverHosts();
    }

    /**
     * Utility method that tells the host catcher to recover hosts from disk
     * if it doesn't have enough hosts.
     */
    private void recoverHosts() {
        // Notify the HostCatcher that it should keep any hosts it has already
        // used instead of discarding them.
        // The HostCatcher can be null in testing.
        if(_catcher != null && _catcher.getNumHosts() < 100) {
            _catcher.recoverHosts();
        }
    }

    /**
     * Utility method to see if the passed in locale matches
     * that of the local client. As of now, we assume that
     * those clients not advertising locale as english locale
     */
    private boolean checkLocale(String loc) {
        if(loc == null)
            loc = /** assume english if locale is not given... */
                ApplicationSettings.DEFAULT_LOCALE;
        return ApplicationSettings.LANGUAGE.equals(loc);
    }

}
