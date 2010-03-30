/*
 * Copyright (C) 2000-2001  Ken McCrary
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Email: jkmccrary@yahoo.com
 */
package com.kenmccrary.jtella;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
//import java.util.LinkedList;

import com.kenmccrary.jtella.util.LogFile;
//import com.kenmccrary.jtella.util.LoggingThreadGroup;

import com.dan.jtella.GetHostsFromCache;
import com.dan.jtella.ConnectedHostsListener;

/**
 * EDITED BY: Daniel Meyers, 2003<br>
 * The GNUTellaConnection represents a connection to the GNUTella 
 * <b>network</b>. The connection consists of one or more socket 
 * connections to servant nodes on the network.<p>
 *
 */
public class GNUTellaConnection {

	// Name of logger used
	public static final String LOGGER = "protocol.com.dan.jtella";
	// Instance of logger
	
	//private boolean shutdownFlag;
	private static ConnectionData connectionData;
	private HostCache hostCache;
	private ConnectionList connectionList;
	private Router router;
	private IncomingConnectionManager incomingConnectionManager;
	private OutgoingConnectionManager outgoingConnectionManager;
	private GetHostsFromCache getHostsFromCache;
	//private SearchMonitorSession searchMonitorSession;
	private KeepAliveThread keepAliveThread;

	/**
	 * Constructs an empty connection, the application must add a host cache or
	 * servant to generate activity
	 */
	public GNUTellaConnection() throws IOException {
		this(null);
	}

	/**
	 * EDITED BY: Daniel Meyers, 2003
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Construct the connection specifying connection data. The connection will
	 * not have access to a host cache unless specified later.
	 *
	 * @param connData connection data
	 **/
	public GNUTellaConnection(ConnectionData connData) throws IOException {

		if (null != connData) {
			connectionData = connData;
		}
		else {
			connectionData = new ConnectionData();
		}

		// the cache of known gnutella hosts
		hostCache = new HostCache();
		
		// the connection list contains the connections to GNUTella
		connectionList = new ConnectionList();

		// the router routes messages received on the connections
		router = new Router(connectionList, connectionData, hostCache);

		// This replaces hostfeed as a means of getting hosts for bootstrapping
		getHostsFromCache = new GetHostsFromCache(hostCache, connectionList, connData);

		// Maintains appropriate incoming connections
		incomingConnectionManager =
			new IncomingConnectionManager(
				router,
				connectionList,
				connectionData,
				hostCache);

		outgoingConnectionManager =
			new OutgoingConnectionManager(
				router,
				hostCache,
				connectionList,
				connectionData);

		keepAliveThread = new KeepAliveThread(connectionList);
	}

	/** 
	 * EDITED BY: Daniel Meyers, 2003
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Starts the connection
	 */
	public void start() {
		try {
			// run the components
			router.start();
			getHostsFromCache.start();
			incomingConnectionManager.start();
			outgoingConnectionManager.start();
			keepAliveThread.start();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * EDITED BY: Daniel Meyers, 2003
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Stop the connection, after execution the <code>GNUTellaConnection</code>
	 * is unusable. A new connection must be created if needed. If a 
	 * temporary disconnect from NodeConnections is desired, the connection count
	 * requests can be set to 0
	 *
	 */
	public void stop() {
		keepAliveThread.shutdown();
		getHostsFromCache.shutdown();
		incomingConnectionManager.shutdown();
		outgoingConnectionManager.shutdown();
		connectionList.reduceActiveIncomingConnections(0);
		connectionList.reduceActiveOutgoingConnections(0);
		connectionList.stopOutgoingConnectionAttempts();
		router.shutdown();
	}

	/**
	 * Get the current <code>HostCache</code>. Using the <code>HostCache</code>
	 * an application can query the known hosts, and add and remove hosts
	 *
	 * @return host cache
	 */
	public HostCache getHostCache() {
		return hostCache;
	}

	/**
	 * Query if we are online with the network, with at least one active
	 * node connection
	 *
	 * @return true if online, false otherwise
	 */
	public boolean isOnline() {
		if (null == connectionList) {
			return false;
		}

		return !connectionList.getActiveIncomingConnections().isEmpty()
			|| !connectionList.getActiveOutgoingConnections().isEmpty();
	}

	/**
	 * Get the <code>ConnectionData</code> settings
	 *
	 * @return connection data
	 */
	public ConnectionData getConnectionData() {
		return connectionData;
	}

	/** 
	 * Creates a session to conduct network searches
	 *
	 * @param query search query
	 * @param maxResults maximum result set size
	 * @param minSpeed minimum speed for responding servants
	 * @param receiver receiver for search responses
	 * @return session
	 */
	public void createSearchSession(
			SearchMessage msg, MessageReceiver receiver) {
			new SearchSession(
				msg,
				this,
				router,
				receiver);
	}

	public void removeGUID(GUID guid) {
		router.removeMessageSender(guid);
	}
	

	/**
	 * Adds a listener to the connectionList
	 *
	 */
	public void addListener(ConnectedHostsListener chl) {
		outgoingConnectionManager.addListener(chl);
		incomingConnectionManager.addListener(chl);
	}
	
	
	// TODO the two methods below should possibly be merged
	// consider if the ConnectionList should be publicly available
	/**
	 * Gets the current list of connections to GNUTella
	 *
	 * @return list of connections
	 */
	public List<NodeConnection> getConnectionList() {
		return connectionList.getList();
	}

	/**
	 * Get the connection list
	 */
	ConnectionList getConnections() {
		return connectionList;
	}

	/**
	 * Cleans dead connections from the connection list
	 */
	public void cleanDeadConnections() {
		connectionList.cleanDeadConnections(Connection.CONNECTION_OUTGOING);
	}

	/**
	 * Attempts an outgoing connection on the specified host
	 *
	 * @param ipAddress host IP
	 * @param port port number
	 */
	public void addConnection(String ipAddress, int port) {
		outgoingConnectionManager.addImmediateConnection(ipAddress, port);
	}

	/**
	 * Get the servant identifier the <code>GnutellaConnection</code> 
	 * is using. The servant identifier is used in connection with Push
	 * message processing
	 *
	 * @return servant identifier
	 */
	public short[] getServantIdentifier() {
		return Utilities.getClientIdentifier();
	}

	/**
	 * Sends a message to all connections
	 *
	 * @param m message to broadcast
	 * @param receiver message receiver
	 */
	void broadcast(Message m, MessageReceiver receiver) {
		List<NodeConnection> connections = connectionList.getActiveConnections();

		LogFile.getLog().logDebug(
			"Broadcasting message, type: "
				+ m.getType()
				+ ", to "
				+ connections.size()
				+ " connections");

		ListIterator<NodeConnection> i = connections.listIterator();

		while (i.hasNext()) {
			NodeConnection connection = (NodeConnection)i.next();

			try {
				connection.sendAndReceive(m, receiver);
			}
			catch (IOException io) {
				LogFile.getLog().log(io);
			}
		}
	}

}
