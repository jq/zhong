package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import com.util.LOG;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.CountingOutputStream;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * a response to an HeadPing.  It is a trimmed down version of the standard HEAD response,
 * since we are trying to keep the sizes of the udp packets small.
 * 
 * This message can also be used for punching firewalls if the ping requests so. 
 * Feature like this can be used to allow firewalled nodes to participate more 
 * in download meshes.
 * 
 * Since headpings will be sent by clients who have started to download a file whose download
 * mesh contains  this host, it needs to contain information that will help those clients whether 
 * this host is a good bet to start an http download from.  Therefore, the following information should
 * be included in the response:
 * 
 *  - available ranges of the file 
 *  - queue status
 *  - some altlocs (if space permits)
 * 
 * the queue status can be an integer representing how many people are waiting in the queue.  If 
 * nobody is waiting in the queue and we have slots available, the integer can be negative.  So if
 * we have 3 people on the queue we'd send the integer 3.  If we have nobody on the queue and 
 * two upload slots available we would send -2.  A value of 0 means all upload slots are taken but 
 * the queue is empty.  This information can be used by the downloaders to better judge chances of
 * successful start of the download. 
 * 
 * Format:
 * 
 * 1 byte - features byte
 * 2 byte - response code
 * 4 bytes - vendor id
 * 1 byte - queue status
 * n*8 bytes - n intervals (if requested && file partial && fits in packet)
 * the rest - altlocs (if requested) 
 */
public class HeadPong extends VendorMessage {
	/**
	 * try to make packets less than this size
	 */
	private static final int PACKET_SIZE = 580;
	
	/**
	 * instead of using the HTTP codes, use bit values.  The first three 
	 * possible values are mutually exclusive though.  DOWNLOADING is
	 * possible only if PARTIAL_FILE is set as well.
	 */
	private static final byte FILE_NOT_FOUND= (byte)0x0;
	private static final byte COMPLETE_FILE= (byte)0x1;
	private static final byte PARTIAL_FILE = (byte)0x2;
	private static final byte FIREWALLED = (byte)0x4;
	private static final byte DOWNLOADING = (byte)0x8;
	
	private static final byte CODES_MASK=(byte)0xF;
	/**
	 * all our slots are full..
	 */
	private static final byte BUSY=(byte)0x7F;
	
	public static final int VERSION = 1;
	
	/**
	 * the features contained in this pong.  Same as those of the originating ping
	 */
	private byte _features;
	
	/**
	 * available ranges
	 */
	private IntervalSet _ranges;
	
	/**
	 * the altlocs that were sent, if any
	 */
	private Set _altLocs;
	
	/**
	 * the firewalled altlocs that were sent, if any
	 */
	private Set _pushLocs;
	
	/**
	 * the queue status, can be negative
	 */
	private int _queueStatus;
	
	/**
	 * whether the other host has the file at all
	 */
	private boolean _fileFound,_completeFile;
	
	/**
	 * the remote host
	 */
	private byte [] _vendorId;
	
	/**
	 * whether the other host can receive tcp
	 */
	private boolean _isFirewalled;
	
	/**
	 * whether the other host is currently downloading the file
	 */
	private boolean _isDownloading;
	
	/**
	 * creates a message object with data from the network.
	 */
	protected HeadPong(byte[] guid, byte ttl, byte hops,
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_HEAD_PONG, version, payload);
		
		//we should have some payload
		if (payload==null || payload.length<2)
			throw new BadPacketException("bad payload");
		
		
		//if we are version 1, the first byte has to be FILE_NOT_FOUND, PARTIAL_FILE, 
		//COMPLETE_FILE, FIREWALLED or DOWNLOADING
		if (version == VERSION && 
				payload[1]>CODES_MASK) 
			throw new BadPacketException("invalid payload for version "+version);
		
		try {
    		DataInputStream dais = new DataInputStream(new ByteArrayInputStream(payload));
    		
    		//read and mask the features
    		_features = (byte) (dais.readByte() & HeadPing.FEATURE_MASK);
    		
    		//read the response code
    		byte code = dais.readByte();
    		
    		//if the other host doesn't have the file, stop parsing
    		if (code == FILE_NOT_FOUND) 
    			return;
    		else
    			_fileFound=true;
    		
    		//is the other host firewalled?
    		if ((code & FIREWALLED) == FIREWALLED)
    			_isFirewalled=true;
    		
    		//read the vendor id
    		_vendorId = new byte[4];
    		dais.readFully(_vendorId);
    		
    		//read the queue status
    		_queueStatus = dais.readByte();
    		
    		//if we have a partial file and the pong carries ranges, parse their list
    		if ((code & COMPLETE_FILE) == COMPLETE_FILE) 
    			_completeFile=true;
    		else {
    			//also check if the host is downloading the file
    			if ((code & DOWNLOADING) == DOWNLOADING)
    				_isDownloading=true;
    			
    			if ((_features & HeadPing.INTERVALS) == HeadPing.INTERVALS)
    				_ranges = readRanges(dais);
    		}
    		
    		//parse any included firewalled altlocs
    		if ((_features & HeadPing.PUSH_ALTLOCS) == HeadPing.PUSH_ALTLOCS) 
    			_pushLocs=readPushLocs(dais);
    		
    			
    		//parse any included altlocs
    		if ((_features & HeadPing.ALT_LOCS) == HeadPing.ALT_LOCS) 
    			_altLocs=readLocs(dais);
		} catch(IOException oops) {
			throw new BadPacketException(oops.getMessage());
		}
	}
	

	/**
	 * 
	 * @return whether the alternate location still has the file
	 */
	public boolean hasFile() {
		return _fileFound;
	}
	
	/**
	 * 
	 * @return whether the alternate location has the complete file
	 */
	public boolean hasCompleteFile() {
		return hasFile() && _completeFile;
	}
	
	/**
	 * 
	 * @return the available ranges the alternate location has
	 */
	public IntervalSet getRanges() {
		return _ranges;
	}
	
	/**
	 * 
	 * @return set of <tt>Endpoint</tt> 
	 * containing any alternate locations this alternate location returned.
	 */
	public Set getAltLocs() {
		return _altLocs;
	}
	
	/**
	 * 
	 * @return set of <tt>PushEndpoint</tt>
	 * containing any firewalled locations this alternate location returned.
	 */
	public Set getPushLocs() {
		return _pushLocs;
	}
	
	/**
	 * @return all altlocs carried in the pong as 
	 * set of <tt>RemoteFileDesc</tt>
	 */
	public Set getAllLocsRFD(RemoteFileDesc original){
		Set ret = new HashSet();
		
		if (_altLocs!=null)
			for(Iterator iter = _altLocs.iterator();iter.hasNext();) {
				IpPort current = (IpPort)iter.next();
				ret.add(new RemoteFileDesc(original,current));
			}
		
		if (_pushLocs!=null)
			for(Iterator iter = _pushLocs.iterator();iter.hasNext();) {
				PushEndpoint current = (PushEndpoint)iter.next();
				ret.add(new RemoteFileDesc(original,current));
			}
		
		return ret;
	}
	
	/**
	 * 
	 * @return the remote vendor as string
	 */
	public String getVendor() {
		return new String(_vendorId);
	}
	
	/**
	 * 
	 * @return whether the remote is firewalled and will need a push
	 */
	public boolean isFirewalled() {
		return _isFirewalled;
	}
	
	public int getQueueStatus() {
		return _queueStatus;
	}
	
	public boolean isBusy() {
		return _queueStatus >= BUSY;
	}
	
	public boolean isDownloading() {
		return _isDownloading;
	}
	
	//*************************************
	//utility methods
	//**************************************
	
	/**
	 * reads available ranges from an inputstream
	 */
	private final IntervalSet readRanges(DataInputStream dais)
		throws IOException{
		int rangeLength=dais.readUnsignedShort();
		byte [] ranges = new byte [rangeLength];
		dais.readFully(ranges);
		return IntervalSet.parseBytes(ranges);
	}
	
	/**
	 * reads firewalled alternate locations from an input stream
	 */
	private final Set readPushLocs(DataInputStream dais) 
		throws IOException, BadPacketException {
		int size = dais.readUnsignedShort();
		byte [] altlocs = new byte[size];
		dais.readFully(altlocs);
		Set ret = new HashSet();
		ret.addAll(NetworkUtils.unpackPushEPs(altlocs));
		return ret;
	}
	
	/**
	 * reads non-firewalled alternate locations from an input stream
	 */
	private final Set readLocs(DataInputStream dais) 
		throws IOException, BadPacketException {
		int size = dais.readUnsignedShort();
		byte [] altlocs = new byte[size];
		dais.readFully(altlocs);
		Set ret = new HashSet();
		ret.addAll(NetworkUtils.unpackIps(altlocs));
		return ret;
	}
	
}
	
