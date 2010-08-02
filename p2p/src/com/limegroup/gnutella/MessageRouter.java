package com.limegroup.gnutella;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;
import com.limegroup.gnutella.search.ResultCounter;
import com.limegroup.gnutella.security.User;
import com.limegroup.gnutella.udpconnect.UDPConnectionMessage;
import com.limegroup.gnutella.udpconnect.UDPMultiplexor;
import com.limegroup.gnutella.util.FixedsizeHashMap;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.NoMoreStorageException;
import com.limegroup.gnutella.util.Sockets;
import com.limegroup.gnutella.util.Utilities;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.util.LOG;


/**
 * One of the three classes that make up the core of the backend.  This
 * class' job is to direct the routing of messages and to count those message
 * as they pass through.  To do so, it aggregates a ConnectionManager that
 * maintains a list of connections.
 */
public abstract class MessageRouter {
    /**
     * Handle to the <tt>ConnectionManager</tt> to access our TCP connections.
     */
    protected static ConnectionManager _manager;

    /**
     * The GUID we attach to QueryReplies to allow PushRequests in
     * responses.
     */
    protected byte[] _clientGUID;


	/**
	 * Reference to the <tt>ReplyHandler</tt> for messages intended for 
	 * this node.
	 */
    private final ReplyHandler FOR_ME_REPLY_HANDLER = 		ForMeReplyHandler.instance();
		
    /**
     * The maximum size for <tt>RouteTable</tt>s.
     */
    private int MAX_ROUTE_TABLE_SIZE = 100;  //actually 100,000 entries

    /**
     * Maps QueryRequest GUIDs to QueryReplyHandlers.  Stores 5-10 minutes,
     * typically around 13000 entries, but never more than 100,000 entries.
     */
    private RouteTable _queryRouteTable = 
        new RouteTable(5*60, MAX_ROUTE_TABLE_SIZE);
    /**
     * Maps QueryReply client GUIDs to PushRequestHandlers.  Stores 7-14
     * minutes, typically around 3500 entries, but never more than 100,000
     * entries.  
     */
    private RouteTable _pushRouteTable = 
        new RouteTable(7*60, MAX_ROUTE_TABLE_SIZE);
    
    /**
     * Maps HeadPong guids to the originating pingers.  Short-lived since
     * we expect replies from our leaves quickly.
     */
    private RouteTable _headPongRouteTable = 
    	new RouteTable(10, MAX_ROUTE_TABLE_SIZE);

    /** How long to buffer up out-of-band replies.
     */
    private static final long CLEAR_TIME = 30 * 1000; // 30 seconds

    /** Time between sending HopsFlow messages.
     */
    private static final long HOPS_FLOW_INTERVAL = 15 * 1000; // 15 seconds

    /** The maximum number of UDP replies to buffer up.  Non-final for 
     *  testing.
     */
    static int MAX_BUFFERED_REPLIES = 250;

    /**
     * Keeps track of QueryReplies to be sent after recieving LimeAcks (sent
     * if the sink wants them).  Cleared every CLEAR_TIME seconds.
     * TimedGUID->QueryResponseBundle.
     */
    private final Map _outOfBandReplies = new Hashtable();

    /**
     * Keeps track of what hosts we have recently tried to connect back to via
     * UDP.  The size is limited and once the size is reached, no more connect
     * back attempts will be honored.
     */
    private static final FixedsizeHashMap _udpConnectBacks = 
        new FixedsizeHashMap(200);
        
    /**
     * The maximum numbers of ultrapeers to forward a UDPConnectBackRedirect
     * message to, per forward.
     */
    private static final int MAX_UDP_CONNECTBACK_FORWARDS = 5;

    /**
     * Keeps track of what hosts we have recently tried to connect back to via
     * TCP.  The size is limited and once the size is reached, no more connect
     * back attempts will be honored.
     */
    private static final FixedsizeHashMap _tcpConnectBacks = 
        new FixedsizeHashMap(200);
        
    /**
     * The maximum numbers of ultrapeers to forward a TCPConnectBackRedirect
     * message to, per forward.
     */
    private static final int MAX_TCP_CONNECTBACK_FORWARDS = 5;        
    
    /**
     * The processingqueue to add tcpconnectback socket connections to.
     */
    private static final ProcessingQueue TCP_CONNECT_BACKER =
        new ProcessingQueue("TCPConnectBack");
	/**
	 * A handle to the thread that deals with QRP Propagation
	 */

    /**
     * The lifetime of OOBs guids.
     */
    private static final long TIMED_GUID_LIFETIME = 25 * 1000; 

    /**
     * Keeps track of Listeners of GUIDs.
     * GUID -> List of MessageListener
     */
    private volatile Map _messageListeners = Collections.EMPTY_MAP;
    
    /**
     * Lock that registering & unregistering listeners can hold
     * while replacing the listeners map / lists.
     */
    private final Object MESSAGE_LISTENER_LOCK = new Object();

     /**
     * Router for UDPConnection messages.
     */
	private final UDPMultiplexor _udpConnectionMultiplexor =
	    UDPMultiplexor.instance(); 

    /**
     * Creates a MessageRouter.  Must call initialize before using.
     */
    protected MessageRouter() {
        _clientGUID=RouterService.getMyGUID();
    }

    /**
     * Links the MessageRouter up with the other back end pieces
     */
    public void initialize() {
        _manager = RouterService.getConnectionManager();
        // schedule a runner to clear unused out-of-band replies
        RouterService.schedule(new Expirer(), CLEAR_TIME, CLEAR_TIME);
        // schedule a runner to clear guys we've connected back to
        RouterService.schedule(new ConnectBackExpirer(), 10 * CLEAR_TIME, 
                               10 * CLEAR_TIME);
        // schedule a runner to send hops-flow messages
        RouterService.schedule(new HopsFlowManager(), HOPS_FLOW_INTERVAL*10, 
                               HOPS_FLOW_INTERVAL);
    }

    public String getQueryRouteTableDump() {
        return _queryRouteTable.toString();
    }

    /**
     * A callback for ConnectionManager to clear a <tt>ReplyHandler</tt> from
     * the routing tables when the connection is closed.
     */
    public void removeConnection(ReplyHandler rh) {
        _queryRouteTable.removeReplyHandler(rh);
        _pushRouteTable.removeReplyHandler(rh);
        _headPongRouteTable.removeReplyHandler(rh);
    }

	/**
     * The handler for all message types.  Processes a message based on the 
     * message type.
	 *
	 * @param m the <tt>Message</tt> instance to route appropriately
	 * @param receivingConnection the <tt>ManagedConnection</tt> over which
	 *  the message was received
     */
    public void handleMessage(Message msg, 
                              ManagedConnection receivingConnection) {
        // Increment hops and decrease TTL.
        msg.hop();
	   
        if(msg instanceof PingRequest) {
        	handlePingRequest((PingRequest)msg, 
											   receivingConnection);
		} else if (msg instanceof PingReply) {
            handlePingReply((PingReply)msg, receivingConnection);
		} else if (msg instanceof ByeRequest) {
			//ReceivedMessageStatHandler.TCP_PING_REPLIES.addMessage(msg);
			//TODO: add stat code 
		    //LOG.logSp("bye " + receivingConnection.getAddress());
			receivingConnection.close();
            //handlePingReply((PingReply)msg, receivingConnection);
		} else if (msg instanceof QueryReply) {
            // if someone sent a TCP QueryReply with the MCAST header,
            // that's bad, so ignore it.
            QueryReply qmsg = (QueryReply)msg;
            handleQueryReply(qmsg, receivingConnection);            
		} else if (msg instanceof PushRequest) {
            handlePushRequest((PushRequest)msg, receivingConnection);
        }
        else if (msg instanceof TCPConnectBackVendorMessage) {
            handleTCPConnectBackRequest((TCPConnectBackVendorMessage) msg,
                                        receivingConnection);
        }
        else if (msg instanceof UDPConnectBackVendorMessage) {
            handleUDPConnectBackRequest((UDPConnectBackVendorMessage) msg,
                                        receivingConnection);
        }
        else if (msg instanceof TCPConnectBackRedirect) {
            handleTCPConnectBackRedirect((TCPConnectBackRedirect) msg,
                                         receivingConnection);
        }
        else if (msg instanceof UDPConnectBackRedirect) {
            handleUDPConnectBackRedirect((UDPConnectBackRedirect) msg,
                                         receivingConnection);
        }
         else if (msg instanceof HeadPing) {
        	//TODO: add the statistics recording code
        	handleHeadPing((HeadPing)msg, receivingConnection);
        }
        else if (msg instanceof HeadPong) {  
            handleHeadPong((HeadPong)msg, receivingConnection); 
        } 
        else if (msg instanceof VendorMessage) {
            receivingConnection.handleVendorMessage((VendorMessage)msg);
        }
        
        //This may trigger propogation of query route tables.  We do this AFTER
        //any handshake pings.  Otherwise we'll think all clients are old
        //clients.
		//forwardQueryRouteTables();
        notifyMessageListener(msg, receivingConnection);
    }

    /**
     * Notifies any message listeners of this message's guid about the message.
     * This holds no locks.
     */
    private final void notifyMessageListener(Message msg, ReplyHandler handler) {
        List all = (List)_messageListeners.get(msg.getGUID());
        if(all != null) {
            for(Iterator i = all.iterator(); i.hasNext(); ) {
                MessageListener next = (MessageListener)i.next();
                next.processMessage(msg, handler);
            }
        }
    }

	/**
     * The handler for all message types.  Processes a message based on the 
     * message type.
	 *
	 * @param msg the <tt>Message</tt> received
	 * @param addr the <tt>InetSocketAddress</tt> containing the IP and 
	 *  port of the client node
     */	
	public void handleUDPMessage(Message msg, InetSocketAddress addr) {
        // Increment hops and decrement TTL.
        msg.hop();

		InetAddress address = addr.getAddress();
		int port = addr.getPort();
		// Verify that the address and port are valid.
		// If they are not, we cannot send any replies to them.
		if(!RouterService.isIpPortValid()) return;

		// Send UDPConnection messages on to the connection multiplexor
		// for routing to the appropriate connection processor
		if ( msg instanceof UDPConnectionMessage ) {
		    _udpConnectionMultiplexor.routeMessage(
			  (UDPConnectionMessage)msg, address, port);
			return;
		}

		ReplyHandler handler = new UDPReplyHandler(address, port);
		
   if (msg instanceof QueryReply) {
            QueryReply qr = (QueryReply) msg;			
            handleQueryReply(qr, handler);
            
		} else if(msg instanceof PingRequest) {
			handleUDPPingRequest((PingRequest)msg, 
												  handler, addr);
		} else if(msg instanceof PingReply) {
            handleUDPPingReply((PingReply)msg, handler, address, port);
		} else if(msg instanceof PushRequest) {
			handlePushRequest((PushRequest)msg, handler);
        }
        else if(msg instanceof ReplyNumberVendorMessage) {
            handleReplyNumberMessage((ReplyNumberVendorMessage) msg, addr);
        }
        else if (msg instanceof HeadPing) {
        	//TODO: add the statistics recording code
        	handleHeadPing((HeadPing)msg, handler);
        }
        notifyMessageListener(msg, handler);
    }

	/**
	 * Sends an ack back to the GUESS client node.  
	 */
	protected void sendAcknowledgement(InetSocketAddress addr, byte[] guid) {
		ConnectionManager manager = RouterService.getConnectionManager();
		Endpoint host = manager.getConnectedGUESSUltrapeer();
		PingReply reply;
		if(host != null) {
			try {
                
                reply = PingReply.createGUESSReply(guid, (byte)1, host);
            } catch(UnknownHostException e) {
				reply = createPingReply(guid);
            }
		} else {
			reply = createPingReply(guid);
		}
		
		// No GUESS endpoints existed and our IP/port was invalid.
		if( reply == null )
		    return;

        UDPService.instance().send(reply, addr.getAddress(), addr.getPort());
	}

	/**
	 * Creates a new <tt>PingReply</tt> from the set of cached
	 * GUESS endpoints, or a <tt>PingReply</tt> for localhost
	 * if no GUESS endpoints are available.
	 */
	private PingReply createPingReply(byte[] guid) {
		    if(RouterService.isIpPortValid())
                return PingReply.create(guid, (byte)1);
            else
                return null;
	}
	
    /**
     * Handles pings from the network.  With the addition of pong caching, this
     * method will either respond with cached pongs, or it will ignore the ping
     * entirely if another ping has been received from this connection very
     * recently.  If the ping is TTL=1, we will always process it, as it may
     * be a hearbeat ping to make sure the connection is alive and well.
     *
     * @param ping the ping to handle
     * @param handler the <tt>ReplyHandler</tt> instance that sent the ping
     */
    final private void handlePingRequest(PingRequest ping,
                                         ReplyHandler handler) {
        // Send it along if it's a heartbeat ping or if we should allow new 
        // pings on this connection.
        if(ping.isHeartbeat() || handler.allowNewPings()) {
            respondToPingRequest(ping, handler);
        } 
    }


    /**
     * The default handler for PingRequests received in
     * ManagedConnection.loopForMessages().  This implementation updates stats,
     * does the broadcast, and generates a response.
     *
     * You can customize behavior in three ways:
     *   1. Override. You can assume that duplicate messages
     *      (messages with the same GUID that arrived via different paths) have
     *      already been filtered.  If you want stats updated, you'll
     *      have to call super.handlePingRequest.
     *   2. Override broadcastPingRequest.  This allows you to use the default
     *      handling framework and just customize request routing.
     *   3. Implement respondToPingRequest.  This allows you to use the default
     *      handling framework and just customize responses.
     */
    protected void handleUDPPingRequest(PingRequest pingRequest,
										ReplyHandler handler, 
										InetSocketAddress addr) {
        if (!pingRequest.isQueryKeyRequest())
            respondToUDPPingRequest(pingRequest, addr, handler);
    }
    



    protected void handleUDPPingReply(PingReply reply, ReplyHandler handler,
                                      InetAddress address, int port) {        
        // normal pong processing...
        handlePingReply(reply, handler);
    }


    /** This is called when a client on the network has results for us that we
     *  may want.  We may contact them back directly or just cache them for
     *  use.
     */
    protected void handleReplyNumberMessage(ReplyNumberVendorMessage reply,
                                            InetSocketAddress addr) {
        GUID qGUID = new GUID(reply.getGUID());
        int numResults = 
        RouterService.getSearchResultHandler().getNumResultsForQuery(qGUID);
        // see if we need more results for this query....
        // if not, remember this location for a future, 'find more sources'
        // targeted GUESS query, as long as the other end said they can receive
        // unsolicited.
        if ((numResults<0) || (numResults>Const.ULTRAPEER_RESULTS)) {
             return;
        }
        
        LimeACKVendorMessage ack = 
            new LimeACKVendorMessage(qGUID, reply.getNumResults());
        UDPService.instance().send(ack, addr.getAddress(), addr.getPort());
    }


    /** Stores (for a limited time) the resps for later out-of-band delivery -
     *  interacts with handleLimeACKMessage
     *  @return true if the operation failed, false if not (i.e. too busy)
     */
    protected boolean bufferResponsesForLaterDelivery(QueryRequest query,
                                                      Response[] resps) {
        // store responses by guid for later retrieval
        synchronized (_outOfBandReplies) {
            if (_outOfBandReplies.size() < MAX_BUFFERED_REPLIES) {
                GUID.TimedGUID tGUID = 
                    new GUID.TimedGUID(new GUID(query.getGUID()),
                                       TIMED_GUID_LIFETIME);
                _outOfBandReplies.put(tGUID, new QueryResponseBundle(query, 
                                                                     resps));
                return true;
            }
            return false;
        }
    }


    /**
     * Forwards the UDPConnectBack to neighboring peers
     * as a UDPConnectBackRedirect request.
     */
    protected void handleUDPConnectBackRequest(UDPConnectBackVendorMessage udp,
                                               Connection source) {

        GUID guidToUse = udp.getConnectBackGUID();
        int portToContact = udp.getConnectBackPort();
        InetAddress sourceAddr = source.getInetAddress();
        Message msg = new UDPConnectBackRedirect(guidToUse, sourceAddr, 
                                                 portToContact);

        int sentTo = 0;
        List peers = new ArrayList(_manager.getInitializedConnections());
        Collections.shuffle(peers);
        for(Iterator i = peers.iterator(); i.hasNext() && sentTo < MAX_UDP_CONNECTBACK_FORWARDS;) {
            ManagedConnection currMC = (ManagedConnection)i.next();
            if(currMC == source)
                continue;

            if (currMC.remoteHostSupportsUDPRedirect() >= 0) {
                currMC.send(msg);
                sentTo++;
            }
        }
    }


    /**
     * Sends a ping to the person requesting the connectback request.
     */
    protected void handleUDPConnectBackRedirect(UDPConnectBackRedirect udp,
                                               Connection source) {
        // only allow other UPs to send you this message....
        if (!source.isSupernodeSupernodeConnection())
            return;

        GUID guidToUse = udp.getConnectBackGUID();
        int portToContact = udp.getConnectBackPort();
        InetAddress addrToContact = udp.getConnectBackAddress();

        // only connect back if you aren't connected to the host - that is the
        // whole point of redirect after all....
        Endpoint endPoint = new Endpoint(addrToContact.getAddress(),
                                         portToContact);
        if (_manager.isConnectedTo(endPoint.getAddress()))
            return;

        // keep track of who you tried connecting back too, don't do it too
        // much....
        String addrString = addrToContact.getHostAddress();
        Object placeHolder = _udpConnectBacks.get(addrString);
        if (placeHolder == null) {
            try {
                _udpConnectBacks.put(addrString, new Object());
            } catch (NoMoreStorageException nomo) {
                return;  // we've done too many connect backs, stop....
            }
        } else {
            return;  // we've connected back to this guy recently....
        }

        PingRequest pr = new PingRequest(guidToUse.bytes(), (byte) 1,
                                         (byte) 0);
        UDPService.instance().send(pr, addrToContact, portToContact);
    }



    /**
     * Forwards the request to neighboring Ultrapeers as a
     * TCPConnectBackRedirect message.
     */
    protected void handleTCPConnectBackRequest(TCPConnectBackVendorMessage tcp,
                                               Connection source) {
        final int portToContact = tcp.getConnectBackPort();
        InetAddress sourceAddr = source.getInetAddress();
        Message msg = new TCPConnectBackRedirect(sourceAddr, portToContact);

        int sentTo = 0;
        List peers = new ArrayList(_manager.getInitializedConnections());
        Collections.shuffle(peers);
        for(Iterator i = peers.iterator(); i.hasNext() && sentTo < MAX_TCP_CONNECTBACK_FORWARDS;) {
            ManagedConnection currMC = (ManagedConnection)i.next();
            if(currMC == source)
                continue;

            if (currMC.remoteHostSupportsTCPRedirect() >= 0) {
                currMC.send(msg);
                sentTo++;
            }
        }        
    }

    /**
     * Basically, just get the correct parameters, create a Socket, and
     * send a "/n/n".
     */
    protected void handleTCPConnectBackRedirect(TCPConnectBackRedirect tcp,
                                                Connection source) {
        // only allow other UPs to send you this message....
        if (!source.isSupernodeSupernodeConnection())
            return;

        final int portToContact = tcp.getConnectBackPort();
        final String addrToContact =tcp.getConnectBackAddress().getHostAddress();

        // only connect back if you aren't connected to the host - that is the
        // whole point of redirect after all....
        Endpoint endPoint = new Endpoint(addrToContact, portToContact);
        if (_manager.isConnectedTo(endPoint.getAddress()))
            return;

        // keep track of who you tried connecting back too, don't do it too
        // much....
        Object placeHolder = _tcpConnectBacks.get(addrToContact);
        if (placeHolder == null) {
            try {
                _tcpConnectBacks.put(addrToContact, new Object());
            } catch (NoMoreStorageException nomo) {
                return;  // we've done too many connect backs, stop....
            }
        } else {
            return;  // we've connected back to this guy recently....
        }

        TCP_CONNECT_BACKER.add(new Runnable() {
            public void run() {
                Socket sock = null;
                try {
                    sock = Sockets.connect(addrToContact, portToContact, 12000);
                    OutputStream os = sock.getOutputStream();
                    os.write("CONNECT BACK\r\n\r\n".getBytes());
                    os.flush();
                    if(LOG.isTraceEnabled())
                        LOG.trace("Succesful connectback to: " + addrToContact);
                    try {
                        Thread.sleep(500); // let the other side get it.
                    } catch(InterruptedException ignored) {
                        LOG.warn("Interrupted connectback", ignored);
                    }
                } catch (IOException ignored) {
                    LOG.warn("IOX during connectback", ignored);
                } catch (Throwable t) {
                    ErrorService.error(t);
                } finally {
                    IOUtils.close(sock);
                }
            }
        });
    }

    /**
     * Sends the ping request to the designated connection,
     * setting up the proper reply routing.
     */
    public void sendPingRequest(PingRequest request,
                                ManagedConnection connection) {
        if(request == null) {
            throw new NullPointerException("null ping");
        }
        if(connection == null) {
            throw new NullPointerException("null connection");
        }
        connection.send(request);
    }

    /**
     * Sends the query request to the designated connection,
     * setting up the proper reply routing.
     */
    public void sendQueryRequest(QueryRequest request,
                                 ManagedConnection connection) {        
        if(request == null) {
            throw new NullPointerException("null query");
        }
        if(connection == null) {
            throw new NullPointerException("null connection");
        }
        _queryRouteTable.routeReply(request.getGUID(), FOR_ME_REPLY_HANDLER);
        connection.send(request);
    }

    /**
     * Broadcasts the ping request to all initialized connections,
     * setting up the proper reply routing.
     */
    public void broadcastPingRequest(PingRequest ping) {
		if(ping == null) {
			throw new NullPointerException("null ping");
		}
        broadcastPingRequest(ping, FOR_ME_REPLY_HANDLER, _manager);
    }

	/**
	 * Generates a new dynamic query.  This method is used to send a new 
	 * dynamic query from this host (the user initiated this query directly,
	 * so it's replies are intended for this node.
	 *
	 * @param query the <tt>QueryRequest</tt> instance that generates
	 *  queries for this dynamic query
	 * @throws <tt>NullPointerException</tt> if the <tt>QueryHandler</tt> 
	 *  argument is <tt>null</tt>
	 */
	public void sendDynamicQuery(QueryRequest query) {
		if(query == null) {
			throw new NullPointerException("null QueryHandler");
		}
		_queryRouteTable.routeReply(query.getGUID(),
		     FOR_ME_REPLY_HANDLER);		// get the result counter so we can track the number of results
        originateLeafQuery(query);
		
		// always send the query to your multicast people
		multicastQueryRequest(QueryRequest.createMulticastQuery(query));		
	}

    /**
     * Broadcasts the ping request to all initialized connections that
     * are not the receivingConnection, setting up the routing
     * to the designated PingReplyHandler.  This is called from the default
     * handlePingRequest and the default broadcastPingRequest(PingRequest)
     *
     * If different (smarter) broadcasting functionality is desired, override
     * as desired.  If you do, note that receivingConnection may be null (for
     * requests originating here).
     */
    private void broadcastPingRequest(PingRequest request,
                                      ReplyHandler receivingConnection,
                                      ConnectionManager manager) {
        // Note the use of initializedConnections only.
        // Note that we have zero allocations here.

        //Broadcast the ping to other connected nodes (supernodes or older
        //nodes), but DON'T forward any ping not originating from me 
        //along leaf to ultrapeer connections.
        List list = manager.getInitializedConnections();
        int size = list.size();

        boolean randomlyForward = false;
        if(size > 3) randomlyForward = true;
        double percentToIgnore;
        for(int i=0; i<size; i++) {
            ManagedConnection mc = (ManagedConnection)list.get(i);
            if(!mc.isStable()) continue;
            if (receivingConnection == FOR_ME_REPLY_HANDLER || 
                (mc != receivingConnection && 
                 !mc.isClientSupernodeConnection())) {

                if(mc.supportsPongCaching()) {
                    percentToIgnore = 0.70;
                } else {
                    percentToIgnore = 0.90;
                }
                if(randomlyForward && 
                   (Math.random() < percentToIgnore)) {
                    continue;
                } else {
                    mc.send(request);
                }
            }
        }
    }

    /**
     * Send the query to the multicast group.
     */
    protected void multicastQueryRequest(QueryRequest query) {
        
		// set the TTL on outgoing udp queries to 1
		query.setTTL((byte)1);
		// record the stat
				
		MulticastService.send(query);
	}	


    /**
     * Originate a new query from this leaf node.
     *
     * @param qr the <tt>QueryRequest</tt> to send
     */
    private void originateLeafQuery(QueryRequest qr) {
		   List list = _manager.getInitializedConnections();

        // only send to at most 4 Ultrapeers, as we could have more
        // as a result of race conditions - also, don't send what is new
        // requests down too many connections
        final int max = 3;
	      int start = 0;
        int limit = Math.min(max, list.size());
        final boolean wantsOOB = qr.desiresOutOfBandReplies();
        for(int i=start; i<start+limit; i++) {
			ManagedConnection mc = (ManagedConnection)list.get(i);
            QueryRequest qrToSend = qr;
            if (wantsOOB && (mc.remoteHostSupportsLeafGuidance() < 0))
                qrToSend = QueryRequest.unmarkOOBQuery(qr);
            sendQueryRequest(qrToSend, mc, FOR_ME_REPLY_HANDLER);
        }
    }
    
    public void resendQuery(ManagedConnection mc, QueryRequest qr) {
        if (qr.desiresOutOfBandReplies() && (mc.remoteHostSupportsLeafGuidance() < 0))
            qr = QueryRequest.unmarkOOBQuery(qr);
        sendQueryRequest(qr, mc, FOR_ME_REPLY_HANDLER);
    }
    
    /**
     * Sends the passed query request, received on handler, 
     * to the passed sendConnection, only if the handler and
     * the sendConnection are authenticated to a common domain
     *
     * To only send it the route table has a hit, use
     * sendRoutedQueryToHost.
     *
     * @param queryRequest Query Request to send
     * @param sendConnection The connection on which to send out the query
     * @param handler The connection on which we originally
     * received the query
     */
    public void sendQueryRequest(QueryRequest request, 
								 ManagedConnection sendConnection, 
								 ReplyHandler handler) {
		if(request == null) {
			throw new NullPointerException("null query");
		}
		if(sendConnection == null) {
			throw new NullPointerException("null send connection");
		}
		if(handler == null) {
			throw new NullPointerException("null reply handler");
		}

        //send the query over this connection only if any of the following
        //is true:
        //1. The query originated from our node 
        //2. The connection under  consideration is an unauthenticated 
        //connection (normal gnutella connection)
        //3. It is an authenticated connection, and the connection on 
        //which we received query and this connection, are both 
        //authenticated to a common domain
        if((handler == FOR_ME_REPLY_HANDLER ||
            containsDefaultUnauthenticatedDomainOnly(sendConnection.getDomains())
            || Utilities.hasIntersection(handler.getDomains(), 
										 sendConnection.getDomains()))) {
            sendConnection.send(request);
		}		
    }
    
    /**
     * Originates a new query request to the ManagedConnection.
     *
     * @param request The query to send.
     * @param mc The ManagedConnection to send the query along
     * @return false if the query was not sent, true if so
     */
    public boolean originateQuery(QueryRequest query, ManagedConnection mc) {
        if( query == null )
            throw new NullPointerException("null query");
        if( mc == null )
            throw new NullPointerException("null connection");
            
        mc.originateQuery(query);
        return true;
    }
    

    /**
     * Checks if the passed set of domains contains only
     * default unauthenticated domain 
     * @param domains Set (of String) of domains to be tested
     * @return true if the passed set of domains contains only
     * default unauthenticated domain, false otherwise
     */
    private static boolean containsDefaultUnauthenticatedDomainOnly(Set domains) {
        //check if the set contains only one entry, and that entry is the
        //default unauthenticated domain 
        if((domains.size() == 1) && domains.contains(
            User.DEFAULT_UNAUTHENTICATED_DOMAIN))
            return true;
        else
            return false;
    }
    
    /**
     * Respond to the ping request.  Implementations typically will either
     * do nothing (if they don't think a response is appropriate) or call
     * sendPingReply(PingReply).
     * This method is called from the default handlePingRequest.
     */
    protected abstract void respondToPingRequest(PingRequest request,
                                                 ReplyHandler handler);

	/**
	 * Responds to a ping received over UDP -- implementations
	 * handle this differently from pings received over TCP, as it is 
	 * assumed that the requester only wants pongs from other nodes
	 * that also support UDP messaging.
	 *
	 * @param request the <tt>PingRequest</tt> to service
     * @param addr the <tt>InetSocketAddress</tt> containing the ping
     * @param handler the <tt>ReplyHandler</tt> instance from which the
     *  ping was received and to which pongs should be sent
	 */
    protected abstract void respondToUDPPingRequest(PingRequest request, 
													InetSocketAddress addr,
                                                    ReplyHandler handler);

    /**
     * The default handler for PingRequests received in
     * ManagedConnection.loopForMessages().  This implementation
     * uses the ping route table to route a ping reply.  If an appropriate route
     * doesn't exist, records the error statistics.  On sucessful routing,
     * the PingReply count is incremented.<p>
     *
     * In all cases, the ping reply is recorded into the host catcher.<p>
     *
     * Override as desired, but you probably want to call super.handlePingReply
     * if you do.
     */
    protected void handlePingReply(PingReply reply,
                                   ReplyHandler handler) {
        //update hostcatcher (even if the reply isn't for me)
        RouterService.getHostCatcher().add(reply);
    }

    /**
     * The default handler for QueryReplies received in
     * ManagedConnection.loopForMessages().  This implementation
     * uses the query route table to route a query reply.  If an appropriate
     * route doesn't exist, records the error statistics.  On sucessful routing,
     * the QueryReply count is incremented.<p>
     *
     * Override as desired, but you probably want to call super.handleQueryReply
     * if you do.  This is public for testing purposes.
     */
    public void handleQueryReply(QueryReply queryReply,
                                 ReplyHandler handler) {
        if(queryReply == null) {
            throw new NullPointerException("null query reply");
        }
        if(handler == null) {
            throw new NullPointerException("null ReplyHandler");
        }
        //For flow control reasons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume have higher priorities (i.e., lower
        //numbers).
        RouteTable.ReplyRoutePair rrp =
            _queryRouteTable.getReplyHandler(queryReply.getGUID(),
                                             queryReply.getTotalLength(),
											 queryReply.getResultCount());

        if(rrp != null) {
            queryReply.setPriority(rrp.getBytesRouted());
            // Prepare a routing for a PushRequest, which works
            // here like a QueryReplyReply
            // Note the use of getClientGUID() here, not getGUID()
            _pushRouteTable.routeReply(queryReply.getClientGUID(),
                                       handler);
            //Simple flow control: don't route this message along other
            //connections if we've already routed too many replies for this
            //GUID.  Note that replies destined for me all always delivered to
            //the GUI.

            ReplyHandler rh = rrp.getReplyHandler();

            if(!shouldDropReply(rrp, rh, queryReply)) {                
                rh.handleQueryReply(queryReply, handler);
            } else {
                handler.countDroppedMessage();
            }
        }
        else {
            handler.countDroppedMessage();
        }
    }

    /**
     * Checks if the <tt>QueryReply</tt> should be dropped for various reasons.
     *
     * Reason 1) The reply has already routed enough traffic.  Based on per-TTL
     * hard limits for the number of bytes routed for the given reply guid.
     * This algorithm favors replies that don't have as far to go on the 
     * network -- i.e., low TTL hits have more liberal limits than high TTL
     * hits.  This ensures that hits that are closer to the query originator
     * -- hits for which we've already done most of the work, are not 
     * dropped unless we've routed a really large number of bytes for that
     * guid.  This method also checks that hard number of results that have
     * been sent for this GUID.  If this number is greater than a specified
     * limit, we simply drop the reply.
     *
     * Reason 2) The reply was meant for me -- DO NOT DROP.
     *
     * Reason 3) The TTL is 0, drop.
     *
     * @param rrp the <tt>ReplyRoutePair</tt> containing data about what's 
     *  been routed for this GUID
     * @param ttl the time to live of the query hit
     * @return <tt>true if the reply should be dropped, otherwise <tt>false</tt>
     */
    private boolean shouldDropReply(RouteTable.ReplyRoutePair rrp,
                                    ReplyHandler rh,
                                    QueryReply qr) {
        int ttl = qr.getTTL();
                                           
        // Reason 2 --  The reply is meant for me, do not drop it.
        if( rh == FOR_ME_REPLY_HANDLER ) return false;
        
        // Reason 3 -- drop if TTL is 0.
        if( ttl == 0 ) return true;                

        // Reason 1 ...
        
        int resultsRouted = rrp.getResultsRouted();

        // drop the reply if we've already sent more than the specified number
        // of results for this GUID
        if(resultsRouted > 100) return true;

        int bytesRouted = rrp.getBytesRouted();
        // send replies with ttl above 2 if we've routed under 50K 
        if(ttl > 2 && bytesRouted < 50    * 1024) return false;
        // send replies with ttl 1 if we've routed under 1000K 
        if(ttl == 1 && bytesRouted < 200 * 1024) return false;
        // send replies with ttl 2 if we've routed under 333K 
        if(ttl == 2 && bytesRouted < 100  * 1024) return false;

        // if none of the above conditions holds true, drop the reply
        return true;
    }

    


    /**
     * The default handler for PushRequests received in
     * ManagedConnection.loopForMessages().  This implementation
     * uses the push route table to route a push request.  If an appropriate
     * route doesn't exist, records the error statistics.  On sucessful routing,
     * the PushRequest count is incremented.
     *
     * Override as desired, but you probably want to call
     * super.handlePushRequest if you do.
     */
    protected void handlePushRequest(PushRequest request,
                                  ReplyHandler handler) {
        if(request == null) {
            throw new NullPointerException("null request");
        }
        if(handler == null) {
            throw new NullPointerException("null ReplyHandler");
        }
        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler = getPushHandler(request.getClientGUID());

        if(replyHandler != null)
            replyHandler.handlePushRequest(request, handler);
        else {
            handler.countDroppedMessage();
        }
    }
    
    /**
     * Returns the appropriate handler from the _pushRouteTable.
     * This enforces that requests for my clientGUID will return
     * FOR_ME_REPLY_HANDLER, even if it's not in the table.
     */
    protected ReplyHandler getPushHandler(byte[] guid) {
        ReplyHandler replyHandler = _pushRouteTable.getReplyHandler(guid);
        if(replyHandler != null)
            return replyHandler;
        else if(Arrays.equals(_clientGUID, guid))
            return FOR_ME_REPLY_HANDLER;
        else
            return null;
    }

    /**
     * Uses the ping route table to send a PingReply to the appropriate
     * connection.  Since this is used for PingReplies orginating here, no
     * stats are updated.
     */
    protected void sendPingReply(PingReply pong, ReplyHandler handler) {
        if(pong == null) {
            throw new NullPointerException("null pong");
        }

        if(handler == null) {
            throw new NullPointerException("null reply handler");
        }
 
        handler.handlePingReply(pong, null);
    }

    /**
     * Uses the query route table to send a QueryReply to the appropriate
     * connection.  Since this is used for QueryReplies orginating here, no
     * stats are updated.
     * @throws IOException if no appropriate route exists.
     */
    protected void sendQueryReply(QueryReply queryReply)
        throws IOException {
        
        if(queryReply == null) {
            throw new NullPointerException("null reply");
        }
        //For flow control reasons, we keep track of the bytes routed for this
        //GUID.  Replies with less volume have higher priorities (i.e., lower
        //numbers).
        RouteTable.ReplyRoutePair rrp =
            _queryRouteTable.getReplyHandler(queryReply.getGUID(),
                                             queryReply.getTotalLength(),
											 queryReply.getResultCount());

        if(rrp != null) {
            queryReply.setPriority(rrp.getBytesRouted());
            rrp.getReplyHandler().handleQueryReply(queryReply, null);
        }
        else
            throw new IOException("no route for reply");
    }

    /**
     * Uses the push route table to send a push request to the appropriate
     * connection.  Since this is used for PushRequests orginating here, no
     * stats are updated.
     * @throws IOException if no appropriate route exists.
     */
    public void sendPushRequest(PushRequest push)
        throws IOException {
        if(push == null) {
            throw new NullPointerException("null push");
        }
        

        // Note the use of getClientGUID() here, not getGUID()
        ReplyHandler replyHandler = getPushHandler(push.getClientGUID());

        if(replyHandler != null)
            replyHandler.handlePushRequest(push, FOR_ME_REPLY_HANDLER);
        else
            throw new IOException("no route for push");
    }
    
    /**
     * Sends a push request to the multicast network.  No lookups are
     * performed in the push route table, because the message will always
     * be broadcast to everyone.
     */
    protected void sendMulticastPushRequest(PushRequest push) {
        if(push == null) {
            throw new NullPointerException("null push");
        }
        
        // must have a TTL of 1
        Assert.that(push.getTTL() == 1, "multicast push ttl not 1");
        
        MulticastService.send(push);
    }


    /**
     * Abstract method for creating query hits.  Subclasses must specify
     * how this list is created.
     *
     * @return a <tt>List</tt> of <tt>QueryReply</tt> instances
     */
    protected abstract List createQueryReply(byte[] guid, byte ttl,
                                            long speed, 
                                             Response[] res, byte[] clientGUID, 
                                             boolean busy, 
                                             boolean uploaded, 
                                             boolean measuredSpeed, 
                                             boolean isFromMcast,
                                             boolean shouldMarkForFWTransfer);

    /**
     * Utility method for checking whether or not the given connection
     * is able to pass QRP messages.
     *
     * @param c the <tt>Connection</tt> to check
     * @return <tt>true</tt> if this is a QRP-enabled connection,
     *  otherwise <tt>false</tt>
     */
    private static boolean isQRPConnection(Connection c) {
        if(c.isSupernodeClientConnection()) return true;
        if(c.isUltrapeerQueryRoutingConnection()) return true;
        return false;
    }

    
    /**
     * Adds the specified MessageListener for messages with this GUID.
     * You must manually unregister the listener.
     *
     * This works by replacing the necessary maps & lists, so that 
     * notifying doesn't have to hold any locks.
     */
    public void registerMessageListener(byte[] guid, MessageListener ml) {
        ml.registered(guid);
        synchronized(MESSAGE_LISTENER_LOCK) {
            Map listeners = new TreeMap(GUID.GUID_BYTE_COMPARATOR);
            listeners.putAll(_messageListeners);
            List all = (List)listeners.get(guid);
            if(all == null) {
                all = new ArrayList(1);
                all.add(ml);
            } else {
                List temp = new ArrayList(all.size() + 1);
                temp.addAll(all);
                all = temp;
                all.add(ml);
            }
            listeners.put(guid, Collections.unmodifiableList(all));
            _messageListeners = Collections.unmodifiableMap(listeners);
        }
    }
    
    /**
     * Unregisters this MessageListener from listening to the GUID.
     *
     * This works by replacing the necessary maps & lists so that
     * notifying doesn't have to hold any locks.
     */
    public void unregisterMessageListener(byte[] guid, MessageListener ml) {
        boolean removed = false;
        synchronized(MESSAGE_LISTENER_LOCK) {
            List all = (List)_messageListeners.get(guid);
            if(all != null) {
                all = new ArrayList(all);
                if(all.remove(ml)) {
                    removed = true;
                    Map listeners = new TreeMap(GUID.GUID_BYTE_COMPARATOR);
                    listeners.putAll(_messageListeners);
                    if(all.isEmpty())
                        listeners.remove(guid);
                    else
                        listeners.put(guid, Collections.unmodifiableList(all));
                    _messageListeners = Collections.unmodifiableMap(listeners);
                }
            }
        }
        if(removed)
            ml.unregistered(guid);
    }

    
    /**
     * Replies to a head ping sent from the given ReplyHandler.
     */
    private void handleHeadPing(HeadPing ping, ReplyHandler handler) {
        GUID clientGUID = ping.getClientGuid();
        ReplyHandler pingee;
        
        if(clientGUID != null)
            pingee = getPushHandler(clientGUID.bytes());
        else
            pingee = FOR_ME_REPLY_HANDLER; // handle ourselves.
        
        //drop the ping if no entry for the given clientGUID
        if (pingee == null) 
           return; 
        
        //don't bother routing if this is intended for me. 
        // TODO:  Clean up ReplyHandler interface so we aren't
        //        afraid to use it like it's intended.
        //        That way, we can do pingee.handleHeadPing(ping)
        //        and not need this anti-OO instanceof check.
        if (pingee instanceof ForMeReplyHandler) {
        } else {
            // Otherwise, remember who sent it and forward it on.
                
            //remember where to send the pong to. 
            //the pong will have the same GUID as the ping. 
            // Note that this uses the messageGUID, not the clientGUID
            _headPongRouteTable.routeReply(ping.getGUID(), handler); 
            
            //and send off the routed ping 
            pingee.reply(ping); 
        }
   } 
    
    
    /** 
     * Handles a pong received from the given handler.
     */ 
    private void handleHeadPong(HeadPong pong, ReplyHandler handler) { 
        ReplyHandler forwardTo =  _headPongRouteTable.getReplyHandler(pong.getGUID()); 

        // TODO: Clean up ReplyHandler interface so we're not afraid
        //       to use it correctly.
        //       Ideally, we'd do forwardTo.handleHeadPong(pong)
        //       instead of this instanceof check
         
        // if this pong is for me, process it as usual (not implemented yet)
        if (forwardTo != null && !(forwardTo instanceof ForMeReplyHandler)) { 
            forwardTo.reply(pong); 
            _headPongRouteTable.removeReplyHandler(forwardTo); 
        } 
    } 
    
    
    private static class QueryResponseBundle {
        public final QueryRequest _query;
        public final Response[] _responses;
        
        public QueryResponseBundle(QueryRequest query, Response[] responses) {
            _query = query;
            _responses = responses;
        }
    }


    /** Can be run to invalidate out-of-band ACKs that we are waiting for....
     */
    private class Expirer implements Runnable {
        public void run() {
            try {
                Set toRemove = new HashSet();
                synchronized (_outOfBandReplies) {
                    Iterator keys = _outOfBandReplies.keySet().iterator();
                    while (keys.hasNext()) {
                        GUID.TimedGUID currQB = (GUID.TimedGUID) keys.next();
                        if ((currQB != null) && (currQB.shouldExpire()))
                            toRemove.add(currQB);
                    }
                    // done iterating through _outOfBandReplies, remove the 
                    // keys now...
                    keys = toRemove.iterator();
                    while (keys.hasNext())
                        _outOfBandReplies.remove(keys.next());
                }
            } 
            catch(Throwable t) {
                ErrorService.error(t);
            }
        }
    }


    /** This is run to clear out the registry of connect back attempts...
     *  Made package access for easy test access.
     */
    static class ConnectBackExpirer implements Runnable {
        public void run() {
            try {
                _tcpConnectBacks.clear();
                _udpConnectBacks.clear();
            } 
            catch(Throwable t) {
                ErrorService.error(t);
            }
        }
    }

    static class HopsFlowManager implements Runnable {
        /* in case we don't want any queries any more */
        private static final byte BUSY_HOPS_FLOW = 0;

    	/* in case we want to reenable queries */
    	private static final byte FREE_HOPS_FLOW = 5;

        /* small optimization:
           send only HopsFlowVendorMessages if the busy state changed */
        private static boolean _oldBusyState = false;
           
        public void run() {
            // busy hosts don't want to receive any queries, if this node is not
            // busy, we need to reset the HopsFlow value
            boolean isBusy = true;
            
            // state changed? don't bother the ultrapeer with information
            // that it already knows. we need to inform new ultrapeers, though.
            final List connections = _manager.getInitializedConnections();
            final HopsFlowVendorMessage hops = 
                new HopsFlowVendorMessage(isBusy ? BUSY_HOPS_FLOW :
                                          FREE_HOPS_FLOW);
            if (isBusy == _oldBusyState) {
                for (int i = 0; i < connections.size(); i++) {
                    ManagedConnection c =
                        (ManagedConnection)connections.get(i);
                    // Yes, we may tell a new ultrapeer twice, but
                    // without a buffer of some kind, we might forget
                    // some ultrapeers. The clean solution would be
                    // to remember the hops-flow value in the connection.
                    if (c != null 
                        && c.getConnectionTime() + 1.25 * HOPS_FLOW_INTERVAL 
                            > System.currentTimeMillis()
                        && c.isClientSupernodeConnection() )
                        c.send(hops);
                }
            } else { 
                _oldBusyState = isBusy;
                for (int i = 0; i < connections.size(); i++) {
                    ManagedConnection c = (ManagedConnection)connections.get(i);
                    if (c != null && c.isClientSupernodeConnection())
                        c.send(hops);
                }
            }
        }
    }
    
}
