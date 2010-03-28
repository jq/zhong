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
package entagged.audioformats.mp3;

import entagged.audioformats.Tag;
import entagged.audioformats.generic.GenericTag;

public class Id3v1Tag extends GenericTag {
	public final static String[] GENRES = Tag.DEFAULT_GENRES;
	
	protected boolean isAllowedEncoding(String enc) {
	    return enc.equals("ISO-8859-1");
	}
	
	public String translateGenre( byte b) {
		int i = b & 0xFF;

		if ( i == 255 || i > GENRES.length - 1 )
			return "";
		return GENRES[i];
	}
	
	public String toString() {
		return "Id3v1 "+super.toString();
	}
}

