package com.limegroup.gnutella;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.util.LOG;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.Cancellable;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.ProcessingQueue;
/**
 * Sends Gnutella messages via UDP to a set of hosts and calls back to a 
 * listener whenever responses are returned.
 */
public class UDPHostRanker {
    private static final MessageRouter ROUTER = 
        RouterService.getMessageRouter();
        
    private static final ProcessingQueue QUEUE = 
         new ProcessingQueue("UDPHostRanker");
        
    /**
     * The time to wait before expiring a message listener.
     *
     * Non-final for testing.
     */
    public static int LISTEN_EXPIRE_TIME = 20 * 1000;
    
    /** Send pings every this often */
    private static final long SEND_INTERVAL = 500;
    
    /** Send this many pings each time */
    private static final int MAX_SENDS = 15;
    
    /**
     * The current number of datagrams we've sent in the past 500 milliseconds.
     */
    private static int _sentAmount;
    
    /**
     * The last time we sent a datagram.
     */
    private static long _lastSentTime;
    
    /**
     * set of endpoints we pinged since last expiration
     */
    private static final Set _recent = new TreeSet(IpPort.COMPARATOR);
    
    /**
     * Ranks the specified Collection of hosts.
     */
    public static void rank(Collection hosts) {
        rank(hosts, null, null, null);
    }
    
    /**
     * Ranks the specified Collection of hosts with the given message.
     */
    public static void rank(Collection hosts, Message message) {
        rank(hosts, null, null, message);
    }
    
    /**
     * Ranks the specified Collection of hosts with the given
     * Canceller.
     */
    public static void rank(Collection hosts, Cancellable canceller) {
        rank(hosts, null, canceller, null);
    }
    
    /**
     * Ranks the specified collection of hosts with the given 
     * MessageListener.
     */
    public static void rank(Collection hosts, MessageListener listener) {
        rank(hosts, listener, null, null);
    }
    
    /**
     * Ranks the specified collection of hosts with the given
     * MessageListener & Cancellable.
     */
    public static void rank(Collection hosts, MessageListener listener,
                            Cancellable canceller) {
        rank(hosts, listener, canceller, null);
    }

    /**
     * Ranks the specified <tt>Collection</tt> of hosts.
     * 
     * @param hosts the <tt>Collection</tt> of hosts to rank
     * @param listener a MessageListener if you want to spy on the message.  can
     * be null.
     * @param canceller a Cancellable that can short-circuit the sending
     * @param message the message to send, can be null. 
     * @return a new <tt>UDPHostRanker</tt> instance
     * @throws <tt>NullPointerException</tt> if the hosts argument is 
     *  <tt>null</tt>
     */
    public static void rank(final Collection hosts,
                            final MessageListener listener,
                            Cancellable canceller,
                            final Message message) {
        if(hosts == null)
            throw new NullPointerException("null hosts not allowed");
        if(canceller == null) {
            canceller = new Cancellable() {
                public boolean isCancelled() { return false; }
            };
        }
        
        QUEUE.add(new SenderBundle(hosts, listener, canceller, message));
    }
    
    /**
     * Waits for UDP listening to be activated.
     */
    private static boolean waitForListening(Cancellable canceller) {
        int waits = 0;
        while(!UDPService.instance().isListening() && waits < 10 &&
              !canceller.isCancelled()) {
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                // Should never happen.
                ErrorService.error(e);
            }
            waits++;
        }
        
        return waits < 10;
    }
        
    /**
     * Sends the given send bundle.
     */
    private static void send(SenderBundle info) {
        final Collection hosts = info.hosts;
        final MessageListener listener = info.listener;
        final Cancellable canceller = info.canceller;
        Message message = info.message;
        
        // something went wrong with UDPService - don't try to send
        if (!waitForListening(canceller))
            return;
    
        if(message == null)
            message = PingRequest.createUDPPing();
            
        final byte[] messageGUID = message.getGUID();
        
        if (listener != null)
            ROUTER.registerMessageListener(messageGUID, listener);

        
        Iterator iter = hosts.iterator();
        while(iter.hasNext() && !canceller.isCancelled()) {
            IpPort host = (IpPort)iter.next();
            
            if (_recent.contains(host))
                continue;
            
            _recent.add(host);
            
            long now = System.currentTimeMillis();
            if(now > _lastSentTime + SEND_INTERVAL) {
                _sentAmount = 0;
            } else if(_sentAmount == MAX_SENDS) {
                try {
                    Thread.sleep(SEND_INTERVAL);
                    now = System.currentTimeMillis();
                } catch(InterruptedException ignored) {}
                _sentAmount = 0;
            }
            
            if(LOG.isTraceEnabled())
                LOG.trace("Sending to " + host + ": " + message);
            UDPService.instance().send(message, host);
            _sentAmount++;
            _lastSentTime = now;
        }

        // also take care of any MessageListeners
        if (listener != null) {
            // Now schedule a runnable that will remove the mapping for the GUID
            // of the above message after 20 seconds so that we don't store it 
            // indefinitely in memory for no reason.
            Runnable udpMessagePurger = new Runnable() {
                    public void run() {
                        ROUTER.unregisterMessageListener(messageGUID, listener);
                    }
                };
         
            // Purge after 20 seconds.
            RouterService.schedule(udpMessagePurger, LISTEN_EXPIRE_TIME, 0);
        }
    }
    
    /**
     * clears the list of Endpoints we pinged since the last reset,
     * after sending all currently queued messages.
     */
    static void resetData() {
        QUEUE.add(new Runnable(){
            public void run() {
                _recent.clear();
            }
        });
    }
    
    /**
     * Simple bundle that can send itself.
     */
    private static class SenderBundle implements Runnable {
        private final Collection hosts;
        private final MessageListener listener;
        private final Cancellable canceller;
        private final Message message;
        
        public SenderBundle(Collection hosts, MessageListener listener,
                      Cancellable canceller, Message message) {
            this.hosts = hosts;
            this.listener = listener;
            this.canceller = canceller;
            this.message = message;
        }
        
        public void run() {
            send(this);
        }
    }
}
