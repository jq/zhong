
package com.limegroup.gnutella.bootstrap;

import java.text.ParseException;

/**
 * The list of default GWebCache urls, used the first time LimeWire starts, or
 * if the gnutella.net file is accidentally deleted.  Entries in the list will
 * eventually be replaced by URLs discovered during urlfile=1 requests.  Order
 * does not matter.
 *
 * THIS FILE IS AUTOMATICALLY GENERATED FROM MAKE_DEFAULT.PY.
 */
public class DefaultBootstrapServers {
    /**
     * Adds all the default servers to bman. 
     */
    public static void addDefaults(BootstrapServerManager bman) {
        for (int i=0; i<urls.length; i++) {
            try {
                BootstrapServer server=new BootstrapServer(urls[i]);
                bman.addBootstrapServer(server);
            } catch (ParseException ignore) {
            }                
        }
    }

    //These should NOT be URL encoded.
    static String[] urls=new String[] {
	"http://crab.bishopston.net:3558/",
        "http://g1.blacknex.net/cgi-bin/perlgcache.cgi",
        "http://cache.kicks-ass.net:8000/",
        "http://g2cache.theg2.net/gwcache/lynnx.asp",
        "http://galvatron.dyndns.org:59009/gwcache",
        "http://gcache.cloppy.net/",
        "http://gwc1.mager.org:8081/GWebCache/req",
        "http://gwebcache.daems.org/GWebCache/req",
        "http://gwebcache2.limewire.com:9000/gwc",
        "http://www.goeg.dk/Gnutella/gcache.php",
        "http://goeg.dk/Gnutella/gcache.php",
        "http://gwc.lame.net/gwcii.php"
    };
}
