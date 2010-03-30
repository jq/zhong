package com.macrohard.musicbug;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

//import protocol.com.kenmccrary.jtella.Connection;
import com.kenmccrary.jtella.ConnectionData;
import com.kenmccrary.jtella.GNUTellaConnection;
import com.kenmccrary.jtella.GUID;
import com.kenmccrary.jtella.LOG;
import com.kenmccrary.jtella.MessageReceiver;
import com.kenmccrary.jtella.PushMessage;
import com.kenmccrary.jtella.SearchMessage;
import com.kenmccrary.jtella.SearchReplyMessage;
import com.kenmccrary.jtella.HostCache;
import com.kenmccrary.jtella.Host;
import com.kenmccrary.jtella.SearchSession;


/**
 * Implements a Network Adapter using the Gnutella 0.6 peer-to-peer protocol (as ultrapeer).
 * 
 * @author Michael Yartsev 
 * @author Alan Davoust
 * @version 1.0
 */

public class JTellaAdapter {
    /** Local port where Gnutella listens for incoming connections. */
    private static final int LOCAL_PORT = 6346;

    /** Name of Logger used by this adapter. */
    public static final String LOGGER = "mynode";
        
    private static GNUTellaConnection c;
            
    /**
     * Creates a JTella Adapter
     */
    public JTellaAdapter() {
    	initialize();
    	initializeHostCache();
    }
            
	public GUID search(String msg, MessageReceiver id) {
		SearchMessage search = new SearchMessage(msg, SearchMessage.GET_BY_NAME, 0);
		GUID guid = search.getGUID();
		searchNetwork(search, id);
		return guid;
	}
    
    private void initialize() {
		try {
			ConnectionData connData = new ConnectionData();
			connData.setIncommingConnectionCount(10);
			connData.setOutgoingConnectionCount(10);
			connData.setUltrapeer(false);
			connData.setIncomingPort(LOCAL_PORT);
			connData.setAgentHeader("up2p");
			
			c = new GNUTellaConnection(connData);
			
			c.getConnectionData().setIncomingPort(LOCAL_PORT);
			c.start();
			LOG.info("JTellaAdapter:: init: GnutellaConnection started" );
		} 
		catch(NumberFormatException e) {
			LOG.debug("NumberFormatException while initializing JTella adapter: " + e.getMessage());	
		}
    	catch (UnknownHostException e) {
			LOG.error("UnknownHostException while initializing JTellaAdapter: " + e.getMessage());
		} 
		catch (IOException e) {
			LOG.error("IOException while initializing JTellaAdapter: " + e.getMessage());
		}
    }
    
    /**
     * Initializes the Host Cache
     */
    private void initializeHostCache() {
    	LOG.info("== Initializing Host Cache ==");
    	
    	//For this simple example the list of hosts will be hard coded right here
    	// TODO: get host cache
		Host host1= new Host("134.117.60.64",6346, 1, 1);
		Host h2 = new Host ("141.41.29.78",16229,1,1);
		Host h3 = new Host ("66.74.15.4",31311,1,1);
		//add new hosts if you want...
		
		
    	//Connect to these hosts (if any)
		HostCache hostCache =  c.getHostCache();
    	
		hostCache.addHost(host1);
		hostCache.addHost(h2);
    	
		hostCache.addHost(h3);
    	
		
    	LOG.info("== Finished initializing Host Cache ==");
    }
    

	/**
     * Performs a search on the GNutella network [responses by callback of this.receiveSearchReply]
     */
	public void searchNetwork(SearchMessage msg, MessageReceiver receiver) {
		c.createSearchSession(msg, receiver);
	}
	public void removeGUID(GUID guid) {
		c.removeGUID(guid);
	}

	public void shutdown() {
		//TODO - is there anything else to the shutdown sequence?
		LOG.info("Stopping Gnutella connection");
		c.stop();
	}
	
	public GNUTellaConnection getConnection() {
		// TODO Auto-generated method stub
		return c;
	}
		
	}
