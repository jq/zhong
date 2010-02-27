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


import java.io.*;

import entagged.audioformats.EncodingInfo;
import entagged.audioformats.Tag;
import entagged.audioformats.exceptions.CannotReadException;
import entagged.audioformats.generic.AudioFileReader;
import entagged.audioformats.generic.GenericTag;
import entagged.audioformats.mp3.util.Id3v1TagReader;
import entagged.audioformats.mp3.util.Id3v2TagReader;
import entagged.audioformats.mp3.util.Mp3InfoReader;

public class Mp3FileReader extends AudioFileReader {
	
	private Mp3InfoReader ir = new Mp3InfoReader();
	private Id3v2TagReader idv2tr = new Id3v2TagReader();
	private Id3v1TagReader idv1tr = new Id3v1TagReader();
	
	protected EncodingInfo getEncodingInfo( RandomAccessFile raf )  throws CannotReadException, IOException {
		return ir.read(raf);
	}
	
	protected Tag getTag( RandomAccessFile raf )  throws IOException {
		String error = "";
		Id3v2Tag v2 = null;
		Id3v1Tag v1 = null;
		
		try {
			v2 = idv2tr.read(raf);
		} catch(CannotReadException e) {
			v2 = null;
			error += "("+e.getMessage()+")";
		}
		
		try {
			v1 = idv1tr.read(raf);
		} catch(CannotReadException e) {
			v1 = null;
			error += "("+e.getMessage()+")";
		}

		if(v1 == null && v2 == null)
			return new GenericTag();
            
		
		if(v2 == null) {
			return v1;
		}
		else if(v1 != null) {
			v2.merge( v1 );
			v2.hasId3v1(true);
		}
		
		return v2;
	}
}
