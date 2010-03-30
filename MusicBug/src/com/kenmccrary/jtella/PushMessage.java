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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *  Push message, represents a request to push a file to a receiving node
 *
 *
 */
public class PushMessage extends Message {
	private SearchReplyMessage searchReplyMessage;
	private int fileIndex;
	private String ipAddress;
	private short port;

	/**
	 *  Construct a PUSH message, indicates a node wants this servant
	 *  to connect and transfer a file
	 *
	 */
	PushMessage() {
		super(Message.PUSH);
	}

	/**
	 *  Construct a PushMessage from network data
	 *
	 *  @param rawMessage binary data from a connection
	 *  @param originatingConnection Connection creating this message
	 */
	PushMessage(short[] rawMessage, Connection originatingConnection) {
		super(rawMessage, originatingConnection);
	}

	/**
	 *  Construct a PushMessage using a previously received SearchReplyMessage.
	 *
	 *  @param searchReplyMessage search reply containing file to push
	 *  @param fileIndex index of file (from FileRecord) to push
	 *  @param ipAddress for push transfer
	 *  @param port port for push transfer
	 */
	public PushMessage(
		SearchReplyMessage searchReplyMessage,
		int fileIndex,
		String ipAddress,
		short port) {
		super(Message.PUSH);
		this.searchReplyMessage = searchReplyMessage;
		this.fileIndex = fileIndex;
		this.ipAddress = ipAddress;
		this.port = port;

		buildPayload();
	}

	/**
	 *  Retrieve the client GUID targeted by this push request
	 *
	 *  @return client GUID
	 */
	public GUID getClientIdentifier() {
		short[] guidData = new short[16];

		for (int i = 0; i < 16; i++) {
			guidData[i] = payload[i];
		}

		return new GUID(guidData);
	}

	/**
	 *  Retrieve the index of the file to push
	 *
	 *  @return file index
	 */
	public int getFileIndex() {
		int byte1 = payload[16];
		int byte2 = payload[17];
		int byte3 = payload[18];
		int byte4 = payload[19];

		return byte1 | (byte2 << 8) | (byte3 << 16) | (byte4 << 24);
	}

	/**
	 *  Get the IP Address to push to 
	 *
	 *  @return IP address
	 */
	public String getIPAddress() {
		int byte1 = payload[20];
		int byte2 = payload[21];
		int byte3 = payload[22];
		int byte4 = payload[23];

		return (new Integer(byte1)).toString()
			+ "."
			+ (new Integer(byte2)).toString()
			+ "."
			+ (new Integer(byte3)).toString()
			+ "."
			+ (new Integer(byte4)).toString();
	}

	/**
	 *  Get the port the connection should use
	 *
	 *  @return port
	 */
	public short getPort() {
		int byte1 = payload[24];
		int byte2 = payload[25];

		return (short) (byte1 | (byte2 << 8));
	}

	/**
	 *   Builds the PUSH message payload
	 *
	 */
	void buildPayload() {
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			DataOutputStream dataStream = new DataOutputStream(byteStream);

			// Servant Identifier
			short[] guidData =
				searchReplyMessage.getClientIdentifier().getData();
			for (int i = 0; i < guidData.length; i++) {
				dataStream.writeByte((byte)guidData[i]);
			}

			// File Index
			int indexByte1 = 0x000000FF & fileIndex;
			int indexByte2 = (0x0000FF00 & fileIndex) >> 8;
			int indexByte3 = (0x00FF0000 & fileIndex) >> 16;
			int indexByte4 = (0xFF000000 & fileIndex) >> 24;
			dataStream.writeByte(indexByte1);
			dataStream.writeByte(indexByte2);
			dataStream.writeByte(indexByte3);
			dataStream.writeByte(indexByte4);

			// IP Address
			int beginIndex = 0;
			int endIndex = ipAddress.indexOf('.');

			int ip1 =
				Integer.parseInt(ipAddress.substring(beginIndex, endIndex));

			beginIndex = endIndex + 1;
			endIndex = ipAddress.indexOf('.', beginIndex);

			int ip2 =
				Integer.parseInt(ipAddress.substring(beginIndex, endIndex));

			beginIndex = endIndex + 1;
			endIndex = ipAddress.indexOf('.', beginIndex);

			int ip3 =
				Integer.parseInt(ipAddress.substring(beginIndex, endIndex));

			beginIndex = endIndex + 1;

			int ip4 =
				Integer.parseInt(
					ipAddress.substring(beginIndex, ipAddress.length()));

			dataStream.write(ip1);
			dataStream.write(ip2);
			dataStream.write(ip3);
			dataStream.write(ip4);

			// Port (little endian)
			int byte1 = 0x00FF & port;
			int byte2 = (0xFF00 & port) >> 8;
			dataStream.write(byte1);
			dataStream.write(byte2);

			addPayload(byteStream.toByteArray());
			dataStream.close();

		}
		catch (IOException io) {}
	}

}
