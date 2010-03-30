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
import java.util.LinkedList;
import java.util.Vector;
//import java.util.Collections;

import com.dan.jtella.ConnectedHostsListener;

//import com.kenmccrary.jtella.util.Log;

/**
 * EDITED BY: Daniel Meyers, 2003<br/>
 * Edited by Alan Davoust, 2009<br/>.
 * Manages the outgoing connections, attempts to connect
 * to the network agressively. Initiates more than required
 * connections in an attempt to quickly achieve connections
 *  
 */
class OutgoingConnectionManager extends ConnectionManager {
	private StarterPool starterPool;
	private Vector<ConnectedHostsListener> listeners;

	/**
	 * Constructs the outgoing connection manager
	 *
	 */
	OutgoingConnectionManager(
		Router router,
		HostCache hostCache,
		ConnectionList connectionList,
		ConnectionData connectionData)
		throws IOException {
		super(
			router,
			connectionList,
			connectionData,
			"OutgoingConnectionManager");
		this.hostCache = hostCache;
		starterPool = new StarterPool(connectionData);
		listeners = new Vector<ConnectedHostsListener>(1, 1);
	}
	
	/**
	 * Adds a listener to this connection manager
	 *
	 */
	public void addListener(ConnectedHostsListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * EDITED BY: Daniel Meyers, 2003
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Main processing loop
	 *
	 */
	public void run() {

		while (!isShutdown()) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				// ignore
				continue;
			}

			// clean dead connections
			int liveCount =
				connectionList.cleanDeadConnections(
					NodeConnection.CONNECTION_OUTGOING);
			
			// precondition, need hosts
			if (hostCache.size() == 0) {
				LOG.debug("no hosts cached");
				continue;
			}

			// count number of active connections
			int activeCount = connectionList.getActiveOutgoingConnectionCount();

			// attempt connections on more servants than necessary to
			// accelerate achieving desired connections
			if (activeCount == connectionData.getOutgoingConnectionCount()) {
				// connection requests satisfied
				LOG.info(activeCount + " connections achieved");
				connectionList.stopOutgoingConnectionAttempts();
				continue;
			}
			else if (activeCount > connectionData.getOutgoingConnectionCount()) {
				// connection requests satisfied  
				LOG.info(
					"Reducing outgoing connections to: "
						+ connectionData.getOutgoingConnectionCount());
				connectionList.stopOutgoingConnectionAttempts();
				connectionList.reduceActiveOutgoingConnections(
					connectionData.getOutgoingConnectionCount());
				continue;

			}
			else if (
				(liveCount - activeCount)
					< ((connectionData.getOutgoingConnectionCount()
						- activeCount)
						* 4)) {
				Host host = hostCache.nextHost();

				LOG.info(
					liveCount
						+ " live connections "
						+ connectionData.getOutgoingConnectionCount()
						+ " requested");

				LOG.info(
					"Connection control attempting a connection: "
						+ host.getIPAddress()
						+ ":"
						+ host.getPort());

				if (connectionList.contains(host.getIPAddress())) { //, host.getPort()
					// avoid duplicate connection  
					LOG.info(
						"Aborting start on duplicate host: " + host.toString());
					continue;
				}

				ConnectionStarter starter = starterPool.getStarter();
				starter.setHost(host);
			}
		}

		// For shutting down, when loop is exited
		starterPool.shutdown();
	}

	/**
	 * Attempts to add an immediate connection, opening a slot if needed
	 *
	 * @param ipAddress host IP address
	 * @param port port number
	 */
	void addImmediateConnection(String ipAddress, int port) {
		int activeCount = connectionList.getActiveOutgoingConnectionCount();

		if (activeCount == connectionData.getOutgoingConnectionCount()) {
			connectionList.reduceActiveOutgoingConnections(
				connectionData.getOutgoingConnectionCount());
		}

		Host host = new Host(ipAddress, port, 0, 0);
		ConnectionStarter starter = starterPool.getStarter();
		starter.setHost(host);
	}

	/**
	 * Asynchronously attempts to start a connection
	 *
	 */
	class ConnectionStarter extends Thread {
		private Host host;
		private ConnectionData connectionData;
		private StarterPool starterPool;
		private boolean shutdown;

		ConnectionStarter(
			StarterPool starterPool,
			ConnectionData connectionData) {
			super("ConnectionStarter");
			this.connectionData = connectionData;
			this.starterPool = starterPool;
		}

		void shutdown() {
			shutdown = true;
			interrupt();
		}

		/**
		 * Set the host for this connection starter to work on
		 *
		 * @param host host to work on
		 */
		void setHost(Host host) {
			this.host = host;
			hostCache.removeHost(host); // TODO rethink this
			synchronized (this) {
				notify();
			}
		}

		/**
		 * Run the starter
		 */
		public void run() {
			while (!shutdown) {
				while (null == host) {
					synchronized (this) {
						try {
							wait();
						}
						catch (InterruptedException e) {
							break;
						}
					}
				}

				if (!shutdown && null != host) {
					try {
						NodeConnection connection =
							new NodeConnection(
								router,
								hostCache,
								host.getIPAddress(),
								host.getPort(),
								connectionData,
								connectionList,
								listeners);
						connectionList.addConnection(connection);
						// Not in NodeConnection. Method is in Connection,
						// which NodeConnection extends
						connection.startOutgoingConnection();
						host = null;
						starterPool.putStarter(this);
					}
					catch (Exception e) {
					}
				}
			}
		}
	}

	/**
	 * Pool of connection starters
	 *
	 */
	class StarterPool {
		private List<ConnectionStarter> starterList;
		private ConnectionData connectionData;
		private boolean shutdown = false;

		/**
		 * Construct the starter pool and populate it
		 *
		 */
		StarterPool(ConnectionData connectionData) {
			starterList = new LinkedList<ConnectionStarter>();
			this.connectionData = connectionData;

			for (int i = 0;
				i < (connectionData.getOutgoingConnectionCount() * 4);
				i++) {
				ConnectionStarter starter =
					new ConnectionStarter(this, connectionData);
				starter.start();
				starterList.add(starter);
			}
		}

		/**
		 * Shutdown the StarterPool
		 */
		synchronized void shutdown() {
			shutdown = true;
			while (!starterList.isEmpty()) {
				ConnectionStarter temp =
					starterList.remove(0);
				temp.shutdown();
			}
		}

		/**
		 * Get a starter thread
		 * 
		 * @return starter thread
		 */
		synchronized ConnectionStarter getStarter() {
			if (starterList.size() == 0) {
				ConnectionStarter starter =
					new ConnectionStarter(this, connectionData);
				starter.start();
				return starter;
			}

			return starterList.remove(0);
		}

		/**
		 * Put a starter back into the pool
		 *
		 * @param connectionStarter connection starter
		 */
		synchronized void putStarter(ConnectionStarter connectionStarter) {
			if (starterList.size()
				> (connectionData.getOutgoingConnectionCount() * 2)
				|| (shutdown)) {
				// not needed
				connectionStarter.shutdown();
				return;
			}

			starterList.add(connectionStarter);
		}
	}
}
