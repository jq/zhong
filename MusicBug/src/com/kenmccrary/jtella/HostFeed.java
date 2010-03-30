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

import com.kenmccrary.jtella.util.LogFile;

/**
 *  Supplies host addresses to the system by periodicaly pinging caches on
 *  the network
 *
 */
class HostFeed extends Thread
{
  private static int FEED_THRESHOLD = 5;
    
  private Router router;
  private ConnectionData connectionData;
  private HostCache hostCache;
  private boolean shutdownFlag;
  private int state;
  private List cacheList;

  /**
   *  Constructs the host feed
   *
   */
  HostFeed(String host, 
           int port, 
           HostCache hostCache, 
           Router router, 
           ConnectionData connectionData)
  {
    this(hostCache, router, connectionData);
    addHost(host, port);
  }

  /**
   *  Constructs the host feed
   *
   */
  HostFeed(HostCache hostCache, 
           Router router, 
           ConnectionData connectionData)
  {
    this.hostCache = hostCache;
    this.router = router;
    this.connectionData = connectionData;
    shutdownFlag = false;
    cacheList = Collections.synchronizedList(new LinkedList());

  }

  /**
   *  Adds a host to list to use for the feed
   *
   *  @param host address of host to remove
   *  @param port port of host to remove
   */
  void addHost(String host, int port)
  {
    cacheList.add(new Host(host, port, 0, 0));
  }
  
  /**
   *  Removes a host from the list
   *
   *  @param host address of host to remove
   *  @param port port of host to remove
   */
  void removeHost(String host, int port)
  {
    cacheList.remove(new Host(host, port, 0, 0));
  }
  
  /**
   *  Shut down the thread
   *
   */
  void shutdown()
  {
    shutdownFlag = true;
		interrupt();
  }

  /**
   *   The host feed operates all the while the system is running
   *  
   */
  public void run()
  {
    while ( !shutdownFlag ) 
    {
      
      try
      {
        LinkedList tempList = new LinkedList(cacheList);
        
        for (int i = 0; i < tempList.size(); i++)
        {  
          Host host = (Host)tempList.get(i);
          
          HostCacheConnection hostCacheConnection = new  HostCacheConnection(router,
                                                                           hostCache,
                                                                           host.getIPAddress(), 
                                                                           host.getPort(),
                                                                           connectionData);  
          hostCacheConnection.startOutgoingConnection();
        }
      }
      catch (Exception e)
      {
        LogFile.getLog().log(e);
      }        
      

      // Pause processing until host cache needs repopulation
      try
      {
        while ( hostCache.size() > FEED_THRESHOLD || 
                0 == cacheList.size() ) 
        {
          Thread.currentThread().sleep(15000);
        }
      }
      catch (InterruptedException ie)
      {
        // ok
      }

      //Log.getLog().logDebug("Hostcache emptying, host feed starting");
    }
  }
} 
