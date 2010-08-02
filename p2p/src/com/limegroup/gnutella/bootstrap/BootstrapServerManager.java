package com.limegroup.gnutella.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.util.LOG;
import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.StringUtils;


/**
 * A list of GWebCache servers.  Provides methods to fetch address addresses
 * from these servers, find the addresses of more such servers, and update the
 * addresses of these and other servers.<p>
 * 
 * Information on the GWebCache protocol can be found at 
 * http://zero-g.net/gwebcache/specs.html
 */
public class BootstrapServerManager {
    /**
     * Constant instance of the boostrap server.
     */
    private static final BootstrapServerManager INSTANCE =
        new BootstrapServerManager(); 
        
    // Constants used as return values for fetchEndpointsAsync
    /**
     * GWebCache use is turned off.
     */
    public static final int CACHE_OFF = 0;
    
    /**
     * A fetch was scheduled.
     */
    public static final int FETCH_SCHEDULED = 1;
    
    /**
     * The fetch wasn't scheduled because one is in progress.
     */
    public static final int FETCH_IN_PROGRESS = 2;
    
    /**
     * Too many endpoints were already fetch, the fetch wasn't scheduled.
     */
    public static final int FETCHED_TOO_MANY = 3;
    
    /**
     * All caches were already contacted atleast once.
     */
    public static final int NO_CACHES_LEFT = 4;
    
    /**
     * The maximum amount of responses to accept before we tell
     * the user that we've already hit a lot of things.
     */
    private static final int MAX_RESPONSES = 50;
    
    /**
     * The maximum amount of gWebCaches to hit before we tell
     * the user that we've already hit a lot of things.
     */
    private static final int MAX_CACHES = 5;

    /** The minimum number of endpoints/urls to fetch at a time. */
    private static final int ENDPOINTS_TO_ADD=10;
    /** The maximum number of bootstrap servers to retain in memory. */
    private static final int MAX_BOOTSTRAP_SERVERS=1000;
    /** The maximum number of hosts to try per request.  Prevents us from
     *  consuming all hosts if disconnected.  Non-final for testing. */
    public static int MAX_HOSTS_PER_REQUEST=20;
    /** The amount of time in milliseconds between update requests. 
     *  Public and non-final for testing purposes. */
    public static int UPDATE_DELAY_MSEC=60*60*1000;

    /** 
     * The bounded-size list of GWebCache servers, each as a BootstrapServer.
     * Order doesn't matter; hosts are chosen randomly from this.  Eventually
     * this may be prioritized by some metric.
     *  LOCKING: this 
     *  INVARIANT: _servers.size()<MAX_BOOTSTRAP_SERVERS
     */        
    private final List /* of BootstrapServer */ SERVERS=new ArrayList();
    
    /** The last bootstrap server we successfully connected to, or null if none.
     *  Used for sending updates.  _lastConnectable will generally be in
     *  SERVERS, though this is not strictly required because of SERVERS'
     *  random replacement strategy.  _lastConnectable should be nulled if we
     *  later unsuccessfully try to reconnect to it. */
    private BootstrapServer _lastConnectable;
    
    /** Source of randomness for picking servers.
     *  TODO: this is thread-safe, right? */
    private Random _rand=new Random();
    
    /** True if a thread is currently executing a hostfile request. 
     *  LOCKING: this (don't want multiple fetches) */
    private volatile boolean _hostFetchInProgress=false;
    
    /**
     * The index of the last server we connected to in the list
     * of servers.
     */
    private volatile int _lastIndex = 0;
    
    /**
     * The total amount of endpoints we've added to HostCatcher so far.
     */
    private volatile int _responsesAdded = 0;

    /**
     * Accessor for the <tt>BootstrapServerManager</tt> instance.
     * 
     * @return the <tt>BootstrapServerManager</tt> instance
     */
    public static BootstrapServerManager instance() {
        return INSTANCE;
    }

    /** 
     * Creates a new <tt>BootstrapServerManager</tt>.  Protected for testing.
     */
    protected BootstrapServerManager() {}

    /**
     * Adds server to this.
     */
    public synchronized void addBootstrapServer(BootstrapServer server) {
		if(server == null) 
			throw new NullPointerException("null bootstrap server not allowed");
        if (!SERVERS.contains(server))
            SERVERS.add(server);
        if (SERVERS.size()>MAX_BOOTSTRAP_SERVERS) {
            removeServer((BootstrapServer)SERVERS.get(0));
        }
    }
    
    /**
     * Notification that all bootstrap servers have been added.
     */
    public synchronized void bootstrapServersAdded() {
        addDefaultsIfNeeded();
        Collections.shuffle(SERVERS);
    }
    
    /**
     * Resets information related to the caches & endpoints we've fetched.
     */
    public synchronized void resetData() {
        _lastIndex = 0;
        _responsesAdded = 0;
        Collections.shuffle(SERVERS);
    }
    
    public boolean isEndpointFetchInProgress() {
        return _hostFetchInProgress;
    }

    /**
     * Returns an iterator of the bootstrap servers in this, each as a
     * BootstrapServer, in any order.  To prevent ConcurrentModification
     * problems, the caller should hold this' lock while using the
     * iterator.
     * @return an Iterator of BootstrapServer.
     */
    public synchronized Iterator /*of BootstrapServer*/ getBootstrapServers() {
        return SERVERS.iterator();
    }

    /** 
     * Asynchronously fetches other bootstrap URLs and stores them in this.
     * Stops after getting "enough" endpoints or exhausting all caches.  Uses
     * the "urlfile=1" message.
     */
    public synchronized void fetchBootstrapServersAsync() {
        addDefaultsIfNeeded();
        requestAsync(new UrlfileRequest(), "GWebCache urlfile");
    }

    /** 
     * Asynchronously fetches host addresses from bootstrap servers and stores
     * them in the HostCatcher.  Stops after getting "enough" endpoints or
     * exhausting all caches.  Does nothing if another endpoint request is in
     * progress.  Uses the "hostfile=1" message.
     */
    public synchronized int fetchEndpointsAsync() {
        addDefaultsIfNeeded();

        if (! _hostFetchInProgress) {
            if(_responsesAdded >= MAX_RESPONSES && _lastIndex >= MAX_CACHES)
               return FETCHED_TOO_MANY;
            
            if(_lastIndex >= size())
                return NO_CACHES_LEFT;
            
            _hostFetchInProgress=true;  //unset in HostfileRequest.done()
            requestAsync(new HostfileRequest(), "GWebCache hostfile");
            return FETCH_SCHEDULED;
        }

        return FETCH_IN_PROGRESS;
    }

    /**
     * Adds default bootstrap servers to this if this needs more entries.
     */
    private void addDefaultsIfNeeded() {
        if (SERVERS.size()>0)
            return;
        DefaultBootstrapServers.addDefaults(this);
        Collections.shuffle(SERVERS);
    }


    /////////////////////////// Request Types ////////////////////////////////

    private abstract class GWebCacheRequest {
        /** Returns the parameters for the given request, minus the "?" and any
         *  leading or trailing "&".  These will be appended after common
         *  parameters (e.g, "client"). */
        protected abstract String parameters();
        /** Called when if were unable to connect to the URL, got a non-standard
         *  HTTP response code, or got an ERROR method.  Default value: remove
         *  it from the list. */
        protected void handleError(BootstrapServer server) {
            if(LOG.isWarnEnabled())
                LOG.warn("Error on server: " + server);
            //For now, we just remove the host.  
            //Eventually we put it on probation.
            synchronized (BootstrapServerManager.this) {
                removeServer(server);        
                if (_lastConnectable==server)
                    _lastConnectable=null;
            }
        }
        protected int responses=0;

        /** Called when we got a line of data.  Implementation may wish
         *  to call handleError if the data is in a bad format. 
         *  @return false if there was an error processing, true otherwise.
         */
        protected boolean handleResponseData(BootstrapServer server, 
                String line) {
            boolean add = handleLine(line);
            if (add) {
                responses++;
            } else {
                handleError(server); 
            }
            return add;
        }
        /** Should we go on to another host? */
        protected abstract boolean needsMoreData();
        /** The next server to contact */
        protected abstract BootstrapServer nextServer();
        /** Called when this is done.  Default: does nothing. */
        protected void done() { }
    }
    
    protected boolean handleLine(String line) {
        String[] res = line.split("\\|");
        if (res.length > 1) {
            String l = "";
            for (int i = 0; i < res.length; ++i) {
                l += res[i] + " ";
            }
            // LOG.lognew(l+ " res1 " + res[1] + " res2 " + res[2]);
            if (res[0].compareTo("H") == 0) {
                return saveHost(res[1]);
            } else {
                // LOG.lognew("saveUrl:" + res[0]);
                return saveUrl(res[1]);
            }
        } else if (res.length == 1) {
            if (res[0].startsWith("h") || res[0].startsWith("H")) {
                return saveUrl(res[0]);
            } else {
                return saveHost(res[0]);
            }
        }
        return false;
    }

    protected boolean saveUrl(String line) {
        try {
            BootstrapServer e=new BootstrapServer(line);
            //Ensure url in this.  If list is too big, remove an
            //element.  Eventually we may remove "worst" element.
            synchronized (BootstrapServerManager.this) {
                addBootstrapServer(e);
            }
            // LOG.lognew("Added bootstrap host: " + e);
            ConnectionSettings.LAST_GWEBCACHE_FETCH_TIME.setValue(
                System.currentTimeMillis());                
        } catch (ParseException error) {
            // LOG.lognew("saveUrl error " + line);
            //One strike and you're out; skip servers that send bad data.
            return false;
        }
        return true;
        
    }
    
    protected boolean saveHost(String line) {
        try {
            //Only accept numeric addresses.  (An earlier version of this
            //did not do strict checking, possibly resulting in HTML in the
            //gnutella.net file!)
            Endpoint host=new Endpoint(line, true);
            //We don't know whether the host is an ultrapeer or not, but we
            //need to force a higher priority to prevent repeated fetching.
            //(See HostCatcher.expire)

            //we don't know locale of host so using Endpoint
            RouterService.getHostCatcher().add(host, 
                                               HostCatcher.CACHE_PRIORITY);
            _responsesAdded++;
        } catch (IllegalArgumentException bad) { 
            //One strike and you're out; skip servers that send bad data.
           // LOG.lognew("HostfileRequest error " + line);
            return false;
        }
        return true;
    }
    
    private final class HostfileRequest extends GWebCacheRequest {
        protected String parameters() {
            return "hostfile=1";
        }
        protected boolean needsMoreData() {
            return responses<ENDPOINTS_TO_ADD;
        }
        protected void done() {
            _hostFetchInProgress=false;
        }
        
        /**
         * Fetches the next server in line.
         */
        protected BootstrapServer nextServer() {
            BootstrapServer e = null;
            synchronized (this) {
                if(_lastIndex >= SERVERS.size()) {
                    if(LOG.isWarnEnabled())
                        LOG.warn("Used up all servers, last: " + _lastIndex);
                } else {
                    e = (BootstrapServer)SERVERS.get(_lastIndex);
                    _lastIndex++;
                }
            }
            return e;
        }            
        
        public String toString() {
            return "hostfile request";
        }   
    }

    private final class UrlfileRequest extends GWebCacheRequest {
        private int responses=0;
        protected String parameters() {
            return "urlfile=1";
        }
        protected boolean needsMoreData() {
            return responses<ENDPOINTS_TO_ADD;
        }
        
        protected BootstrapServer nextServer() {
            if(SERVERS.size() == 0)
                return null;
            else
                return (BootstrapServer)SERVERS.get(randomServer());
        }
        
        public String toString() {
            return "urlfile request";
        }
    }
    ///////////////////////// Generic Request Functions //////////////////////

    /** @param threadName a name for the thread created, for debugging */
    private void requestAsync(final GWebCacheRequest request,
                              String threadName) {
		if(request == null) {
			throw new NullPointerException("asynchronous request to null cache");
		}
		
        Thread runner=new ManagedThread() {
            public void managedRun() {
                try {
                    requestBlocking(request);
                } catch (Throwable e) {
                    //Internal error!  Display to GUI for debugging.
                    ErrorService.error(e);
                } finally {
                    request.done();
                }
            }
        };
        runner.setName(threadName);
        runner.setDaemon(true);
        runner.start();
    }

    private void requestBlocking(GWebCacheRequest request) {        
		if(request == null) {
			throw new NullPointerException("blocking request to null cache");
		}
		
        for (int i=0; request.needsMoreData() && i<MAX_HOSTS_PER_REQUEST; i++) {
            BootstrapServer e = request.nextServer();
            if(e == null)
                break;
            else
                requestFromOneHost(request, e);
        }
    }
                                        
    private void requestFromOneHost(GWebCacheRequest request,
                                    BootstrapServer server) {		
        // LOG.lognew("requesting: " + request + " from " + server);
		
        BufferedReader in = null;
        String urlString = server.getURLString();
        String connectTo = urlString
                 +"?hostfile=1&get=1&client="+CommonUtils.QHD_VENDOR_NAME
                 +"&version="+URLEncoder.encode(CommonUtils.getLimeWireVersion());
               //  +"&"+request.parameters();
        // add the guid if it's our cache, so we can see if we're hammering
        // from a single client, or if it's a bunch of clients behind a NAT
       // if(urlString.indexOf(".limewire.com/") > -1)
       //     connectTo += "&clientGUID=" + 
       //                  RouterService.MYGUID;
        HttpGet get;
        try {
            get = new HttpGet(connectTo);
        } catch(IllegalArgumentException iae) {
            // LOG.lognew("Invalid server", iae);
            // invalid uri? begone.
            request.handleError(server);
            return;
        }
            
        get.addHeader("Cache-Control", "no-cache");
        /*
        get.addRequestHeader("User-Agent", CommonUtils.getHttpServer());
        get.addRequestHeader(HTTPHeaderName.CONNECTION.httpStringValue(),
                             "close");
                             */
        //get.setFollowRedirects(false);
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse res = httpClient.execute(get);
            //HttpClientManager.executeMethodRedirecting(client, get);
            InputStream is = res.getEntity().getContent();// get.getResponseBodyAsStream();
            
            if(is == null) {
                // LOG.lognew("Invalid server: "+server);
                // invalid uri? begone.
                request.handleError(server);
                return;
            }
            in = new BufferedReader(new InputStreamReader(is));
            int code = res.getStatusLine().getStatusCode();
            if(code < 200 || code >= 300) {
                // LOG.lognew("Invalid status code: " + get.getStatusCode());
                throw new IOException("no 2XX ok.");
            }

            //For each line of data (excludes HTTP headers)...
            boolean firstLine = true;
            boolean errors = false;

            while (true) {                          
                String line = in.readLine();
                if (line == null)
                    break;
                    
                if (firstLine && StringUtils.startsWithIgnoreCase(line,"ERROR")){
                    // LOG.lognew(" readline ERROR" + urlString);

                    request.handleError(server);
                    errors = true;
                } else {
                    // LOG.lognew(" readline success " + urlString);

                    boolean retVal = request.handleResponseData(server, line);
                    if (!errors) errors = !retVal;
                }

                firstLine = false;
            }

            //If no errors, record the address AFTER sending requests so we
            //don't send a host its own url in update requests.
            if (!errors) {
                _lastConnectable = server;
                // LOG.lognew("success " + urlString);
            }
        } catch (IOException ioe) {
            // LOG.lognew("Exception in server " + ioe.getMessage());
            request.handleError(server);
        } 
    }

    /** Returns the number of servers in this. */
    protected synchronized int size() {
        return SERVERS.size();
    }
    
     /** Returns an random valid index of SERVERS.  Protected so we can override
      *  in test cases.  PRECONDITION: SERVERS.size>0. */
    protected int randomServer() {
        return _rand.nextInt(SERVERS.size());
    }
    
    /**
     * Removes the server.
     */
    protected synchronized void removeServer(BootstrapServer server) {
        SERVERS.remove(server);
        _lastIndex = Math.max(0, _lastIndex - 1);
    }
}
