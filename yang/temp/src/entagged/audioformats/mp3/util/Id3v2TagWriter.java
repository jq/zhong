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
import entagged.audioformats.exceptions.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * $Id: Id3v2TagWriter.java,v 1.15 2005/11/19 14:28:44 liree Exp $
 * 
 * @author Raphael Slinckx (KiKiDonK)
 * @version 16 d?cembre 2003
 */
public class Id3v2TagWriter {
	private Id3v2TagCreator tc = new Id3v2TagCreator();

	public RandomAccessFile delete(RandomAccessFile raf,
			RandomAccessFile tempRaf) throws IOException {
		FileChannel fc = raf.getChannel();
		fc.position(0);

		if (!tagExists(fc))
			return raf;

		fc.position(6);

		ByteBuffer b = ByteBuffer.allocate(4);
		fc.read(b);
		b.rewind();

		int tagSize = (b.get() & 0xFF) << 21;
		tagSize += (b.get() & 0xFF) << 14;
		tagSize += (b.get() & 0xFF) << 7;
		tagSize += b.get() & 0xFF;

		FileChannel tempFC = tempRaf.getChannel();
		tempFC.position(0);

		fc.position(tagSize + 10);

		// Here we will try to skip eventual trash afer the tag and before the
		// audio data
		b = ByteBuffer.allocate(4);
		int skip = 0;
		while (fc.read(b) != -1) {
			if ((b.get(0) & 0xFF) == 0xFF && (b.get(1) & 0xE0) == 0xE0
					&& (b.get(1) & 0x06) != 0 && (b.get(2) & 0xF0) != 0xF0
					&& (b.get(2) & 0x08) != 0x08) {
				fc.position(fc.position() - 4);
				break;
			}

			fc.position(fc.position() - 3);
			b.rewind();
			skip++;
		}

		tempFC.transferFrom(fc, 0, fc.size() - tagSize - 10 - skip);
		return tempRaf;
	}

	private boolean tagExists(FileChannel fc) throws IOException {
		ByteBuffer b = ByteBuffer.allocate(3);
		fc.position(0);
		fc.read(b);
		String tagString = new String(b.array());

		return tagString.equals("ID3");
	}

	/**
	 * Assuming the file has an id3v2 tag, returns true if the tag can be
	 * overwritten. We cannot overwrite id3v2 tags not supported
	 * 
	 * @param raf
	 * @return
	 * @throws IOException
	 */
	private boolean canOverwrite(RandomAccessFile raf) throws IOException {
		raf.seek(3);
		// Version du tag ID3v2.xx.xx
		String versionHigh = raf.read() + "";
		if (!(versionHigh.equals("4") || versionHigh.equals("3") || versionHigh
				.equals("2")))
			return false; // only version 2.3.xx
		// raf.read();
		// int flag = raf.read() & 128;
		// if (flag == 128)
		// return false; //unsynchronised tags not supported
		return true;
	}

	public void write(Tag tag, RandomAccessFile raf, RandomAccessFile tempRaf)
			throws CannotWriteException, IOException {
		FileChannel fc = raf.getChannel();

		int oldTagSize = 0;

		if (tagExists(fc)) {
			// read the length
			if (!canOverwrite(raf))
				throw new CannotWriteException(
						"Overwritting of this kind of ID3v2 tag not supported yet");
			fc.position(6);

			ByteBuffer buf = ByteBuffer.allocate(4);
			fc.read(buf);
			oldTagSize = (buf.get(0) & 0xFF) << 21;
			oldTagSize += (buf.get(1) & 0xFF) << 14;
			oldTagSize += (buf.get(2) & 0xFF) << 7;
			oldTagSize += buf.get(3) & 0xFF;
			oldTagSize += 10;

			// System.err.println("Old tag size: "+oldTagSize);
			int newTagSize = tc.getTagLength(tag);

			if (oldTagSize >= newTagSize) {
				// replace
				// System.err.println("Old ID32v Tag found, replacing the old
				// tag");
				fc.position(0);

				fc.write(tc.convert(tag, oldTagSize - newTagSize));

				// ID3v2 Tag Written

				return;
			}
		}

		// create new tag with padding
		// System.err.println("Creating a new ID3v2 Tag");
		fc.position(oldTagSize);

		if (fc.size() > 15 * 1024 * 1024) {
			FileChannel tempFC = tempRaf.getChannel();

			tempFC.position(0);
			tempFC.write(tc.convert(tag, Id3v2TagCreator.DEFAULT_PADDING));
			tempFC.transferFrom(fc, tempFC.position(), fc.size() - oldTagSize);

			fc.close();
		} else {
			ByteBuffer[] content = new ByteBuffer[2];

			content[1] = ByteBuffer.allocate((int) fc.size());
			fc.read(content[1]);
			content[1].rewind();
			content[0] = tc.convert(tag, Id3v2TagCreator.DEFAULT_PADDING);
			fc.position(0);
			fc.write(content);
		}
	}
}
