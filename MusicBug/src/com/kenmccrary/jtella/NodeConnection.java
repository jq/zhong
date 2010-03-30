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
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Vector;

import com.dan.jtella.ConnectedHostsListener;
//import com.kenmccrary.jtella.util.Log;

/**
 * EDITED BY: Daniel Meyers, 2003/2004<br>
 * Connection to a servant on the network
 *
 */
public class NodeConnection extends Connection {

	private static final int SEQUENTIAL_READ_ERROR_LIMIT = 5;

	/**
	 * EDITED BY: Daniel Meyers, 2003
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Construct the Connection using host/port information
	 *
	 * @param router message router
	 * @param host can be a machine name or IP address
	 * @param port port to use
	 * @param connectionData ConnectionData instance
	 * @param listeners ConnectedHostsListeners to notify when the ConnectionList changes
	 */
	NodeConnection(
		Router router,
		HostCache hostCache,
		String host,
		int port,
		ConnectionData connectionData,
		ConnectionList connectionList,
		Vector<ConnectedHostsListener> listeners)
		throws UnknownHostException, IOException {
		super(router, hostCache, host, port, connectionData, connectionList, listeners);
	}

	/**
	 * EDITED BY: Daniel Meyers, 2003
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Construct the connection with an existing socket
	 *
	 * @param router message router
	 * @param socket socket connection to another servant
	 * @param connectionData ConnectionData instance
	 * @param listeners ConnectedHostsListeners to notify when the ConnectionList changes
	 */
	NodeConnection(
		Router router,
		HostCache hostCache,
		Socket socket,
		ConnectionData connectionData,
		ConnectionList connectionList,
		Vector<ConnectedHostsListener> listeners)
		throws IOException {
		super(router, hostCache, socket, connectionData, connectionList, listeners);
	}

	/**
	 * EDITED BY: Daniel Meyers, 2004
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Connection operation
	 */
	public void run() {
		status = STATUS_OK;
		int sequentialReadError = 0;

		try {
			PingMessage temp = new PingMessage();
			temp.setTTL((byte)1);
			send(temp);
			
			while (!shutdownFlag)
	START : {

				if (sequentialReadError >= SEQUENTIAL_READ_ERROR_LIMIT) {
					shutdown();
					continue;
				}

				// Read a message
				//byte[] data = new byte[Message.SIZE];
				short[] message = new short[Message.SIZE]; //create empty array the length of the expected header

				//int i = 0;
				//while (i < data.length) {
				for (int i = 0; i < message.length; i++) {//read the message header into the array
					try {
						
						message[i] = (short)inputStream.readUnsignedByte();
						
					}
					catch (IOException io) {
						LOG.debug("Read timeout, sending ping to " + this.getHost());

						// try to recover from read timeout with a ping
						PingMessage keepAlivePing = new PingMessage();
						keepAlivePing.setTTL((byte)1);
						prioritySend(keepAlivePing);
						sequentialReadError++;
						// Wait briefly to allow data to filter through over the internet
						//waitMethod(100);
						break START;
					}
					/*catch (Exception e) {
						System.err.println("NodeConnection\r\n" + e);
						sequentialReadError = SEQUENTIAL_READ_ERROR_LIMIT + 1;
						break START;
					}*/
				}
				
				/*for (int j=0; j < data.length; j++) {
					// & 0xFF to ensure it shows up as an unsigned byte converted to the short
					message[j] = (short)(data[j] & 0xFF);
				}*/

				sequentialReadError = 0;
				
				/*We have now read the header and we send it to the Message Factory
				 */
				Message readMessage = MessageFactory.createMessage(message, this);
				//the parsed result is now stored in the Message object
				
				if (null == readMessage) {
					LOG.error("MessageFactory.createMessage() returned null");
					continue;
				}

				int payloadSize = readMessage.getPayloadLength();

				
				//this filters out messages that do not conform to the format specified in the Gnutella protocol
				if (!readMessage.validatePayloadSize()) {  
					handleConnectionError(null);
					LOG.info(
						"Received invalid message from: "
							+ host
							+ ", message type: "
							+ readMessage.getType());
					continue;
				}
				
				/*
				* We now read the rest of the message (so far we only have the header) depending on the 
				* payload length declared in the header
				*/
				if (payloadSize > 0) {
					short[] payload = new short[payloadSize];
					// Read the payload
					for (int p = 0; p < payloadSize; p++) {
						payload[p] = (short)inputStream.readUnsignedByte();
					}

					readMessage.addPayload(payload);
				}

				LOG.debug("Read message from " + host + " : " + readMessage.toString());

				// count the i/o
				inputCount++;

				// Message is read, route it
				boolean routeOK = router.route(readMessage, this);

				if (!routeOK) {
					// indicates an overrun router, too many connections
					LOG.debug("Connection shut " + "down, overrun router");
					shutdown();
					continue;
				}

				// always give an ack pong to avoid disconnection
				if (readMessage instanceof PingMessage) {
					LOG.info("Responding to ping");
					PongMessage pong =
						new PongMessage(
							readMessage.getGUID(),
							(short)connectionData.getIncomingPort(),
							InetAddress.getLocalHost().getHostAddress(),
							connectionData.getSharedFileCount(),
							connectionData.getSharedFileSize());
					pong.setTTL((byte)readMessage.getTTL());
					LOG.debug("Pong to:"+InetAddress.getLocalHost().getHostAddress()+":"+String.valueOf(connectionData.getIncomingPort()));
					send(pong);
				}
			}
		}
		catch (Exception e) {
			handleConnectionError(e);
		}
	}
}
