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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

//import protocol.com.kenmccrary.jtella.util.Log;

/**
 * EDITED BY: Daniel Meyers, 2003<br>
 * General purpose utilities
 *
 */
public class Utilities {
	private static short[] clientID = null;
	private static Random rand = new Random();

	/** Name of Logger used by this class. */
	public static final String LOGGER = "com.kenmccrary.jtella";

	/**
	 * EDITED BY: Daniel Meyers, 2003
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Generate something remotely resembling a windows guid
	 * The short this returns is cast to a byte when it is actually used
	 *
	 */
	public static short[] generateGUID() {
		short[] data = new short[16];
		int arrayIndex = 15;
		int randInt = 0x00000000;

		for (int i = 0; i < 4; i++) {
			randInt = rand.nextInt();
			for (int j = 0; j < 4; j++) {
				int mask = 0x000000FF;

				mask = mask << (4 * j);
				int result = randInt & mask;

				result = result >> (4 * j);

				data[arrayIndex--] = (short)result;
			}
		}
		
		return data;
	}

	/**
	 * Generate something resembling a guid for this host
	 *
	 *
	 */
	public static short[] getClientIdentifier() {
		if (null == clientID) {
			clientID = new short[16];
			short[] address = getHostAddress();

			int addressIndex = 0;
			for (int i = 0; i < clientID.length; i++) {
				if (addressIndex == address.length) {
					addressIndex = 0;
				}

				clientID[i] = address[addressIndex++];
			}

			StringBuffer message = new StringBuffer();
			message.append("Client GUID: ");

			for (int i = 0; i < clientID.length; i++) {
				message.append("[" + Integer.toHexString(clientID[i]) + "]");
			}

			LOG.debug(message.toString());
		}

		return clientID;
	}

	/**
	 * Returns the client guid in the form of the wrapper GUID
	 *
	 */
	public static GUID getClientGUID() {
		return new GUID(getClientIdentifier());
	}

	/**
	 * Gets the host address, works around byte[] getAddress()
	 * looking negative
	 *
	 * @return address
	 */
	static short[] getHostAddress() {
		short[] address = new short[4];
		try {
			InetAddress netAddress = InetAddress.getLocalHost();
			String ipAddress = netAddress.getHostAddress();

			int beginIndex = 0;
			int endIndex = ipAddress.indexOf('.');

			address[0] =
				(short)Integer.parseInt(
					ipAddress.substring(beginIndex, endIndex));

			beginIndex = endIndex + 1;
			endIndex = ipAddress.indexOf('.', beginIndex);

			address[1] =
				(short)Integer.parseInt(
					ipAddress.substring(beginIndex, endIndex));

			beginIndex = endIndex + 1;
			endIndex = ipAddress.indexOf('.', beginIndex);

			address[2] =
				(short)Integer.parseInt(
					ipAddress.substring(beginIndex, endIndex));

			beginIndex = endIndex + 1;

			address[3] =
				(short)Integer.parseInt(
					ipAddress.substring(beginIndex, ipAddress.length()));
		}
		catch (UnknownHostException e) {
		}

		return address;
	}

	// test
	public static void main(String[] args) {
		short[] guid = Utilities.generateGUID();

		System.out.println("GUID: ");
		for (int i = 0; i < guid.length; i++) {

			System.out.println(Integer.toHexString(guid[i]));
		}
	}
}
