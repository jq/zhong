package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.util.LOG;
import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.ProcessingQueue;


/**
 * A control point for all access to the file being downloaded to, also does 
 * on-the-fly verification.
 * 
 * Every region of the file can be in one of five states, and can move from one
 * state to another only in the following order:
 * 
 *   1. available for download 
 *   2. currently being downloaded 
 *   3. waiting to be written.
 *   4. written (and immediately into, if possible..)
 *   5. verified, or if it doesn't verify back to
 *   1. available for download   
 *   
 * In order to maintain these constraints, the only possible operations are:
 *   Lease a block - find an area which is available for download and claim it
 *   Write a block - report that the specified block has been read from the network.
 *   Release a block - report that the specified block will not be downloaded.
 */
public class VerifyingFile {
    /**
     * The thread that does the actual verification & writing
     */
    private static final ProcessingQueue QUEUE = new ProcessingQueue("BlockingVF");
    
    /**
     * If the number of corrupted data gets over this, assume the file will not be recovered
     */
    private static final float MAX_CORRUPTION = 0.1f;
    
    /** The default chunk size - if we don't have a tree we request chunks this big */
    static final int DEFAULT_CHUNK_SIZE = 100000; //100 KB
    
    /**
     * The file we're writing to / reading from.
     * LOCKING: itself. this->fos is ok
     */
    private RandomAccessFile fos;
    
    /**
     * Whether this file is open for writing
     */
    private volatile boolean isOpen;

    /**
     * The eventual completed size of the file we're writing.
     */
    private final int completedSize;
	
	/**
	 * How much data did we lose due to corruption
	 */
	private int lostSize;
    
    /**
     * Ranges that are currently being written by the ManagedDownloader. 
     * 
     * Replaces the IntervalSet of needed ranges previously stored in the 
     * ManagedDownloader but which could get out of sync with the verifiedBlocks
     * IntervalSet and is therefore replaced by a more failsafe implementation.
     */
    private IntervalSet leasedBlocks;
    
    /**
     * Ranges that are currently written to disk, but do not form complete chunks
     * so cannot be verified by the HashTree.
     */
    private IntervalSet partialBlocks;
    
    /**
     * Ranges which are pending writing & verification.
     */
    private IntervalSet pendingBlocks;
        
    /**
     * The IOException, if any, we got while writing.
     */
    private IOException storedException;
    
    /**
     * Constructs a new VerifyingFile, without a given completion size.
     *
     * Useful for tests.
     */
    public VerifyingFile() {
        this(-1);
    }
    
    /**
     * Constructs a new VerifyingFile for the specified size.
     * If checkOverlap is true, will scan for overlap corruption.
     */
    public VerifyingFile(int completedSize) {
        this.completedSize = completedSize;
        leasedBlocks = new IntervalSet();
        pendingBlocks = new IntervalSet();
        partialBlocks = new IntervalSet();
        storedException = null;
    }
    
    /**
     * Opens this VerifyingFile for writing.
     * MUST be called before anything else.
     *
     * If there is no completion size, this fails.
     */
    public void open(File file) throws IOException {
        if(completedSize == -1)
            throw new IllegalStateException("cannot open for unknown size.");
        
        // Ensure that the directory this file is in exists & is writeable.
        File parentFile = FileUtils.getParentFile(file);
        if( parentFile != null ) {
            parentFile.mkdirs();
            FileUtils.setWriteable(parentFile);
        }
        FileUtils.setWriteable(file);
        this.fos =  new RandomAccessFile(file,"rw");
        // cleanup leased blocks
        leasedBlocks = new IntervalSet();
        isOpen = true;
    }

    /**
     * used to add blocks direcly. Blocks added this way are marked
     * partial.
     */
    public synchronized void addInterval(Interval interval) {
        //delegates to underlying IntervalSet
        partialBlocks.add(interval);
    }

    public void writeBlock(long pos,byte[] data) {
        writeBlock(pos,data.length,data);
    }
    
    /**
     * Writes bytes to the underlying file.
     */
    public synchronized void writeBlock(long currPos, int length, byte[] buf) {
        
        if (LOG.isDebugEnabled())
            LOG.debug(" trying to write block at offset "+currPos+" with size "+length);
        
        if(buf.length==0) //nothing to write? return
            return;
        if(fos == null)
            throw new IllegalStateException("no fos!");
        
        if (!isOpen())
            return;
		
		Interval intvl = new Interval((int)currPos,(int)currPos+length-1);
		
		/// some stuff to help debugging ///
		if (!leasedBlocks.contains(intvl)) {
			Assert.silent(false, "trying to write an interval "+intvl+
                    " that wasn't leased "+dumpState());
        }
		
		
		if (partialBlocks.contains(intvl) ||
            pendingBlocks.contains(intvl)) {
            Assert.silent(false,"trying to write an interval "+intvl+
                    " that was already written"+dumpState());
		}
		
		// Remove from lease, put in pending for writing.
        leasedBlocks.delete(intvl);
        pendingBlocks.add(intvl);
        
        saveToDisk(buf, intvl);
    }

	/**
	 * Saves the given interval to disk. 
	 */
	private void saveToDisk(byte [] buf, Interval invtl) {
	    QUEUE.add(new ChunkHandler(buf, invtl));
    }
    
    public String dumpState() {
        return "\npartial:"+partialBlocks+
        	"\npending:"+pendingBlocks+"\nleased:"+leasedBlocks;
    }
    
    /**
     * Returns the first full block of data that needs to be written.
     */
    public synchronized Interval leaseWhite() throws NoSuchElementException {
        if (LOG.isDebugEnabled())
            LOG.debug("leasing white, state: "+dumpState());
        IntervalSet freeBlocks = pendingBlocks.invert(completedSize);
        freeBlocks.delete(leasedBlocks);
        freeBlocks.delete(partialBlocks);
        Interval ret = freeBlocks.removeFirst();
        if (LOG.isDebugEnabled())
            LOG.debug(" freeblocks: "+freeBlocks+" selected "+ret);
        leaseBlock(ret);
        return ret;
    }
    
    /**
     * Returns the first block of data that needs to be written.
     * The returned block will NEVER be larger than chunkSize.
     */
    public synchronized Interval leaseWhite(int chunkSize) 
      throws NoSuchElementException {
        Interval temp = leaseWhite();
        return allignInterval(temp, chunkSize);
    }
    
    /**
     * Returns the first block of data that needs to be written
     * and is within the specified set of ranges.
     * The parameter IntervalSet is modified
     */
    public synchronized Interval leaseWhite(IntervalSet ranges)
      throws NoSuchElementException {
        ranges.delete(leasedBlocks);
        ranges.delete(partialBlocks);
        ranges.delete(pendingBlocks);
        Interval ret = ranges.removeFirst();
        leaseBlock(ret);
        return ret;
    }
    
    /**
     * Returns the first block of data that needs to be written
     * and is within the specified set of ranges.
     * The returned block will NEVER be larger than chunkSize.
     */
    public synchronized Interval leaseWhite(IntervalSet ranges, int chunkSize)
      throws NoSuchElementException {
        Interval temp = leaseWhite(ranges);
        return allignInterval(temp, chunkSize);
    }

    /**
     * Removes the specified internal from the set of leased intervals.
     */
    public synchronized void releaseBlock(Interval in) {
        if(LOG.isDebugEnabled())
            LOG.debug("Releasing interval: " + in+" state "+dumpState());
        leasedBlocks.delete(in);
    }
	
    /**
     * Returns all downloaded blocks with an Iterator.
     */
    public synchronized Iterator getBlocks() {
        return getBlocksAsList().iterator();
    }
    
    public String toString() {
        return dumpState();
    }

    /**
     * @return all downloaded blocks as list
     */
    public synchronized List getBlocksAsList() {
        List l = new ArrayList();
        l.addAll(partialBlocks.getAllIntervalsAsList());
        l.addAll(pendingBlocks.getAllIntervalsAsList());
        IntervalSet ret = new IntervalSet();
        for (Iterator iter = l.iterator();iter.hasNext();)
            ret.add((Interval)iter.next());
        return ret.getAllIntervalsAsList();
    }
     /**
     * Returns the total number of bytes written to disk.
     */
    public synchronized int getBlockSize() {
        return 
        	partialBlocks.getSize() +
        	pendingBlocks.getSize();
    }
    
	/**
	 * @return how much data was lost due to corruption
	 */
	public synchronized int getAmountLost() {
		return lostSize;
	}
	
    /**
     * Determines if all blocks have been written to disk and verified
     */
    public synchronized boolean isComplete() {
            return partialBlocks.getSize()== completedSize;
    }
    
    /**
     * If the last remaining chunks of the file are currently pending writing & verification,
     * wait until it finishes.
     */
    public synchronized void waitForPendingIfNeeded() throws InterruptedException, DiskException {
        if(storedException != null)
            throw new DiskException(storedException);
        
        while (!isComplete() &&
                 pendingBlocks.getSize()  == completedSize) {
            if(storedException != null)
                throw new DiskException(storedException);
            if (LOG.isDebugEnabled())
                LOG.debug("waiting for a pending chunk to verify or write..");
            wait();
        }
    }
    
    /**
     * @return whether we think we will not be able to complete this file
     */
    public synchronized boolean isHopeless() {
        return lostSize >= MAX_CORRUPTION * completedSize;
    }
    
    public boolean isOpen() {
        return isOpen;
    }
    /**
     * Determines if there are any blocks that are not assigned
     * or written.
     */
    public synchronized int hasFreeBlocksToAssign() {
        return  completedSize - ( 
                leasedBlocks.getSize() +
                partialBlocks.getSize() +
                pendingBlocks.getSize()); 
    }
    
    /**
     * Closes the file output stream.
     */
    public void close() {
        // This does not clear the ManagedDownloader because
        // it could still be in a waiting state, and we need
        // it to allow IncompleteFileDescs to funnel alt-locs
        // as sources to the downloader.
        isOpen = false;
        if(fos==null)
            return;
        try { 
            fos.close();
        } catch (IOException ioe) {}
    }
    
    /////////////////////////private helpers//////////////////////////////
    
    /**
     * Fits an interval inside a chunk.  This ensures that the interval is never larger
     * than chunksize and finishes at exact chunk offset.
     */
    private synchronized Interval allignInterval(Interval temp, int chunkSize) {
        if (LOG.isDebugEnabled())
            LOG.debug("alligning "+temp +" with chunk size "+chunkSize+"\n"+dumpState());
        
        Interval interval;
        
        int intervalSize = temp.high - temp.low+1;
        
        // find where the next chunk starts
        int chunkStart = ( 1 + temp.low / chunkSize ) * chunkSize ;
        
        // if we're already covering an exact chunk, return 
        if (chunkStart == temp.high+1 && intervalSize == chunkSize) {
            LOG.debug("already at exact chunk");
            return temp;
        }
        
        // try to map the area until the chunk border
        if (chunkStart < temp.high) {
            interval = new Interval(temp.low, chunkStart-1);
            temp = new Interval(chunkStart,temp.high);
            releaseBlock(temp);
        } else
            interval = temp;

        if (LOG.isDebugEnabled())
            LOG.debug("aligned to interval: "+interval+" state is: "+dumpState());
        
        return interval;
    }

    /**
     * Leases the specified interval.
     */
    private synchronized void leaseBlock(Interval in) {
        //if(LOG.isDebugEnabled())
            //LOG.debug("Obtaining interval: " + in);
        leasedBlocks.add(in);
    }
        
    public synchronized int getChunkSize() {
        return DEFAULT_CHUNK_SIZE ;
    }
        
    /**
     * Runnable that writes chunks to disk & verifies partial blocks.
     */
    private class ChunkHandler implements Runnable {
        private final byte[] buf;
        private final Interval intvl;
        
        public ChunkHandler(byte[] buf, Interval intvl) {
           this.buf = new byte[buf.length];
           System.arraycopy(buf, 0, this.buf, 0, buf.length);
           this.intvl = intvl;
        }
        
        public void run() {
    		try {
    		    if(LOG.isDebugEnabled())
    		        LOG.debug("Writing intvl: " + intvl);
    		        
    			synchronized(fos) {
    				fos.seek(intvl.low);
    				fos.write(buf, 0, intvl.high - intvl.low + 1);
    			}
    			
    			synchronized(VerifyingFile.this) {
    			    pendingBlocks.delete(intvl);
    			    partialBlocks.add(intvl);
    			}
    			
            } catch(IOException diskIO) {
                synchronized(VerifyingFile.this) {
                    storedException = diskIO;
                }
            } finally {
                synchronized(VerifyingFile.this) {
                    VerifyingFile.this.notify();
                }
            }
        }
	}
}
