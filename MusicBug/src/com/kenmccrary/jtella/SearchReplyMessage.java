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

import java.util.Vector;
//import java.util.Enumeration;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//import protocol.com.kenmccrary.jtella.util.Log;

/**
 * EDITED BY: Daniel Meyers, 2004<br>
 * Search Reply message(QUERY HIT), response to a search request
 *
 *
 */
public class SearchReplyMessage extends Message {
	private Vector<FileRecord> fileRecords = new Vector<FileRecord>();

	// These are used to send the message
	private SearchMessage searchMessage;
	private short port;
	private String ipAddress;
	private int speed;
	private String vendorCode;

	public SearchMessage getSearchMessage() {
	  return searchMessage;
  }

	/**
	 * Construct a SearchReply message
	 *
	 *
	 */
	SearchReplyMessage() {
		super(Message.QUERYREPLY);
	}

	/**
	 * Construct a search reply message from data
	 * read from network
	 *
	 * @param rawMessage binary data from a connection
	 * @param originatingConnection Connection creating this message
	 */
	SearchReplyMessage(short[] rawMessage, Connection originatingConnection) {
		super(rawMessage, originatingConnection);
	}

	/**
	 * Used to respond to a query message
	 *
	 * @param searchMessage the search thats being responded to
	 * @param port the point used for download
	 * @param ipAddress of the servant
	 * @param speed download speed in kilobytes/sec
	 */
	public SearchReplyMessage(
		SearchMessage searchMessage,
		short port,
		String ipAddress,
		int speed) {
		this(searchMessage, port, ipAddress, speed, null);
	}

	/**
	 * Used to respond to a query message
	 *
	 * @param searchMessage the search thats being responded to
	 * @param port the point used for download
	 * @param ipAddress of the servant
	 * @param speed download speed in kilobytes/sec
	 * @param vendorCode option 4 byte value identifying the servant vendor
	 */
	public SearchReplyMessage(
		SearchMessage searchMessage,
		short port,
		String ipAddress,
		int speed,
		String vendorCode) {
		super(Message.QUERYREPLY);
		setGUID(searchMessage.getGUID());
		this.searchMessage = searchMessage;
		this.port = port;
		this.ipAddress = ipAddress;
		this.speed = speed;
		this.vendorCode = vendorCode;
	}

	/**
	 * Contructs the payload for the search reply message
	 *
	 */
	void buildPayload() {
		LOG.debug("entering buildPayload");
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			DataOutputStream dataStream = new DataOutputStream(byteStream);

			// hit count
			if (fileRecords !=null){
			dataStream.writeByte(fileRecords.size());}
			else{
				LOG.debug("file records null!!");
			}

			// port, little endian
			int portByte1 = 0x00FF & port;
			int portByte2 = (0xFF00 & port) >> 8;
			dataStream.write(portByte1);
			dataStream.write(portByte2);

			if (ipAddress==null){
				LOG.debug("ipAddress null!!");
			}
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

			dataStream.writeByte(ip1);
			dataStream.writeByte(ip2);
			dataStream.writeByte(ip3);
			dataStream.writeByte(ip4);

			// download speed, little endian
			int speedByte1 = 0x000000FF & speed;
			int speedByte2 = (0x0000FF00 & speed) >> 8;
			int speedByte3 = (0x00FF0000 & speed) >> 16;
			int speedByte4 = (0xFF000000 & speed) >> 24;
			dataStream.writeByte(speedByte1);
			dataStream.writeByte(speedByte2);
			dataStream.writeByte(speedByte3);
			dataStream.writeByte(speedByte4);
			LOG.debug("about to put in file records");
			// result set
			for (int i = 0; i < fileRecords.size(); i++) {
				FileRecord fileRecord = fileRecords.elementAt(i);
				byte[] fileRecordData = fileRecord.getBytes();

				dataStream.write(fileRecordData);
			}
			LOG.debug("done put in file records");
			// bearshare trailer
			if (null != vendorCode) {
				byte[] byteData = vendorCode.getBytes();

				// write the 4 byte vendor code
				for (int i = 0; i < 4; i++) {
					dataStream.writeByte(byteData[i]);
				}

				// open data size
				// todo support Quality of Server statistics here
				dataStream.writeByte(0); // no further datafs

			}

			// client identifier
			short[] clientID = Utilities.getClientIdentifier();

			for (int i = 0; i < clientID.length; i++) {
				dataStream.writeByte(clientID[i]);
			}
			LOG.debug("about to go to addpayload()");
			addPayload(byteStream.toByteArray());
			dataStream.close();
		}
		catch (IOException io) {
		}

	}

	/**
	 * Query the number of files found for the search
	 *
	 */
	public int getFileCount() {
		return payload[0];
	}

	/**
	 * Query the port for this search reply
	 *
	 * @return port
	 */
	public int getPort() {
		int port = 0;
		int byte1 = 0;
		int byte2 = 0;
		byte1 |= payload[1];
		byte2 |= payload[2];

		// the port is in little endian format
		port |= byte1;
		port |= (byte2 << 8);

		return port;
	}

	/**
	 * Query the IP address for this pong message
	 * result is an IP address in the form of
	 * "206.26.48.100".
	 *
	 * @return IP address
	 */
	public String getIPAddress() {
		StringBuffer ipBuffer = new StringBuffer();

		ipBuffer
			.append(Integer.toString(payload[3]))
			.append(".")
			.append(Integer.toString(payload[4]))
			.append(".")
			.append(Integer.toString(payload[5]))
			.append(".")
			.append(Integer.toString(payload[6]));

		return ipBuffer.toString();
	}

	/**
	 * Returns the replying host's connection bandwidth
	 *
	 * @return download speed, in kilobytes/sec
	 */
	public int getDownloadSpeed() {
		int byte1 = payload[7];
		int byte2 = payload[8];
		int byte3 = payload[9];
		int byte4 = payload[10];

		return byte1 | (byte2 << 8 | byte3 << 16 | byte4 << 24);

	}

	/**
	 * Adds a file record. This is for originating a message
	 * for a query hit
	 *
	 * @param fileRecord file information
	 */
	public void addFileRecord(FileRecord fileRecord) {
		fileRecords.addElement(fileRecord);
	}

	/**
	 * EDITED BY: Daniel Meyers, 2004
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Get information about the files found
	 *
	 * @param index the index of the FileRecord to obtain
	 */
	public FileRecord getFileRecord(int index) {
		boolean sha1Present = false;
		
		if (0 == fileRecords.size()) {
			int payloadIndex = 11;

			for (int i = 0; i < getFileCount(); i++) {

				// Read the index
				int index1 = payload[payloadIndex++];
				int index2 = payload[payloadIndex++];
				int index3 = payload[payloadIndex++];
				int index4 = payload[payloadIndex++];

				int fileIndex = 0;
				fileIndex |= index1;
				fileIndex |= index2 << 8;
				fileIndex |= index3 << 16;
				fileIndex |= index4 << 24;

				// Read the size
				int size1 = payload[payloadIndex++];
				int size2 = payload[payloadIndex++];
				int size3 = payload[payloadIndex++];
				int size4 = payload[payloadIndex++];

				int fileSize = 0;
				fileSize |= size1;
				fileSize |= size2 << 8;
				fileSize |= size3 << 16;
				fileSize |= size4 << 24;

				// Read the file terminated by double null
				int nullCount = 0;
				Vector stringData = new Vector();
				Vector sha1Data = new Vector();
				
				while (nullCount != 2) {

					byte b1 = (byte)payload[payloadIndex++];

					// If the byte read was null, increment the number of nulls read
					if (b1 == 0) {
						nullCount++;
					}
					
					if (nullCount == 0) {
						// The file name is terminated by null which is sometimes followed
						// by another null but in some cases (mp3) extra data is between
						// the null characters
						stringData.addElement(new Byte(b1));
					}

					// Check for urn string here, it may be followed by other
					// data without a null separating them
					// Form is 'urn:sha1:[32 bytes sha1 string]
					else if (nullCount == 1) {
						
						byte[] comparison = new byte[] {
							(byte)'u',
							(byte)'r',
							(byte)'n',
							(byte)':',
							(byte)'s',
							(byte)'h',
							(byte)'a',
							(byte)'1',
							(byte)':'};
							
						boolean testingSha1 = false;
						
						int j = 0;
						
						while (nullCount != 2) {
							byte b2 = (byte)payload[payloadIndex++];
						
							// If the byte read was null, increment the number of nulls read
							if (b2 == 0) {
								nullCount++;
							}
							
							else if ((j < comparison.length) && (b2 == comparison[j])) {
								if (j == (comparison.length - 1)) {
									sha1Present = true;
									sha1Data.addElement(new Byte((byte)'u'));
									sha1Data.addElement(new Byte((byte)'r'));
									sha1Data.addElement(new Byte((byte)'n'));
									sha1Data.addElement(new Byte((byte)':'));
									sha1Data.addElement(new Byte((byte)'s'));
									sha1Data.addElement(new Byte((byte)'h'));
									sha1Data.addElement(new Byte((byte)'a'));
									sha1Data.addElement(new Byte((byte)'1'));
									sha1Data.addElement(new Byte((byte)':'));
								}
								j++;
								continue;
							}
							
							if ((nullCount != 2) && (j < 41) && (sha1Present)) {
								sha1Data.addElement(new Byte(b2));
								j++;
							}
						}
					}
				}

				byte[] stringBytes = new byte[stringData.size()];
				byte[] sha1Bytes = new byte[sha1Data.size()];
				String sha1 = null;

				for (int z = 0; z < stringBytes.length; z++) {
					stringBytes[z] = ((Byte)stringData.get(z)).byteValue();
				}
				
				if (sha1Present) {
					for (int z = 0; z < sha1Bytes.length; z++) {
						sha1Bytes[z] = ((Byte)sha1Data.get(z)).byteValue();
					}
					sha1 = new String(sha1Bytes);
				}
				
				fileRecords.addElement(
					new FileRecord(
						fileIndex,
						fileSize,
						new String(stringBytes),
						sha1));	
			}
		}

		return (FileRecord)fileRecords.elementAt(index);
	}

	/**
	 * Retrieve the client GUID for the replying servant
	 *
	 * @return client GUID
	 */
	public GUID getClientIdentifier() {
		int startIndex = payloadSize - 16; // GUID is last 16 bytes
		short[] guidData = new short[16];

		for (int i = 0; i < 16; i++) {
			guidData[i] = payload[startIndex++];
		}

		return new GUID(guidData);
	}

	/**
	 * Retrieve the vendor code for the responding servant
	 *
	 * @return vendor code or NONE if the code is not present
	 */
	public String getVendorCode() {
		String code = "NONE";

		if (isBearShareTrailerPresent()) {
			int trailerIndex = getFileRecordBounds();

			byte[] codeData = new byte[4];
			for (int i = 0; i < 4; i++) {
				codeData[i] = (byte)payload[trailerIndex++];
			}

			code = new String(codeData);
		}

		return code;
	}

	/**
	 * Produces a byte[] suitable for
	 * GNUTELLA network
	 *
	 */
	byte[] getByteArray() {
		// construct the payload prior to sending if the payload doesn't exist
		// the payload will not exist for a SearchReply generated by the JTella
		// application. If this is a message we are routing, the payload is read
		// from the stream
		if (0 == getPayloadLength()) {
			buildPayload();
		}

		return super.getByteArray();
	}

	/**
	 * Check if the bearshare trailer is present between file records
	 * and GUID
	 *
	 */
	private boolean isBearShareTrailerPresent() {
		return getFileRecordBounds() != (payloadSize - 16);
	}

	/**
	 * Method to determine the ending for file data. This can be used
	 * to determine if the message contains the BearShare trailer
	 *
	 * @return ending index
	 */
	private int getFileRecordBounds() {
		int boundsIndex = 11;

		for (int i = 0; i < getFileCount(); i++) {
			boundsIndex += 4; // File Index
			boundsIndex += 4; // File Size

			// ascii file name terminated by nulls which may not be continguous
			int nullCount = 0;
			while (2 != nullCount) {
				byte b = (byte)payload[boundsIndex++];
				if (0 == b) {
					nullCount++;
				}
			}
		}

		return boundsIndex;
	}

	/**
	 * EDITED BY: Daniel Meyers, 2004<br>
	 * Represents information about a single file served
	 *
	 */
	static public class FileRecord {
		int index;
		int size;
		String fileName;
		String sha1Hash;

		/**
		 * EDITED BY: Daniel Meyers, 2004
	     * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
		 * Constructs a record describing a shared file
		 *
		 * @param index index of the file
		 * @param size size of the file in bytes
		 * @param fileName file name
		 */
		public FileRecord(int index, int size, String fileName, String sha1Hash) {
			this.index = index;
			this.size = size;
			this.fileName = fileName;
			this.sha1Hash = sha1Hash;
		}

		/**
		 * Get the index of the file
		 *
		 * @return index
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * Get the size of the file
		 *
		 * @return file size
		 */
		public int getSize() {
			return size;
		}

		/**
		 * Get the file name
		 *
		 * @return file name
		 */
		public String getName() {
			return fileName;
		}
		
		/**
		 * ADDED BY: Daniel Meyers, 2004
	     * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
		 * Get the sha1 hash
		 *
		 * @return sha1Hash
		 */
		public String getHash() {
			return sha1Hash;
		}

		/**
		 *  Flatten the <code>FileRecord</code>
		 *
		 *  @return bytes
		 */
		byte[] getBytes() {
			byte[] result = null;
			try {
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				DataOutputStream dataStream = new DataOutputStream(byteStream);

				// write the index, little endian
				int index1 = 0x000000FF & index;
				int index2 = 0x0000FF00 & index;
				index2 = index2 >> 8;
				int index3 = 0x00FF0000 & index;
				index3 = index3 >> 16;
				int index4 = 0xFF000000 & index;
				index4 = index4 >> 24;

				dataStream.write(index1);
				dataStream.write(index2);
				dataStream.write(index3);
				dataStream.write(index4);

				int size1 = 0x000000FF & size;
				int size2 = 0x0000FF00 & size;
				size2 = size2 >> 8;
				int size3 = 0x00FF0000 & size;
				size3 = size3 >> 16;
				int size4 = 0xFF000000 & size;
				size4 = size4 >> 24;

				dataStream.write(size1);
				dataStream.write(size2);
				dataStream.write(size3);
				dataStream.write(size4);

				// write the file name, terminated by a null
				dataStream.write(fileName.getBytes());
				dataStream.writeByte(0);
				
				// wirte the sha1 hash
				dataStream.write(sha1Hash.getBytes());
				dataStream.writeByte(0);

				result = byteStream.toByteArray();
				dataStream.close();
			}
			catch (IOException io) {
			}

			return result;
		}

	}
}