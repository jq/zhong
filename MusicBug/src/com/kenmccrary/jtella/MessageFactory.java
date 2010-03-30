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

//import protocol.com.kenmccrary.jtella.util.Log;
/**
 *  Construct the appropriate <code>Message</code>
 *  subclass
 *
 */
public class MessageFactory {
	/**
	 *  Location of the function type in the header
	 *
	 */
	final static int typeLocation = 16;
	/** Name of Logger used by this class. */
    public static final String LOGGER = "com.kenmccrary.jtella";

	/**
	 *  Factory method providing messages
	 *
	 */
	static Message createMessage(
		short[] rawMessage,
		Connection originatingConnection) {
		short type = rawMessage[typeLocation];
		Message message = null;

		switch (type) {
			case Message.PING :
				{
					message =
						new PingMessage(rawMessage, originatingConnection);
					break;
				}

			case Message.PONG :
				{
					// LOGinfo(						"MessageFactory created a pong message");
					message =
						new PongMessage(rawMessage, originatingConnection);
					break;
				}

			case Message.QUERY :
				{
					// LOGinfo(						"MessageFactory created a query message");
					message =
						new SearchMessage(rawMessage, originatingConnection);
					break;
				}

			case Message.QUERYREPLY :
				{
					// LOGinfo(						"MessageFactory created a query reply message");
					message =
						new SearchReplyMessage(
							rawMessage,
							originatingConnection);
					break;
				}

			case Message.PUSH :
				{
					// LOGinfo(						"MessageFactory created a push message");
					message =
						new PushMessage(rawMessage, originatingConnection);
					break;
				}

			default :
				{
					// LOGerror(						"Message factory can't create message, type: " 						+ Integer.toHexString(type));
					// LOGerror(						"Message factory can't create message, raw bytes: \n");

					StringBuffer buffer = new StringBuffer();
					for (int i = 0; i < rawMessage.length; i++) {
						buffer.append(
							"[" + Integer.toHexString(rawMessage[i]) + "]");

					}
					// LOGerror(buffer.toString());
					break;
				}
		}

		return message;
	}
}
