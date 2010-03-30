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

import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Iterator;


//import protocol.com.kenmccrary.jtella.util.Log;

/**
 *  Contains the set of current connections, the node connections
 *  can be in different states, not all may be active
 *
 */
public class ConnectionList {
	/** Name of Logger used by this class. */
    public static final String LOGGER = "com.kenmccrary.jtella";

	private List<NodeConnection> currentConnectionList;

	ConnectionList() {
		currentConnectionList = Collections.synchronizedList(new LinkedList<NodeConnection>());
	}

	
	/**
	 *  Gets a list containing the connections
	 *
	 *  @return list of connections
	 */
	public LinkedList<NodeConnection> getList() {
		return (new LinkedList<NodeConnection>(currentConnectionList));
	}

	/**
	 *  Adds a connection
	 *
	 *  @param connection new connection
	 */
	void addConnection(NodeConnection connection) {
		synchronized (currentConnectionList) {
			currentConnectionList.add(connection);
		}
	}

	/**
	 *  Removes a connection
	 *
	 *  @param connection to remove
	 */
	void removeConnection(NodeConnection connection) {
		synchronized (currentConnectionList) {
			currentConnectionList.remove(connection);
		}
	}

	/**
	 *  Check if a connection exists to a host
	 *
	 *  @param ipAddress host address
	 *  @return true if a connection exists to this IP address
	 */
	boolean contains(String ipAddress) {
		boolean result = false;

		synchronized (currentConnectionList) {
			Iterator<NodeConnection> i = currentConnectionList.iterator();

			while (i.hasNext()) {
				NodeConnection connection = (NodeConnection)i.next();

				if (connection.getConnectedServant().equals(ipAddress)) {
					return true;
				}
			}
		}

		return result;
	}

	/**
	 *  Check if a connection exists to a host with a specific port
	 *
	 *  @param ipAddress host address
	 *  @return true if a connection exists to this IP address
	 */
	boolean contains(String ipAddress, int port) {
		boolean result = false;

		synchronized (currentConnectionList) {
			Iterator<NodeConnection> i = currentConnectionList.iterator();

			while (i.hasNext()) {
				NodeConnection connection = (NodeConnection)i.next();

				if (connection.getConnectedServant().equals(ipAddress) && connection.getConnectedServantPort()==port) {
					return true;
				}
			}
		}

		return result;
	}
	/**
	 *  Get active outgoing and incoming connections
	 *  (A copy of the List)
	 *
	 *  @return active connections
	 */
	List<NodeConnection> getActiveConnections() {
		List<NodeConnection> all = getActiveOutgoingConnections();

		all.addAll(getActiveIncomingConnections());

		return all;
	}

	/**
	 *  Gets the active outgoing connections
	 *
	 *  @return list of connections
	 */
	List<NodeConnection> getActiveOutgoingConnections() {
		return getActiveConnections(NodeConnection.CONNECTION_OUTGOING);
	}

	/**
	 *  Gets the active incoming connections
	 *
	 *  @return active list
	 */
	List<NodeConnection> getActiveIncomingConnections() {
		return getActiveConnections(NodeConnection.CONNECTION_INCOMING);
	}

	/**
	 *  Gets the active outgoing connections
	 *
	 *  @return list of connections
	 */
	private List<NodeConnection> getActiveConnections(int type) {
		LinkedList<NodeConnection> activeList = new LinkedList<NodeConnection>();

		synchronized (currentConnectionList) {
			Iterator<NodeConnection> i = currentConnectionList.iterator();

			while (i.hasNext()) {
				NodeConnection connection = (NodeConnection)i.next();

				if (connection.getType() == type
					&& connection.getStatus() == NodeConnection.STATUS_OK) {
					activeList.add(connection);
				}
			}
		}

		return activeList;
	}

	/**
	 *  Gets a count of the active (running outgoing connections)
	 *
	 *  @return count
	 */
	int getActiveOutgoingConnectionCount() {
		return getActiveConnectionCount(NodeConnection.CONNECTION_OUTGOING);
	}

	/**
	 *  Get the count of active incoming connections
	 *
	 *  @return count
	 */
	int getActiveIncomingConnectionCount() {
		return getActiveConnectionCount(NodeConnection.CONNECTION_INCOMING);
	}

	/**
	 *  Gets a count of the active connections of the given type
	 *
	 *  @return count
	 */
	public int getActiveConnectionCount(int type) {
		int activeCount = 0;

		synchronized (currentConnectionList) {
			Iterator<NodeConnection> i = currentConnectionList.iterator();

			while (i.hasNext()) {
				NodeConnection connection = (NodeConnection)i.next();

				if (connection.getType() == type
					&& connection.getStatus() == NodeConnection.STATUS_OK) {
					activeCount++;
				}
			}
		}

		return activeCount;
	}

	/**
	 *  Reduce the number of outgoing connections
	 *
	 */
	void reduceActiveOutgoingConnections(int newCount) {
		int activeCount = getActiveOutgoingConnectionCount();

		if (activeCount <= newCount) {
			// nothing to do
			return;
		}

		int killCount = activeCount - newCount;
		int killed = 0;
		synchronized (currentConnectionList) {
			Iterator<NodeConnection> i = currentConnectionList.iterator();

			while (i.hasNext() && killed != killCount) {
				NodeConnection connection = (NodeConnection)i.next();

				if (connection.getType() == NodeConnection.CONNECTION_OUTGOING
					&& connection.getStatus() == NodeConnection.STATUS_OK) {
					connection.shutdown();
					killed++;
				}
			}
		}
	}

	/**
	 *  Reduce the number of incoming connections
	 *
	 */
	void reduceActiveIncomingConnections(int newCount) {
		int activeCount = getActiveIncomingConnectionCount();

		if (activeCount <= newCount) {
			// nothing to do
			return;
		}

		int killCount = activeCount - newCount;
		int killed = 0;
		synchronized (currentConnectionList) {
			Iterator<NodeConnection> i = currentConnectionList.iterator();

			while (i.hasNext() && killed != killCount) {
				NodeConnection connection = (NodeConnection)i.next();

				if (connection.getType() == NodeConnection.CONNECTION_INCOMING
					&& connection.getStatus() == NodeConnection.STATUS_OK) {
					connection.shutdown();
					killed++;
				}
			}
		}
	}

	/**
	 *  Remove any dead connections from the list
	 *
	 *  @param type of collection to clean
	 *  @return number of live connections remaining
	 */
	int cleanDeadConnections(int type) {
		int liveCount = 0;
		//LOG.info("Live connection list start: ");

		synchronized (currentConnectionList) {
			Iterator<NodeConnection> i = currentConnectionList.iterator();

			while (i.hasNext()) {
				NodeConnection connection = (NodeConnection)i.next();

				if (connection.getType() == type) {
					if (connection.getStatus() != NodeConnection.STATUS_FAILED
						&& connection.getStatus()
							!= NodeConnection.STATUS_STOPPED) {
						liveCount++;
						//LOG.debug("Live connection: " + connection.getHost() + " status:"+ connection.getStatus());
					}
					else {
						//LOG.debug("removing dead connection: "+ connection.getHost());
						i.remove();
					}
				}
			}
		}

		//LOG.info("Live connection list end");
		return liveCount;
	}

	/**
	 *  Shuts down non-running connections
	 *
	 */
	void stopOutgoingConnectionAttempts() {
		synchronized (currentConnectionList) {
			Iterator<NodeConnection> i = currentConnectionList.iterator();

			while (i.hasNext()) {
				NodeConnection connection = (NodeConnection)i.next();

				if (connection.getType()
					== NodeConnection.CONNECTION_OUTGOING) {
					if (connection.getStatus()
						== NodeConnection.STATUS_CONNECTING) {
						connection.shutdown();
						i.remove();
					}
				}
			}
		}
	}

	/**
	 *  Closes all connections and removes them from the collection
	 *
	 */
	void empty() {
		synchronized (currentConnectionList) {
			Iterator<NodeConnection> i = currentConnectionList.iterator();

			while (i.hasNext()) {
				NodeConnection connection = (NodeConnection)i.next();

				connection.shutdown();
				i.remove();
			}
		}
	}
}
