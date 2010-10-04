package com.limegroup.gnutella.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.settings.ConnectionSettings;

/**
 * Provides socket operations that are not available on all platforms,
 * like connecting with timeouts and settings the SO_KEEPALIVE option.
 * Obsoletes the old SocketOpener class.
 */
public class Sockets {
    
    /**
     * The maximum number of concurrent connection attempts.
     */
    private static final int MAX_CONNECTING_SOCKETS = 8;
    
    /**
     * The current number of waiting socket attempts.
     */
    private static int _socketsConnecting = 0;
    

    private static volatile int _attempts=0;
	/**
	 * Ensure this cannot be constructed.
	 */
	private Sockets() {}

    /**
     * Sets the SO_KEEPALIVE option on the socket, if this platform supports it.
     * (Otherwise, it does nothing.)  
     *
     * @param socket the socket to modify
     * @param on the desired new value for SO_KEEPALIVE
     * @return true if this was able to set SO_KEEPALIVE
     */
    public static boolean setKeepAlive(Socket socket, boolean on) {
        try {
            socket.setKeepAlive(on);
            return true;
        } catch(SocketException se) {
            return false;
        }
    }

    /**
     * Connects and returns a socket to the given host, with a timeout.
     *
     * @param host the address of the host to connect to
     * @param port the port to connect to
     * @param timeout the desired timeout for connecting, in milliseconds,
	 *  or 0 for no timeout. In case of a proxy connection, this timeout
	 *  might be exceeded
     * @return the connected Socket
     * @throws IOException the connections couldn't be made in the 
     *  requested time
	 * @throws <tt>IllegalArgumentException</tt> if the port is invalid
     */
    public static Socket connect(String host, int port, int timeout) 
		throws IOException {
        if(!NetworkUtils.isValidPort(port)) {
            throw new IllegalArgumentException("port out of range: "+port);
        }

		_attempts++;
		return connectPlain(host, port, timeout);
	}

	/** 
	 * connect to a host directly
	 * @see connect(String, int, int)
	 */
	private static Socket connectPlain(String host, int port, int timeout)
		throws IOException {
      //return Sockets14.getSocket(host, port, timeout);
      
	    if (timeout!=0)
	         //b) Emulation using threads
          return (new SocketOpener(host, port)).connect(timeout);
	    else
	         //c) No timeouts
	        return new Socket(host, port);
	     

    }


	public static int getAttempts() {
	    return _attempts;
	}
	
	public static void clearAttempts() {
	    _attempts=0;
	}
	private static class SocketOpener {
	    private String host;
	    private int port;
	    /** The established socket, or null if not established OR couldn't be
	    * established.. Notify this when socket becomes non-null. */
	    private Socket socket=null;
	    /** True iff the connecting thread should close the socket if/when it
	    * is established. */
	    private boolean timedOut=false;
	    private boolean completed=false;
	    public SocketOpener(String host, int port) {
	    if((port & 0xFFFF0000) != 0) {
	    throw new IllegalArgumentException("port out of range: "+port);
	    }
	    this.host=host;
	    this.port=port;
	    }
	    /**
	    * Returns a new socket to the given host/port. If the socket couldn't be
	    * established withing timeout milliseconds, throws IOException. If
	    * timeout==0, no timeout occurs. If this thread is interrupted while
	    * making connection, throws IOException.
	    *
	    * @requires connect has only been called once, no other thread calling
	    * connect. Timeout must be non-negative.
	    */
	    public synchronized Socket connect(int timeout)
	    throws IOException {
	    //Asynchronously establish socket.
	    Thread t = new ManagedThread(new SocketOpenerThread(), "SocketOpener");
	    t.setDaemon(true);
	    t.start();
	    //Wait for socket to be established, or for timeout.
	    try {
	    this.wait(timeout);
	    } catch (InterruptedException e) {
	    if (socket==null)
	    timedOut=true;
	    else
	    try { socket.close(); } catch (IOException e2) { }
	    throw new IOException();
	    }
	    // Ensure that the SocketOpener is killed.
	    if( !completed )
	    t.interrupt();
	    //a) Normal case
	    if (socket!=null) {
	    return socket;
	    }
	    //b) Timeout case
	    else {
	    timedOut=true;
	    throw new IOException();
	    }
	    }
	    private class SocketOpenerThread implements Runnable {
	    public void run() {
	    Socket sock = null;
	    try {
	    try {
	    sock=new Socket(host, port);
	    } catch (IOException e) { }
	    synchronized (SocketOpener.this) {
	    completed = true;
	    if (timedOut && sock!=null)
	    try { sock.close(); } catch (IOException e) { }
	    else {
	    socket=sock; //may be null
	    SocketOpener.this.notify();
	    }
	    }
	    } catch(Throwable t) {
	    //We actively call Thread.interrupt() on this thread,
	    //and we've received reports of the Socket constructor
	    //throwing InterruptedException.
	    //(See: http://www9.limewire.com:82/dev/exceptions/3.4.4/
	    // java.lang.InterruptedException/Socket.19534.txt)
	    //However, nothing declares it to be thrown, so we can't
	    //catch and discard seperately.
	    //As a workaround, we only error if t is not an
	    //instanceof InterruptedException.
	    if(!(t instanceof InterruptedException))
	    ErrorService.error(t);
	    }
	    }
	    }
	    }	
}
