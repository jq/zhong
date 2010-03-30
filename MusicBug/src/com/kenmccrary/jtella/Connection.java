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

import java.net.Socket;
import java.net.UnknownHostException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.Vector;

import com.kenmccrary.jtella.HostCache;
import com.kenmccrary.jtella.util.LogFile;
import com.kenmccrary.jtella.util.SocketFactory;

import com.dan.jtella.ConnectedHostsListener;
import com.dan.jtella.HostsChangedEvent;
import com.dan.jtella.PushWaits;

/**
 * EDITED BY: Daniel Meyers, 2003<br>
 * Represents a connection to an application communicating with the GNUTella protocol
 *
 */
public abstract class Connection implements Runnable {
    /** Name of Logger used by this adapter. */
    public static final String LOGGER = "protocol.com.kenmccrary.jtella";
	/**
	 * Connection is attempting to connected to GNUTella
	 */
	public final static int STATUS_CONNECTING = 0;

	/**
	 * Connection is operating normally
	 */
	public final static int STATUS_OK = 1;

	/**
	 * Connection is not operating normally
	 */
	public final static int STATUS_FAILED = 2;

	/**
	 * Connection has been stopped
	 */
	public final static int STATUS_STOPPED = 3;

	/**
	 * Connection created by another servant
	 */
	public final static int CONNECTION_INCOMING = 0;

	/**
	 * Connection created by JTella servant
	 */
	public final static int CONNECTION_OUTGOING = 1;

	/**
	 * Backlog level which will drop ping messages
	 */
	private final static int BACKLOG_PING_LEVEL = 5;

	/**
	 * Backlog level which will drop pong messages
	 */
	private final static int BACKLOG_PONG_LEVEL = 10;

	/**
	 * Backlog level which will drop query messages
	 */
	private final static int BACKLOG_QUERY_LEVEL = 15;

	/**
	 * Backlog level which will drop query reply messages
	 */
	private final static int BACKLOG_QUERYREPLY_LEVEL = 20;

	/**
	 * Backlog level which will drop push messages
	 */
	private final static int BACKLOG_PUSH_LEVEL = 25;

	// Listeners to notify whenever the ConnectionList changes
	private Vector<ConnectedHostsListener> listeners;

	private static String SERVER_READY = "GNUTELLA OK\n\n";
	private static String SERVER_REJECT = "JTella Reject\n\n";
	private static String CONNECT_STRING = "GNUTELLA CONNECT/0.4\n\n";
	private static String CONNECT_STRING_COMPARE = "GNUTELLA CONNECT/0.4";
	private static String V06_CONNECT_STRING = "GNUTELLA CONNECT/0.6\r\n";
	private static String V06_CONNECT_STRING_COMPARE = "GNUTELLA CONNECT/0.6";
	private static String V06_SERVER_READY = "GNUTELLA/0.6 200 OK\r\n";
	private static String V06_SERVER_READY_COMPARE = "GNUTELLA/0.6 200";
	private static String V06_SERVER_REJECT = "GNUTELLA/0.6 503 Service Unavailable\r\n";
	// These 3 are set by the ConnectionData instance on instantiation of
	// this class
	private String V06_AGENT_HEADER;
	private String V06_X_ULTRAPEER;
	private String V06_LISTEN_IP;
	private static String V06_X_ULTRAPEER_NEEDED = "X-Ultrapeer-Needed: true\r\n";
	private static String V06_X_TRY_ULTRAPEERS_COMPARE = "X-Try-Ultrapeers:";
	private static String CRLF = "\r\n";
	private static int MAX_HEADER_SIZE = 4096;
	private List<MessageData> messageBacklog;
	private Thread connectionThread;
	private HostCache hostCache;
	protected boolean shutdownFlag = false;
	protected Socket socket;
	protected DataInputStream inputStream;
	protected DataOutputStream outputStream;
	protected BufferedReader bufferedReader;
	protected BufferedWriter bufferedWriter;
	protected boolean pushRequest = false;
	protected AsyncSender asyncSender;
	protected Router router;
	protected ConnectionData connectionData;
	protected ConnectionList connectionList;
	protected String host;
	protected int port;
	protected int status;
	protected int type;
	// True if the *remote* servent is an ultrapeer
	protected boolean ultrapeer = false;
	protected int inputCount = 0;
	protected int outputCount = 0;
	protected int droppedCount = 0;
	protected long createTime;
	protected long sendTime;

	/**
	 * EDITED BY Daniel Meyers, 2003
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Construct the Connection using host/port information
	 *
	 * @param router message router
	 * @param host can be a machine name or IP address
	 * @param port port to use
	 * @param connectionData contains useful information
	 * @param listeners a Vector of listeners to notify when the ConnectionList changes
	 */
	Connection(
		Router router,
		HostCache hostCache,
		String host,
		int port,
		ConnectionData connectionData,
		ConnectionList connectionList,
		Vector<ConnectedHostsListener> listeners)
		throws UnknownHostException, IOException {
		createTime = System.currentTimeMillis();
		this.V06_AGENT_HEADER = connectionData.getAgentHeader();
		if (connectionData.getUltrapeer()) {
			this.V06_X_ULTRAPEER = "X-Ultrapeer: true\r\n";
		}
		else {
			this.V06_X_ULTRAPEER = "X-Ultrapeer: false\r\n";
		}
		if (connectionData.getGatewayIP() != null) {
			V06_LISTEN_IP =
				"Listen-IP: "
					+ connectionData.getGatewayIP()
					+ ":"
					+ connectionData.getIncomingPort()
					+ CRLF;
		}
		else {
			V06_LISTEN_IP = "";
		}
		this.router = router;
		this.hostCache = hostCache;
		this.host = host;
		this.port = port;
		this.connectionData = connectionData;
		this.connectionList = connectionList;
		this.listeners = listeners;
		messageBacklog = Collections.synchronizedList(new LinkedList<MessageData>());
		type = CONNECTION_OUTGOING;
	}

	/**
	      *  Construct the Connection using host/port information
	      *
	      *  @param router message router
	      *  @param host can be a machine name or IP address
	      *  @param port port to use
	      */
	     Connection(Router router,
	                String host,
	                int port,
	                ConnectionData connectionData) throws UnknownHostException,
	                                                      IOException
	     {
	   		createTime = System.currentTimeMillis();
	       this.router = router;
	       this.host = host;
	       this.port = port;
	       this.connectionData = connectionData;
	       messageBacklog = Collections.synchronizedList(new LinkedList<MessageData>());
	       type = CONNECTION_OUTGOING;
	     }

	
	/**
	 * EDITED BY: Daniel Meyers, 2003
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Construct the connection with an existing socket
	 *
	 * @param router message router
	 * @param socket socket connection to another servant
	 * @param connectionData contains useful information
	 * @param listeners a Vector of listeners to notify when the ConnectionList changes
	 */
	Connection(
		Router router,
		HostCache hostCache,
		Socket socket,
		ConnectionData connectionData,
		ConnectionList connectionList,
		Vector<ConnectedHostsListener> listeners)
		throws IOException {
		createTime = System.currentTimeMillis();
		this.V06_AGENT_HEADER = connectionData.getAgentHeader();
		if (connectionData.getUltrapeer()) {
			this.V06_X_ULTRAPEER = "X-Ultrapeer: true\r\n";
		}
		else {
			this.V06_X_ULTRAPEER = "X-Ultrapeer: false\r\n";
		}
		if (connectionData.getGatewayIP() != null) {
			V06_LISTEN_IP =
				"Listen-IP: "
					+ connectionData.getGatewayIP()
					+ ":"
					+ connectionData.getIncomingPort()
					+ CRLF;
		}
		else {
			V06_LISTEN_IP = "";
		}
		this.router = router;
		this.hostCache = hostCache;
		this.socket = socket;
		this.connectionData = connectionData;
		this.connectionList = connectionList;
		this.listeners = listeners;
		host = socket.getInetAddress().getHostAddress();
		port = socket.getPort();
		type = CONNECTION_INCOMING;
		messageBacklog = Collections.synchronizedList(new LinkedList<MessageData>());
		initSocket();
	}

	/**
	 * Constructor helper
	 *
	 */
	private void initSocket() throws IOException {
		socket.setSoTimeout(7000);
		socket.setTcpNoDelay(true);
		inputStream = new DataInputStream(socket.getInputStream());
		bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		outputStream = new DataOutputStream(socket.getOutputStream());
		bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
	}

	/**
	 * ADDED BY: Daniel Meyers, 2004
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Notifies all listeners of some change to the connection
	 *
	 */
	void notifyListeners() {
		if (!listeners.isEmpty()) {
			Vector<ConnectedHostsListener> tempListeners = (Vector<ConnectedHostsListener>)listeners.clone();
			for (int i = 0; i < tempListeners.size(); i++) {
				ConnectedHostsListener temp = (ConnectedHostsListener)tempListeners.get(i);
				temp.hostsChanged(new HostsChangedEvent(this));
			}
		}
	}

	/**
	 * Retrieve the host this connection links to
	 *
	 */
	String getHost() {
		return host;
	}

	/**
	 * EDITED BY: Daniel Meyers, 2004
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Stops the connection and cleans up
	 *
	 */
	public void shutdown() {
		shutdownFlag = true;

		if (null != connectionThread) {
			connectionThread.interrupt();
		}

		if (null != asyncSender) {
			asyncSender.shutdown();
		}

		if ((status == STATUS_OK) || (status == STATUS_FAILED)) {
			status = STATUS_STOPPED;
			notifyListeners();
		}
		else {
			status = STATUS_STOPPED;
		}

		// Only close the socket if it is not going to be handed on as
		// part of the response to a push request
		if (!pushRequest) {
			try {
				if (null != bufferedWriter) {
					bufferedWriter.close();
				}
			}
			catch (IOException ioe) {}
			try {
				if (null != bufferedReader) {
					bufferedReader.close();
				}
			}
			catch (IOException ioe) {}
			try {
				if (null != outputStream) {
					outputStream.close();
				}
			}
			catch (IOException ioe) {}
			try {
				if (null != inputStream) {
					inputStream.close();
				}
			}
			catch (IOException ioe) {}
			try {
				if (null != socket) {
					socket.close();
				}
			}
			catch (IOException ioe) {}
		}
	}

	/**
	 * ADDED BY: Daniel Meyers, 2004
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * The Thread.wait() method cannot be called from an unsyncronised method, so a synchronised
	 * one was created for the purpose of allowing the Connection to wait()
	 * 
	 * @param millis The number of milliseconds to wait for. 0 is equal to no limit
	 */
	protected synchronized void waitMethod(int millis) {
		try {
			if (millis == 0) {
				wait();
			}
			else {
				wait(millis);
			}
		}
		catch (InterruptedException ie) {}
	}

	/**
	 * EDITED BY: Daniel Meyers, 2004
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Starts an incomming connection to a node on the network,
	 * does initial handshake
	 *
	 * @param true to accept connection, false to reject
	 * @return true for a good start, false otherwise
	 */
	boolean startIncomingConnection(boolean accept) {
		boolean result = false;
		pushRequest = false;
		StringBuffer inputData = new StringBuffer(64);

		try {
			String request = "";
			request = bufferedReader.readLine();
			/*byte[] data = new byte[V06_CONNECT_STRING.length()];
			inputStream.readFully(data);
			String requestPart = new String(data);
			request += requestPart;
			
			while (!requestPart.equals("")) {
				data = new byte[inputStream.available()];
				inputStream.readFully(data);
				requestPart = new String(data);
				request += requestPart;
				if (inputStream.available() <= 0) {
					waitMethod(100);
				}
			}*/

			if (request == null) {
				shutdown();
				return result;
			}
			// Gnutella 0.4 connection
			else if (request.startsWith(CONNECT_STRING_COMPARE)) {
				String response = SERVER_REJECT;

				// GNUTella 0.4 handshake
				if (connectionData.getUltrapeer()) {
					// the 0.4 handshake is good
					result = true;
					response = SERVER_READY;
				}

				// write the response
				//outputStream.write(response.getBytes());
				//outputStream.flush();
				bufferedWriter.write(response);
				bufferedWriter.flush();

			}
			// Gnutella 0.6 connection
			else if (request.startsWith(V06_CONNECT_STRING_COMPARE)) {

				// Check if we should accept the connection
				if (connectionList.getActiveIncomingConnectionCount()
					< connectionData.getIncommingConnectionCount()) {

					// LOG.info("Incoming connection");

					// Look for X-Ultrapeer header (only used if we are not an ultrapeer)
					while ((request != null) && (!request.equals(""))) {

						if ((request.equalsIgnoreCase("X-Ultrapeer: true"))
							|| (request.equalsIgnoreCase("X-Ultrapeer: yes"))) {

							ultrapeer = true;
						}

						try {
							// TODO Store any others for use?
							request = bufferedReader.readLine();
						}
						catch (IOException ioe) {
							// LOG.error("#1: " + ioe);
							break;
						}
					}

					// Second level check
					if (connectionData.getUltrapeer() || ultrapeer) {
						// A connect string was recognized, send the response indicating
						// the version number and user agent
						// TODO Send X-Try-Ultrapeers
						String response = "";

						if (connectionData.getUltrapeer()) {
							response =
								V06_SERVER_READY
									+ V06_LISTEN_IP
									+ "Remote-IP: "
									+ host
									+ CRLF
									+ V06_X_ULTRAPEER
									+ V06_AGENT_HEADER
									+ CRLF;
						}
						else {
							response =
								V06_SERVER_READY
									+ V06_LISTEN_IP
									+ "Remote-IP: "
									+ host
									+ CRLF
									+ V06_AGENT_HEADER
									+ CRLF;
						}

						bufferedWriter.write(response);
						bufferedWriter.flush();

						// Read the client's response and any headers
						// Only if the client accepts our headers will the connect succeed
						String line = "";
						try {
							line = bufferedReader.readLine();
						}
						catch (IOException ioe) {
							// LOG.error("#2: " + ioe);
						}

						if (line != null) {
							if (line.startsWith(V06_SERVER_READY_COMPARE)) {
								// success
								result = true;

								// read all headers
								/*while (line != "") {
									// TODO store these for use
									try {
										line = bufferedReader.readLine();
									}
									catch (IOException ioe) {
										// LOG.error("#3: " + ioe);
										break;
									}
								}*/
							}
						}
					}
					else {
						String response = V06_SERVER_REJECT + CRLF;
						bufferedWriter.write(response);
						bufferedWriter.flush();
					}
				}
				else {
					String response = V06_SERVER_REJECT + CRLF;
					bufferedWriter.write(response);
					bufferedWriter.flush();
				}
			}
			// Incoming connection in response to a push message
			else if (request.startsWith("GIV")) {
				pushRequest = true;

				// Get servent ID as a string
				String guidString = (request.split(".*:", 2))[1].split("/.*", 2)[0];

				// Notify waiting PushResponseInterface
				PushWaits.pushReceived(
					guidString,
					socket,
					inputStream,
					outputStream,
					bufferedReader,
					bufferedWriter);
			}
		}
		catch (IOException ioe) {}

		if ((result) && (!shutdownFlag)) {

			status = STATUS_OK;

			// start the asynchronous sender
			asyncSender = new AsyncSender();
			asyncSender.start();

			// Now start monitoring network messages
			connectionThread = new Thread(this, "ConnectionThread");
			connectionThread.start();
			/*Log.getLog().logDebug*/
			// LOG.info("Incoming connection accepted");

			notifyListeners();
		}
		else {
			shutdown();
		}

		return result;
	}

	/**
	 * EDITED BY: Daniel Meyers, 2004
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Starts an outgoing connection to a node on the network,
	 * does initial handshaking
	 *
	 * @return true for a good start, false for failure
	 */
	boolean startOutgoingConnection() {
		boolean result = true;

		status = STATUS_CONNECTING;

		try {
			if (null == socket) {
				/*
				socket = new Socket(//InetAddress.getByName(host),
				                    host,
				                    port);
				 */
				// ten second max wait
				socket = SocketFactory.getSocket(host, port, 10000);

				initSocket();

			}
			// LOG.debug("Sending greeting to : " + host);

			String remoteHost = socket.getInetAddress().getHostAddress();

			connectionData.setConnectionGreeting(
				V06_CONNECT_STRING
					+ V06_AGENT_HEADER
					+ V06_X_ULTRAPEER
					+ V06_X_ULTRAPEER_NEEDED
					+ V06_LISTEN_IP
					+ "Remote-IP: "
					+ remoteHost
					+ CRLF
					+ CRLF);
			byte[] greetingData = connectionData.getConnectionGreeting().getBytes();
			outputStream.write(greetingData, 0, greetingData.length);
			outputStream.flush();

			// Is this blocking the entire system for too long?
			String response = bufferedReader.readLine();
			/*Log.getLog().logDebug*/
			// LOG.info("Received greeting response: " + response + " from host: " + host);

			if (!response.startsWith(V06_SERVER_READY_COMPARE)) {
				LogFile.getLog().logWarning("Connection rejection: " + host);
				status = STATUS_FAILED;
				result = false;
			}

			boolean foundXTryUltrapeers = false;
			boolean foundXUltrapeer = false;

			while (response != null) {

				// Look for X-Try headers and cache them. 
				// Form X-Try-Ultrapeers: 1.2.3.4:1234, 5.6.7.8:5678\r\n
				if (response.startsWith(V06_X_TRY_ULTRAPEERS_COMPARE)) {

					StringTokenizer ultrapeers = new StringTokenizer(response, ":, ");
					// Remove 'X-Try-Ultrapeers:'
					ultrapeers.nextToken();

					// Set delims to ", " to seperate out the ultrapeers, and 
					// remove one on each iteration of the loop
					// Tokenise the host IP and port, and create a new 
					// Ultrapeer type host
					while (ultrapeers.hasMoreTokens()) {
						Host host =
							new Host(
								ultrapeers.nextToken(),
								Integer.parseInt(ultrapeers.nextToken()),
								0,
								0);
						host.setUltrapeer(true);
						hostCache.addHost(host);
					}

					foundXTryUltrapeers = true;
				}

				// Look for ultrapeer status of node
				if ((response.equalsIgnoreCase("X-Ultrapeer: true"))
					|| (response.equalsIgnoreCase("X-Ultrapeer: yes"))) {
					ultrapeer = true;

					foundXUltrapeer = true;
				}

				if (foundXTryUltrapeers && foundXUltrapeer) {
					break;
				}

				if (bufferedReader.ready())
					response = bufferedReader.readLine();
				else break;
			}

			if (result) {
				try {

					bufferedWriter.write(V06_SERVER_READY + CRLF);
					bufferedWriter.flush();
					
					// LOG.info("Connection started on: " + host);
					status = STATUS_OK;

					// start the async sender
					asyncSender = new AsyncSender();
					asyncSender.start();

					// Now start monitoring network messages
					connectionThread = new Thread(this, "ConnectionThread");
					// Starts NodeConnection's run method
					connectionThread.start();

					notifyListeners();
				}
				catch (IOException ioe) {
					// LOG.error("I/O Exception transmitting ack to server");
					status = STATUS_FAILED;
					result = false;
				}
			}
		}
		catch (Exception e) {
			// LOG.error(e);
			result = false;
		}
		if (!result) {
			shutdown();
		}

		return result;
	}

	// TODO return a boolean indicating if the message was enqueued

	/**
	 * Send a priority message. Priority message are not subject to flow control
	 * dropping
	 *
	 * @param m message
	 */
	public void prioritySend(Message m) throws IOException {
		if (shutdownFlag) {
			return;
		}

		enqueueMessage(m, true);
	}

	/**
	 * Sends a Message through this connection
	 *
	 * @param m message
	 */
	public void send(Message m) throws IOException {
		if (shutdownFlag) {
			return;
		}
		// LOG.debug("About to enqueue message "+ m.toString());

		enqueueMessage(m, false);

	}

	/**
	 * Sends a message down the connection and sends any response
	 * to <code>MessageReceiver</code>
	 *
	 */
	public void sendAndReceive(Message m, MessageReceiver receiver) throws IOException {
		if (shutdownFlag) {
			return;
		}

		// LOG.debug(			"Connection sendAndReceive: "				+ m.getType()				+ " to "				+ socket.getInetAddress().getHostAddress());

		router.routeBack(m, receiver);

		prioritySend(m);
	}

	/**
	 * Adds a message to the send queue, subject to dropping due to a message
	 * backlog
	 *
	 */
	public void enqueueMessage(Message message, boolean priority) {

		if (droppedCount > (.5 * outputCount)) {
			// drop this connection, it is hung or performing at less than half our
			// send rate
			shutdown();
			return;
		}

		int type = message.getType();
		int backlogSize = messageBacklog.size();

		if (!priority) {
			switch (type) {
				case Message.PING :
					{
						if (backlogSize > BACKLOG_PING_LEVEL) {
							// LOG.debug("dropping PING message");
							droppedCount++;
							return;
						}

						break;
					}

				case Message.PONG :
					{
						if (backlogSize > BACKLOG_PONG_LEVEL) {
							// LOG.debug("dropping PONG message");
							droppedCount++;
							return;
						}

						break;
					}

				case Message.QUERY :
					{
						if (backlogSize > BACKLOG_QUERY_LEVEL) {
							droppedCount++;
							return;
						}

						break;
					}

				case Message.QUERYREPLY :
					{
						if (backlogSize > BACKLOG_QUERYREPLY_LEVEL) {
							droppedCount++;
							return;
						}

						break;
					}

				case Message.PUSH :
					{
						if (backlogSize > BACKLOG_PUSH_LEVEL) {
							droppedCount++;

							// give up on this connection
							shutdown();
							return;
						}

						break;
					}
			}
		}

		MessageData messageData = new MessageData(message, priority);

		if (priority) {
			synchronized (messageBacklog) { //lock object backlog while we're adding messages
			messageBacklog.add(0, messageData);
			// LOG.debug("Connection:: enqueued priority message of type + "+ message.getType());
			}
		}
		else {
			// add non-priority messages at the end of the list
			synchronized (messageBacklog) {
				// prevent race between size/add
				int currentSize = messageBacklog.size();
				messageBacklog.add(0 == currentSize ? 0 : currentSize - 1, messageData);
				// LOG.debug("Connection:: enqueued non-priority message of type + "+ message.getType());
			}
		}

		synchronized (asyncSender) {
			asyncSender.notify();
		}

	}

	/**
	 * Handles a serious error on the connection
	 *
	 */
	void handleConnectionError(Exception e) {
		// LOG.debug("Shutting down connection: " + host);
		status = STATUS_FAILED;
		if (null != e) {
			LogFile.getLog().log(e);
		}
		shutdown();
	}

	/**
	 * Get the connected host
	 *
	 * @return host name
	 */
	public String getConnectedServant() {
		return host;
	}
	
	/**
	 * Get port for the connected host
	 *
	 * @return host port
	 */
	public int getConnectedServantPort() {
		return port;
	}
	/**
	 * ADDED BY: Daniel Meyers, 2003
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Get the ultrapeer status of this servent
	 * 
	 * @return a boolean representing the ultrapeer status of this servent
	 */
	public boolean getUltrapeer() {
		return ultrapeer;
	}

	/**
	 * Get the current status of the connection
	 *
	 * @return status
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Get the type of connection, incoming or outgoing
	 *
	 * @return connection type
	 */
	public int getType() {
		return type;
	}

	/**
	 * Get the message output count
	 *
	 * @return output
	 */
	public int getMessageOutput() {
		return outputCount;
	}

	/**
	 * Get the message input count
	 *
	 * @return input
	 */
	public int getMessageInput() {
		return inputCount;
	}

	/**
	 * Get the number of messages dropped on this connection
	 *
	 * @return dropcount
	 */
	public int getMessageDropCount() {
		return droppedCount;
	}

	/**
	 * Get the lenght of time the connection has lived
	 *
	 * @return time in seconds
	 */
	public int getUpTime() {
		long msLife = System.currentTimeMillis() - createTime;

		return (int) (msLife / 1000);
	}

	/**
	 * Returns the timestamp of the last send
	 *
	 * @return timestamp
	 */
	public long getSendTime() {
		return sendTime;
	}

	/**
	 * Message data stored in the message backlog
	 *
	 */
	class MessageData {
		private Message message;
		private boolean priority;

		/**
		 * Constructs message data
		 *
		 * @param message GNUTella message
		 * @param priority priority messages are not subject to dropping
		 */
		MessageData(Message message, boolean priority) {
			this.message = message;
			this.priority = priority;
		}

		/**
		 * Get the message
		 *
		 * @return message
		 */
		Message getMessage() {
			return message;
		}

		/**
		 * Check if this is a priority message. Generally, messages originated by
		 * the JTella servant are considered priority messages
		 *
		 * @return priority flag
		 */
		boolean isPriority() {
			return priority;
		}
	}

	/**
	 * Provides a mechanism to send a message and handle the problem of
	 * blocking on write, due to an unresponsive servant on the connection
	 *
	 */
	class AsyncSender extends Thread {
		private boolean shutdown = false;

		AsyncSender() {
			super("AsyncSender");
		}

		/**
		 * Get the message
		 *
		 */
		public Message getMessage() {
			int size = messageBacklog.size();

			while (0 == size && !shutdown) {
				try {
					// LOG.debug("AsyncSender waits for a new message!");
					synchronized (this) {
						wait();
					}
				}
				catch (InterruptedException ie) {}
				// LOG.debug("AsyncSender awakens!");
				size = messageBacklog.size();
			}

			if (shutdown) {
				return null;
			}
			Message m=null;
			synchronized (messageBacklog){ //lock the msg queue while we retrieve a message
				m = (messageBacklog.remove(0)).getMessage();
			}
			return m;
		}

		public void shutdown() {
			shutdown = true;
			interrupt();
		}

		public void run() {
			while (!shutdown) {
				Message message = getMessage();

				if (message != null) {
					try {
						//// LOG.debug("line1");
						Connection.this.sendTime = System.currentTimeMillis();
						//// LOG.debug("line2");
						byte[] messageBytes = message.getByteArray();
						//// LOG.debug("line3");
						outputCount++;

						// LOG.info("Sending message of type "+message.getType()+ " to host " + Connection.this.getHost());
						// LOG.debug(message.toString());
						Connection.this.outputStream.write(messageBytes, 0, messageBytes.length);
						//// LOG.debug("line5");
						Connection.this.outputStream.flush();

						// temp
						/*
						StringBuffer buffer = new StringBuffer();
						for (int i = 0; i < messageBytes.length; i++) 
						{
						  buffer.append("[" +
						                Integer.toHexString(messageBytes[i]) +
						                "]");
						
						}
						Log.getLog().logDebug("Sent message: " +
						                      message.getType() +
						                      "\n" +
						                      buffer.toString());
						*/
						// end temp

					}
					catch (Exception e) {
						shutdown();
						// LOG.error("AsyncSender error!");
						// LOG.error(e);
						// LOG.error(e.getMessage());
						
					}
				}

			}
		}
	}
}
