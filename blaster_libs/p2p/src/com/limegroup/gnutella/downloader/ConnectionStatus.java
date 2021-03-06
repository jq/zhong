package com.limegroup.gnutella.downloader;

/**
 * Simple class that enumerates values for the status of
 * requesting a file.
 *
 * Possible options are:
 *   NoFile (the server is not giving us the file)
 *   Queued (the server queued us)
 *   Connected (we are connected and should download)
 *   NoData (we have no data to request)
 *   PartialData (the server has other data to use)
 *   ThexResponse (the server just gave us a HashTree)
 */
public class ConnectionStatus {
    
    static final int TYPE_NO_FILE = 0;
    static final int TYPE_QUEUED = 1;
    static final int TYPE_CONNECTED = 2;
    static final int TYPE_NO_DATA = 3;
    static final int TYPE_PARTIAL_DATA = 4;

    /**
     * The status of this connection.
     */
    private final int STATUS;
    
    /**
     * The queue position.  Only valid if queued.
     */
    private final int QUEUE_POSITION;
    
    /**
     * The queue poll time.  Only valid if queued.
     */
    private final int QUEUE_POLL_TIME;
    
    
    /**
     * The sole NO_FILE instance.
     */
    private static final ConnectionStatus NO_FILE =
        new ConnectionStatus(TYPE_NO_FILE);
        
    /**
     * The sole CONNECTED instance.
     */
    private static final ConnectionStatus CONNECTED =
        new ConnectionStatus(TYPE_CONNECTED);
        
    /**
     * The sole NO_DATA instance.
     */
    private static final ConnectionStatus NO_DATA =
        new ConnectionStatus(TYPE_NO_DATA);
        
    /**
     * The sole PARTIAL_DATA instance.
     */
    private static final ConnectionStatus PARTIAL_DATA =
        new ConnectionStatus(TYPE_PARTIAL_DATA);
       
    /**
     * Constructs a ConnectionStatus of the specified status.
     */
    private ConnectionStatus(int status) {
        if(status == TYPE_QUEUED)
            throw new IllegalArgumentException();
        STATUS = status;
        QUEUE_POSITION = -1;
        QUEUE_POLL_TIME = -1;
    }
    
    /**
     * Constructs a ConnectionStatus for being queued.
     */
    private ConnectionStatus(int status, int queuePos, int queuePoll) {
        if(status != TYPE_QUEUED)
            throw new IllegalArgumentException();
            
        STATUS = status;
        QUEUE_POSITION = queuePos;
        QUEUE_POLL_TIME = queuePoll;
    }

    
    /**
     * Returns a ConnectionStatus for the server not having the file.
     */
    static ConnectionStatus getNoFile() {
        return NO_FILE;
    }
    
    /**
     * Returns a ConnectionStatus for being connected.
     */
    static ConnectionStatus getConnected() {
        return CONNECTED;
    }
    
    /**
     * Returns a ConnectionStatus for us not having data.
     */
    static ConnectionStatus getNoData() {
        return NO_DATA;
    }
    
    /**
     * Returns a ConnectionStatus for the server having other partial data.
     */
    static ConnectionStatus getPartialData() {
        return PARTIAL_DATA;
    }
    
    /**
     * Returns a ConnectionStatus for being queued with the specified position
     * and poll time (in seconds).
     */
    static ConnectionStatus getQueued(int pos, int poll) {
        // convert to milliseconds & add an extra second.
        poll *= 1000;
        poll += 1000;
        return new ConnectionStatus(TYPE_QUEUED, pos, poll);
    }
    
    
    /**
     * Returns the type of this ConnectionStatus.
     */
    int getType() {
        return STATUS;
    }
    
    /**
     * Determines if this is a NoFile ConnectionStatus.
     */
    boolean isNoFile() {
        return STATUS == TYPE_NO_FILE;
    }
    
    /**
     * Determines if this is a Connected ConnectionStatus.
     */    
    boolean isConnected() {
        return STATUS == TYPE_CONNECTED;
    }
    
    /**
     * Determines if this is a NoData ConnectionStatus.
     */
    boolean isNoData() {
        return STATUS == TYPE_NO_DATA;
    }
    
    /**
     * Determines if this is a PartialData ConnectionStatus.
     */
    boolean isPartialData() {
        return STATUS == TYPE_PARTIAL_DATA;
    }
    
    /**
     * Determines if this is a Queued ConnectionStatus.
     */
    boolean isQueued() {
        return STATUS == TYPE_QUEUED;
    }
    

    /**
     * Determines the queue position.  Throws IllegalStateException if called
     * when the status is not queued.
     */
    int getQueuePosition() {
        if(!isQueued())
            throw new IllegalStateException();
        return QUEUE_POSITION;
    }
    
    /**
     * Determines the queue poll time (in milliseconds).
     * Throws IllegalStateException if called when the status is not queued.
     */
    int getQueuePollTime() {
        if(!isQueued())
            throw new IllegalStateException();
        return QUEUE_POLL_TIME;
    }
    
}
