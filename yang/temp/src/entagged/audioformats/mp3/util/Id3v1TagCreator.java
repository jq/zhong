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

import entagged.audioformats.mp3.*;
import entagged.audioformats.*;

import java.nio.*;
import java.io.*;
import java.util.*;

public class Id3v1TagCreator  {
	
	private static Hashtable translateTable;
	
	static {
	    translateTable = new Hashtable( 130 );
		for ( int i = 0; i < Id3v1Tag.GENRES.length; i++ )
			translateTable.put( Id3v1Tag.GENRES[i].toLowerCase(), new Byte((byte)i) );
	}
	
	public Id3v1TagCreator() {
		/* Nothing to do */
	}
	
	public ByteBuffer convert(Tag tag) throws UnsupportedEncodingException {
		ByteBuffer buf = ByteBuffer.allocate( 128 );

		buf.put( (byte) 84 ).put( (byte) 65 ).put( (byte) 71 );  //TAG

		put(buf, tag.getFirstTitle() , 30);
		//------------------------------------------------
		put(buf, tag.getFirstArtist() , 30);
		//------------------------------------------------
		put(buf, tag.getFirstAlbum() , 30);
		//------------------------------------------------
		put(buf, tag.getFirstYear() , 4);
		//------------------------------------------------
		if ( tag.getTrack().size() != 0 ) {
			put(buf, tag.getFirstComment() , 28);
			//------------------------------------------------
			buf.put( (byte) 0 );

			int integ = 0;
			try {
				integ = Integer.parseInt( tag.getFirstTrack() );
			} catch ( NumberFormatException e ) {
				integ = 0;
			}

			buf.put( (byte) integ );
		}
		else {
			put(buf, tag.getFirstComment() , 30);
		}
		//------------------------------------------------
		buf.put( translateGenre(tag.getFirstGenre()) );
		buf.rewind();
		return buf;
	}
	
	private void put(ByteBuffer buf, String s, int length) throws UnsupportedEncodingException {
		byte[] b = new byte[length];
		byte[] text = null;
		text = truncate(s, length).getBytes( "ISO-8859-1" );
		
		for ( int i = 0; i < text.length; i++ )
			b[i] = text[i];
		for ( int i = text.length; i < ( length - text.length ); i++ )
			b[i] = 0;
		buf.put( b, 0, length );
	}
	
	private String truncate( String s, int len ) {
		return ( s.length() > len ) ? s.substring( 0, len ) : s;
	}
	
	private byte translateGenre( String genre ) {
	    Byte b = (Byte) translateTable.get( genre.toLowerCase() );
		if ( b == null )
			return -1; //Empty genre field
		
		return b.byteValue();
	}
}
