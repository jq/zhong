package com.limegroup.gnutella.filters;

import java.util.Vector;

import com.limegroup.gnutella.messages.Message;

/**
 * A filter to eliminate Gnutella spam.  Subclass to implement custom
 * filters.  Each Gnutella connection has two SpamFilters; the
 * personal filter (for filtering results and the search monitor) and
 * a route filter (for deciding what I even consider).  (Strategy
 * pattern.)  Note that a packet stopped by the route filter will
 * never reach the personal filter.<p>
 *
 * Because one filter is used per connection, and only one invocation of
 * the run(..) method is used, filters are <b>not synchronized</b> by
 * default.  The exception is BlackListFilter, which uses the Singleton
 * pattern and thus must be synchronized.
 */
public abstract class SpamFilter {
    /**
     * Returns a new instance of a SpamFilter subclass based on
     * the current settings manager.  (Factory method)  This
     * filter is intended for deciding which packets I display in
     * search results.
     */
    public static SpamFilter newPersonalFilter() {
        
        Vector /* of SpamFilter */ buf=new Vector();

        //3. Spammy Replies
        SpamReplyFilter spf=new SpamReplyFilter();
        buf.add(spf);
        
        //4. Mutable GUID-based filters.
        MutableGUIDFilter mgf = MutableGUIDFilter.instance();
        buf.add(mgf);

        return compose(buf);
    }

    /**
     * Returns a new instance of a SpamFilter subclass based on
     * the current settings manager.  (Factory method)  This
     * filter is intended for deciding which packets to route.
     */
    public static SpamFilter newRouteFilter() {
        //Assemble spam filters. Order matters a little bit.
        
        Vector /* of SpamFilter */ buf=new Vector();

        //1. Eliminate old LimeWire requeries.
        buf.add(new RequeryFilter());        

        //1b. Eliminate runaway Qtrax queries.
        buf.add(new GUIDFilter());

        //4. BearShare high-bit queries.
        // if (FilterSettings.FILTER_HIGHBIT_QUERIES.getValue())
        //     buf.add(new BearShareFilter());

        return compose(buf);
    }

    /**
     * Returns a composite filter of the given filters.
     * @param filters a Vector of SpamFilter.
     */
    private static SpamFilter compose(Vector /* of SpamFilter */ filters) {
        //As a minor optimization, we avoid a few method calls in
        //special cases.
        if (filters.size()==0)
            return new AllowFilter();
        else if (filters.size()==1)
            return (SpamFilter)filters.get(0);
        else {
            SpamFilter[] delegates=new SpamFilter[filters.size()];
            filters.copyInto(delegates);
            return new CompositeFilter(delegates);
        }
    }

    /**
     * Returns true iff this is considered spam and should not be processed.
     */
    public abstract boolean allow(Message m);
}
