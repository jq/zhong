package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * This class handles Multicast messages.
 * Currently, this only listens for messages from the Multicast group.
 * Sending is done on the GUESS port, so that other nodes can reply
 * appropriately to the individual request, instead of multicasting
 * replies to the whole group.
 *
 * @see UDPService
 * @see MessageRouter
 */
public final class MulticastService {
    
    /**
     * The port of the group we're listening to.
     */
    public final static int _port = 6347;
    private static InetAddress _group;
    static {
    	try {
	      _group = InetAddress.getByName("234.21.81.1");
      } catch (UnknownHostException e) {
      }
    }
    private static ErrorCallbackImpl _err = new ErrorCallbackImpl();
	/**
	 * Sends the <tt>Message</tt> using UDPService to the multicast
	 * address/port.
     *
	 * @param msg  the <tt>Message</tt> to send
	 */
    public static synchronized void send(Message msg) {
        // only send the msg if we've initialized the port.
        UDPService.instance().send(msg, _group, _port, _err);
	}
    
    private static class ErrorCallbackImpl implements ErrorCallback {
        public void error(Throwable t) {}
        public void error(Throwable t, String msg) {}
    }

}
