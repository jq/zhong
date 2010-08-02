package com.limegroup.gnutella;

import java.net.InetSocketAddress;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.ProcessingQueue;

/**
 * Dispatches messages to the MessageRouter.
 */
class MessageDispatcher {
    
    private static final MessageDispatcher INSTANCE = new MessageDispatcher();
    private MessageDispatcher() {}
    public static MessageDispatcher instance() { return INSTANCE; }
    
    private final ProcessingQueue DISPATCH = new ProcessingQueue("MessageDispatch");
    
    /**
     * Dispatches a UDP message.
     */
    public void dispatchUDP(Message m, InetSocketAddress addr) {
        DISPATCH.add(new UDPDispatch(m, addr));
    }
        
    /**
     * Dispatches a TCP message.
     */
    public void dispatchTCP(Message m, ManagedConnection conn) {
        DISPATCH.add(new TCPDispatch(m, conn));
    }
    
    
    private static class UDPDispatch implements Runnable {
        private static final MessageRouter ROUTER = RouterService.getMessageRouter();
        private final Message m;
        private final InetSocketAddress addr;
        
        UDPDispatch(Message m, InetSocketAddress addr) {
            this.m = m; this.addr = addr;
        }
        
        public void run() {
            ROUTER.handleUDPMessage(m, addr);
        }
    }
    
    private static class TCPDispatch implements Runnable {
        private static final MessageRouter ROUTER = RouterService.getMessageRouter();
        private final Message m;
        private final ManagedConnection conn;

        TCPDispatch(Message m, ManagedConnection conn) {
            this.m = m; this.conn = conn;
        }
        
        public void run() {
            ROUTER.handleMessage(m, conn);
        }
    }
}