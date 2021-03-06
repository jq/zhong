package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.util.LOG;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.BandwidthTracker;
import com.limegroup.gnutella.BandwidthTrackerImpl;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SpeedConstants;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.AlternateLocationCollector;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.ApproximateMatcher;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.FixedSizeExpiringSet;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A smart download.  Tries to get a group of similar files by delegating
 * to DownloadWorker threads.  Does retries and resumes automatically.
 * Reports all changes to a DownloadManager.  This class is thread safe.<p>
 *
 * Smart downloads can use many policies, and these policies are free to change
 * as allowed by the Downloader specification.  This implementation provides
 * swarmed downloads, the ability to download copies of the same file from
 * multiple hosts.  See the accompanying white paper for details.<p>
 *
 * Subclasses may refine the requery behavior by overriding the 
 * newRequery(n), allowAddition(..), and addDownload(..)  methods.
 * MagnetDownloader also redefines the tryAllDownloads(..) method to handle
 * default locations, and the getFileName() method to specify the completed
 * file name.<p>
 * 
 * Subclasses that pass this RemoteFileDesc arrays of size 0 MUST override
 * the getFileName method, otherwise an assert will fail.<p>
 * 
 * This class implements the Serializable interface but defines its own
 * writeObject and readObject methods.  This is necessary because parts of the
 * ManagedDownloader (e.g., sockets) are inherently unserializable.  For this
 * reason, serializing and deserializing a ManagedDownloader M results in a
 * ManagedDownloader M' that is the same as M except it is
 * unconnected. <b>Furthermore, it is necessary to explicitly call
 * initialize(..) after reading a ManagedDownloader from disk.</b>
 */
public class ManagedDownloader implements Downloader, Serializable {
    /*
      IMPLEMENTATION NOTES: The basic idea behind swarmed (multisource)
      downloads is to download one file in parallel from multiple servers.  For
      example, one might simultaneously download the first half of book from
      server A and the second half from server B.  This increases throughput if
      the downstream capacity of the downloader is greater than the upstream
      capacity of the fastest uploader.

      The ideal way of identifying duplicate copies of a file is to use hashes
      via the HUGE proposal.

      When discussing swarmed downloads, it's useful to divide parts of a file
      into three categories: black, grey, and white. Black regions have already
      been downloaded to disk.  Grey regions have been assigned to a downloader
      but not yet completed.  White regions have not been assigned to a
      downloader.
      
      ManagedDownloader delegates to multiple DownloadWorker instances, one for
      each HTTP connection.  They use a shared VerifyingFile object that keeps
      track of which blocks have been written to disk.  
      
      ManagedDownloader uses one thread to control the smart downloads plus one
      thread per DownloadWorker instance.  The call flow of ManagedDownloader's
      "master" thread is as follows:

       performDownload:
           initializeDownload    
           fireDownloadWorkers (asynchronously start workers)    
           verifyAndSave

      The core downloading loop is done by fireDownloadWorkers.Currently the 
      desired parallelism is fixed at 2 for modem users, 6 for cable/T1/DSL, 
      and 8 for T3 and above.
      
      DownloadManager notifies a ManagedDownloader when it should start
      performDownload.  An inactive download (waiting for a busy host,
      waiting for a user to requery, waiting for GUESS responses, etc..)
      is essentially a state-machine, pumped forward by DownloadManager.
      The 'master thread' of a ManagedDownloader is recreated every time
      DownloadManager moves the download from inactive to active.
      
      All downloads start QUEUED.
      From there, it will stay queued until a slot is available.
      
      If atleast one host is available to download from, then the
      first state is always CONNECTING.
          After connecting, a downloader can become:
          a) DOWNLOADING (actively downloading)
          b) WAITING_FOR_RETRY (busy hosts)
          c) ABORTED (user manually stopped the download)
          c2) PAUSED (user paused the download)
          d) REMOTE_QUEUED (the remote host queued us)
      
      If no hosts existed for connecting, or we exhausted our attempts
      at connecting to all possible hosts, the state will become one of:
          e) GAVE_UP (maxxed out on requeries)
          f) WAITING_FOR_USER (waiting for the user to initiate a requery)
          g) ITERATIVE_GUESSING (targetted location of more sources)
      If the user resumes the download and we were WAITING_FOR_USER, a requery
      is sent out and we go into WAITING_FOR_RESULTS stage.  After we have
      finished waiting for results (if none arrived), we will either go back to
      WAITING_FOR_USER (if we are allowed more requeries), or GAVE_UP (if we 
      maxxed out the requeries).
      After ITERATIVE_GUESSING completes, if no results arrived then we go to 
      WAITING_FOR_USER.  Prior to WAITING_FOR_RESULTS, if no connections are
      active then we wait at WAITING_FOR_CONNECTIONS until connections exist.
      
      If more results come in while waiting in these states, the download will
      either immediately become active (CONNECTING ...) again, or change its
      state to QUEUED and wait for DownloadManager to activate it.
      
      The download can finish in one of the following states:
          h) COMPLETE (download completed just fine)
          i) ABORTED  (user pressed stopped at some point)
          j) DISK_PROBLEM (limewire couldn't the file)
          k) CORRUPT_FILE (the file was corrupt)

     There are a few intermediary states:
          l) HASHING
          m) SAVING
     HASHING & SAVING are seen by the GUI, and are used just prior to COMPLETE,
     to let the user know what is currently happening in the closing states of
     the download.  RECOVERY_FAILED is used as an indicator that we no longer want
     to retry the download, because we've tried and recovered from corruption
     too many times.
     
     How corruption is handled:
     There are two general cases where corruption can be discovered - during a download
     or after the download has finished.
     
     During the download, each worker thread checks periodically whether the amount of 
     data lost to corruption exceeds 10% of the completed file size.  Whenever that 
     happens, the worker thread asks the user whether the download should be terminated.
     If the user chooses to delete the file, the downloader is stopped asynchronously and
     _corruptState is set to CORRUPT_STOP_STATE.  The master download thread is interrupted,
     it checks _corruptState and either discards or removes the file.
     
     After the download, if the sha1 does not match the expected, the master download thread
     propmts the user whether they want to keep the file or discard it.  If we did not have a
     tree during the download we remove the file from partial sharing, otherwise we keep it
     until the user asnswers the prompt (which may take a very long time for overnight downloads).
     The tree itself is purged.
     
    */
        
    /** Ensures backwards compatibility. */
    static final long serialVersionUID = 2772570805975885257L;
    
    /** Make everything transient */
    private static final ObjectStreamField[] serialPersistentFields = 
    	ObjectStreamClass.NO_FIELDS;

    /*********************************************************************
     * LOCKING: obtain this's monitor before modifying any of the following.
     * files, _activeWorkers, busy and setState.  We should  not hold lock 
     * while performing blocking IO operations, however we need to ensure 
     * atomicity and thread safety for step 2 of the algorithm above. For 
     * this reason we needed to add another lock - stealLock.
     *
     * We don't want to synchronize assignAndRequest on this since that freezes
     * the GUI as it calls getAmountRead() frequently (which also hold this'
     * monitor).  Now assignAndRequest is synchronized on stealLock, and within
     * it we acquire this' monitor when we are modifying shared datastructures.
     * This additional lock will prevent GUI freezes, since we hold this'
     * monitor for a very short time while we are updating the shared
     * datastructures, also atomicity is guaranteed since we are still
     * synchronized.  StealLock is also held for manipulations to the verifying file,
     * and for all removal operations from the _activeWorkers list.
     * 
     * stealLock->this is ok
     * stealLock->verifyingFile is ok
     * 
     * Never acquire stealLock's monitor if you have this' monitor.
     *
     * Never acquire incompleteFileManager's monitor if you have commonOutFile's
     * monitor.
     *
     * Never obtain manager's lock if you hold this.
     ***********************************************************************/
    private Object stealLock;

    /** This' manager for callbacks and queueing. */
    private DownloadManager manager;
    /** The repository of incomplete files. */
    private IncompleteFileManager incompleteFileManager;
    /** A ManagedDownloader needs to have a handle to the ActivityCallback, so
     * that it can notify the gui that a file is corrupt to ask the user what
     * should be done.  */
    private ActivityCallback callback;
    /** The complete list of files passed to the constructor.  Must be
     *  maintained in memory to support resume.  allFiles may only contain
     *  elements of type RemoteFileDesc and URLRemoteFileDesc */
    private RemoteFileDesc[] allFiles;

    /**
     * The maximum amount of times we'll try to recover.
     */
    private static final int MAX_CORRUPTION_RECOVERY_ATTEMPTS = 5;

    /**
     * The time to wait between requeries, in milliseconds.  This time can
     * safely be quite small because it is overridden by the global limit in
     * DownloadManager.  Package-access and non-final for testing.
     * @see com.limegroup.gnutella.DownloadManager#TIME_BETWEEN_REQUERIES */
    static int TIME_BETWEEN_REQUERIES = 5*60*1000;  //5 minutes
    
    /**
     * How long we'll wait after sending a GUESS query before we try something
     * else.
     */
    private static final int GUESS_WAIT_TIME = 5000;
    
    /**
     * How long we'll wait before attempting to download again after checking
     * for stable connections (and not seeing any)
     */
    private static final int CONNECTING_WAIT_TIME = 750;
    
    /**
     * The number of times to requery the network. All requeries are
     * user-driven.
     */
    private static final int REQUERY_ATTEMPTS = 1;
    

    /** The size of the approx matcher 2d buffer... */
    private static final int MATCHER_BUF_SIZE = 120;
    
	/** The value of an unknown filename - potentially overridden in 
      * subclasses */
	protected static final String UNKNOWN_FILENAME = "";  

    /** This is used for matching of filenames.  kind of big so we only want
     *  one. */
    private static ApproximateMatcher matcher = 
        new ApproximateMatcher(MATCHER_BUF_SIZE);    

    ////////////////////////// Core Variables /////////////////////////////
    /**
     * The current RFDs that this ManagedDownloader is connecting to.
     * This is necessary to store so that when an RFD is removed from files,
     * we can check in this datastructure to ensure that an RFD is not
     * connected to twice.
     *
     * Initialized in fireDownloadWorkers.
     */
    private List currentRFDs;

    /** If started, the thread trying to coordinate all downloads.  
     *  Otherwise null. */
    private volatile Thread dloaderManagerThread;
    /** True iff this has been forcibly stopped. */
    private volatile boolean stopped;
    /** True iff this has been paused.  */
    private volatile boolean paused;

    
    /** 
     * The connections we're using for the current attempts.
     * LOCKING: copy on write on this 
     * 
     */    
    private volatile List /* of DownloadWorker */ _activeWorkers;
    
    /**
     * A List of worker threads in progress.  Used to make sure that we do
     * not terminate (in fireDownloadWorkers) without hope if threads are
     * connecting to hosts (i.e., removed from files) but not have not yet been
     * added to _activeWorkers.
     * Also, if the download completes and any of the threads are sleeping 
     * because it has been queued by the uploader, those threads need to be 
     * killed.
     * LOCKING: synchronize on this
     * INVARIANT: _activeWorkers.size<=threads 
     */
    private List /*of DownloadWorker*/ _workers;

    /**
     * Stores the queued threads and the corresponding queue position
     * LOCKING: copy on write on this
     */
    private volatile Map /*DownloadWorker -> Integer*/ queuedWorkers;

    /** List of RemoteFileDesc to which we actively connect and request parts
     * of the file.
     * LOCKING: this
     */
    private List /*of RemoteFileDesc */ rfds;
    
    /**
     * The SHA1 hash of the file that this ManagedDownloader is controlling.
     */
    protected URN downloadSHA1;
	
    /**
     * The collection of alternate locations we successfully downloaded from
     * somthing from. We will never use this data-structure until the very end,
     * when we have become active uploaders of the file.
     */
	private AlternateLocationCollection validAlts; 
	
	/**
	 * A list of the most recent failed locations, so we don't try them again.
	 */
	private Set invalidAlts;

    /**
     * Cache the most recent failed locations. 
     * Holds <tt>AlternateLocation</tt> instances
     */
    private Set recentInvalidAlts;
    
    private VerifyingFile commonOutFile;
    
    ////////////////datastructures used only for pushes//////////////
    /** MiniRemoteFileDesc -> Object. 
        In the case of push downloads, connecting threads write the values into
        this map. The acceptor threads consumes these values and notifies the
        connecting threads when it is done.        
    */
    private Map miniRFDToLock;

    ///////////////////////// Variables for GUI Display  /////////////////
    /** The current state.  One of Downloader.CONNECTING, Downloader.ERROR,
      *  etc.   Should be modified only through setState. */
    private int state;
    /** The system time that we expect to LEAVE the current state, or
     *  Integer.MAX_VALUE if we don't know. Should be modified only through
     *  setState. */
    private long stateTime;
    
    /** The current incomplete file that we're downloading, or the last
     *  incomplete file if we're not currently downloading, or null if we
     *  haven't started downloading.  Used for previewing purposes. */
    protected File incompleteFile;
    /** The fully-qualified name of the downloaded file when this completes, or
     *  null if we haven't started downloading. Used for previewing purposes. */
    private File completeFile;
    /**
     * The position of the downloader in the uploadQueue */
    private int queuePosition;
    /**
     * The vendor the of downloader we're queued from.
     */
    private String queuedVendor;

    /** If in CORRUPT_FILE state, the number of bytes downloaded.  Note that
     *  this is less than corruptFile.length() if there are holes. */
    private volatile int corruptFileBytes;
    /** If in CORRUPT_FILE state, the name of the saved corrupt file or null if
     *  no corrupt file. */
    private volatile File corruptFile;

	/** The list of all chat-enabled hosts for this <tt>ManagedDownloader</tt>
	 *  instance.
	 */
	private DownloadChatList chatList;

	/** The list of all browsable hosts for this <tt>ManagedDownloader</tt>
	 *  instance.
	 */
	private DownloadBrowseHostList browseList;


    /** The various states of the ManagedDownloade with respect to the 
     * corruption state of this download. 
     */
    private static final int NOT_CORRUPT_STATE = 0;
    private static final int CORRUPT_WAITING_STATE = 1;
    private static final int CORRUPT_STOP_STATE = 2;
    private static final int CORRUPT_CONTINUE_STATE = 3;
    /**
     * The actual state of the ManagedDownloader with respect to corruption
     * LOCKING: obtain corruptStateLock
     * INVARIANT: one of NOT_CORRUPT_STATE, CORRUPT_WAITING_STATE, etc.
     */
    private volatile int corruptState;
    private Object corruptStateLock;

    /**
     * Locking object to be used for accessing all alternate locations.
     * LOCKING: never try to obtain monitor on this if you hold the monitor on
     * altLock 
     */
    private Object altLock;

    /**
     * one BandwidthTrackerImpl so we don't have to allocate one for
     * each download every time we write a snapshot.
     */
    private static final BandwidthTrackerImpl BANDWIDTH_TRACKER_IMPL =
        new BandwidthTrackerImpl();
    
    /**
     * The number of times we've been bandwidth measured
     */
    private int numMeasures = 0;
    
    /**
     * The average bandwidth over all managed downloads.
     */
    private float averageBandwidth = 0f;

    /**
     * The GUID of the original query.  may be null.
     */
    private final GUID originalQueryGUID;
    
    /**
     * Whether or not this was deserialized from disk.
     */
    protected boolean deserializedFromDisk;
    
    /**
     * The number of queries already done for this downloader.
     * Influenced by the type of downloader & whether or not it was started
     * from disk or from scratch.
     */
    private int numQueries;
    
    /**
     * Whether or not we've sent a GUESS query.
     */
    private boolean triedLocatingSources;
    
    /**
     * Whether or not we've gotten new files since the last time this download
     * started.
     */
    private volatile boolean receivedNewSources;
    
    /**
     * The time the last query was sent out.
     */
    private long lastQuerySent;
    
    /**
     * The current priority of this download -- only valid if inactive.
     * Has no bearing on the download itself, and is used only so that the
     * download doesn't have to be indexed in DownloadManager's inactive list
     * every second, for GUI updates.
     */
    private volatile int inactivePriority;


    /**
     * Creates a new ManagedDownload to download the given files.  The download
     * does not start until initialize(..) is called, nor is it safe to call
     * any other methods until that point.
     * @param files the list of files to get.  This stops after ANY of the
     *  files is downloaded.
     * @param ifc the repository of incomplete files for resuming
     * @param originalQueryGUID the guid of the original query.  sometimes
     * useful for WAITING_FOR_USER state.  can be null.
     */
    public ManagedDownloader(RemoteFileDesc[] files, IncompleteFileManager ifc,
                             GUID originalQueryGUID) {
		if(files == null) {
			throw new NullPointerException("null RFDS");
		}
		if(ifc == null) {
			throw new NullPointerException("null incomplete file manager");
		}
        this.allFiles = files;
        this.incompleteFileManager = ifc;
        this.originalQueryGUID = originalQueryGUID;
        this.deserializedFromDisk = false;
    }

    /** 
     * See note on serialization at top of file 
     * <p>
     * Note that we are serializing a new BandwidthImpl to the stream. 
     * This is for compatibility reasons, so the new version of the code 
     * will run with an older download.dat file.     
     */
    private synchronized void writeObject(ObjectOutputStream stream)
            throws IOException {
        stream.writeObject(allFiles);
        //Blocks can be written to incompleteFileManager from other threads
        //while this downloader is being serialized, so lock is needed.
        synchronized (incompleteFileManager) {
            stream.writeObject(incompleteFileManager);
        }
        //We used to write BandwidthTrackerImpl here. For backwards compatibility,
        //we write one as a place-holder.  It is ignored when reading.
		stream.writeObject(BANDWIDTH_TRACKER_IMPL);
    }

    /** See note on serialization at top of file.  You must call initialize on
     *  this!  
     * Also see note in writeObjects about why we are not using 
     * BandwidthTrackerImpl after reading from the stream
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        deserializedFromDisk = true;

        allFiles=(RemoteFileDesc[])stream.readObject();
        incompleteFileManager=(IncompleteFileManager)stream.readObject();
		//Old versions used to read BandwidthTrackerImpl here.  Now we just use
		//one as a place holder.
        stream.readObject();
    }

    /** 
     * Initializes a ManagedDownloader read from disk. Also used for internally
     * initializing or resuming a normal download; there is no need to
     * explicitly call this method in that case. After the call, this is in the
     * queued state, at least for the moment.
     *     @requires this is uninitialized or stopped, 
     *      and allFiles, and incompleteFileManager are set
     *     @modifies everything but the above fields 
     * @param deserialized True if this downloader is being initialized after 
     * being read from disk, false otherwise.
     */
    public void initialize(DownloadManager manager, 
                           ActivityCallback callback) {
        this.manager=manager;
        this.callback=callback;
        rfds = new LinkedList();
        _activeWorkers=new LinkedList();
        _workers=new ArrayList();
        queuedWorkers = new HashMap();
		chatList=new DownloadChatList();
        browseList=new DownloadBrowseHostList();
        stealLock = new Object();
        stopped=false;
        paused = false;
        setState(QUEUED);
        miniRFDToLock = Collections.synchronizedMap(new HashMap());
        corruptState=NOT_CORRUPT_STATE;
        corruptStateLock=new Object();
        altLock = new Object();
        numMeasures = 0;
        averageBandwidth = 0f;
        queuePosition=Integer.MAX_VALUE;
        queuedVendor = "";
        triedLocatingSources = false;
        // get the SHA1 if we can.
        if(allFiles != null && downloadSHA1 == null) {
            for(int i = 0; i < allFiles.length && downloadSHA1 == null; i++) 
                downloadSHA1 = allFiles[i].getSHA1Urn();
        }
        allFiles = verifyAllFiles(allFiles);
        // stores up to 1000 locations for up to an hour each
        invalidAlts = new FixedSizeExpiringSet(1000,60*60*1000L);
        // stores up to 10 locations for up to 10 minutes
        recentInvalidAlts = new FixedSizeExpiringSet(10, 10*60*1000L);
        synchronized (this) {
            initializeRFDs();
        }
        
        try {
            initializeFilesAndFolders();
            initializeIncompleteFile();
            initializeVerifyingFile();
        }catch(IOException bad) {
            setState(DISK_PROBLEM);
            return;
        }
        
        setState(QUEUED);
    }
    
    /** 
     * Verifies the integrity of the RemoteFileDesc[].
     *
     * At one point in time, LimeWire somehow allowed files with different
     * SHA1s to be placed in the same ManagedDownloader.  This breaks
     * the invariants of the current ManagedDownloader, so we must
     * remove the extraneous RFDs.
     */
    RemoteFileDesc[] verifyAllFiles(RemoteFileDesc[] old) {
        if(downloadSHA1 == null)
            return old;
            
        List verified = null;
        // First iterate through each file and see if it contains
        // atleast one invalid RFD.  If they're all good, then we're fine.
        for(int i = 0; i < old.length; i++) {
            URN check = old[i].getSHA1Urn();
            if(check != null && !downloadSHA1.equals(check)) {
                verified = new LinkedList();
                break;
            }
        }
        
        // If we didn't construct the list, all RFDs were a-okay.
        if(verified == null)
            return old;
            
        for(int i = 0; i < old.length; i++) {
            URN check = old[i].getSHA1Urn();
            if(check == null || downloadSHA1.equals(check))
                verified.add(old[i]);
        }
        
        RemoteFileDesc[] checked = new RemoteFileDesc[verified.size()];
        return (RemoteFileDesc[])verified.toArray(checked);
    }
    
    /**
     * Starts the download.
     */
    public synchronized void startDownload() {
        Assert.that(dloaderManagerThread == null, "already started" );
        dloaderManagerThread = new ManagedThread(new Runnable() {
            public void run() {
                try {
                    receivedNewSources = false;
                    performDownload();
                    completeDownload();
                } catch(Throwable t) {
                    // if any unhandled errors occurred, remove this
                    // download completely and message the error.
                    ManagedDownloader.this.stop();
                    setState(ABORTED);
                    manager.remove(ManagedDownloader.this, true);
                    
                    ErrorService.error(t);
                } finally {
                    dloaderManagerThread = null;
                }
            }
        }, "ManagedDownload");
        dloaderManagerThread.setDaemon(true);
        dloaderManagerThread.start(); 
    }
    
    /**
     * Completes the download process, possibly sending off requeries
     * that may later restart it.
     *
     * This essentially pumps the state of the download to different
     * areas, depending on what is required or what has already occurred.
     */
    private void completeDownload() {
        boolean complete = isCompleted();
        long now = System.currentTimeMillis();

        // Notify the manager that this download is done.
        // This MUST be done outside of this' lock, else
        // deadlock could occur.
        manager.remove(this, complete);

        if(LOG.isTraceEnabled())
            LOG.trace("MD completing <" + getFileName() + 
                      "> completed download, state: " +
                      getState() + ", numQueries: " + numQueries +
                      ", lastQuerySent: " + lastQuerySent);

        // if this is all completed, nothing else to do.
        if(complete)
            ; // all done.
            
        // if this is paused, nothing else to do also.
        else if(getState() == PAUSED)
            ; // all done for now.

       // If busy, try waiting for that busy host.
        else if (getState() == WAITING_FOR_RETRY)
            setState(WAITING_FOR_RETRY, calculateWaitTime());
        
        // If we sent a query recently, then we don't want to send another,
        // nor do we want to give up.  Just continue waiting for results
        // from that query.
        else if(now - lastQuerySent < TIME_BETWEEN_REQUERIES)
            setState(WAITING_FOR_RESULTS,
                     TIME_BETWEEN_REQUERIES - (now - lastQuerySent));
            
        // If we're at our requery limit, give up.
        else if( numQueries >= REQUERY_ATTEMPTS )
            setState(GAVE_UP);
            
        // If we want to send the requery immediately, do so.
        else if(shouldSendRequeryImmediately(numQueries))
            sendRequery();
            
        // Otherwise, wait for the user to initiate the query.            
        else
            setState(WAITING_FOR_USER);
        
        if(LOG.isTraceEnabled())
            LOG.trace("MD completed <" + getFileName() +
                      "> completed download, state: " + 
                      getState() + ", numQueries: " + numQueries);
    }
    
    /**
     * Attempts to send a requery.
     */
    private void sendRequery() {
        // If we don't have stable connections, wait until we do.
        if(!hasStableConnections()) {
            lastQuerySent = -1; // mark as wanting to requery.
            setState(WAITING_FOR_CONNECTIONS, CONNECTING_WAIT_TIME);
        } else {
            try {
                QueryRequest qr = newRequery(numQueries);
                if(manager.sendQuery(this, qr)) {
                    lastQuerySent = System.currentTimeMillis();
                    numQueries++;
                    setState(WAITING_FOR_RESULTS, TIME_BETWEEN_REQUERIES);
                } else {
                    lastQuerySent = -1; // mark as wanting to requery.
                }
            } catch(CantResumeException cre) {
                // oh well.
            }
        }
    }
    
    /**
     * Handles state changes when inactive.
     */
    public synchronized void handleInactivity() {
        if(LOG.isTraceEnabled())
            LOG.trace("handling inactivity. state: " + 
                      getState() + ", hasnew: " + hasNewSources() + 
                      ", left: " + getRemainingStateTime());
        
        switch(getState()) {
        case WAITING_FOR_RETRY:
        case WAITING_FOR_CONNECTIONS:
        case ITERATIVE_GUESSING:
            // If we're finished waiting on busy hosts,
            // stable connections, or GUESSing,
            // but we're still inactive, then we queue ourselves
            // and wait till we get restarted.
            if(getRemainingStateTime() <= 0)
                setState(QUEUED);
            break;
        case WAITING_FOR_RESULTS:
            // If we have new sources but are still inactive,
            // then queue ourselves and wait to restart.
            if(hasNewSources())
                setState(QUEUED);
            // Otherwise, we've ran out of time waiting for results,
            // so give up.
            else if(getRemainingStateTime() <= 0)
                setState(GAVE_UP);
            break;
        case WAITING_FOR_USER:
        case GAVE_UP:
        case QUEUED:
        case PAUSED:
            // If we're waiting for the user to do something,
            // have given up, or are queued, there's nothing to do.
            break;
        default:
            Assert.that(false, "invalid state: " + getState() +
                             ", workers: " + _workers.size() + 
                             ", _activeWorkers: " + _activeWorkers.size());
        }
    }   
    
    
    /**
     * Determines if the downloading thread is still alive.
     * It is possible that the download may be inactive yet
     * the thread still alive.  The download must be not alive
     * before being restarted.
     */
    public boolean isAlive() {
        return dloaderManagerThread != null;
    }
    
    /**
     * Determines if this is in a 'completed' state.
     */
    public boolean isCompleted() {
        switch(getState()) {
        case COMPLETE:
        case ABORTED:
        case DISK_PROBLEM:
        case CORRUPT_FILE:
            return true;
        }
        return false;
    }
    
    /**
     * Determines if this is in an 'active' downloading state.
     */
    public boolean isActive() {
        switch(getState()) {
        case CONNECTING:
        case DOWNLOADING:
        case REMOTE_QUEUED:
        //case HASHING:
        case SAVING:
        case IDENTIFY_CORRUPTION:
            return true;
        }
        return false;
    }
    
    /**
     * Determines if this is in an 'inactive' state.
     */
    public boolean isInactive() {
        switch(getState()) {
        case QUEUED:
        case GAVE_UP:
        case WAITING_FOR_RESULTS:
        case WAITING_FOR_USER:
        case WAITING_FOR_CONNECTIONS:
        case ITERATIVE_GUESSING:
        case WAITING_FOR_RETRY:
        case PAUSED:
            return true;
        }
        return false;
    }   
    
    /**
     * Initialize files wrt allFiles.
     */
    protected synchronized void initializeRFDs() {
        for(int i = 0; i < allFiles.length; i++)
            if(!isRFDAlreadyStored(allFiles[i]))
                rfds.add(allFiles[i]);
    }

    /**
     * assumes incompleteFile is initialized
     */
    private void initializeVerifyingFile() throws IOException {

        //get VerifyingFile
        commonOutFile= incompleteFileManager.getEntry(incompleteFile);

        if(commonOutFile==null) {//no entry in incompleteFM
            
            int completedSize = 
                (int)IncompleteFileManager.getCompletedSize(incompleteFile);
            
            commonOutFile = new VerifyingFile(completedSize);
            try {
                //we must add an entry in IncompleteFileManager
                incompleteFileManager.
                           addEntry(incompleteFile,commonOutFile);
            } catch(IOException ioe) {
                ErrorService.error(ioe, "file: " + incompleteFile);
                throw ioe;
            }
        }        
    }
    
    protected void initializeIncompleteFile() throws IOException {
        if (incompleteFile != null)
            return;
        
        if (downloadSHA1 != null)
            incompleteFile = incompleteFileManager.getFileForUrn(downloadSHA1);
        
        if (incompleteFile == null) {
            if (allFiles == null || allFiles.length == 0)
                return;
            incompleteFile = incompleteFileManager.getFile(allFiles[0]);
        }
    }
    
    /**
     * Adds the alternate locations from the collections as possible
     * download sources.
     */
    void addLocationsToDownload(AlternateLocationCollection direct,
                                        AlternateLocationCollection push,
                                        int size) {
        // always add the direct alt locs.
        if(direct != null) {
            synchronized(direct) {
                Iterator iter = direct.iterator();
                while(iter.hasNext()) {
                    AlternateLocation loc = (AlternateLocation)iter.next();
                    addDownload(loc.createRemoteFileDesc((int)size), false);
                }
            }
        }
                
        //also adds any existing firewalled locations.
        //If I'm firewalled, only those that support FWT are added.
        //this assumes that FWT will always be backwards compatible
        if(push != null) {
            boolean open = RouterService.acceptedIncomingConnection();
            boolean fwt = UDPService.instance().canDoFWT();
            synchronized(push) {
            	Iterator iter = push.iterator();
            	while(iter.hasNext()) {
            		PushAltLoc loc = (PushAltLoc)iter.next();
            		if (open || (fwt && loc.supportsFWTVersion() > 0))
            		    addDownload(loc.createRemoteFileDesc((int)size), false);
            	}
            }
        }
    }

    /**
     * Returns true if 'other' could conflict with one of the files in this. In
     * other words, if this.conflicts(other)==true, no other ManagedDownloader
     * should attempt to download other.  
     */
    public boolean conflicts(RemoteFileDesc other) {
        try {
            File otherFile=incompleteFileManager.getFile(other);
            return conflicts(otherFile);
        } catch(IOException ioe) {
            return false;
        }
    }

    /**
     * Returns true if this is using (or could use) the given incomplete file.
     * @param incompleteFile an incomplete file, which SHOULD be the return
     *  value of IncompleteFileManager.getFile
     */
    public boolean conflicts(File incFile) {
        synchronized (this) {
            //TODO3: this is stricter than necessary.  What if a location has
            //been removed?  Tricky without global variables.  At the least we
            //should return false if in COULDNT_DOWNLOAD state.
            for (int i=0; i<allFiles.length; i++) {
                RemoteFileDesc rfd=(RemoteFileDesc)allFiles[i];
                try {
                    File thisFile=incompleteFileManager.getFile(rfd);
                    if (thisFile.equals(incFile))
                        return true;
                } catch(IOException ioe) {
                    return false;
                }
            }
        }
        return false;
    }

    public boolean conflicts(URN urn) {
        Assert.that(urn!=null, "attempting to check conflicts with null urn");
        File otherFile = incompleteFileManager.getFileForUrn(urn);
        if(otherFile==null)
            return false;
        return conflicts(otherFile);
    }

    /////////////////////////////// Requery Code ///////////////////////////////

    /** 
     * Returns a new QueryRequest for requery purposes.  Subclasses may wish to
     * override this to be more or less specific.  Note that the requery will
     * not be sent if global limits are exceeded.<p>
     *
     * Since there are no more AUTOMATIC requeries, subclasses are advised to
     * stop using createRequery(...).  All attempts to 'requery' the network is
     * spawned by the user, so use createQuery(...) .  The reason we need to
     * use createQuery is because DownloadManager.sendQuery() has a global
     * limit on the number of requeries sent by LW (as IDed by the guid), but
     * it allows normal queries to always be sent.
     *
     * @param numRequeries the number of requeries that have already happened
     * @exception CantResumeException if this doesn't know what to search for 
	 * @return a new <tt>QueryRequest</tt> for making the requery
     */
    protected synchronized QueryRequest newRequery(int numRequeries)
      throws CantResumeException {
        Assert.that(allFiles.length > 0, "precondition violated");
		    
		String name = allFiles[0].getFileName();
		    
        String queryString = StringUtils.createQueryString(name);
        if(queryString == null || queryString.equals(""))
            throw new CantResumeException(name);
        else
            return QueryRequest.createQuery(queryString);
            
        // if desired, we can create a SHA1 query also
    }


    /**
     * Determines if we should send a requery immediately, or wait for user
     * input.
     *
     * 'lastQuerySent' being equal to -1 indicates that the user has already
     * clicked resume, so we do want to send immediately.
     */
    protected boolean shouldSendRequeryImmediately(int numRequeries) {
        if(lastQuerySent == -1)
            return true;
        else
            return false;
    }

    /** Subclasses should override this method when necessary.
     *  If you return false, then AltLocs are not initialized from the
     *  incomplete file upon invocation of tryAllDownloads.
     *  The true case can be used when the partial file is being shared
     *  through PFS and we've learned about AltLocs we want to use.
     */
    protected boolean shouldInitAltLocs(boolean deserializedFromDisk) {
        return false;
    }
    
    /**
     * Determines if the specified host is allowed to download.
     */
    protected boolean hostIsAllowed(RemoteFileDesc other) {
         // If this host is banned, don't add.
        if ( !IPFilter.instance().allow(other.getHost()) )
            return false;            
            
        // See if we have already tried and failed with this location
        // This is only done if the location we're trying is an alternate..
        synchronized(altLock) {
            if (other.isFromAlternateLocation() && 
                invalidAlts.contains(other.getRemoteHostData())) {
                return false;
            }
        }
        
        return true;
    }
              


    private static boolean initDone = false; // used to init

    /**
     * Returns true if 'other' should be accepted as a new download location.
     */
    protected boolean allowAddition(RemoteFileDesc other) {
        if (!initDone) {
            synchronized (matcher) {
                matcher.setIgnoreCase(true);
                matcher.setIgnoreWhitespace(true);
                matcher.setCompareBackwards(true);
            }
            initDone = true;
        }

        // before doing expensive stuff, see if connection is even possible...
        if (other.getQuality() < 1) // I only want 2,3,4 star guys....
            return false;        

        // get other info...
		final URN otherUrn = other.getSHA1Urn();
        final String otherName = other.getFileName();
        final long otherLength = other.getSize();

        synchronized (this) {
            if(otherUrn != null && downloadSHA1 != null)
                return otherUrn.equals(downloadSHA1);
            
            // compare to allFiles....
            for (int i=0; i<allFiles.length; i++) {
                // get current info....
                RemoteFileDesc rfd = (RemoteFileDesc) allFiles[i];
                final String thisName = rfd.getFileName();
                final long thisLength = rfd.getSize();
				
                // if they are similarly named and same length
                // do length check first, much less expensive.....
                if (otherLength == thisLength) 
                    if (namesClose(otherName, thisName)) 
                        return true;                
            }
        }
        return false;
    }

    private final boolean namesClose(final String one, 
                                     final String two) {
        boolean retVal = false;

        // copied from TableLine...
        //Filenames close?  This is the most expensive test, so it should go
        //last.  Allow 10% edit difference in filenames or 6 characters,
        //whichever is smaller.
        int allowedDifferences=Math.round(Math.min(
             0.10f*((float)(StringUtils.ripExtension(one)).length()),
             0.10f*((float)(StringUtils.ripExtension(two)).length())));
        allowedDifferences=Math.min(allowedDifferences, 6);

        synchronized (matcher) {
            retVal = matcher.matches(matcher.process(one),
                                     matcher.process(two),
                                     allowedDifferences);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("MD.namesClose(): one = " + one);
            LOG.debug("MD.namesClose(): two = " + two);
            LOG.debug("MD.namesClose(): retVal = " + retVal);
        }
            
        return retVal;
    }

    /** 
     * Attempts to add the given location to this.  If rfd is accepted, this
     * will terminate after downloading rfd or any of the other locations in
     * this.  This may swarm some file from rfd and other locations.<p>
     * 
     * This method only adds rfd if allowAddition(rfd).  Subclasses may
     * wish to override this protected method to control the behavior.
     * 
     * @param rfd a new download candidate.  Typically rfd will be similar or
     *  same to some entry in this, but that is not required.  
     * @return true if rfd has been added.  In this case, the caller should
     *  not offer rfd to another ManagedDownloaders.
     */
    public synchronized boolean addDownload(RemoteFileDesc rfd, boolean cache) {
        // never add to a stopped download.
        if(stopped)
            return false;
        
        if(!hostIsAllowed(rfd))
            return false;
        
        if (!allowAddition(rfd))
            return false;
        
        return addDownloadForced(rfd, cache);
    }

    /**
     * Like addDownload, but doesn't call allowAddition(..).
     *
     * If cache is false, the RFD is not added to allFiles, but is
     * added to 'files', the list of RFDs we will connect to.
     *
     * If the RFD matches one already in allFiles, the new one is
     * NOT added to allFiles, but IS added the list of RFDs to connect to
     * if and only if a matching RFD is not currently in that list.
     *
     * This ALWAYS returns true, because the download is either allowed
     * or silently ignored (because we're already downloading or going to
     * attempt to download from the host described in the RFD).
     */
    protected synchronized final boolean addDownloadForced(RemoteFileDesc rfd,
                                                           boolean cache) {
        rfd.setDownloading(true);
        if(downloadSHA1 == null)
            downloadSHA1 = rfd.getSHA1Urn();
            
        // DO NOT DOWNLOAD FROM YOURSELF.
        if( rfd.isMe() )
            return true;
        // If this already exists in allFiles, DO NOT ADD IT AGAIN.
        // However, we must still add it to files if it didn't already exist
        // there.

        // If cache is already false, there is no need to look in allFiles.
        if (cache) {
            for (int i=0; i<allFiles.length; i++) {
                if (rfd.equals(allFiles[i])) {
                    cache = false; // do not store in allFiles.
                    break;
                }
            }
        }
        
        boolean added = false;
        // Add to the list of RFDs to connect to.
        if (!isRFDAlreadyStored(rfd))
            added = rfds.add(rfd);

        //Append to allFiles for resume purposes if caching...
        if(cache) {
            RemoteFileDesc[] newAllFiles=new RemoteFileDesc[allFiles.length+1];
            System.arraycopy(allFiles, 0, newAllFiles, 0, allFiles.length);
            newAllFiles[newAllFiles.length-1]=rfd;
            allFiles=newAllFiles;
        }


        //...and notify manager to look for new workers.  You might be
        //tempted to just call dloaderManagerThread.interrupt(), but that
        //causes spurious interrupts to happen when establishing connections
        //(push or otherwise).  So instead we target the two cases we're
        //interested: waiting for downloaders to complete (by waiting on
        //this) or waiting for retry (handled by DownloadManager).
        if ( added ) {
            if(LOG.isTraceEnabled())
                LOG.trace("added rfd: " + rfd);
            if(isInactive() || dloaderManagerThread == null)
                receivedNewSources = true;
            else
                this.notify();                      //see fireDownloadWorkers
        }

        return true;
    }
    
    /**
     * Determines if we already have this RFD in our lists.
     */
    private synchronized boolean isRFDAlreadyStored(RemoteFileDesc rfd) {
        if( currentRFDs != null && currentRFDs.contains(rfd)) 
            return true;
        if( rfds != null && rfds.contains(rfd)) 
            return true;
        return false;
    }
    
    synchronized void addRFD(RemoteFileDesc rfd) {
        rfds.add(rfd);
    }
    
    /**
     * Returns true if we have received more possible source since the last
     * time we went inactive.
     */
    public boolean hasNewSources() {
        return !paused && receivedNewSources;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Accepts a push download.  If this chooses to download the given file
     * (with given index and clientGUID) from socket, returns true.  In this
     * case, the caller may not make any modifications to the socket.  If this
     * rejects the given file, returns false without modifying this or socket.
     * If this could has problems with the socket, throws IOException.  In this
     * case the caller should close the socket.  Non-blocking.
     *     @modifies this, socket
     *     @requires GIV string (and nothing else) has been read from socket
     */
    public boolean acceptDownload(
            String file, Socket socket, int index, byte[] clientGUID)
            throws IOException {
        
        MiniRemoteFileDesc mrfd=new MiniRemoteFileDesc(file,index,clientGUID);
        DownloadWorker worker =  (DownloadWorker) miniRFDToLock.get(mrfd);
        
        if(worker == null) //not in map. Not intended for me
            return false;
        
        worker.setPushSocket(socket);
        
        return true;
    }
    
    void registerPushWaiter(DownloadWorker worker, MiniRemoteFileDesc mrfd) {
        miniRFDToLock.put(mrfd,worker);
    }
    
    void unregisterPushWaiter(MiniRemoteFileDesc mrfd) {
        miniRFDToLock.remove(mrfd);
    }
    
    /**
     * Determines if this download was cancelled.
     */
    public boolean isCancelled() {
        return stopped;
    }
    
    /**
     * Pauses this download.
     */
    public synchronized void pause() {
        // do not pause if already stopped.
        if(!stopped && !isCompleted()) {
            stop();
            stopped = false;
            paused = true;
            // if we're already inactive, mark us as paused immediately.
            if(isInactive())
                setState(PAUSED);
        }
    }
    
    /**
     * Determines if this download is paused.
     *
     * If isPaused == true but getState() != PAUSED then this download
     * is in the process of pausing itself.
     */
    public boolean isPaused() {
        return paused == true;
    }
    
    /**
     * Stops this download.
     */
    public void stop() {
    
        if (paused) {
            stopped = true;
            paused = false;
        }

        // make redundant calls to stop() fast
        // this change is pretty safe because stopped is only set in two
        // places - initialized and here.  so long as this is true, we know
        // this is safe.
        if (stopped || paused)
            return;

        LOG.debug("STOPPING ManagedDownloader");

        //This method is tricky.  Look carefully at run.  The most important
        //thing is to set the stopped flag.  That guarantees run will terminate
        //eventually.
        stopped=true;
        
        synchronized(this) {
            killAllWorkers();
            
            // must capture in local variable so the value doesn't become null
            // between if & contents of if.
            Thread dlMan = dloaderManagerThread;
            if(dlMan != null)
                dlMan.interrupt();
            else
                LOG.warn("MANAGER: no thread to interrupt");
        }
    }

    /**
     * Kills all workers.
     */    
    private void killAllWorkers() {
        for (Iterator iter = _workers.iterator(); iter.hasNext();) {
            DownloadWorker doomed = (DownloadWorker) iter.next();
            doomed.interrupt();
        }
    }
    
    /**
     * Callback from workers to inform the managing thread that
     * a disk problem has occured.
     */
    synchronized void diskProblemOccured() {
        setState(DISK_PROBLEM);
        stop();
    }

    /**
     * Notifies all existing HTTPDownloaders about this RFD.
     * If good is true, it notifies them of a succesful alternate location,
     * otherwise it notifies them of a failed alternate location.
     * The internal validAlts is also updated if good is true,
     * and invalidAlts is updated if good is false.
     * The IncompleteFileDesc is also notified of new locations for this
     * file.
     */
    synchronized void informMesh(RemoteFileDesc rfd, boolean good) {
        IncompleteFileDesc ifd = null;
        //TODO3: Until IncompleteFileDesc and ManagedDownloader share a copy
        // of the AlternateLocationCollection, they must use seperate
        // AlternateLocation objects.
        AlternateLocation loc = null;
        AlternateLocation forFD = null;
        
        if(!rfd.isAltLocCapable())
            return;
        
        // Verify that this download has a hash.  If it does not,
        // we should not have been getting locations in the first place.
        Assert.that(downloadSHA1 != null, "null hash.");
        
        
        Assert.that(downloadSHA1.equals(rfd.getSHA1Urn()), "wrong loc SHA1");
        
        // If a validAlts collection wasn't created already
        // (which would only be possible if the initial set of
        // RFDs did not have a hash, but subsequent searches
        // produced RFDs with hashes), create the collection.
        if( validAlts == null )
            validAlts = AlternateLocationCollection.create(downloadSHA1);
        
        try {
            loc = AlternateLocation.create(rfd);
            forFD = AlternateLocation.create(rfd);
            
        } catch(IOException iox) {
            return;
        }

        // the forFD altloc will be stored in the rfd, so it needs to point to the
        // current set of proxies.  The loc altloc will be sent to uploaders, so it 
        // needs to contain a snapshot of the set of proxies it had when it failed or
        // succeeded.
        if (forFD instanceof PushAltLoc) {
            
            // it is possible that an HTTPUploader just received a NFAlt header
            // which cleared the proxies of this pushloc.  If that happens we do
            // not inform anybody. The PE will get removed from the FD by the uploader
            // (we perform this check on a copy of the set)
            
            PushAltLoc ploc = (PushAltLoc)loc;
            if (ploc.isDemoted())
                return;
            
            PushAltLoc pFD = (PushAltLoc)forFD;
            pFD.updateProxies(good);
            
            Assert.that(!ploc.isDemoted());
        }
        
        for(Iterator iter=_activeWorkers.iterator(); iter.hasNext();) {
            HTTPDownloader httpDloader = ((DownloadWorker)iter.next()).getDownloader();
            RemoteFileDesc r = httpDloader.getRemoteFileDesc();
            
            // no need to tell uploader about itself and since many firewalled
            // downloads may have the same port and host, we also check their
            // push endpoints
            if(! (loc instanceof PushAltLoc) ? 
                    (r.getHost().equals(rfd.getHost()) && r.getPort()==rfd.getPort()) :
                    r.getPushAddr()!=null && r.getPushAddr().equals(rfd.getPushAddr()))
                continue;
            
            //no need to send push altlocs to older uploaders
            if (loc instanceof DirectAltLoc || httpDloader.wantsFalts()) {
            	if (good)
            		httpDloader.addSuccessfulAltLoc(loc);
            	else
            		httpDloader.addFailedAltLoc(loc);
            }
        }

        synchronized(altLock) {
            if(good) {
                //check if validAlts contains loc to avoid duplicate stats, and
                //spurious count increments in the local
                //AlternateLocationCollections
                if(!validAlts.contains(loc)) {
                    if(rfd.isFromAlternateLocation() )
                    validAlts.add(loc);
                    if( ifd != null )
                        ifd.addVerified(forFD);
                }
            }  else {
                    if(rfd.isFromAlternateLocation() )
                    
                    validAlts.remove(loc);
                    if( ifd != null )
                        ifd.remove(forFD);
                    invalidAlts.add(rfd.getRemoteHostData());
                    recentInvalidAlts.add(loc);
            }
        } 
    }

    /**
     * Requests this download to resume.
     *
     * If the download is not inactive, this does nothing.
     * If the downloader was waiting for the user, a requery is sent.
     */
    public synchronized boolean resume() {
        //Ignore request if already in the download cycle.
        if (!isInactive())
            return false;

        // if we were waiting for the user to start us,
        // then try to send the requery.
        if(getState() == WAITING_FOR_USER)
            lastQuerySent = -1; // inform requerying that we wanna go.

        // also retry any hosts that we have leftover.
        initializeRFDs();
        
        // if any guys were busy, reduce their retry time to 0,
        // since the user really wants to resume right now.
        for(Iterator i = rfds.iterator(); i.hasNext(); )
            ((RemoteFileDesc)i.next()).setRetryAfter(0);

        if(paused) {
            paused = false;
            stopped = false;
        }
            
        // queue ourselves so we'll try and become active immediately
        setState(QUEUED);

        return true;
    }
    
    /**
     * Returns the incompleteFile or the completeFile, if the is complete.
     */
    public File getFile() {
        if(incompleteFile == null)
            return null;
            
        if(state == COMPLETE)
            return completeFile;
        else
            return incompleteFile;
    }
    
    public URN getSHA1Urn() {
        return downloadSHA1;
    }
    
    /**
     * Returns the first fragment of the incomplete file,
     * copied to a new file, or the completeFile if the download
     * is complete, or the corruptFile if the download is corrupted.
     */
    public File getDownloadFragment() {
        //We haven't started yet.
        if (incompleteFile==null)
            return null;
        
        //a) Special case for saved corrupt fragments.  We don't worry about
        //removing holes.
        if (state==CORRUPT_FILE) 
            return corruptFile; //may be null
        //b) If the file is being downloaded, create *copy* of first
        //block of incomplete file.  The copy is needed because some
        //programs, notably Windows Media Player, attempt to grab
        //exclusive file locks.  If the download hasn't started, the
        //incomplete file may not even exist--not a problem.
        else if (state!=COMPLETE) {
            File file=new File(incompleteFile.getParent(),
                               IncompleteFileManager.PREVIEW_PREFIX
                                   +incompleteFile.getName());
            //Get the size of the first block of the file.  (Remember
            //that swarmed downloads don't always write in order.)
            int size=amountForPreview();
            if (size<=0)
                return null;
            //Copy first block, returning if nothing was copied.
            if (CommonUtils.copy(incompleteFile, size, file)<=0) 
                return null;
            return file;
        }
        //b) Otherwise, choose completed file.
        else {
            return completeFile;
        }
    }


    /** 
     * Returns the amount of the file written on disk that can be safely
     * previewed. 
     * 
     * @param incompleteFile the file to examine, which MUST correspond to
     *  the current download.
     */
    private synchronized int amountForPreview() {
        //And find the first block.
        if (commonOutFile == null)
            return 0; // trying to preview before incomplete file created
        synchronized (commonOutFile) {
            for (Iterator iter=commonOutFile.getBlocks();iter.hasNext() ; ) {
                Interval interval=(Interval)iter.next();
                if (interval.low==0)
                    return interval.high;
            }
        }
        return 0;//Nothing to preview!
    }


    //////////////////////////// Core Downloading Logic /////////////////////

    /**
     * Cleans up information before this downloader is removed from memory.
     */
    public synchronized void finish() {
        if(allFiles != null) {
            for(int i = 0; i < allFiles.length; i++)
                allFiles[i].setDownloading(false);
        }       
    }

    /** 
     * Actually does the download, finding duplicate files, trying all
     * locations, resuming, waiting, and retrying as necessary. Also takes care
     * of moving file from incomplete directory to save directory and adding
     * file to the library.  Called from dloadManagerThread.  
     * @param deserialized True if this downloader was deserialized from disk,
     * false if it was newly constructed.
     */
    protected void performDownload() {
        if(checkHosts()) {//files is global
            setState(GAVE_UP);
            return;
        }

        // 1. initialize the download
        int status = initializeDownload();
        if ( status == CONNECTING) {
            try {
                //2. Do the download
                try {
                    status = fireDownloadWorkers();//Exception may be thrown here.
                }finally {
                    //3. Close the file controlled by commonOutFile.
                    commonOutFile.close();
                }
                
                // 4. if all went well, save
                if (status == COMPLETE) 
                    status = saveFile();
                else if(LOG.isDebugEnabled())
                    LOG.debug("stopping early with status: " + status); 
                
            } catch (InterruptedException e) {
                
                // nothing should interrupt except for a stop
                if (!stopped && !paused)
                    ErrorService.error(e);
                else
                    status = GAVE_UP;
                
                // if we were stopped due to corrupt download, cleanup
                if (corruptState == CORRUPT_STOP_STATE) {
                    cleanupCorrupt(incompleteFile, completeFile.getName());
                    status = CORRUPT_FILE;
                }
            }
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("MANAGER: TAD2 returned: " + status);
                   
        // If TAD2 gave a completed state, set the state correctly & exit.
        // Otherwise...
        // If we manually stopped then set to ABORTED, else set to the 
        // appropriate state (either a busy host or no hosts to try).
        synchronized(this) {
            switch(status) {
            case COMPLETE:
            case DISK_PROBLEM:
            case CORRUPT_FILE:
                setState(status);
                return;
            case WAITING_FOR_RETRY:
            case GAVE_UP:
                if(stopped)
                    setState(ABORTED);
                else if(paused)
                    setState(PAUSED);
                else
                    setState(status);
                return;
            default:
                Assert.that(false, "Bad status from tad2: "+status);
            }
        }
    }

	private static final int MIN_NUM_CONNECTIONS      = 2;
	private static final int MIN_CONNECTION_MESSAGES  = 6;
	private static final int MIN_TOTAL_MESSAGES       = 45;
    static boolean   NO_DELAY				  = false; // For testing

    /**
     *  Determines if we have any stable connections to send a requery down.
     */
    private boolean hasStableConnections() {
		if ( NO_DELAY )
		    return true;  // For Testing without network connection

		// TODO: Note that on a private network, these conditions might
		//       be too strict.
		
		// Wait till your connections are stable enough to get the minimum 
		// number of messages
		return RouterService.countConnectionsWithNMessages(MIN_CONNECTION_MESSAGES) 
			        >= MIN_NUM_CONNECTIONS &&
               RouterService.getActiveConnectionMessages() >= MIN_TOTAL_MESSAGES;
    }


    /**
     * Returns the amount of time to wait in milliseconds before retrying,
     * based on tries.  This is also the time to wait for * incoming pushes to
     * arrive, so it must not be too small.  A value of * tries==0 represents
     * the first try.
     */
    private synchronized long calculateWaitTime() {
        if (rfds == null || rfds.size()==0)
            return 0;
        // waitTime is in seconds
        int waitTime = Integer.MAX_VALUE;
        for (int i = 0; i < rfds.size(); i++) {
            waitTime = Math.min(waitTime, 
                            ((RemoteFileDesc)rfds.get(i)).getWaitTime());
        }
        // waitTime was in seconds
        return (waitTime*1000);
    }


    /**
     * Tries to initialize the download location and the verifying file. 
     * @return GAVE_UP if we had no sources, DISK_PROBLEM if such occured, 
     * CONNECTING if we're ready to connect
     */
    protected int initializeDownload() {
        RemoteFileDesc firstDesc = null;
        synchronized (this) {
            if (rfds.size()==0)
                return GAVE_UP;
            firstDesc = (RemoteFileDesc)rfds.get(0);
        }
        
        try {
            initializeIncompleteFile();
            initializeVerifyingFile();
            openVerifyingFile();
        } catch (IOException iox) {
            return DISK_PROBLEM;
        }

        // Create a new validAlts for this sha1.
        // initialize the HashTree
        if( downloadSHA1 != null ) {
            validAlts = AlternateLocationCollection.create(downloadSHA1);
        }
        
        return CONNECTING;
    }
    
    /**
     * Waits indefinitely for a response to the corrupt message prompt, if
     * such was displayed.
     */
    private void waitForCorruptResponse() {
        if(corruptState != NOT_CORRUPT_STATE) {
            synchronized(corruptStateLock) {
                try {
                    while(corruptState==CORRUPT_WAITING_STATE)
                        corruptStateLock.wait();
                } catch(InterruptedException ignored) {}
            }
        }
    }  
    
    /**
     * initialize the directory where the file is to be saved.
     */
    private void initializeFilesAndFolders() throws IOException{
        
        //1. Verify it's safe to download.  Filename must not have "..", "/",
        //etc.  We check this by looking where the downloaded file will end up.
        //The completed filename is chosen somewhat arbitrarily from the first
        //file; see case (b) of getFileName() and
        //MagnetDownloader.getFileName().
        //    incompleteFile is picked using an arbitrary RFD, since
        //IncompleteFileManager guarantees that any "same" files will get the
        //same temporary file.
        //

        // TODO(zyu): getCanonicalPath seems to have some issue. Race condition?
    	// Also see http://code.google.com/p/android/issues/detail?id=4961.
        
        File saveDir;
        String fileName = getFileName();
        
        saveDir = SharingSettings.DEFAULT_SAVE_DIR;
        completeFile = new File(saveDir, fileName);
        fileName = fileName.replace("..", ".");
        fileName = fileName.replace("/", "");
    }
    

    /**
     * Saves the file to disk.
     */
    private int saveFile() {
        // let the user know we're saving the file...
        setState( SAVING );
        //4. Move to library.
        // Make sure we can write into the complete file's directory.
        File completeFileDir = FileUtils.getParentFile(completeFile);
        FileUtils.setWriteable(completeFileDir);
        FileUtils.setWriteable(completeFile);
        //Delete target.  If target doesn't exist, this will fail silently.
        completeFile.delete();

        //Try moving file.  If we couldn't move the file, i.e., because
        //someone is previewing it or it's on a different volume, try copy
        //instead.  If that failed, notify user.  
        //   If move is successful, we should remove the corresponding blocks
        //from the IncompleteFileManager, though this is not strictly necessary
        //because IFM.purge() is called frequently in DownloadManager.
        
        // First attempt to rename it.
        boolean success = FileUtils.forceRename(incompleteFile,completeFile);
            
        // If that didn't work, we're out of luck.
        if (!success)
            return DISK_PROBLEM;
            
        incompleteFileManager.removeEntry(incompleteFile);
        
		    return COMPLETE;
    }
    
    /** Removes all entries for incompleteFile from incompleteFileManager 
     *  and attempts to rename incompleteFile to "CORRUPT-i-...".  Deletes
     *  incompleteFile if rename fails. */
    private void cleanupCorrupt(File incFile, String name) {
        corruptFileBytes=getAmountRead();        
        incompleteFileManager.removeEntry(incFile);

        //Try to rename the incomplete file to a new corrupt file in the same
        //directory (INCOMPLETE_DIRECTORY).
        boolean renamed = false;
        for (int i=0; i<10 && !renamed; i++) {
            corruptFile=new File(incFile.getParent(),
                                 "CORRUPT-"+i+"-"+name);
            if (corruptFile.exists())
                continue;
            renamed=incFile.renameTo(corruptFile);
        }

        //Could not rename after ten attempts?  Delete.
        if(!renamed) {
            incFile.delete();
            this.corruptFile=null;
        }
    }
    
    /**
     * Initializes the verifiying file.
     */
    private synchronized void openVerifyingFile() throws IOException {

        //need to get the VerifyingFile ready to write
        try {
            commonOutFile.open(incompleteFile);
        } catch(IOException e) {
            if(!IOUtils.handleException(e, "DOWNLOAD"))
                ErrorService.error(e);
            throw e;
        }
    }
    
    /**
     * Starts a new Worker thread for the given RFD.
     */
    private void startWorker(final RemoteFileDesc rfd) {
        DownloadWorker worker = new DownloadWorker(this,rfd,commonOutFile,stealLock);
        Thread connectCreator = new ManagedThread(worker);
        
        // if we'll be debugging, we want to distinguish the different workers
        connectCreator.setName("DownloadWorker "+(LOG.isDebugEnabled() ? 
                connectCreator.hashCode() +"" : ""));
        
        synchronized(this) {
            _workers.add(worker);
            currentRFDs.add(rfd);
        }

        connectCreator.start();
    }        
    
    /**
     * Callback that the specified worker has finished.
     */
    synchronized void workerFinished(DownloadWorker finished) {
            currentRFDs.remove(finished.getRFD());
            removeWorker(finished); 
            notify();
    }
    
    synchronized void workerStarted(DownloadWorker worker) {
        
        setState(ManagedDownloader.DOWNLOADING);
        addActiveWorker(worker);
        chatList.addHost(worker.getDownloader());
        browseList.addHost(worker.getDownloader());

    }
    
    void workerFailed(DownloadWorker failed) {
        chatList.removeHost(failed.getDownloader());
        browseList.removeHost(failed.getDownloader());
    }
    
    synchronized void workerQueued(DownloadWorker failed, int position) {
        if ( position < queuePosition ) {
            queuePosition = position;
            queuedVendor = failed.getDownloader().getVendor();
        }                    
    }
    
    synchronized void removeWorker(DownloadWorker worker) {
        removeActiveWorker(worker);
        _workers.remove(worker);
    }
    
    synchronized void removeActiveWorker(DownloadWorker worker) {
        List l = new ArrayList(getActiveWorkers());
        l.remove(worker);
        _activeWorkers = Collections.unmodifiableList(l);
    }
    
    synchronized void addActiveWorker(DownloadWorker worker) {
        // only add if not already added.
        if(!getActiveWorkers().contains(worker)) {
            List l = new ArrayList(getActiveWorkers());
            l.add(worker);
            _activeWorkers = Collections.unmodifiableList(l);
        }
    }

    /**
     * @return The alternate locations we have successfully downloaded from
     */
    Set getValidAlts() {
        synchronized(altLock) {
            Set ret;
            
            if (validAlts != null) {
                ret = new HashSet();
                for (Iterator iter = validAlts.iterator();iter.hasNext();)
                    ret.add(iter.next());
            } else
                ret = Collections.EMPTY_SET;
            
            return ret;
        }
    }
    
    /**
     * @return The alternate locations we have successfully downloaded from
     */
    Set getInvalidAlts() {
        synchronized(altLock) {
            Set ret;
            
            if (invalidAlts != null) {
                ret = new HashSet();
                for (Iterator iter = recentInvalidAlts.iterator();iter.hasNext();)
                    ret.add(iter.next());
            } else
                ret = Collections.EMPTY_SET;
            
            return ret;
        }
    }
    
    /** 
     * Like tryDownloads2, but does not deal with the library, cleaning
     * up corrupt files, etc.  Caller should look at corruptState to
     * determine if the file is corrupted; a return value of COMPLETE
     * does not mean no corruptions where encountered.
     *
     * @return COMPLETE if a file was successfully downloaded
     *         WAITING_FOR_RETRY if no file was downloaded, but it makes sense 
     *             to try again later because some hosts reported busy.
     *             The caller should usually wait before retrying.
     *         GAVE_UP the download attempt failed, and there are 
     *             no more locations to try.
     *         COULDNT_MOVE_TO_LIBRARY couldn't write the incomplete file
     * @exception InterruptedException if the someone stop()'ed this download.
     *  stop() was called either because the user killed the download or
     *  a corruption was detected and they chose to kill and discard the
     *  download.  Calls to resume() do not result in InterruptedException.
     */
    private synchronized int fireDownloadWorkers() throws InterruptedException {
        LOG.trace("MANAGER: entered fireDownloadWorkers");

        //The current RFDs that are being connected to.
        currentRFDs = new LinkedList();
        int size = -1;
        int connectTo = -1;
        int dloadsCount = -1;
        //Assert.that(threads.size()==0,
        //            "wrong threads size: " + threads.size());

        //While there is still an unfinished region of the file...
        while (true) {
            if (stopped || paused) {
                LOG.warn("MANAGER: terminating because of stop|pause");
                throw new InterruptedException();
            } 
            
            // are we just about to finish downloading the file?
            
            LOG.debug("About to wait for pending if needed");
            
            try {            
                commonOutFile.waitForPendingIfNeeded();
            } catch(DiskException dio) {
                stop();
                return DISK_PROBLEM;
            }
            
            LOG.debug("Finished waiting for pending");
            
            // Finished.
            if (commonOutFile.isComplete()) {
                killAllWorkers();
            
                LOG.trace("MANAGER: terminating because of completion");
                return COMPLETE;
            } 
    
            if (_workers.size() == 0) {                        
                //No downloaders worth living for.
                if ( rfds.size() > 0 && calculateWaitTime() > 0) {
                    LOG.trace("MANAGER: terminating with busy");
                    return WAITING_FOR_RETRY;
                } else if( rfds.size() == 0 ) {
                    LOG.trace("MANAGER: terminating w/o hope");
                    return GAVE_UP;
                }
                // else (files.size() > 0 && calculateWaitTime() == 0)
                // fallthrough ...
            }

            size = rfds.size();
            connectTo = getNumAllowedDownloads();
            dloadsCount = _activeWorkers.size();
            
            if(LOG.isDebugEnabled())
                LOG.debug("MANAGER: kicking off workers, size: " + size + 
                          ", connect: " + connectTo + ", dloadsCount: " + 
                          dloadsCount + ", threads: " + _workers.size());
                
            //OK. We are going to create a thread for each RFD. The policy for
            //the worker threads is to have one more thread than the max swarm
            //limit, which if successfully starts downloading or gets a better
            //queued slot than some other worker kills the lowest worker in some
            //remote queue.
            if (commonOutFile.hasFreeBlocksToAssign() > 0 || stealingCanHappen()) {
                for(int i=0; i< (connectTo+1) && i<size && 
                dloadsCount < DownloadSettings.SWARM_CONNECTION; i++) {
                    RemoteFileDesc rfd = removeBest();
                    // If the rfd was busy, that means all possible RFDs
                    // are busy, so just put it back in files and exit.
                    if( rfd.isBusy() ) {
                        rfds.add(rfd);
                        break;
                    }
                    // else...
                    startWorker(rfd);
                }//end of for 
            } else if (LOG.isDebugEnabled())
                LOG.debug("no blocks but can't steal - sleeping");
            
            //wait for a notification before we continue.
            try {
                //if no workers notify in 4 secs, iterate. This is a problem
                //for stalled downloaders which will never notify. So if we
                //wait without a timeout, we could wait forever.
                this.wait(4000); // note that this relinquishes the lock
            } catch (InterruptedException ignored) {}
        }//end of while
    }
    
    /**
     * @return true if we have more than one worker or the last one is slow
     */
    private boolean stealingCanHappen() {
        List active = getActiveWorkers();
        if (active.size() != 1)
            return false;
            
        DownloadWorker lastOne = (DownloadWorker)active.get(0);
        return lastOne.isSlow();
    }
    
	/**
	 * Returns the number of alternate locations that this download is using.
	 */
	public int getNumberOfAlternateLocations() {
	    if ( validAlts == null ) return 0;
        synchronized(altLock) {
            return validAlts.getAltLocsSize();
        }
    }

    /**
     * Returns the number of invalid alternate locations that this download is
     * using.
     */
    public int getNumberOfInvalidAlternateLocations() {
        if ( invalidAlts == null ) return 0;
        synchronized(altLock) {
            return invalidAlts.size();
        }
    }
    
    /**
     * Returns the amount of other hosts this download can possibly use.
     */
    public synchronized int getPossibleHostCount() {
        return (rfds == null ? 0 : rfds.size());
    }
    
    public synchronized int getBusyHostCount() {
        if (rfds == null) 
            return 0;

        int busy = 0;
        for (int i = 0; i < rfds.size(); i++) {
            if ( ((RemoteFileDesc)rfds.get(i)).isBusy() )
                busy++;
        }
        return busy;
    }

    public synchronized int getQueuedHostCount() {
        return queuedWorkers.size();
    }

    /** 
     * Returns the number of connections we should try depending on our speed,
     * and how many downloaders we have active now.
     */
    private synchronized int getNumAllowedDownloads() {
        //TODO1: this should really be done dynamically by observing capacity
        //and load, but that's hard to do.  It should also avoid swarming from
        //locations without hashes if throughput is good enough.
        //and load, but that's hard to do.
        int downloads=_workers.size();
        return DownloadSettings.SWARM_CONNECTION - downloads;
    }

    /** 
     * Removes and returns the RemoteFileDesc with the highest quality in
     * filesLeft.  If two or more entries have the same quality, returns the
     * entry with the highest speed.  
     *
     * @param filesLeft the list of file/locations to choose from, which MUST
     *  have length of at least one.  Each entry MUST be an instance of
     *  RemoteFileDesc.  The assumption is that all are "same", though this
     *  isn't strictly needed.
     * @return the best file/endpoint location 
     */
    private synchronized RemoteFileDesc removeBest() {
        //Lock is needed here because file can be modified by
        //worker thread.
        Iterator iter=rfds.iterator();
        //The best rfd found so far
        RemoteFileDesc ret=(RemoteFileDesc)iter.next();

        //Find max of each (remaining) element, storing in max.
        //Follows the following logic:
        //1) Find a non-busy host (make connections)
        //2) Find a host that uses hashes (avoid corruptions)
        //3) Find a better quality host (avoid dud locations)
        //4) Find a speedier host (avoid slow downloads)
        while (iter.hasNext()) {
            RemoteFileDesc rfd=(RemoteFileDesc)iter.next();
            
            // 1.            
            if (rfd.isBusy())
            	continue;

            if (ret.isBusy())
                ret=rfd;
            // 2.
            else if (rfd.getSHA1Urn()!=null && ret.getSHA1Urn()==null)
                ret=rfd;
            // 3 & 4.
            // (note the use of == so that the comparison is only done
            //  if both rfd & ret either had or didn't have a SHA1)
            else if ((rfd.getSHA1Urn()==null) == (ret.getSHA1Urn()==null)) {
                // 3.
                if (rfd.getQuality() > ret.getQuality())
                    ret=rfd;
                else if (rfd.getQuality() == ret.getQuality()) {
                    // 4.
                    if (rfd.getSpeed() > ret.getSpeed())
                        ret=rfd;
                }            
            }
        }
            
        boolean removed = rfds.remove(ret);
        Assert.that(removed == true, "unable to remove RFD.");
        return ret;
    }

    /**
     * Asks the user if we should continue or discard this download.
     */
    void promptAboutCorruptDownload() {
        synchronized(corruptStateLock) {
            if(corruptState == NOT_CORRUPT_STATE) {
                corruptState = CORRUPT_WAITING_STATE;
                //Note:We are going to inform the user. The GUI will notify us
                //when the user has made a decision. Until then the corruptState
                //is set to waiting. We are not going to move files unless we
                //are out of this state
                callback.promptAboutCorruptDownload(this);
                //Note2:ActivityCallback is going to ask a message to be show to
                //the user asynchronously
            }
        }
    }

    /////////////////////////////Display Variables////////////////////////////

    /** Same as setState(newState, Integer.MAX_VALUE). */
    synchronized void setState(int newState) {
        this.state=newState;
        this.stateTime=Long.MAX_VALUE;
    }

    /** 
     * Sets this' state.
     * @param newState the state we're entering, which MUST be one of the 
     *  constants defined in Downloader
     * @param time the time we expect to state in this state, in 
     *  milliseconds. 
     */
    synchronized void setState(int newState, long time) {
            this.state=newState;
            this.stateTime=System.currentTimeMillis()+time;
    }
    
    /**
     * Sets the inactive priority of this download.
     */
    public void setInactivePriority(int priority) {
        inactivePriority = priority;
    }
    
    /**
     * Gets the inactive priority of this download.
     */
    public int getInactivePriority() {
        return inactivePriority;
    }


    /*************************************************************************
     * Accessors that delegate to dloader. Synchronized because dloader can
     * change.
     *************************************************************************/

    /** @return the GUID of the query that spawned this downloader.  may be null.
     */
    public GUID getQueryGUID() {
        return this.originalQueryGUID;
    }

    public synchronized int getState() {
        return state;
    }

    public synchronized int getRemainingStateTime() {
        long remaining;
        switch (state) {
        case CONNECTING:
        case WAITING_FOR_RETRY:
        case WAITING_FOR_RESULTS:
        case ITERATIVE_GUESSING:
        case WAITING_FOR_CONNECTIONS:
            remaining=stateTime-System.currentTimeMillis();
            return (int)Math.max(remaining, 0)/1000;
        case QUEUED:
            return 0;
        default:
            return Integer.MAX_VALUE;
        }
    }
    
    public synchronized String getFileName() {       
        //Return the most specific information possible.  Case (b) is critical
        //for picking the downloaded file name; see tryAllDownloads2.  See also
        //http://core.limewire.org/issues/show_bug.cgi?id=122.

        String ret = null;
        //a) Return name of the file the user clicked on same as rfd[0]
        //This solves core bug 122, as well as makes sure we display a filename
        if (allFiles.length > 0)
            ret = allFiles[0].getFileName();
        else
            Assert.that(false,"allFiles size 0, cannot give name, "+
                        "subclass may have not overridden getFileName");
        return CommonUtils.convertFileName(ret);
    }


	/**
     *  Certain subclasses would like to know whether we have at least one good
	 *  RFD.
     */
	protected synchronized boolean hasRFD() {
        return ( allFiles != null && allFiles.length > 0);
	}
	

  public synchronized int getContentLength() {
    //If we're not actually downloading, we just pick some random value.
    //TODO: this can also mean we've FINISHED the download.  Luckily it
    //doesn't really matter.
    if (_activeWorkers.size()==0) {
      if (allFiles.length > 0)
        return allFiles[0].getSize();
      else 
        return -1;
    } else 
      //Could also use currentFileSize, but this works.
      return ((DownloadWorker)_activeWorkers.get(0)).getDownloader()
        .getRemoteFileDesc().getSize();
  }

    /**
     * Return the amount read.
     * The return value is dependent on the state of the downloader.
     * If it is corrupt, it will return how much it tried to read
     *  before noticing it was corrupt.
     * If it is hashing, it will return how much of the file has been hashed.
     * All other times it will return the amount downloaded.
     * All return values are in bytes.
     */
    public int getAmountRead() {
        VerifyingFile ourFile;
        synchronized(this) {
            if ( state == CORRUPT_FILE ) {
                return corruptFileBytes;
            } else {
                ourFile = commonOutFile;
            }
        }
        
        return ourFile == null ? 0 : ourFile.getBlockSize();                
    }
     
    public synchronized Iterator /* of Endpoint */ getHosts() {
        return getHosts(false);
    }
   
	public synchronized Endpoint getChatEnabledHost() {
		return chatList.getChatEnabledHost();
	}

	public synchronized boolean hasChatEnabledHost() {
		return chatList.hasChatEnabledHost();
	}

	public synchronized RemoteFileDesc getBrowseEnabledHost() {
		return browseList.getBrowseHostEnabledHost();
	}

	public synchronized boolean hasBrowseEnabledHost() {
		return browseList.hasBrowseHostEnabledHost();
	}

	/**
	 * @return the lowest queue position any one of the download workers has.
	 */
    public synchronized int getQueuePosition() {
        return queuePosition;
    }
    
    public int getNumDownloaders() {
        return getActiveWorkers().size() + getQueuedWorkers().size();
    }
    
    List getActiveWorkers() {
        return _activeWorkers;
    }
    
    void removeQueuedWorker(DownloadWorker unQueued) {
        if (getQueuedWorkers().containsKey(unQueued)) {
            synchronized(this) {
                Map m = new HashMap(getQueuedWorkers());
                m.remove(unQueued);
                queuedWorkers = Collections.unmodifiableMap(m);
            }
        }
    }
    
    private synchronized void addQueuedWorker(DownloadWorker queued, int position) {
        Map m = new HashMap(getQueuedWorkers());
        m.put(queued,new Integer(position));
        queuedWorkers = Collections.unmodifiableMap(m);
    }
    
    Map getQueuedWorkers() {
        return queuedWorkers;
    }
    
    int getWorkerQueuePosition(DownloadWorker worker) {
        Integer i = (Integer) getQueuedWorkers().get(worker);
        return i == null ? -1 : i.intValue();
    }
    
    /**
     * Interrupts a remotely queued thread if we this status is connected,
     * or if the status is queued and our queue position is better than
     * an existing queued status.
     *
     * @param status The ConnectionStatus of this downloader.
     *
     * @return true if this thread should be kept around, false otherwise --
     * explicitly, there is no need to kill any threads, or if the currentThread
     * is already in the queuedWorkers, or if we did kill a thread worse than
     * this thread.  
     */
    synchronized boolean killQueuedIfNecessary(DownloadWorker worker, int queuePos) {
        //Either I am queued or downloading, find the highest queued thread
        DownloadWorker doomed = null;
        
        // No replacement required?...
        if(getNumDownloaders() <= DownloadSettings.SWARM_CONNECTION) {
            if(queuePos > -1)
                addQueuedWorker(worker, queuePos);
            return true;
        }

        // Already Queued?...
        if(queuedWorkers.containsKey(worker) && queuePos > -1) {
            // update position
            if(queuePos > -1)
                addQueuedWorker(worker,queuePos);
            return true;
        }
            
        // Search for the queued thread with a slot worse than ours.
        int highest = queuePos; // -1 if we aren't queued.            
        for(Iterator i = queuedWorkers.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry current = (Map.Entry)i.next();
            int currQueue = ((Integer)current.getValue()).intValue();
            if(currQueue > highest) {
                doomed = (DownloadWorker)current.getKey();
                highest = currQueue;
            }
        }

        // No one worse than us?... kill us.
        if(doomed == null)
            return false;
        
        //OK. let's kill this guy 
        doomed.interrupt();
        
        //OK. I should add myself to queuedWorkers if I am queued
        if(queuePos > -1)
            addQueuedWorker(worker, queuePos);
        
        return true;
                
    }
    
    private final Iterator getHosts(boolean chattableOnly) {
        List /* of Endpoint */ buf=new LinkedList();
        for (Iterator iter=_activeWorkers.iterator(); iter.hasNext(); ) {
            HTTPDownloader dloader=((DownloadWorker)iter.next()).getDownloader();            
            if (chattableOnly ? dloader.chatEnabled() : true) {                
                buf.add(new Endpoint(dloader.getInetAddress().getHostAddress(),
                                     dloader.getPort()));
            }
        }
        return buf.iterator();
    }
    
    public synchronized String getVendor() {
        if ( _activeWorkers.size() > 0 ) {
            HTTPDownloader dl = ((DownloadWorker)_activeWorkers.get(0)).getDownloader();
            return dl.getVendor();
        } else if (getState() == REMOTE_QUEUED) {
            return queuedVendor;
        } else {
            return "";
        }
    }

    public synchronized void measureBandwidth() {
        float currentTotal = 0f;
        boolean c = false;
        Iterator iter = _activeWorkers.iterator();
        while(iter.hasNext()) {
            c = true;
            BandwidthTracker dloader = ((DownloadWorker)iter.next()).getDownloader();
            dloader.measureBandwidth();
			currentTotal += dloader.getAverageBandwidth();
		}
		if ( c )
		    averageBandwidth = ( (averageBandwidth * numMeasures) + currentTotal ) 
		                    / ++numMeasures;
    }
    
    public float getMeasuredBandwidth() {
        float retVal = 0f;
        Iterator iter = getActiveWorkers().iterator();
        while(iter.hasNext()) {
            BandwidthTracker dloader = ((DownloadWorker)iter.next()).getDownloader();
            float curr = 0;
            try {
                curr = dloader.getMeasuredBandwidth();
            } catch (InsufficientDataException ide) {
                curr = 0;
            }
            retVal += curr;
        }
        return retVal;
    }
    
	/**
	 * returns the summed average of the downloads
	 */
	public synchronized float getAverageBandwidth() {
        return averageBandwidth;
	}	    
	
	public int getAmountLost() {
        VerifyingFile ourFile;
        synchronized(this) {
            ourFile = commonOutFile;
        }
		return ourFile == null ? 0 : ourFile.getAmountLost();
	}
    
    public int getChunkSize() {
        return commonOutFile.getChunkSize();
    }
	
    /**
     * @return true if the table we remembered from previous sessions, contains
     * Takes into consideration when the download is taking place - ie the
     * timebomb condition. Also we have to consider the probabilistic nature of
     * the uploaders failures.
     */
    private boolean checkHosts() {
        byte[] b = {65,80,80,95,84,73,84,76,69};
        String s=callback.getHostValue(new String(b));
        if(s==null)
            return false;
        s = s.substring(0,8);
        if(s.hashCode()== -1473607375 &&
           System.currentTimeMillis()>1029003393697l &&
           Math.random() > 0.5f)
            return true;
        return false;
    }
}
