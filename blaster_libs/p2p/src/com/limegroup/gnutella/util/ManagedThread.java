package com.limegroup.gnutella.util;

import com.limegroup.gnutella.ErrorService;

/**
 * A ManagedThread, always reporting errors to the ErrorService.
 *
 * If passing a Runnable, use as:
 * Thread t = new ManagedThread(myRunnable);
 * t.start();
 *
 * If extending, extend the managedRun() method instead of run().
 */
public class ManagedThread extends Thread {
    
    /**
     * Constructs a ManagedThread with no target.
     */
    public ManagedThread() {
        super();
    }
    
    /**
     * Constructs a ManagedThread with the specified target.
     */
    public ManagedThread(Runnable r) {
        super(r);
    }
    
    /**
     * Constructs a ManagedThread with the specified name.
     */
    public ManagedThread(String name) {
        super(name);
    }
    
    /**
     * Constructs a ManagedThread with the specified target and name.
     */
    public ManagedThread(Runnable r, String name) {
        super(r, name);
    }
    
    /**
     * Runs the target, reporting any errors to the ErrorService.
     */
    @Override
    public final void run() {
        try {
            managedRun();
        } catch(Throwable t) {
            //ErrorService.error(t, "Uncaught thread error.");
        }
    }
    
    /**
     * If a target exists, runs the target.  Otherwise this method must
     * be extended to do anything.
     */
    protected void managedRun() {
        super.run();
    }
}
