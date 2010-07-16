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

import entagged.audioformats.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class Id3v1TagWriter {
	private Id3v1TagCreator tc = new Id3v1TagCreator();

	public void delete(RandomAccessFile raf) throws IOException {
		if(!tagExists(raf))
			return;
		
		raf.setLength(raf.length() - 128);
	}

	private boolean tagExists(RandomAccessFile raf) throws IOException {
		if (raf.length() <= 128)
			return false;
		
		raf.seek(raf.length() - 128);
		byte[] b = new byte[3];
		raf.read(b);

		return new String(b).equals("TAG");
	}

	public void write(Tag tag, RandomAccessFile raf) throws IOException {
		FileChannel fc = raf.getChannel();

		ByteBuffer tagBuffer = tc.convert(tag);

		if (!tagExists(raf)) {
			//System.err.println("Creating a new ID3v1 Tag");
			fc.position(fc.size());
			fc.write(tagBuffer);
			//ID3v1 Tag Written
		} else {
			//System.err.println("Old ID3v1 Tag found, replacing the old tag");
			fc.position(fc.size() - 128);
			fc.write(tagBuffer);
		}
	}
}
