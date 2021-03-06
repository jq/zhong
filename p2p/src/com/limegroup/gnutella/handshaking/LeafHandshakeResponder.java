package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.util.Properties;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.ApplicationSettings;

/**
 * A very simple responder to be used by leaf-nodes during the
 * connection handshake while accepting incoming connections
 */
public final class LeafHandshakeResponder 
    extends AuthenticationHandshakeResponder {
    
    private boolean _pref = false;
    
    /**
     * Creates a new instance of LeafHandshakeResponder
     * @param manager Instance of connection manager, managing this
     * connection
     * @param router Instance of message router, to get correct local
     * address at runtime.
     * @param host The host with whom we are handshaking
     */
    public LeafHandshakeResponder(String host) {
        super(RouterService.getConnectionManager(), host);
    }
    
    //inherit doc comment
    protected HandshakeResponse respondUnauthenticated(
        HandshakeResponse response, boolean outgoing) throws IOException {
        if (outgoing) return respondToOutgoing(response);
        return respondToIncoming(response);
    }


    /**
     * Responds to an outgoing connection handshake.
     *
     * @return the <tt>HandshakeResponse</tt> with the handshake 
     *  headers to send in response to the connection attempt
     */
    private HandshakeResponse 
        respondToOutgoing(HandshakeResponse response) {

        // only connect to ultrapeers.
        if(!response.isUltrapeer()) {
            return HandshakeResponse.createLeafRejectOutgoingResponse();
        }

        //check if this is a preferenced connection
        if(_pref) {
            /* TODO: ADD STAT
              HandshakingStat.LEAF_OUTGOING_REJECT_LOCALE.incrementStat();
            */
            if(!ApplicationSettings.LANGUAGE
               .equals(response.getLocalePref())) {
                return HandshakeResponse.createLeafRejectLocaleOutgoingResponse();
            }
        }
        
        if(!_manager.allowConnection(response)) {
            return HandshakeResponse.createLeafRejectOutgoingResponse();
        }
        
        Properties ret = new Properties();

        // might as well save a little bandwidth.
		if(response.isDeflateAccepted()) {
		 //   ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
		}
        
        
        return HandshakeResponse.createAcceptOutgoingResponse(ret);
    }

    
    /**
     * Responds to an incoming connection handshake.
     *
     * @return the <tt>HandshakeResponse</tt> with the handshake 
     *  headers to send in response to the connection attempt
     */
    private HandshakeResponse respondToIncoming(HandshakeResponse hr) {
		if(hr.isCrawler()) {
			return HandshakeResponse.createCrawlerResponse();
		}
		
        //if not an ultrapeer, reject.
        if(!hr.isUltrapeer()) {
            return HandshakeResponse.createLeafRejectOutgoingResponse();
        }		
        
        Properties ret = new LeafHeaders(getRemoteIP());
        
        //If we already have enough ultrapeers, reject.
        if(!_manager.allowConnection(hr)) {
            return HandshakeResponse.createLeafRejectIncomingResponse(hr);
        } 

		//deflate if we can ...
		if(hr.isDeflateAccepted()) {
		    ret.put(HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
		}         


        //b) We're not a leaf yet, so accept the incoming connection
        return HandshakeResponse.createAcceptIncomingResponse(hr, ret);
    }

    /**
     * set preferencing
     */
    public void setLocalePreferencing(boolean b) {
        _pref = b;
    }
}



