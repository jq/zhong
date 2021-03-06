package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;

import com.util.LOG;
import com.util.Utils;
import com.limegroup.gnutella.chat.ChatManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.http.HTTPRequestMethod;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;


/**
 * Listens on ports, accepts incoming connections, and dispatches threads to
 * handle those connections.  Currently supports Gnutella messaging, HTTP, and
 * chat connections over TCP; more may be supported in the future.<p> 
 * This class has a special relationship with UDPService and should really be
 * the only class that intializes it.  See setListeningPort() for more
 * info.
 */
public class Acceptor implements Runnable {
    // various time delays for checking of firewalled status.
    static long INCOMING_EXPIRE_TIME = 30 * 60 * 1000;   // 30 minutes
    static long WAIT_TIME_AFTER_REQUESTS = 30 * 1000;    // 30 seconds
    static long TIME_BETWEEN_VALIDATES = 10 * 60 * 1000; // 10 minutes
   
    /**
     * The socket that listens for incoming connections. Can be changed to
     * listen to new ports.
     *
     * LOCKING: obtain _socketLock before modifying either.  Notify _socketLock
     * when done.
     */
    private volatile ServerSocket _socket=null;

    /**
     * The port of the server socket.
     */
    private volatile int _port = 6346;

    /**
     * The object to lock on while setting the listening socket
     */
    private final Object SOCKET_LOCK = new Object();

    /**
     * The real address of this host--assuming there's only one--used for pongs
     * and query replies.  This value is ignored if FORCE_IP_ADDRESS is
     * true. This is initialized in three stages:
     *   1. Statically initialized to all zeroes.
     *   2. Initialized in the Acceptor thread to getLocalHost().
     *   3. Initialized each time a connection is initialized to the local
     *      address of that connection's socket. 
     *
     * Why are all three needed?  Step (3) is needed because (2) can often fail
     * due to a JDK bug #4073539, or if your address changes via DHCP.  Step (2)
     * is needed because (3) ignores local addresses of 127.x.x.x.  Step (1) is
     * needed because (2) can't occur in the main thread, as it may block
     * because the update checker is trying to resolve addresses.  (See JDK bug
     * #4147517.)  Note this may delay the time to create a listening socket by
     * a few seconds; big deal!
     *
     * LOCKING: obtain Acceptor.class' lock 
     */
    private static byte[] _address = new byte[4];
    
    /**
     * The external address.  This is the address as visible from other peers.
     *
     * LOCKING: obtain Acceptor.class' lock
     */
    private static byte[] _externalAddress = new byte[4];
    
	/**
	 * Variable for whether or not we have accepted an incoming connection --
	 * used to determine firewall status.
	 */
	private volatile boolean _acceptedIncoming = false;
	
    /**
     * Keep track of the last time _acceptedIncoming was set - we want to
     * revalidate it every so often.
     */
    private volatile long _lastIncomingTime = 0;

    /**
     * The last time you did a connect back check.  It is set to the time
     * we start up since we try once when we start up.
     */
    private volatile long _lastConnectBackTime = System.currentTimeMillis();

	/**
     * @modifes this
     * @effects sets the IP address to use in pongs and query replies.
     *  If addr is invalid or a local address, this is not modified.
     *  This method must be to get around JDK bug #4073539, as well
     *  as to try to handle the case of a computer whose IP address
     *  keeps changing.
	 */
	public void setAddress(InetAddress address) {
		byte[] byteAddr = address.getAddress();
		if( !NetworkUtils.isValidAddress(byteAddr) )
		    return;
		    
		if( byteAddr[0] == 127) {
            return;
        }

        boolean addrChanged = false;
		synchronized(Acceptor.class) {
		    if( !Arrays.equals(_address, byteAddr) ) {
			    _address = byteAddr;
			    addrChanged = true;
			}
		}
		
		if( addrChanged )
		    RouterService.addressChanged();
	}
	
	/**
	 * Sets the external address.
	 */
	public void setExternalAddress(InetAddress address) {
	    byte[] byteAddr = address.getAddress();

		if( byteAddr[0] == 127) {
            return;
        }

		synchronized(Acceptor.class) {
		    _externalAddress = byteAddr;
		}
    }

	/**
	 * tries to bind the serversocket and create UPnPMappings.
	 * call before running.
	 */
	public void init() {
	    int tempPort = ConnectionSettings.PORT;

        //0. Get local address.  This must be done here because it can
        //   block under certain conditions.
        //   See the notes for _address.
        try {
            setAddress(Utils.getLocalIpAddress());
        } catch (UnknownHostException e) {
            LOG.error("UnknownHostException " + e.getMessage());
        } catch (SecurityException e) {
            LOG.error("SecurityException " + e.getMessage());
        }

        // Create the server socket, bind it to a port, and listen for
        // incoming connections.  If there are problems, we can continue
        // onward.
        //1. Try suggested port.
		int oldPort = tempPort;
        try {
			setListeningPort(tempPort);
			_port = tempPort;
        } catch (IOException e) {
            // 2. Try 20 different ports. The first 10 tries increment
            // sequentially from 6346. The next 10 tries are random ports between
            // 2000 and 52000
            int numToTry = 20;
            Random gen = null;
            for (int i=0; i<numToTry; i++) {
                if(i < 10)
                    tempPort = i+6346;
                else {
                    if(gen==null)
                        gen = new Random();
                    tempPort = gen.nextInt(50000);
                    tempPort += 2000;//avoid the first 2000 ports
                }
				// do not try to bind to the multicast port.
				if (tempPort == MulticastService._port) {
				    numToTry++;
				    continue;
				}
                try {
                    setListeningPort(tempPort);
					_port = tempPort;
                    break;
                } catch (IOException e2) { 
                }
            }

            // If we still don't have a socket, there's an error
            if(_socket == null) {
                MessageService.showError("ERROR_NO_PORTS_AVAILABLE");
            }
        }
        
        if (_port != oldPort) {
            ConnectionSettings.PORT = _port;
            RouterService.addressChanged();
        }


	}
	
    /**
     * Launches the port monitoring thread, MulticastService, and UDPService.
     */
	public void start() {	    
		Thread at = new ManagedThread(this, "Acceptor");
		at.setDaemon(true);
		at.start();
        RouterService.schedule(new IncomingValidator(), TIME_BETWEEN_VALIDATES,
                               TIME_BETWEEN_VALIDATES);
	}
	
	/**
	 * Returns whether or not our advertised IP address
	 * is the same as what remote peers believe it is.
	 */
	public boolean isAddressExternal() {
	    synchronized(Acceptor.class) {
	        return Arrays.equals(getAddress(), _externalAddress);
	    }
	}
	
	/**
	 * Returns this' external address.
	 */
	public byte[] getExternalAddress() {
	    synchronized(Acceptor.class) {
	        return _externalAddress;
        }
	}

    /**
     * Returns this' address to use for ping replies, query replies,
     * and pushes.
     * 
     * @param checkForce whether or not to check if the IP address is forced.
     *   If false, the forced IP address will never be used.
     *   If true, the forced IP address will only be used if one is set.
     */
    public byte[] getAddress() {        
        synchronized (Acceptor.class) {
            return _address;
        }
    }

    /**
     * Returns the port at which the Connection Manager listens for incoming
     * connections
     *
     * @param checkForce whether or not to check if the port is forced.     
     * @return the listening port
     */
    public int getPort() {
        return _port;
    }

    /**
     * @requires only one thread is calling this method at a time
     * @modifies this
     * @effects sets the port on which the ConnectionManager AND the UDPService
     *  is listening.  If either service CANNOT bind TCP/UDP to the port,
     *  <i>neither<i> service is modified and a IOException is throw.
     *  If port==0, tells this to stop listening for incoming GNUTELLA TCP AND
     *  UDP connections/messages.  This is properly synchronized and can be 
     *  called even while run() is being called.  
     */
    public void setListeningPort(int port) throws IOException {
        LOG.trace("Acceptor.setListeningPort(): entered.");
        //1. Special case: if unchanged, do nothing.
        if (_socket!=null && _port==port)
            return;
        //2. Special case if port==0.  This ALWAYS works.
        //Note that we must close the socket BEFORE grabbing
        //the lock.  Otherwise deadlock will occur since
        //the acceptor thread is listening to the socket
        //while holding the lock.  Also note that port
        //will not have changed before we grab the lock.
        else if (port==0) {
            LOG.trace("Acceptor.setListeningPort(): shutting off service.");
            //Close old socket (if non-null)
            if (_socket!=null) {
                try {
                    _socket.close();
                } catch (IOException e) { }
            }
            synchronized (SOCKET_LOCK) {
                _socket=null;
                _port=0;
                SOCKET_LOCK.notify();
            }

            //Shut off UDPService also!
            UDPService.instance().setListeningSocket(null);

            LOG.trace("Acceptor.setListeningPort(): service OFF.");
            return;
        }
        //3. Normal case.  See note about locking above.
        /* Since we want the UDPService to bind to the same port as the 
         * Acceptor, we need to be careful about this case.  Essentially, we 
         * need to confirm that the port can be bound by BOTH UDP and TCP 
         * before actually acceping the port as valid.  To effect this change,
         * we first attempt to bind the port for UDP traffic.  If that fails, a
         * IOException will be thrown.  If we successfully UDP bind the port 
         * we keep that bound DatagramSocket around and try to bind the port to 
         * TCP.  If that fails, a IOException is thrown and the valid 
         * DatagramSocket is closed.  If that succeeds, we then 'commit' the 
         * operation, setting our new TCP socket and UDP sockets.
         */
        else {
            
            if(LOG.isDebugEnabled())
                LOG.debug("Acceptor.setListeningPort(): changing port to " +
                          port);

            DatagramSocket udpServiceSocket = 
                UDPService.instance().newListeningSocket(port);

            LOG.trace("Acceptor.setListeningPort(): UDP Service is ready.");
        
            //a) Try new port.
            ServerSocket newSocket=null;
            try {
                newSocket=new com.limegroup.gnutella.io.NIOServerSocket(port);
            } catch (IOException e) {
                udpServiceSocket.close();
                throw e;
            } catch (IllegalArgumentException e) {
                udpServiceSocket.close();
                throw new IOException("could not create a listening socket");
            }
            //b) Close old socket (if non-null)
            if (_socket!=null) {
                try {
                    _socket.close();
                } catch (IOException e) { }
            }
            //c) Replace with new sock.  Notify the accept thread.
            synchronized (SOCKET_LOCK) {
                _socket=newSocket;
                _port=port;
                SOCKET_LOCK.notify();
            }

            LOG.trace("Acceptor.setListeningPort(): I am ready.");

            // Commit UDPService's new socket
            UDPService.instance().setListeningSocket(udpServiceSocket);
 
            if(LOG.isDebugEnabled())
                LOG.debug("Acceptor.setListeningPort(): listening UDP/TCP on " + 
                          _port);
        }
    }


	/**
	 * Determines whether or not LimeWire has detected it is firewalled or not.
	 */
	public boolean acceptedIncoming() {
        return _acceptedIncoming;
	}
	
	/**
	 * Sets the new incoming status.
	 * Returns whether or not the status changed.
	 */
	private boolean setIncoming(boolean status) {
	    boolean old = _acceptedIncoming;
	    _acceptedIncoming = status;
	    return old != status;
	}
	
	/**
	 * Updates the firewalled status with info from this socket.
	 */
	private void checkFirewall(Socket socket) {
		// we have accepted an incoming socket -- only record
        // that we've accepted incoming if it's definitely
        // not from our local subnet and we aren't connected to
        // the host already.
        boolean changed = false;
        if(isOutsideConnection(socket.getInetAddress())) {
            synchronized (Acceptor.class) {
                changed = setIncoming(true);
                ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(Boolean.TRUE);
                _lastIncomingTime = System.currentTimeMillis();
            }
        }
        if(changed)
            RouterService.incomingStatusChanged();
    }


    /** @modifies this, network, SettingsManager
     *  @effects accepts new incoming connections on a designated port
     *   and services incoming requests.  If the port was changed
     *   in order to accept incoming connections, SettingsManager is
     *   changed accordingly.
     */
    public void run() {
		
        
        while (true) {
            try {
                //Accept an incoming connection, make it into a
                //Connection object, handshake, and give it a thread
                //to service it.  If not bound to a port, wait until
                //we are.  If the port is changed while we are
                //waiting, IOException will be thrown, forcing us to
                //release the lock.
                Socket client=null;
                synchronized (SOCKET_LOCK) {
                    if (_socket!=null) {
                        try {
                            client=_socket.accept();
                        } catch (IOException e) {
                            LOG.warn("IOX while accepting", e);
                            continue;
                        }
                    } else {
                        // When the socket lock is notified, the socket will
                        // be available.  So, just wait for that to happen and
                        // go around the loop again.
                        try {
                            SOCKET_LOCK.wait();
                        } catch (InterruptedException e) {
                        }
                        continue;
                    }
                }

                // If the client was closed before we were able to get the address,
                // then getInetAddress will return null.
				InetAddress address = client.getInetAddress();
				if(address == null) {
				    try {
				        client.close();
				    } catch(IOException ignored) {}
				    continue;
				}
				    
                //Check if IP address of the incoming socket is in _badHosts
                if (isBannedIP(address.getAddress())) {
                    if(LOG.isWarnEnabled())
                        LOG.warn("Ignoring banned host: " + address);
                    try {
                        client.close();
                    } catch(IOException ignored) {}
                    continue;
                }
                
                // if we want to unset firewalled from any connection, 
                // do it here.
                if(!ConnectionSettings.UNSET_FIREWALLED_FROM_CONNECTBACK)
                    checkFirewall(client);
				
                // Set our IP address of the local address of this socket.
                // Check to see if this is an attempt to connect to ourselves
                InetAddress localAddress = null;
                try {
                  localAddress = client.getLocalAddress();
                  setAddress( localAddress );                
                } catch (Exception e) {
                    LOG.error("getLocalAddress in Acceptor " + e.getMessage());
                }

                //Dispatch asynchronously.
                ConnectionDispatchRunner dispatcher =
					new ConnectionDispatchRunner(client);
				Thread dispatchThread = 
                    new ManagedThread(dispatcher, "ConnectionDispatchRunner");
				dispatchThread.setDaemon(true);
				dispatchThread.start();

            } catch (Throwable e) {
                ErrorService.error(e);
            }
        }
    }
    
    /**
     * Determines whether or not this INetAddress is found an outside
     * source, so as to correctly set "acceptedIncoming" to true.
     *
     * This ignores connections from private or local addresses,
     * ignores those who may be on the same subnet, and ignores those
     * who we are already connected to.
     */
    private boolean isOutsideConnection(InetAddress addr) {    
        byte[] bytes = addr.getAddress();
        return !RouterService.isConnectedTo(addr) &&
               !NetworkUtils.isCloseIP(bytes, getAddress()) &&
               !NetworkUtils.isLocalAddress(addr);
	}

    /**
     * Specialized class for dispatching incoming TCP connections to their
     * appropriate handlers.  Gnutella connections are handled via 
     * <tt>ConnectionManager</tt>, and HTTP connections are handled
     * via <tt>UploadManager</tt> and <tt>DownloadManager</tt>.
     */
    private static class ConnectionDispatchRunner implements Runnable {

        /**
         * The <tt>Socket</tt> instance for the connection.
         */
        private final Socket _socket;

        /**
         * @modifies socket, this' managers
         * @effects starts a new thread to handle the given socket and
         *  registers it with the appropriate protocol-specific manager.
         *  Returns once the thread has been started.  If socket does
         *  not speak a known protocol, closes the socket immediately and
         *  returns.
         */
        public ConnectionDispatchRunner(Socket socket) {
            _socket = socket;
        }

        /**
         * Dispatches the new connection based on connection type, such
         * as Gnutella, HTTP, or MAGNET.
         */
        public void run() {
			ConnectionManager cm = RouterService.getConnectionManager();
			DownloadManager dm   = RouterService.getDownloadManager();
			Acceptor ac = RouterService.getAcceptor();
            try {
                //The try-catch below is a work-around for JDK bug 4091706.
                InputStream in=null;
                try {
                    in=_socket.getInputStream(); 
                } catch (IOException e) {
                    throw e;
                } catch(NullPointerException e) {
                    // This should only happen extremely rarely.
                    // JDK bug 4091706
                    throw new IOException(e.getMessage());
                }
                _socket.setSoTimeout(Constants.TIMEOUT);
                //dont read a word of size more than 8 
                //("GNUTELLA" is the longest word we know at this time)
                String word = IOUtils.readLargestWord(in,8);
                _socket.setSoTimeout(0);
                
                
                boolean localHost = NetworkUtils.isLocalHost(_socket);
				// Only selectively allow localhost connections
				if ( !word.equals("MAGNET") ) {
					if (localHost) {
					    LOG.trace("Killing localhost connection with non-magnet.");
						_socket.close();
						return;
					}
				} else if(!localHost) { // && word.equals(MAGNET)
				    LOG.trace("Killing non-local ExternalControl request.");
				    _socket.close();
				    return;
				}

                //1. Gnutella connection.  If the user hasn't changed the
                //   handshake string, we accept the default ("GNUTELLA 
                //   CONNECT/0.4") or the proprietary limewire string
                //   ("LIMEWIRE CONNECT/0.4").  Otherwise we just accept
                //   the user's value.
                boolean useDefaultConnect= true;


                if (word.equals(ConnectionSettings.CONNECT_STRING_FIRST_WORD)) {
                    cm.acceptConnection(_socket);
                }
                else if (useDefaultConnect && word.equals("LIMEWIRE")) {
                    cm.acceptConnection(_socket);
                }
                
				                else if (word.equals("GIV")) {
                    dm.acceptDownload(_socket);
                }
				else if (word.equals("CHAT")) {
                    ChatManager.instance().accept(_socket);
				}
                else if (word.equals("CONNECT") || word.equals("\n\n")) {
                    //HTTPStat.CONNECTBACK_RESPONSE.incrementStat();
                    // technically we could just always checkFirewall here, since
                    // we really always want to -- but since we're gonna check
                    // all incoming connections if this isn't set, might as well
                    // check and prevent a double-check.
                    if(ConnectionSettings.UNSET_FIREWALLED_FROM_CONNECTBACK)
                        ac.checkFirewall(_socket);
                    IOUtils.close(_socket);
                }
                else {
                    if(LOG.isErrorEnabled())
                        LOG.error("Unknown protocol: " + word);
                    IOUtils.close(_socket);
                }
            } catch (IOException e) {
                LOG.warn("IOX while dispatching", e);
                IOUtils.close(_socket);
            } catch(Throwable e) {
				ErrorService.error(e);
			}
        }
    }

    /**
     * Returns whether <tt>ip</tt> is a banned address.
     * @param ip an address in resolved dotted-quad format, e.g., 18.239.0.144
     * @return true iff ip is a banned address.
     */
    public boolean isBannedIP(byte[] addr) {        
        return !IPFilter.instance().allow(addr);
    }
    
    /**
     * Resets the last connectback time.
     */
    void resetLastConnectBackTime() {
        _lastConnectBackTime = 
             System.currentTimeMillis() - INCOMING_EXPIRE_TIME;
    }
    
    /**
     * (Re)validates acceptedIncoming.
     */
    private class IncomingValidator implements Runnable {
        public IncomingValidator() {}
        public void run() {
            // clear and revalidate if 1) we haven't had in incoming 
            // or 2) we've never had incoming and we haven't checked
            final long currTime = System.currentTimeMillis();
            final ConnectionManager cm = RouterService.getConnectionManager();
            if (
                (_acceptedIncoming && //1)
                 ((currTime - _lastIncomingTime) > INCOMING_EXPIRE_TIME)) 
                || 
                (!_acceptedIncoming && //2)
                 ((currTime - _lastConnectBackTime) > INCOMING_EXPIRE_TIME))
                ) {
                // send a connectback request to a few peers and clear
                // _acceptedIncoming IF some requests were sent.
                if(cm.sendTCPConnectBackRequests())  {
                    _lastConnectBackTime = System.currentTimeMillis();
                    Runnable resetter = new Runnable() {
                        public void run() {
                            boolean changed = false;
                            synchronized (Acceptor.class) {
                                if (_lastIncomingTime < currTime) {
                                    changed = setIncoming(false);
                                }
                            }
                            if(changed)
                                RouterService.incomingStatusChanged();
                        }
                    };
                    RouterService.schedule(resetter, 
                                           WAIT_TIME_AFTER_REQUESTS, 0);
                }
            }
        }
    }
}
