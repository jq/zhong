package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.AlternateLocationCollector;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * This class handles sending HTTP HEAD requests to alternate locations,
 * propagating the download "mesh" of alternate locations for files.
 */
final class HeadRequester implements Runnable {

	/**
	 * The <tt>Set</tt> of hosts to send HEAD requests to.
	 */
	private final Set HOSTS;

	/**
	 * The <tt>URN</tt> instance to propagate through the mesh.
	 */
	private final URN RESOURCE_NAME;
	
	/**
	 * The <tt>AlternateLocationCollector</tt> to notify of any new 
	 * alternate locations that are discovered while making HEAD 
	 * requests.
	 */
	private final AlternateLocationCollector COLLECTOR;

	/**
	 * The total collection of all new alternate locations for the
	 * file.
	 */
	private final AlternateLocationCollection TOTAL_ALTS;

	/**
	 * Constructs a new <tt>HeadRequester</tt> instance for the specified
	 * <tt>List</tt> of hosts to be notified, the <tt>URN</tt> to
	 * propagate, the <tt>AlternateLocationCollector</tt> to store newly
	 * discovered locations, and the list of alternate locations to report.
	 *
	 * @param uploaders the hosts to send HEAD requests to
	 * @param resourceName the <tt>URN</tt> of the resource
	 * @param collector the <tt>AlternateLocationCollector</tt> that will
	 *  store any newly discovered locations
	 * @param totalAlts the total known alternate locations to report
	 */
	public HeadRequester(Set hosts, 
						 URN resourceName,
						 AlternateLocationCollector collector,
						 AlternateLocationCollection totalAlts) {
		HOSTS = hosts;
		RESOURCE_NAME = resourceName;
		COLLECTOR = collector;
		TOTAL_ALTS = totalAlts;
	}

	/**
	 * Implements <tt>Runnable</tt>.<p>
	 *
	 * Performs HEAD requests on the <tt>List</tt> of hosts to propagate the
	 * download mesh.
	 */
	public void run() {
        try {
            Iterator iter = HOSTS.iterator();
            while(iter.hasNext()) {
                RemoteFileDesc rfd = (RemoteFileDesc)iter.next();
                if(QueryReply.isFirewalledQuality(rfd.getQuality())) {
                    // do not attempt to make a HEAD request to firewalled hosts
                    continue;
                }
                URN urn = rfd.getSHA1Urn();
                if(urn == null) continue;
                if(!urn.equals(RESOURCE_NAME)) continue;
                
                URL url = rfd.getUrl();
                String connectTo = url.toExternalForm();
                HttpHead head = new HttpHead(connectTo);
                head.addHeader("User-Agent",
                                      CommonUtils.getHttpServer());
                head.addHeader("Cache-Control", "no-cache");
                head.addHeader(
                    HTTPHeaderName.GNUTELLA_CONTENT_URN.httpStringValue(),
                    RESOURCE_NAME.httpStringValue());
                head.addHeader(
                    HTTPHeaderName.ALT_LOCATION.httpStringValue(),
                    TOTAL_ALTS.httpStringValue());
                head.addHeader(
                    HTTPHeaderName.CONNECTION.httpStringValue(),
                    "close");
                //head.setFollowRedirects(false);
                DefaultHttpClient client = new DefaultHttpClient();
                try {
                    HttpResponse res = client.execute(head);
                    String contentUrn = getHeader(res, HTTPHeaderName.GNUTELLA_CONTENT_URN_STR);
                    if(contentUrn == null)
                        continue;

                    try {
                        URN reportedUrn = URN.createSHA1Urn(contentUrn); 
                        if(!reportedUrn.equals(RESOURCE_NAME)) {
                            continue;
                        }
                    } catch(IOException e) {
                        continue;
                    }
                    
                    String altLocs = getHeader(res, HTTPHeaderName.ALT_LOCATION_STR);
                    if(altLocs == null)
                        continue;

                    AlternateLocationCollection alc = 
                        AlternateLocationCollection.createCollectionFromHttpValue(altLocs);
                    if (alc == null)
                        continue;
                        
                    if(alc.getSHA1Urn().equals(COLLECTOR.getSHA1Urn()))
                        COLLECTOR.addAll(alc);

                } catch(IOException e) {
                    continue;
                } 
            }
        } catch(Throwable e) {
            ErrorService.error(e);
        }
	}
	
	/**
	 * Simple helper method to retrieve a header from an HttpMethod.
	 */
	private static String getHeader(HttpResponse methid, String name) {
        Header header = methid.getFirstHeader(name);
        if(header == null)
            return null;
        else
            return header.getValue();
    }
}
