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

import entagged.audioformats.*;
import entagged.audioformats.exceptions.*;
import entagged.audioformats.generic.AudioFileWriter;
import entagged.audioformats.mp3.util.*;

import java.io.*;

public class Mp3FileWriter extends AudioFileWriter {

	private Id3v1TagWriter idv1tw = new Id3v1TagWriter();
	private Id3v2TagWriter idv2tw = new Id3v2TagWriter();
	
	protected void writeTag(Tag tag, RandomAccessFile raf, RandomAccessFile rafTemp) throws CannotWriteException, IOException {
	    idv1tw.write(tag, raf);
		idv2tw.write(tag, raf, rafTemp);
	}
	
	protected void deleteTag(RandomAccessFile raf, RandomAccessFile rafTemp) throws IOException {
		RandomAccessFile used = idv2tw.delete(raf, rafTemp);
		idv1tw.delete(used);
	}
}

