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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
//import java.util.Enumeration;
import java.util.List;
import java.util.LinkedList;
import java.util.Random;

//import protocol.com.kenmccrary.jtella.util.Log;

/**
 * EDITED BY: Daniel Meyers, 2003<br>
 * A cache of the known hosts on the network
 * 
 * edited by Alan oct 7th, made the hosts a HashSet (instead of a vector)
 *
 */
// TODO complete
public class HostCache {

	private static int MAX_CACHED_HOSTS = 20;

	private Set<Host> hosts; 
	private boolean removingAllHosts = false;
	/** Name of Logger used by this class. */
    public static final String LOGGER = "protocol.com.kenmccrary.jtella";

	/**
	 * Constructs an empty HostCache
	 *
	 */
	HostCache() {
		hosts = new HashSet<Host>();
	}
	
	// Add to beginning, remove from beginning (get newest all the time)

	/**
	 * EDITED BY: Daniel Meyers, 2003
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Adds a host to the cache
	 *
	 * @param host Host object representing the host to add
	 */
	public synchronized void addHost(Host host) {
		boolean hostAdded = false;
		
		if (!removingAllHosts) {
			if (!contains(host)) {
				if (hosts.size() < MAX_CACHED_HOSTS) {
					hosts.add(host);
					hostAdded = true;
					// LOG.info("Adding host: " + host.toString());
				}

				else if ((hosts.size() >= MAX_CACHED_HOSTS) && (host.getUltrapeer() == true)) {
					// Iterate thought hosts and if you find one that is not an Ultrapeer replace 
					// it with the Ultrapeer you have got
					// If all hosts cached are ultrapeers replace oldest with this one
					Iterator<Host> iter = hosts.iterator(); 
					
					while (iter.hasNext()) {
						Host thehost =iter.next(); 
						if (!thehost.getUltrapeer()) {
							remove(thehost);
							hosts.add(host);
							// LOG.info("adding host " + host.getIPAddress() + "as replacement of:"+thehost.getIPAddress());
							hostAdded = true;
							break;
						}
					}
			
	
				}
			}
			else{
				// LOG.debug("HostCache: host already known: "+ host.getIPAddress());
			}
		}
	}

	/**
	 * ADDED BY: Daniel Meyers, 2003
	 * <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	 * Adds a host to the Hostcache by IP address and port
	 * 
	 * @param ipAddress A string representation of the IP address of the host to add
	 * @param port The port that the host accepts incoming connections on
	 */
	public synchronized void addHost(String ipAddress, int port) {
		Host host = new Host(ipAddress, port, 0, 0);
		addHost(host);
	}

	/**
	 *  Removes a host from the cache
	 *
	 *  @param ipAddress address of host to remove
	 *  @param port port of host to remove
	 * /
	public synchronized void removeHost(String ipAddress, int port) {
		remove(new Host(ipAddress, port, 0, 0));
	}*/

	/**
	 *  Removes a host from the cache
	 *
	 *  @param host host to remove
	 */
	public synchronized void remove(Host host) {
		hosts.remove(host);
	}

	/**
	 *  Removes all host from the cache, if all hosts are replying as busy
	 *  after time limit X it might be an idea to clear the cache and 
	 *  repopulate from GWebCaches instead of X-Try-Ultrapeers
	 */
	public synchronized void removeAllHosts() {
		removingAllHosts = true;
		while (!hosts.isEmpty()) {
			hosts.remove(0);
		}
		removingAllHosts = false;
	}

	/**
	 *  Get a list of the Hosts cached
	 *
	 *  @return host list
	 */
	public List<Host> getKnownHosts() {
		return new LinkedList<Host>(hosts);
	}

	/**
	 *  Remove a host from the cache, probably because its not responding
	 *
	 */
	public synchronized void removeHost(Host host) {
		hosts.remove(host);
	}

	/**
	 * Return the maximum number of hosts to have cached
	 *
	 */
	public int getMaxHosts() {
		return MAX_CACHED_HOSTS;
	}

	/**
	 *  Query how many hosts are cached
	 *  
	 *  @return number of hosts
	 */
	public int size() {
		return hosts.size();
	}

	/**
	 *  Get the next host available and remove it from hostcache
	 *  
	 *  TODO: reconsider removing it from hostcache?
	 *
	 *  @return host or null if none available
	 */
	Host nextHost() {
		if (size() == 0) {
			return null;
		}

		//return (Host) (getHosts().nextElement());
		//return (Host)hosts.remove(0);
		Host thehost=null;
		Iterator<Host> it = hosts.iterator();
		if (it.hasNext()) {
			 thehost=it.next();
			 it.remove(); //remove the host from the hostcache
		}
		return thehost;
	}

	/**
	 *  Get an iterator of the hosts cached
	 *
	 */
	Iterator<Host> getHosts() {
		return hosts.iterator();// .elements();
	}
	
	/**
	 * Checks if the specified host is present in the Vector
	 * 
	 * @param host the host to check for the presence of
	 * 
	 */
	synchronized boolean contains(Host host) {
		Iterator<Host> iter = hosts.iterator();
		while (iter.hasNext()){
			if (iter.next().getIPAddress().equals(host.getIPAddress())) {
				return true;
			}
		}
		return false;
	}

	/**
	 *  Retrieve a random sample of known hosts. The returned sample may be equal
	 *  to or smaller than the requested size
	 *
	 *  @param sampleSize desired sample size
	 *  @return sample
	 */
	Host[] getRandomSample(int sampleSize) {
		Vector<Host> knownHosts = new Vector<Host>(hosts);

		if (knownHosts.size() > sampleSize) {
			// collect a random sample
			Random random = new Random();
			Host[] hosts = new Host[sampleSize];

			for (int i = 0; i < hosts.length; i++) {
				int randomIndex = random.nextInt(knownHosts.size());
				hosts[i] = (Host)knownHosts.elementAt(randomIndex);
			}

			return hosts;
		}
		else {
			// Known hosts is smaller/equal to the requested sample
			Host[] hosts = new Host[knownHosts.size()];

			for (int i = 0; i < hosts.length; i++) {
				hosts[i] = (Host)knownHosts.elementAt(i);
			}

			return hosts;
		}
	}
}
