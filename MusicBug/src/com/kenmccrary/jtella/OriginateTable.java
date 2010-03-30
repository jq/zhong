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

import java.util.HashMap;
//import java.util.Set;


/**
 *   Contains information for routing originated messages
 *
 *
 */
class OriginateTable {
	private HashMap<GUID, MessageReceiver> hashMap;
	/** Name of Logger used by this class. */
    public static final String LOGGER = "com.kenmccrary.jtella";

	OriginateTable() {
		hashMap = new HashMap<GUID, MessageReceiver>();
	}

	/**
	 *  Puts a GUID to MessageReceiver mapping in the table
	 *
	 *
	 */
	synchronized void put(GUID guid, MessageReceiver receiver) {
		hashMap.put(guid, receiver);
		LOG.debug(
			"OriginateTable storing: "
				+ guid.toString()
				+ ", new size: "
				+ hashMap.size());

	}

	/**
	 *  Removes the guide/receiver mapping
	 *
	 */
	synchronized void remove(GUID guid) {
		hashMap.remove(guid);

		LOG.debug(
			"OriginateTable removing: "
				+ guid.toString()
				+ ", new size: "
				+ hashMap.size());
	}

	/**
	 *  Get a message receiver for a GUID
	 *
	 *
	 */
	synchronized MessageReceiver get(GUID guid) {
		return (MessageReceiver)hashMap.get(guid);
	}

	/**
	 *  Check if <code>MessageReceiver</code> exists for guid
	 *  This is equivalent to checking if we sent a message
	 *
	 */
	boolean containsGUID(GUID guid) {
		return hashMap.containsKey(guid);
	}
}
