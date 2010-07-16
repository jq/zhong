/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package entagged.audioformats.mp3.util;

import java.nio.ByteBuffer;

public class Id3v2TagSynchronizer {

    public ByteBuffer synchronize(ByteBuffer b) {
        ByteBuffer bb = ByteBuffer.allocate(b.capacity());
        
        int cap = b.capacity();
        while(b.remaining() >= 1) {
        	byte cur = b.get();
            bb.put(cur);
            
            if((cur&0xFF) == 0xFF && b.remaining() >=1 && b.get(b.position()) == 0x00) { //First part of synchronization
                b.get();
            }
        }
        
        //We have finished filling the new bytebuffer, so set the limit, and rewind
        bb.limit(bb.position());
        bb.rewind();
        
        return bb;
    }

}
