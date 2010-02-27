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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

import entagged.audioformats.exceptions.CannotReadException;
import entagged.audioformats.mp3.Id3v2Tag;

/**
 * This class is more a dispatching unit for selecting the right reader based on
 * the found ID3V2 minor version.<br>
 * 
 * @author Rapha?l Slinckx
 */
public class Id3v2TagReader {

	/**
	 * The current flags of the ID3V2 tag header.<br>
	 * By apperance:<br>
	 * <ol>
	 * <li> Unsynchronisation </li>
	 * <li> Extended Header present </li>
	 * <li> Experimental Indicator </li>
	 * <li> Footer Present </li>
	 * </ol>
	 */
	private boolean[] ID3Flags;

	/**
	 * This field stores the class which performs the reversion of the
	 * unsynchronisation process.<br>
	 */
	private final Id3v2TagSynchronizer synchronizer = new Id3v2TagSynchronizer();

	private final Id3v24TagReader tagReader = new Id3v24TagReader();

	/**
	 * Converts a byte representing a series of Flags to an array of booleans
	 * for an easier handling
	 * 
	 * @param b
	 *            A byte containing ID3 Flags
	 * @return An array of booleans reflecting the state of each bit in the byte
	 *         representing the ID3 Flags
	 */
	private boolean[] processID3Flags(byte b) {
		boolean[] flags;

		if (b != 0) {
			flags = new boolean[4];

			int flag = b & 128;

			if (flag == 128)
				flags[0] = true;
			else
				flags[0] = false; // unsynchronisation
			flag = b & 64;
			if (flag == 64)
				flags[1] = true;
			else
				flags[1] = false; // Extended Header
			flag = b & 32;
			if (flag == 32)
				flags[2] = true;
			else
				flags[2] = false; // Experimental Indicator
			flag = b & 16;
			if (flag == 16)
				flags[3] = true;
			else
				flags[3] = false; // Footer Present
		} else {
			flags = new boolean[4];
			flags[0] = false;
			flags[1] = false;
			flags[2] = false;
			flags[3] = false;
		}
		// System.err.println("Flags:unsynchronisation "+flags[0]+"Extended
		// Header"+flags[1]);
		return flags;
	}

	/**
	 * This method reads the given file and searches for an ID3V2 Tag. If it is
	 * found and there is a reader for that version, a ID3V2 tag will be created
	 * upon the contents.<br>
	 * <b>Hint for developers:</b><br>
	 * Since {@link #ID3Flags} is an instance variable of the reader and the
	 * array is passed without copying, this method must remain
	 * &quot;synchronized&quot; so no two threads would change this field of the
	 * current reader instance.
	 * 
	 * @param raf
	 *            The mp3 file containing the ID3V2 Tag.
	 * @return an ID3V2 tag representation.
	 * @throws CannotReadException
	 *             If the file doesn't contain <b>valid</b> ID3V2 tag data.
	 * @throws IOException
	 *             On I/O errors.
	 */
	public synchronized Id3v2Tag read(RandomAccessFile raf)
			throws CannotReadException, IOException {
		// Create the Result object
		Id3v2Tag tag = null;

		// Buffer for Identifying the ID3 signature.
		byte[] b = new byte[3];
		raf.read(b);

		// Convert byte data into String
		String ID3 = new String(b);

		// Check signature, if not "ID3" the file does not contain a ID3V2 tag
		// at all or the specification of ID3.org wasn't respected.
		if (!ID3.equals("ID3")) {
			throw new CannotReadException("Not an ID3 tag");
		}
		// Begins tag parsing --------------------------------------------------

		// ---------------------------------------------------------------------
		// Version of tag ID3v2.xx.xx
		String versionHigh = String.valueOf(raf.read());
		String versionID3 = versionHigh + "." + raf.read();

		// parsing the ID3V2 tag header flags.
		ID3Flags = processID3Flags(raf.readByte());
		// ---------------------------------------------------------------------
		// Read the tagsize from header, which is a sync safe integer
		int tagSize = readSyncsafeInteger(raf);

		// ---------------------------------------------------------------------
		// Fill a byte buffer, then process according to correct version
		b = new byte[tagSize + 2];
//		ByteBuffer bb = ByteBuffer.allocateDirect(tagSize+2);
		raf.readFully(b);
		ByteBuffer bb = ByteBuffer.wrap(b);
//		bb.put(b);
//		bb.position(0);
//		raf.readFully(b);
		
//		 MappedByteBuffer buffer = raf.getChannel().map(MapMode.READ_ONLY,
//				raf.getFilePointer(), tagSize + 1);
//		 buffer.load();
//		ByteBuffer bb = buffer;
//		 ByteBuffer bb = ByteBuffer.wrap(b);

		if (ID3Flags[0] == true) {
			// We have unsynchronization, first re-synchronize
			bb = synchronizer.synchronize(bb);
		}

		// Up to now we use the same reader for all versions and pass a flag
		if (versionHigh.equals("2")) {
			tag = tagReader.read(bb, ID3Flags, Id3v2Tag.ID3V22);
		} else if (versionHigh.equals("3")) {
			tag = tagReader.read(bb, ID3Flags, Id3v2Tag.ID3V23);
		} else if (versionHigh.equals("4")) {
			tag = tagReader.read(bb, ID3Flags, Id3v2Tag.ID3V24);
		} else {
			/*
			 * The implementation of entagges ID3V2 tag parsing does not ignore
			 * unknown ID3V2 tag definitions, so we must throw an error.
			 */
			throw new CannotReadException("ID3v2 tag version " + versionID3
					+ " not supported !");
		}
		return tag;
	}

	/**
	 * Take the first four bytes from the provided MBB and convert them into an
	 * int, by treating them as a sync. safe integer.
	 * 
	 * @param raf
	 *            A MappedByteBuffer containing the bytes to be dealt with.
	 * @return The integer value represented by the bytes.
	 * @exception IOException
	 *                IO Error
	 */
	private int readSyncsafeInteger(RandomAccessFile raf) throws IOException {
		int value = 0;

		value += (raf.read() & 0xFF) << 21;
		value += (raf.read() & 0xFF) << 14;
		value += (raf.read() & 0xFF) << 7;
		value += raf.read() & 0xFF;

		return value;
	}
}
