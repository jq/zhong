package com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

import com.bitzi.util.Base32;
import com.util.LOG;
import com.limegroup.gnutella.downloader.AlreadyDownloadingException;
import com.limegroup.gnutella.downloader.CantResumeException;
import com.limegroup.gnutella.downloader.FileExistsException;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.MagnetDownloader;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.downloader.RequeryDownloader;
import com.limegroup.gnutella.downloader.ResumeDownloader;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.udpconnect.UDPConnection;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.ConverterObjectInputStream;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.gnutella.util.URLDecoder;



/** 
 * The list of all downloads in progress.  DownloadManager has a fixed number 
 * of download slots given by the MAX_SIM_DOWNLOADS property.  It is
 * responsible for starting downloads and scheduling and queing them as 
 * needed.  This class is thread safe.<p>
 *
 * As with other classes in this package, a DownloadManager instance may not be
 * used until initialize(..) is called.  The arguments to this are not passed
 * in to the constructor in case there are circular dependencies.<p>
 *
 * DownloadManager provides ways to serialize download state to disk.  Reads 
 * are initiated by RouterService, since we have to wait until the GUI is
 * initiated.  Writes are initiated by this, since we need to be notified of
 * completed downloads.  Downloads in the COULDNT_DOWNLOAD state are not 
 * serialized.  
 */
public class DownloadManager implements BandwidthTracker {    
    /** The time in milliseconds between checkpointing downloads.dat.  The more
     * often this is written, the less the lost data during a crash, but the
     * greater the chance that downloads.dat itself is corrupt.  */
    private int SNAPSHOT_CHECKPOINT_TIME=30*1000; //30 seconds

    /** The callback for notifying the GUI of major changes. */
    private ActivityCallback callback;
    /** The message router to use for pushes. */
    private MessageRouter router;
    /** The repository of incomplete files 
     *  INVARIANT: incompleteFileManager is same as those of all downloaders */
    private IncompleteFileManager incompleteFileManager
        =new IncompleteFileManager();

    /** The list of all ManagedDownloader's attempting to download.
     *  INVARIANT: active.size()<=slots() && active contains no duplicates 
     *  LOCKING: obtain this' monitor */
    private List /* of ManagedDownloader */ active=new LinkedList();
    /** The list of all queued ManagedDownloader. 
     *  INVARIANT: waiting contains no duplicates 
     *  LOCKING: obtain this' monitor */
    private List /* of ManagedDownloader */ waiting=new LinkedList();
    
    /**
     * files that we have sent an udp pushes and are waiting a connection from.
     * LOCKING: obtain UDP_FAILOVER if manipulating the contained sets as well!
     */
    private final Map /* of byte [] guids -> Set of Strings*/ 
        UDP_FAILOVER = new TreeMap(new GUID.GUIDByteComparator());
    
    private final ProcessingQueue FAILOVERS 
        = new ProcessingQueue("udp failovers");
    
    /**
     * how long we think should take a host that receives an udp push
     * to connect back to us.
     */
    private static long UDP_PUSH_FAILTIME=5000;


    /** The global minimum time between any two requeries, in milliseconds.
     *  @see com.limegroup.gnutella.downloader.ManagedDownloader#TIME_BETWEEN_REQUERIES*/
    public static long TIME_BETWEEN_REQUERIES = 45 * 60 * 1000; 

    /** The last time that a requery was sent.
     */
    private long lastRequeryTime = 0;

    /** This will hold the MDs that have sent requeries.
     *  When this size gets too big - meaning bigger than active.size(), then
     *  that means that all MDs have been serviced at least once, so you can
     *  clear it and start anew....
     */
    private List querySentMDs = new ArrayList();
    
    /**
     * The number of times we've been bandwidth measures
     */
    private int numMeasures = 0;
    
    /**
     * The average bandwidth over all downloads
     */
    private float averageBandwidth = 0;
    
    /**
     * The runnable that pumps inactive downloads to the correct state.
     */
    private Runnable _waitingPump;

    //////////////////////// Creation and Saving /////////////////////////

    /** 
     * Initializes this manager. <b>This method must be called before any other
     * methods are used.</b> 
     *     @uses RouterService.getCallback for the UI callback 
     *       to notify of download changes
     *     @uses RouterService.getMessageRouter for the message 
     *       router to use for sending push requests
     *     @uses RouterService.getFileManager for the FileManager
     *       to check if files exist
     */
    public void initialize() {
        initialize(
                   RouterService.getCallback(),
                   RouterService.getMessageRouter()
                  );
    }
    
    protected void initialize(ActivityCallback callback, MessageRouter router
                              ) {
        this.callback = callback;
        this.router = router;
        scheduleWaitingPump();
    }

    /**
     * Performs the slow, low-priority initialization tasks: reading in
     * snapshots and scheduling snapshot checkpointing.
     */
    public void postGuiInit() {
        File real = SharingSettings.DOWNLOAD_SNAPSHOT_FILE;
        File backup = SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE;
        // Try once with the real file, then with the backup file.
        if( !readSnapshot(real) ) {
            LOG.debug("Reading real downloads.dat failed");
            // if backup succeeded, copy into real.
            if( readSnapshot(backup) ) {
                LOG.debug("Reading backup downloads.bak succeeded.");
                copyBackupToReal();
            // only show the error if the files existed but couldn't be read.
            } else if(backup.exists() || real.exists()) {
                LOG.debug("Reading both downloads files failed.");
                MessageService.showError("DOWNLOAD_COULD_NOT_READ_SNAPSHOT");
            }   
        } else {
            LOG.debug("Reading downloads.dat worked!");
        }
        
        Runnable checkpointer=new Runnable() {
            public void run() {
                if (downloadsInProgress() > 0) { //optimization
                    // If the write failed, move the backup to the real.
                    if(!writeSnapshot())
                        copyBackupToReal();
                }
            }
        };
        RouterService.schedule(checkpointer, 
                               SNAPSHOT_CHECKPOINT_TIME, 
                               SNAPSHOT_CHECKPOINT_TIME);
    }
    
    /**
     * Schedules the runnable that pumps through waiting downloads.
     */
    public void scheduleWaitingPump() {
        if(_waitingPump != null)
            return;
            
        _waitingPump = new Runnable() {
            public void run() {
                pumpDownloads();
            }
        };
        RouterService.schedule(_waitingPump,
                               1000,
                               1000);
    }
    
    /**
     * Pumps through each waiting download, either removing it because it was
     * stopped, or adding it because there's an active slot and it requires
     * attention.
     */
    private synchronized void pumpDownloads() {
        int index = 1;
        for(Iterator i = waiting.iterator(); i.hasNext(); ) {
            ManagedDownloader md = (ManagedDownloader)i.next();
            if(md.isAlive()) {
                continue;
            } else if(md.isCancelled()) {
                i.remove();
                cleanupCompletedDownload(md, false);
            } else if(hasFreeSlot() && (md.hasNewSources() || md.getRemainingStateTime() <= 0)) {
                i.remove();
                active.add(md);
                md.startDownload();
            } else {
                if(!md.isPaused())
                    md.setInactivePriority(index++);
                md.handleInactivity();
            }
        }
    }
    
    /**
     * Copies the backup downloads.dat (downloads.bak) file to the
     * the real downloads.dat location.
     */
    private synchronized void copyBackupToReal() {
        File real = SharingSettings.DOWNLOAD_SNAPSHOT_FILE;
        File backup = SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE;        
        real.delete();
        CommonUtils.copy(backup, real);
    }
    
    /**
     * Determines if the given URN has an incomplete file.
     */
    public boolean isIncomplete(URN urn) {
        return incompleteFileManager.getFileForUrn(urn) != null;
    }
    
    /**
     * Returns the IncompleteFileManager used by this DownloadManager
     * and all ManagedDownloaders.
     */
    public IncompleteFileManager getIncompleteFileManager() {
        return incompleteFileManager;
    }    

    public synchronized int downloadsInProgress() {
        return active.size() + waiting.size();
    }
    
    public synchronized int getNumIndividualDownloaders() {
        int ret = 0;
        for (Iterator iter=active.iterator(); iter.hasNext(); ) {  //active
            ManagedDownloader md=(ManagedDownloader)iter.next();
            ret += md.getNumDownloaders();
       }
       return ret;
    }
    
    public synchronized int getNumActiveDownloads() {
        return active.size();
    }
   
    public synchronized int getNumWaitingDownloads() {
        return waiting.size();
    }
    
    public ManagedDownloader getDownloaderForURN(URN sha1) {
        List l;
        synchronized(this) {
            l = new ArrayList(active);
            l.addAll(waiting);
        }
        
        for (Iterator iter = l.iterator(); iter.hasNext();) {
            ManagedDownloader current = (ManagedDownloader) iter.next();
            if (current.getSHA1Urn().equals(sha1))
                return current;
        }
        return null;
    }

    public synchronized boolean isGuidForQueryDownloading(GUID guid) {
        for (Iterator iter=active.iterator(); iter.hasNext(); ) {
            GUID dGUID = ((ManagedDownloader) iter.next()).getQueryGUID();
            if ((dGUID != null) && (dGUID.equals(guid)))
                return true;
        }
        for (Iterator iter=waiting.iterator(); iter.hasNext(); ) {
            GUID dGUID = ((ManagedDownloader) iter.next()).getQueryGUID();
            if ((dGUID != null) && (dGUID.equals(guid)))
                return true;
        }
        return false;
    }
    
    /**
     * Clears all downloads.
     */
    public void clearAllDownloads() {
        List buf;
        synchronized(this) {
            buf = new ArrayList(active.size() + waiting.size());
            buf.addAll(active);
            buf.addAll(waiting);
            active.clear();
            waiting.clear();
        }
        for(Iterator i = buf.iterator(); i.hasNext(); ) {
            ManagedDownloader md = (ManagedDownloader)i.next();
            md.stop();
        }
    }   

    /** Writes a snapshot of all downloaders in this and all incomplete files to
     *  the file named DOWNLOAD_SNAPSHOT_FILE.  It is safe to call this method
     *  at any time for checkpointing purposes.  Returns true iff the file was
     *  successfully written. */
    synchronized boolean writeSnapshot() {
        List buf=new ArrayList(active.size() + waiting.size());
        buf.addAll(active);
        buf.addAll(waiting);
        
        File outFile = SharingSettings.DOWNLOAD_SNAPSHOT_FILE;
        //must delete in order for renameTo to work.
        SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE.delete();
        outFile.renameTo(
            SharingSettings.DOWNLOAD_SNAPSHOT_BACKUP_FILE);
        
        // Write list of active and waiting downloaders, then block list in
        //   IncompleteFileManager.
        ObjectOutputStream out = null;
        try {
            out=new ObjectOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(
                                SharingSettings.DOWNLOAD_SNAPSHOT_FILE)));
            out.writeObject(buf);
            //Blocks can be written to incompleteFileManager from other threads
            //while this downloader is being serialized, so lock is needed.
            synchronized (incompleteFileManager) {
                out.writeObject(incompleteFileManager);
            }
            out.flush();
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (out != null)
                try {out.close();}catch(IOException ignored){}
        }
    }

    /** Reads the downloaders serialized in DOWNLOAD_SNAPSHOT_FILE and adds them
     *  to this, queued.  The queued downloads will restart immediately if slots
     *  are available.  Returns false iff the file could not be read for any
     *  reason.  THIS METHOD SHOULD BE CALLED BEFORE ANY GUI ACTION. 
     *  It is public for testing purposes only!  
     *  @param file the downloads.dat snapshot file */
    public synchronized boolean readSnapshot(File file) {
        //Read downloaders from disk.
        List buf=null;
        try {
            ObjectInputStream in = new ConverterObjectInputStream(
                                    new BufferedInputStream(
                                        new FileInputStream(file)));
            //This does not try to maintain backwards compatibility with older
            //versions of LimeWire, which only wrote the list of downloaders.
            //Note that there is a minor race condition here; if the user has
            //started some downloads before this method is called, the new and
            //old downloads will use different IncompleteFileManager instances.
            //This doesn't really cause an errors, however.
            buf=(List)in.readObject();
            incompleteFileManager=(IncompleteFileManager)in.readObject();
        } catch(Throwable t) {
            LOG.error("Unable to read download file", t);
            return false;
        }
        
        //Remove entries that are too old or no longer existent.  This is done
        //before starting downloads in the rare case that a downloader uses one
        //of these incomplete files.  Then commit changes to disk.  (This last
        //step isn't really needed.)
        if (incompleteFileManager.purge(true))
            writeSnapshot();

        // Pump the downloaders through a set, to remove duplicate values.
        // This is necessary in case LimeWire got into a state where a
        // downloader was written to disk twice.
        buf = new LinkedList(new HashSet(buf));

        //Initialize and start downloaders.  Must catch ClassCastException since
        //the data could be corrupt.  This code is a little tricky.  It is
        //important that instruction (3) follow (1) and (2), because we must not
        //pass an uninitialized Downloader to the GUI.  (The call to getFileName
        //will throw NullPointerException.)  I believe the relative order of (1)
        //and (2) does not matter since this' monitor is held.  (The download
        //thread must obtain the monitor to acquire a queue slot.)
        try {
            for (Iterator iter=buf.iterator(); iter.hasNext(); ) {
                ManagedDownloader downloader=(ManagedDownloader)iter.next();
                
                // ignore RequeryDownloaders -- they're legacy
                if(downloader instanceof RequeryDownloader)
                    continue;

                waiting.add(downloader);                                 //1
                downloader.initialize(this, callback); //2
                callback.addDownload(downloader);                        //3
            }
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }
     
    ////////////////////////// Main Public Interface ///////////////////////
           
    /** 
     * Tries to "smart download" any of the given files.<p>  
     *
     * If any of the files already being downloaded (or queued for downloaded)
     * has the same temporary name as any of the files in 'files', throws
     * AlreadyDownloadingException.  Note, however, that this doesn't guarantee
     * that a successfully downloaded file can be moved to the library.<p>
     *
     * If overwrite==false, then if any of the files already exists in the
     * download directory, FileExistsException is thrown and no files are
     * modified.  If overwrite==true, the files may be overwritten.<p>
     * 
     * Otherwise returns a Downloader that allows you to stop and resume this
     * download.  The ActivityCallback will also be notified of this download,
     * so the return value can usually be ignored.  The download begins
     * immediately, unless it is queued.  It stops after any of the files
     * succeeds.
     *
     * @param queryGUID the guid of the query that resulted in the RFDs being
     * downloaded.
     *
     *     @modifies this, disk 
     */
    public synchronized Downloader download(RemoteFileDesc[] files,
                                            Set<Endpoint> alts, 
                                            boolean overwrite,
                                            GUID queryGUID) 
            throws FileExistsException, AlreadyDownloadingException, 
                   java.io.FileNotFoundException {
        //Check if file would conflict with any other downloads in progress.
        //TODO3: if only a few of many files conflicts, we could just ignore
        //them.
        String conflict=conflicts(files, null);
        if (conflict!=null)
            throw new AlreadyDownloadingException(conflict);


        //Check if file exists.  TODO3: ideally we'd pass ALL conflicting files
        //to the GUI, so they know what they're overwriting.
        if (! overwrite) {
            File downloadDir = SharingSettings.DEFAULT_SAVE_DIR;
            String filename=files[0].getFileName();
            File completeFile = new File(downloadDir, filename);  
            if ( completeFile.exists() ) 
                throw new FileExistsException(filename);            
        }

        //Purge entries from incompleteFileManager that have no corresponding
        //file on disk.  This protects against stupid users who delete their
        //temporary files while LimeWire is running, either through the command
        //prompt or the library.  Note that you could optimize this by just
        //purging files corresponding to the current download, but it's not
        //worth it.
        incompleteFileManager.purge(false);

        //Start download asynchronously.  This automatically moves downloader to
        //active if it can.
        ManagedDownloader downloader =
            new ManagedDownloader(files, incompleteFileManager, queryGUID);

        initializeDownload(downloader);
        
        //Now that the download is started, add the alts without caching.
        for(Iterator iter = alts.iterator(); iter.hasNext(); ) {
            RemoteFileDesc rfd = new RemoteFileDesc(files[0], (Endpoint)iter.next());
            downloader.addDownload(rfd, false);
        }
        
        return downloader;
    }   
    
    /**
     * Creates a new MAGNET downloader.  Immediately tries to download from
     * <tt>defaultURL</tt>, if specified.  If that fails, or if defaultURL does
     * not provide alternate locations, issues a requery with <tt>textQuery</tt>
     * and </tt>urn</tt>, as provided.  (At least one must be non-null.)  If
     * <tt>filename</tt> is specified, it will be used as the name of the
     * complete file; otherwise it will be taken from any search results or
     * guessed from <tt>defaultURLs</tt>.
     *
     * @param urn the hash of the file (exact topic), or null if unknown
     * @param textQuery requery keywords (keyword topic), or null if unknown
     * @param filename the final file name, or null if unknown
     * @param defaultURLs the initial locations to try (exact source), or null 
     *  if unknown
     *
     * @exception AlreadyDownloadingException couldn't download because the
     *  another downloader is getting the file
     * @exception IllegalArgumentException both urn and textQuery are null 
     */
    public synchronized Downloader download(URN urn, String textQuery, 
            String filename, String [] defaultURL, boolean overwrite) 
            throws IllegalArgumentException, AlreadyDownloadingException,
                                                       FileExistsException {
        if (textQuery==null && urn==null && filename==null && 
            (defaultURL == null || defaultURL.length == 0) )
            throw new IllegalArgumentException("Need something for requeries");
        
        //if we have a valid filename to check against, and we are not supposed
        //to overwrite, thrown an exception if the file already exists
        if(!overwrite && (filename!=null && !filename.equals(""))) {
            File downloadDir = SharingSettings.DEFAULT_SAVE_DIR;
            File completeFile = new File(downloadDir,filename);
            if(completeFile.exists()) 
                throw new FileExistsException(filename);
        }
        
        //remove entry from IFM if the incomplete file was deleted.
        incompleteFileManager.purge(false);
        
        if(urn!=null) {
            if(conflicts(urn)) {
                String ex = 
                (filename!=null&&!filename.equals(""))?filename:urn.toString();
                throw new AlreadyDownloadingException(ex);
            }
        }

        //Note: If the filename exists, it would be nice to check that we are
        //not already downloading the file by calling conflicts with the
        //filename...the problem is we cannot do this effectively without the
        //size of the file (atleast, not without being risky in assuming that
        //two files with the same name are the same file). So for now we will
        //just leave it and download the same file twice.

        //Instantiate downloader, validating incompleteFile first.
        MagnetDownloader downloader = 
            new MagnetDownloader(incompleteFileManager, urn, textQuery,
                filename, defaultURL);
        initializeDownload(downloader);
        return downloader;
    }

    /**
     * Starts a resume download for the given incomplete file.
     * @exception AlreadyDownloadingException couldn't download because the
     *  another downloader is getting the file
     * @exception CantResumeException incompleteFile is not a valid 
     *  incomplete file
     */ 
    public synchronized Downloader download(File incompleteFile)
            throws AlreadyDownloadingException, CantResumeException { 
        //Check for conflicts.  TODO: refactor to make less like conflicts().
        for (Iterator iter=active.iterator(); iter.hasNext(); ) {  //active
            ManagedDownloader md=(ManagedDownloader)iter.next();
            if (md.conflicts(incompleteFile))                   
                throw new AlreadyDownloadingException(md.getFileName());
        }
        for (Iterator iter=waiting.iterator(); iter.hasNext(); ) { //queued
            ManagedDownloader md=(ManagedDownloader)iter.next();
            if (md.conflicts(incompleteFile))                   
                throw new AlreadyDownloadingException(md.getFileName());
        }

        //Check if file exists.  TODO3: ideally we'd pass ALL conflicting files
        //to the GUI, so they know what they're overwriting.
        //if (! overwrite) {
        //    try {
        //        File downloadDir=SettingsManager.instance().getSaveDirectory();
        //        File completeFile=new File(
        //            downloadDir, 
        //            incompleteFileManager.getCompletedName(incompleteFile));
        //        if (completeFile.exists())
        //            throw new FileExistsException(filename);
        //    } catch (IllegalArgumentException e) {
        //        throw new CantResumeException(incompleteFile.getName());
        //    }
        //}

        //Purge entries from incompleteFileManager that have no corresponding
        //file on disk.  This protects against stupid users who delete their
        //temporary files while LimeWire is running, either through the command
        //prompt or the library.  Note that you could optimize this by just
        //purging files corresponding to the current download, but it's not
        //worth it.
        incompleteFileManager.purge(false);

        //Instantiate downloader, validating incompleteFile first.
        ResumeDownloader downloader=null;
        try {
            incompleteFile = FileUtils.getCanonicalFile(incompleteFile);
            String name=IncompleteFileManager.getCompletedName(incompleteFile);
            int size=ByteOrder.long2int(
                IncompleteFileManager.getCompletedSize(incompleteFile));
            downloader = new ResumeDownloader(incompleteFileManager,
                                              incompleteFile,
                                              name,
                                              size);
        } catch (IllegalArgumentException e) {
            throw new CantResumeException(incompleteFile.getName());
        } catch (IOException ioe) {
            throw new CantResumeException(incompleteFile.getName());
        }
        
        initializeDownload(downloader);
        return downloader;
    }
    
    /**
     * Performs common tasks for initializing the download.
     * 1) Initializes the downloader.
     * 2) Adds the download to the waiting list.
     * 3) Notifies the callback about the new downloader.
     * 4) Writes the new snapshot out to disk.
     */
    private synchronized void initializeDownload(ManagedDownloader md) {
        md.initialize(this, callback);
        waiting.add(md);
        callback.addDownload(md);
        writeSnapshot(); // Save state for crash recovery.
    }


    /**
     * Returns the name of any of the files in 'files' conflict with any of the
     * downloads in this except for dloader, which may be null.  Returns null if
     * there are no conflicts.  This is used before starting and resuming
     * downloads.  
     */
    public synchronized String conflicts(RemoteFileDesc[] files,
                                         ManagedDownloader dloader) {
        for (int i=0; i<files.length; i++) {
            //Active downloads...
            for (Iterator iter=active.iterator(); iter.hasNext(); ) {
                ManagedDownloader md=(ManagedDownloader)iter.next();
                if (dloader!=null && md==dloader)
                    continue;
                if (md.conflicts(files[i]))                   
                    return files[i].getFileName();
            }
            //Queued downloads...
            for (Iterator iter=waiting.iterator(); iter.hasNext(); ) {
                ManagedDownloader md=(ManagedDownloader)iter.next();
                if (dloader!=null && md==dloader)
                    continue;
                if (md.conflicts(files[i]))
                    return files[i].getFileName();
            }
        }
        return null;
    }


    private synchronized boolean conflicts(URN urn) {
        Iterator iter = active.iterator();
        while(iter.hasNext()) {
            ManagedDownloader md = (ManagedDownloader)iter.next();
            if(md.conflicts(urn))
                return true;
        }
        iter = waiting.iterator();
        while(iter.hasNext()) {
            ManagedDownloader md = (ManagedDownloader)iter.next();
            if(md.conflicts(urn))
                return true;
        }
        return false;
    }

    /** 
     * Adds all responses (and alternates) in qr to any downloaders, if
     * appropriate.
     */
    public void handleQueryReply(QueryReply qr) {
        // first check if the qr is of 'sufficient quality', if not just
        // short-circuit.
        if (qr.calculateQualityOfService(
                !RouterService.acceptedIncomingConnection()) < 1)
            return;

        List responses;
        HostData data;
        try {
            responses = qr.getResultsAsList();
            data = qr.getHostData();
        } catch(BadPacketException bpe) {
            return; // bad packet, do nothing.
        }
        
        addDownloadWithResponses(responses, data);
    }

    /**
     * Iterates through all responses seeing if they can be matched
     * up to any existing downloaders, adding them as possible
     * sources if they do.
     */
    private void addDownloadWithResponses(List responses, HostData data) {
        if(responses == null)
            throw new NullPointerException("null responses");
        if(data == null)
            throw new NullPointerException("null hostdata");

        // need to synch because active and waiting are not thread safe
        List downloaders = new ArrayList(active.size() + waiting.size());
        synchronized (this) { 
            // add to all downloaders, even if they are waiting....
            downloaders.addAll(active);
            downloaders.addAll(waiting);
        }
        
        // short-circuit.
        if(downloaders.isEmpty())
            return;

        //For each response i, offer it to each downloader j.  Give a response
        // to at most one downloader.
        // TODO: it's possible that downloader x could accept response[i] but
        //that would cause a conflict with downloader y.  Check for this.
        for(Iterator i = responses.iterator(); i.hasNext(); ) {
            Response r = (Response)i.next();
            // Don't bother with making XML from the EQHD.
            RemoteFileDesc rfd = r.toRemoteFileDesc(data);
            for(Iterator j = downloaders.iterator(); j.hasNext(); ) {
                ManagedDownloader currD = (ManagedDownloader)j.next();
                // If we were able to add this specific rfd,
                // add any alternates that this response might have
                // also.
                if (currD.addDownload(rfd, true)) {
                    Set alts = r.getLocations();
                    for(Iterator k = alts.iterator(); k.hasNext(); ) {
                        Endpoint ep = (Endpoint)k.next();
                        // don't cache alts.
                        currD.addDownload(new RemoteFileDesc(rfd, ep), false);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Accepts the given socket for a push download to this host.
     * If the GIV is for a file that was never requested or has already
     * been downloaded, this will deal with it appropriately.  In any case
     * this eventually closes the socket.  Non-blocking.
     *     @modifies this
     *     @requires "GIV " was just read from s
     */
    public void acceptDownload(Socket socket) {
        Thread.currentThread().setName("PushDownloadThread");
        try {
            //1. Read GIV line BEFORE acquiring lock, since this may block.
            GIVLine line=parseGIV(socket);
            String file=line.file;
            int index=line.index;
            byte[] clientGUID=line.clientGUID;
            
            synchronized(UDP_FAILOVER) {
                // if the push was sent through udp, make sure we cancel
                // the failover push.
                byte [] key = clientGUID;
                Set files = (Set)UDP_FAILOVER.get(key);
            
                if (files!=null) {
                    files.remove(file);
                    if (files.isEmpty())
                        UDP_FAILOVER.remove(key);
                }
            }

            //2. Attempt to give to an existing downloader.
            synchronized (this) {
                if (BrowseHostHandler.handlePush(index, new GUID(clientGUID), 
                                                 socket))
                    return;
                for (Iterator iter=active.iterator(); iter.hasNext();) {
                    ManagedDownloader md=(ManagedDownloader)iter.next();
                    if (md.acceptDownload(file, socket, index, clientGUID))
                        return;
                }
                for (Iterator iter=waiting.iterator(); iter.hasNext();) {
                    ManagedDownloader md=(ManagedDownloader)iter.next();
                    if (md.acceptDownload(file, socket, index, clientGUID))
                        return;
                }
            }
        } catch (IOException e) {
        }            

        //3. We never requested the file or already got it.  Kill it.
        try {
            socket.close();
        } catch (IOException e) { }
    }


    ////////////// Callback Methods for ManagedDownloaders ///////////////////

    /** @requires this monitor' held by caller */
    private boolean hasFreeSlot() {
        return active.size() < DownloadSettings.MAX_SIM_DOWNLOAD;
    }

    /**
     * Removes downloader entirely from the list of current downloads.
     * Notifies callback of the change in status.
     * If completed is true, finishes the download completely.  Otherwise,
     * puts the download back in the waiting list to be finished later.
     *     @modifies this, callback
     */
    public synchronized void remove(ManagedDownloader downloader, 
                                    boolean completed) {
        active.remove(downloader);
        waiting.remove(downloader);
        if(completed)
            cleanupCompletedDownload(downloader, true);
        else
            waiting.add(downloader);
    }

    /**
     * Bumps the priority of an inactive download either up or down
     * by amt (if amt==0, bump to start/end of list).
     */
    public synchronized void bumpPriority(Downloader downloader,
                                          boolean up, int amt) {
        int idx = waiting.indexOf(downloader);
        if(idx == -1)
            return;

        if(up && idx != 0) {
            waiting.remove(idx);
            if (amt > idx)
                amt = idx;
            if (amt != 0)
                waiting.add(idx - amt, downloader);
            else
                waiting.add(0, downloader);     //move to top of list
        } else if(!up && idx != waiting.size() - 1) {
            waiting.remove(idx);
            if (amt != 0) {
                amt += idx;
                if (amt > waiting.size())
                    amt = waiting.size();
                waiting.add(amt, downloader);
            } else {
                waiting.add(downloader);    //move to bottom of list
            }
        }
    }

    /**
     * Cleans up the given ManagedDownloader after completion.
     *
     * If ser is true, also writes a snapshot to the disk.
     */
    private void cleanupCompletedDownload(ManagedDownloader dl, boolean ser) {
        querySentMDs.remove(dl);
        dl.finish();
        callback.removeDownload(dl);
        
        //Save this' state to disk for crash recovery.
        if(ser)
            writeSnapshot();

        // Enable auto shutdown
        if(active.isEmpty() && waiting.isEmpty())
            callback.downloadsComplete();
    }           
    
    /** 
     * Attempts to send the given requery to provide the given downloader with 
     * more sources to download.  May not actually send the requery if it doing
     * so would exceed the maximum requery rate.
     * 
     * @param query the requery to send, which should have a marked GUID.
     *  Queries are subjected to global rate limiting iff they have marked 
     *  requery GUIDs.
     * @param requerier the downloader requesting more sources.  Needed to 
     *  ensure fair requery scheduling.  This MUST be in the waiting list,
     *  i.e., it MUST NOT have a download slot.
     * @return true iff the query was actually sent.  If false is returned,
     *  the downloader should attempt to send the query later.
     */
    public synchronized boolean sendQuery(ManagedDownloader requerier, 
                                          QueryRequest query) {
        //NOTE: this algorithm provides global but not local fairness.  That is,
        //if two requeries x and y are competing for a slot, patterns like
        //xyxyxy or xyyxxy are allowed, though xxxxyx is not.
        if(LOG.isTraceEnabled())
            LOG.trace("DM.sendQuery():" + query.getQuery());
        Assert.that(waiting.contains(requerier),
                    "Unknown or non-waiting MD trying to send requery.");

        //Disallow if global time limits exceeded.  These limits don't apply to
        //queries that are requeries.
        boolean isRequery=GUID.isLimeRequeryGUID(query.getGUID());
        long elapsed=System.currentTimeMillis()-lastRequeryTime;
        if (isRequery && elapsed<=TIME_BETWEEN_REQUERIES) {
            return false;
        }

        //Has everyone had a chance to send a query?  If so, clear the slate.
        if (querySentMDs.size() >= waiting.size()) {
            LOG.trace("DM.sendQuery(): reseting query sent queue");
            querySentMDs.clear();
        }

        //If downloader has already sent a query, give someone else a turn.
        if (querySentMDs.contains(requerier)) {
            // nope, sorry, must lets others go first...
            if(LOG.isWarnEnabled())
                LOG.warn("DM.sendQuery(): out of turn:" + query.getQuery());
            return false;
        }
        
        if(LOG.isTraceEnabled())
            LOG.trace("DM.sendQuery(): requery allowed:" + query.getQuery());  
        querySentMDs.add(requerier);                  
        lastRequeryTime = System.currentTimeMillis();
        router.sendDynamicQuery(query);
        return true;
    }

    /**
     * Sends a push through multicast.
     *
     * Returns true only if the RemoteFileDesc was a reply to a multicast query
     * and we wanted to send through multicast.  Otherwise, returns false,
     * as we shouldn't reply on the multicast network.
     */
    private boolean sendPushMulticast(RemoteFileDesc file, byte []guid) {
        // Send as multicast if it's multicast.
        if( file.isReplyToMulticast() ) {
            byte[] addr = RouterService.getNonForcedAddress();
            int port = RouterService.getNonForcedPort();
            if( NetworkUtils.isValidAddress(addr) &&
                NetworkUtils.isValidPort(port) ) {
                PushRequest pr = new PushRequest(guid,
                                         (byte)1, //ttl
                                         file.getClientGUID(),
                                         file.getIndex(),
                                         addr,
                                         port,
                                         Message.N_MULTICAST);
                router.sendMulticastPushRequest(pr);
                if (LOG.isInfoEnabled())
                    LOG.info("Sending push request through multicast " + pr);
                return true;
            }
        }
        return false;
    }

    /**
     * Sends a push through UDP.
     *
     * This always returns true, because a UDP push is always sent.
     */    
    private boolean sendPushUDP(RemoteFileDesc file, byte[] guid) {
        PushRequest pr = 
                new PushRequest(guid,
                                (byte)2,
                                file.getClientGUID(),
                                file.getIndex(),
                                RouterService.getAddress(),
                                RouterService.getPort(),
                                Message.N_UDP);
        if (LOG.isInfoEnabled())
                LOG.info("Sending push request through udp " + pr);
                    
        UDPService udpService = UDPService.instance();
        //and send the push to the node 
        try {
            InetAddress address = InetAddress.getByName(file.getHost());
            
            //don't bother sending direct push if the node reported invalid
            //address and port.
            if (NetworkUtils.isValidAddress(address) &&
                    NetworkUtils.isValidPort(file.getPort()))
                udpService.send(pr, address, file.getPort());
        } catch(UnknownHostException notCritical) {
            //We can't send the push to a host we don't know
            //but we can still send it to the proxies.
        } finally {
            IPFilter filter = IPFilter.instance();
            //make sure we send it to the proxies, if any
            Set proxies = file.getPushProxies();
            for (Iterator iter = proxies.iterator();iter.hasNext();) {
                PushProxyInterface ppi = (PushProxyInterface)iter.next();
                if (filter.allow(ppi.getPushProxyAddress().getAddress()))
                    udpService.send(pr,ppi.getPushProxyAddress(),ppi.getPushProxyPort());
            }
        }
        return true;
    }
    
    /**
     * Sends a push through TCP.
     *
     * Returns true if we have a valid push route, or if a push proxy
     * gave us a succesful sending notice.
     */
    private boolean sendPushTCP(final RemoteFileDesc file, final byte[] guid) {
        // if this is a FW to FW transfer, we must consider special stuff
        final boolean shouldDoFWTransfer = file.supportsFWTransfer() &&
                         UDPService.instance().canDoFWT() &&
                        !RouterService.acceptedIncomingConnection();

        // try sending to push proxies...
        if(sendPushThroughProxies(file, guid, shouldDoFWTransfer))
            return true;
            
        // if push proxies failed, but we need a fw-fw transfer, give up.
        if(shouldDoFWTransfer && !RouterService.acceptedIncomingConnection())
            return false;
            
        byte[] addr = RouterService.getAddress();
        int port = RouterService.getPort();
        if(!NetworkUtils.isValidAddressAndPort(addr, port))
            return false;

        PushRequest pr = 
            new PushRequest(guid,
                            ConnectionSettings.TTL,
                            file.getClientGUID(),
                            file.getIndex(),
                            addr, port);
        if(LOG.isInfoEnabled())
            LOG.info("Sending push request through Gnutella: " + pr);
        try {
            router.sendPushRequest(pr);
        } catch (IOException e) {
            // this will happen if we have no push route.
            return false;
        }

        return true;
    }
    
    /**
     * Sends a push through push proxies.
     *
     * Returns true if a push proxy gave us a succesful reply,
     * otherwise returns false is all push proxies tell us the sending failed.
     */
    private boolean sendPushThroughProxies(final RemoteFileDesc file,
                                           final byte[] guid,
                                           boolean shouldDoFWTransfer) {
        Set proxies = file.getPushProxies();
        if(proxies.isEmpty())
            return false;
            
        byte[] externalAddr = RouterService.getExternalAddress();
        // if a fw transfer is necessary, but our external address is invalid,
        // then exit immediately 'cause nothing will work.
        if (shouldDoFWTransfer && !NetworkUtils.isValidAddress(externalAddr))
            return false;

        byte[] addr = RouterService.getAddress();
        int port = RouterService.getPort();

        //TODO: investigate not sending a HTTP request to a proxy
        //you are directly connected to.  How much of a problem is this?
        //Probably not much of one at all.  Classic example of code
        //complexity versus efficiency.  It may be hard to actually
        //distinguish a PushProxy from one of your UP connections if the
        //connection was incoming since the port on the socket is ephemeral 
        //and not necessarily the proxies listening port
        // we have proxy info - give them a try

        // set up the request string --
        // if a fw-fw transfer is required, add the extra "file" parameter.
        final String request = "/gnutella/push-proxy?ServerID=" + 
                               Base32.encode(file.getClientGUID()) +
          (shouldDoFWTransfer ? ("&file=" + PushRequest.FW_TRANS_INDEX) : "");
            
        final String nodeString = "X-Node";
        final String nodeValue =
            NetworkUtils.ip2string(shouldDoFWTransfer ? externalAddr : addr) +
            ":" + port;

        IPFilter filter = IPFilter.instance();
        // try to contact each proxy
        for(Iterator iter = proxies.iterator(); iter.hasNext(); ) {
            PushProxyInterface ppi = (PushProxyInterface)iter.next();
            if (!filter.allow(ppi.getPushProxyAddress().getAddress()))
                continue;
            final String ppIp = ppi.getPushProxyAddress().getHostAddress();
            final int ppPort = ppi.getPushProxyPort();
            String connectTo =  "http://" + ppIp + ":" + ppPort + request;
            HttpHead head = new HttpHead(connectTo);
            head.addHeader(nodeString, nodeValue);
            head.addHeader("Cache-Control", "no-cache");
            DefaultHttpClient httpClient = new DefaultHttpClient();
            if(LOG.isTraceEnabled())
                LOG.trace("Push Proxy Requesting with: " + connectTo);
            try {
                HttpResponse res = httpClient.execute(head);
                if(res.getStatusLine().getStatusCode() == 202) {
                    if(LOG.isInfoEnabled())
                        LOG.info("Succesful push proxy: " + connectTo);
                    if (shouldDoFWTransfer)
                        startFWIncomingThread(file);
                    return true; // push proxy succeeded!
                } else {
                    if(LOG.isWarnEnabled())
                        LOG.warn("Invalid push proxy: " + connectTo);
                }
            } catch (IOException ioe) {
                LOG.warn("PushProxy request exception", ioe);
            }
        }
        
        // they all failed.
        return false;
    }
    
    /**
     * Starts a thread waiting for an incoming fw-fw transfer.
     */
    private void startFWIncomingThread(final RemoteFileDesc file) {
        // we need to open up our NAT for incoming UDP, so
        // start the UDPConnection.  The other side should
        // do it soon too so hopefully we can communicate.
        Thread startPushThread = new ManagedThread("FWIncoming") {
            public void managedRun() {
                Socket fwTrans=null;
                try {
                    fwTrans = 
                        new UDPConnection(file.getHost(), file.getPort());
                    // TODO: put this out to Acceptor in // the future
                    InputStream is = fwTrans.getInputStream();
                    String word = IOUtils.readWord(is, 4);
                    if (word.equals("GIV"))
                        acceptDownload(fwTrans);
                    else
                        fwTrans.close();
                } catch (IOException crap) {
                    LOG.debug("failed to establish UDP connection",crap);
                    if (fwTrans!=null)
                        try {fwTrans.close();}catch(IOException ignored){}
                }
            }
        };
        startPushThread.setDaemon(true);
        startPushThread.start();
    }
    
    /**
     * Sends a push for the given file.
     */
    public void sendPush(RemoteFileDesc file) {
        sendPush(file, null);
    }

    /**
     * Sends a push request for the given file.
     *
     * @param file the <tt>RemoteFileDesc</tt> constructed from the query 
     *  hit, containing data about the host we're pushing to
     * @param the object to notify if a failover TCP push fails
     * @return <tt>true</tt> if the push was successfully sent, otherwise
     *  <tt>false</tt>
     */
    public void sendPush(final RemoteFileDesc file, final Object toNotify) {
        //Make sure we know our correct address/port.
        // If we don't, we can't send pushes yet.
        byte[] addr = RouterService.getAddress();
        int port = RouterService.getPort();
        if(!NetworkUtils.isValidAddress(addr) || !NetworkUtils.isValidPort(port)) {
            notify(toNotify);
            return;
        }
        
        final byte[] guid = GUID.makeGuid();
        
        // If multicast worked, try nothing else.
        if (sendPushMulticast(file,guid))
            return;
        
        // if we can't accept incoming connections, we can only try
        // using the TCP push proxy, which will do fw-fw transfers.
        if(!RouterService.acceptedIncomingConnection()) {
            // if we can't do FWT, or we can and the TCP push failed,
            // then notify immediately.
            if(!UDPService.instance().canDoFWT() || !sendPushTCP(file, guid))
                notify(toNotify);
            return;
        }
        
        // remember that we are waiting a push from this host 
        // for the specific file.
        // do not send tcp pushes to results from alternate locations.
        if (!file.isFromAlternateLocation()) {
            synchronized(UDP_FAILOVER) {
                byte[] key = file.getClientGUID();
                Set files = (Set)UDP_FAILOVER.get(key);
                if (files==null)
                    files = new HashSet();
                files.add(file.getFileName());
                UDP_FAILOVER.put(key,files);
            }
            
            // schedule the failover tcp pusher, which will run
            // if we don't get a response from the UDP push
            // within the UDP_PUSH_FAILTIME timeframe
            RouterService.schedule(new Runnable(){
                public void run() {
                    // Add it to a ProcessingQueue, so the TCP connection 
                    // doesn't bog down RouterService's scheduler
                    // The FailoverRequestor will thus run in another thread.
                    FAILOVERS.add(new PushFailoverRequestor(file, guid, toNotify));
                }
            }, UDP_PUSH_FAILTIME, 0);
        }

        sendPushUDP(file,guid);
    }


    /////////////////// Internal Method to Parse GIV String ///////////////////

    private static final class GIVLine {
        final String file;
        final int index;
        final byte[] clientGUID;
        GIVLine(String file, int index, byte[] clientGUID) {
            this.file=file;
            this.index=index;
            this.clientGUID=clientGUID;
        }
    }

    /** 
     * Returns the file, index, and client GUID from the GIV request from s.
     * The input stream of s is positioned just after the GIV request,
     * immediately before any HTTP.  If s is closed or the line couldn't
     * be parsed, throws IOException.
     *     @requires "GIV " just read from s
     *     @modifies s's input stream.
     */
    private static GIVLine parseGIV(Socket s) throws IOException {
        //1. Read  "GIV 0:BC1F6870696111D4A74D0001031AE043/sample.txt\n\n"
        String command;
        try {
            //The try-catch below is a work-around for JDK bug 4091706.
            InputStream istream=null;
            try {
                istream = s.getInputStream();
            } catch (Exception e) {
                throw new IOException();
            }
            ByteReader br = new ByteReader(istream);
            command = br.readLine();      // read in the first line
            if (command==null)
                throw new IOException();
            String next=br.readLine();    // read in empty line
            if (next==null || (! next.equals(""))) {
                throw new IOException();
            }
        } catch (IOException e) {      
            throw e;                   
        }   

        //2. Parse and return the fields.
        try {
            //a) Extract file index.  IndexOutOfBoundsException
            //   or NumberFormatExceptions will be thrown here if there's
            //   a problem.  They're caught below.
            int i=command.indexOf(":");
            int index=Integer.parseInt(command.substring(0,i));
            //b) Extract clientID.  This can throw
            //   IndexOutOfBoundsException or
            //   IllegalArgumentException, which is caught below.
            int j=command.indexOf("/", i);
            byte[] guid=GUID.fromHexString(command.substring(i+1,j));
            //c). Extract file name.
            String filename=URLDecoder.decode(command.substring(j+1));

            return new GIVLine(filename, index, guid);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException();
        } catch (NumberFormatException e) {
            throw new IOException();
        } catch (IllegalArgumentException e) {
            throw new IOException();
        }          
    }


    /** Calls measureBandwidth on each uploader. */
    public synchronized void measureBandwidth() {
        float currentTotal = 0f;
        boolean c = false;
        for (Iterator iter = active.iterator(); iter.hasNext(); ) {
            c = true;
            BandwidthTracker bt = (BandwidthTracker)iter.next();
            bt.measureBandwidth();
            currentTotal += bt.getAverageBandwidth();
        }
        if ( c )
            averageBandwidth = ( (averageBandwidth * numMeasures) + currentTotal ) 
                            / ++numMeasures;
    }

    /** Returns the total upload throughput, i.e., the sum over all uploads. */
    public synchronized float getMeasuredBandwidth() {
        float sum=0;
        for (Iterator iter = active.iterator(); iter.hasNext(); ) {
            BandwidthTracker bt = (BandwidthTracker)iter.next();
            float curr = 0;
            try{
                curr = bt.getMeasuredBandwidth();
            } catch(InsufficientDataException ide) {
                curr = 0;//insufficient data? assume 0
            }
            sum+=curr;
        }
        return sum;
    }
    
    /**
     * returns the summed average of the downloads
     */
    public synchronized float getAverageBandwidth() {
        return averageBandwidth;
    }
    
    /**
     * Notifies the given object, if it isn't null.
     */
    private void notify(Object o) {
        if(o == null)
            return;
        synchronized(o) {
            o.notify();
        }
    }
    
    /**
     * sends a tcp push if the udp push has failed.
     */
    private class PushFailoverRequestor implements Runnable {
        
        final RemoteFileDesc _file;
        final byte [] _guid;
        final Object _toNotify;
        
        public PushFailoverRequestor(RemoteFileDesc file,
                                     byte[] guid,
                                     Object toNotify) {
            _file = file;
            _guid = guid;
            _toNotify = toNotify;
        }
        
        public void run() {
            boolean proceed = false;
            
            byte[] key =_file.getClientGUID();

            synchronized(UDP_FAILOVER) {
                Set files = (Set) UDP_FAILOVER.get(key);
            
                if (files!=null && files.contains(_file.getFileName())) {
                    proceed = true;
                    files.remove(_file.getFileName());
                    if (files.isEmpty())
                        UDP_FAILOVER.remove(key);
                }
            }
            
            if (proceed) 
                if(!sendPushTCP(_file,_guid))
                    DownloadManager.this.notify(_toNotify);
        }
    }

}
