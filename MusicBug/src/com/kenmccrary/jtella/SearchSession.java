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

import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.io.IOException;

import android.util.Log;

import com.kenmccrary.jtella.NodeConnection;
 
/**
 *  A session for initiating searches on the network
 *
 */
public class SearchSession extends Thread {
	private MessageReceiver receiver;
	private GNUTellaConnection connection;
	private Router router;
	private SearchMessage msg;

	SearchSession(
		SearchMessage msg,
		GNUTellaConnection connection,
		Router router,
		MessageReceiver receiver) {
			this.connection = connection;
			this.receiver = receiver;
			this.router = router;
			this.msg = msg;
			start();
	}

	/**
	 *   Request a replying servant push a file
	 *
	 *   @param searchReplyMessage search reply containing file to push
	 *   @param pushMessage push message
	 *   @return true if the message could be sent
	 */
	public static boolean sendPushMessage(
		SearchReplyMessage searchReplyMessage,
		PushMessage pushMessage) {
		// the push message will be sent on the connection the searchReply
		// arrived on if it is available
		LOG.debug("In sendPushmessage");
		Connection connection = searchReplyMessage.getOriginatingConnection();

		/*Log.getLog().logInformation*/
		System.out.println("qr connection status: " + connection.getStatus());
		if (connection.getStatus() == NodeConnection.STATUS_OK) {
			try {

				LOG.debug("Sending push");
				connection.prioritySend(pushMessage);
				return true;
			}
			catch (IOException io) {}
		}

		return false;
	}

	public void run() {
		try {
			List<NodeConnection> activeList =
				connection.getConnections().getActiveConnections();

			Log.e("p2p","Active connection list has "+ activeList.size()+" hosts");

			for (int i = 0; i < activeList.size(); i++) {
				NodeConnection nodeConnection =
					(NodeConnection)activeList.get(i);

				nodeConnection.sendAndReceive(msg, receiver);
			}
		}
		catch (Exception e) {
			// keep running
		}
	}
	
}
