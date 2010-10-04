package com.limegroup.gnutella.messages;

import com.limegroup.gnutella.ByteOrder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.LinkedList;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.NameValue;

/**
 * A Gnutella bye message.
 */

public class ByeRequest extends Message {

	private short _code=0;
	private String _message=null; 
	private byte[] payload = null;
	
	// normal
	public static final byte[] EXIT_NORMAL = {(byte)0x00,(byte)0xC8}; //200
	public static final byte[] EXPLICIT_EXIT =  {(byte)0x00,(byte)0xC9}; //201
	
	//client fault
	public static final byte[] BIG_PACKET = {(byte)0x01, (byte)0x90}; //400
	public static final byte[] DUPLICATE = {(byte)0x01, (byte)0x91}; //401
	public static final byte[] IMPROPER_QUERY = {(byte)0x01, (byte)0x92}; //402
	public static final byte[] LONG_LIVED = {(byte)0x01, (byte)0x93}; //403
	public static final byte[] UNKNOWN_MSG = {(byte)0x01, (byte)0x94}; //404
	public static final byte[] TIMEOUT = {(byte)0x01, (byte)0x95}; //405
	public static final byte[] PONG_FAIL = {(byte)0x01, (byte)0x96}; //406
	public static final byte[] MOOCHER = {(byte)0x01, (byte)0x97}; //407
	
	//servent internal error
	public static final byte[] BAD_ERROR = {(byte)0x01, (byte)0xF4}; //500
	public static final byte[] DESYNC = {(byte)0x01, (byte)0xF5}; //501
	public static final byte[] FULL_QUEUE = {(byte)0x01, (byte)0xF6}; //502
	
    /////////////////Constructors for incoming messages/////////////////
    /**
     * Creates a normal ping from data read on the network
	 *	A Bye packet MUST be sent with TTL=1 (to avoid accidental propagation
	 *	by an unaware servent), and hops=0 (of course).
     */

    public ByeRequest(byte[] guid, byte[] payload) {
        super(guid, Message.F_BYE, (byte)1, (byte)0, payload.length);
        
        _code = ByteOrder.beb2short(payload, 0);        
        if( payload.length >2 )
        	_message = new String(payload,2,(payload.length-2));
        
        this.payload = payload;
    }
    
    public static byte[] makePayload(String errorString, byte[] code) {
    	byte[] errorArray = errorString.getBytes();
    	byte[] byePayload = new byte[code.length+errorArray.length];
    	System.arraycopy(code,0,byePayload,0,code.length);
    	System.arraycopy(errorArray,0,byePayload,code.length,errorArray.length);
    	return byePayload;
    }

    public ByeRequest(byte[] guid) {
        super(guid, Message.F_BYE, (byte)1, (byte)0, 0);
    }
   
	// inherit doc comment
	public void recordDrop() {
		
		// maybe we should implement enum style so that we won't dup code 
		// when add a new type of message
		//DroppedSentMessageStatHandler.TCP_PING_REQUESTS.addMessage(this);
	}

    public String toString() {
        return "ByeRequest("+super.toString()+")";
    }
    
    // this is try to strip GGEP
    public Message stripExtendedPayload() {    	
        return this;
    }
    
    protected void writePayload(OutputStream out) throws IOException {
    	
        if(payload != null) {
            out.write(payload);
        }
        // the Bye is still written even if there's no payload
        //SentMessageStatHandler.TCP_PING_REQUESTS.addMessage(this);
        //Do nothing...there is no payload!
    }

}