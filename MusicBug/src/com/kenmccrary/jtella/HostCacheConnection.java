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
//import java.net.InetAddress;

import com.kenmccrary.jtella.util.LogFile;

/**
 *  Connection to a host cache, a servant primarily intended to provide the
 *  locations of active servants on the network.
 *
 */
public class HostCacheConnection extends Connection
{
  private HostCache hostCache;
  
  /**
   *  Construct the Cache Connection using host/port for the cache
   *
   *  @param router message router
   *  @param hostCache cache for servants
   *  @param host can be a machine name or IP address
   *  @param port port to use
   *  @param connectionData data regarding connections
   */
  HostCacheConnection(Router router,
                      HostCache hostCache,
                      String host,
                      int port,
                      ConnectionData connectionData) throws UnknownHostException,
                                                            IOException
  {
    super(router, host, port, connectionData);
    this.hostCache = hostCache;
  }    

  /**
   *  Connection operation
   */
  public void run()
  {
   // int errorCount = 0;
    status = STATUS_OK;

    try
    {
      // Give an inital ping
      prioritySend(new PingMessage());

      while( !shutdownFlag )
      {
        // Read a message
        short[] message = new short[Message.SIZE];
        for (int i = 0; i < message.length; i++)
        {
          message[i] = (short)inputStream.readUnsignedByte();
        }

        Message readMessage = MessageFactory.createMessage(message, this);

        if ( null == readMessage )
        {
          LogFile.getLog().logError("MessageFactory.createMessage() returned null");
          continue;
        }

        int payloadSize = readMessage.getPayloadLength();

        if ( !readMessage.validatePayloadSize() )
        {
          handleConnectionError(null);
          LogFile.getLog().logInformation("Received invalid message from: " +
                                      host +
                                      ", message type: " +
                                      readMessage.getType());
          continue;
        }

        if (payloadSize > 0 )
        {
          short[] payload = new short[payloadSize];
          // Read the payload
          for (int p = 0; p < payloadSize; p++ )
          {
            payload[p] = (short)inputStream.readUnsignedByte();
          }

          readMessage.addPayload(payload);
        }

        LogFile.getLog().logDebug("HostCacheConnection read message from " +
                              host +
                              " : " +
                              readMessage.toString());

				// count the i/o
				inputCount++;

				// always give an ack pong to avoid disconnection
        if ( readMessage instanceof PongMessage )
        {
          // cache the servant tcp address
          PongMessage pongMessage = (PongMessage)readMessage;
          hostCache.addHost(new Host(pongMessage));

        }
      }

    }
    catch (Exception e)
    {
      handleConnectionError(e);
    }
  }
}
