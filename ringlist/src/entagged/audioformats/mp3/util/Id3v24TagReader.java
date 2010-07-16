/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Rapha?l Slinckx <raphael@slinckx.net>
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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Hashtable;

import entagged.audioformats.mp3.Id3v2Tag;
import entagged.audioformats.mp3.util.id3frames.ApicId3Frame;
import entagged.audioformats.mp3.util.id3frames.CommId3Frame;
import entagged.audioformats.mp3.util.id3frames.GenericId3Frame;
import entagged.audioformats.mp3.util.id3frames.Id3Frame;
import entagged.audioformats.mp3.util.id3frames.TextId3Frame;
import entagged.audioformats.mp3.util.id3frames.TimeId3Frame;
import entagged.audioformats.mp3.util.id3frames.UfidId3Frame;

/**
 * This class parses an ID3V2 tag from a given {@link java.nio.ByteBuffer}.<br>
 * It handles the versions 2,3 and 4.
 * 
 * @author Rapha?l Slinckx , Christian Laireiter
 */
public class Id3v24TagReader {

	/**
	 * This field maps the field names of the version 2 frames to the one of
	 * version 3.<br>
	 */
	private Hashtable conversion22to23;

	/**
	 * Creates an instance.
	 * 
	 */
	public Id3v24TagReader() {
		initConversionTable();
	}

	private String convertFromId3v22(String field) {
		String s = (String) this.conversion22to23.get(field);

		if (s == null)
			return "";

		return s;
	}

	private Id3Frame createId3Frame(String field, byte[] data, byte version)
			throws UnsupportedEncodingException {
		if (version == Id3v2Tag.ID3V22)
			field = convertFromId3v22(field);

		// Text frames
		if (field.startsWith("T") && !field.startsWith("TX")) {
			if (field.equalsIgnoreCase("TDRC")) {
				return new TimeId3Frame(field, data, version);
			}
			return new TextId3Frame(field, data, version);
		}
		// Comment
		else if (field.startsWith("COMM"))
			return new CommId3Frame(data, version);
		// Universal file id
		else if (field.startsWith("UFID"))
			return new UfidId3Frame(data, version);
		else if (field.startsWith("APIC"))
			return new ApicId3Frame(data, version);
		// Any other frame
		else
			return new GenericId3Frame(field, data, version);
	}

	/**
	 * This Method fills {@link #conversion}.
	 * 
	 */
	private void initConversionTable() {

		// TODO: APIC frame must update the mime-type to be converted ??
		// TODO: LINK frame (2.3) has a frame ID of 3-bytes making it
		// incompatible with 2.3 frame ID of 4bytes, WTF???

		this.conversion22to23 = new Hashtable(100);
		String[] v22 = { "BUF", "CNT", "COM", "CRA", "CRM", "ETC", "EQU",
				"GEO", "IPL", "LNK", "MCI", "MLL", "PIC", "POP", "REV", "RVA",
				"SLT", "STC", "TAL", "TBP", "TCM", "TCO", "TCR", "TDA", "TDY",
				"TEN", "TFT", "TIM", "TKE", "TLA", "TLE", "TMT", "TOA", "TOF",
				"TOL", "TOR", "TOT", "TP1", "TP2", "TP3", "TP4", "TPA", "TPB",
				"TRC", "TRD", "TRK", "TSI", "TSS", "TT1", "TT2", "TT3", "TXT",
				"TXX", "TYE", "UFI", "ULT", "WAF", "WAR", "WAS", "WCM", "WCP",
				"WPB", "WXX" };
		String[] v23 = { "RBUF", "PCNT", "COMM", "AENC", "", "ETCO", "EQUA",
				"GEOB", "IPLS", "LINK", "MCDI", "MLLT", "APIC", "POPM", "RVRB",
				"RVAD", "SYLT", "SYTC", "TALB", "TBPM", "TCOM", "TCON", "TCOP",
				"TDAT", "TDLY", "TENC", "TFLT", "TIME", "TKEY", "TLAN", "TLEN",
				"TMED", "TOPE", "TOFN", "TOLY", "TORY", "TOAL", "TPE1", "TPE2",
				"TPE3", "TPE4", "TPOS", "TPUB", "TSRC", "TRDA", "TRCK", "TSIZ",
				"TSSE", "TIT1", "TIT2", "TIT3", "TEXT", "TXXX", "TYER", "UFID",
				"USLT", "WOAF", "WOAR", "WOAS", "WCOM", "WCOP", "WPUB", "WXXX" };

		for (int i = 0; i < v22.length; i++) {
			this.conversion22to23.put(v22[i], v23[i]);
		}
	}

	/**
	 * This method is used to skip the extended header in the reading process.
	 * 
	 * @param data
	 *            the buffer containing the extended header. (at current
	 *            location)
	 * @param version
	 *            the ID3V2 version {@link Id3v2Tag#ID3V22}.<br>
	 * @return the size of the extended Header. (skipping already performed)
	 */
	private int processExtendedHeader(ByteBuffer data, byte version) {
		// TODO Verify that we have an syncsfe int
		int extsize = 0;
		byte[] exthead = new byte[4];
		data.get(exthead);
		if (version == Id3v2Tag.ID3V23) {
			extsize = readSize(data, Id3v2Tag.ID3V23);
			// The extended header size excludes those first four bytes.
			data.position(data.position() + (extsize));
		} else {
			extsize = readSyncsafeInteger(data);
			data.position(data.position() + (extsize));
		}
		return extsize;
	}

	/**
	 * This method reads an ID3V2 tag from the given {@link ByteBuffer} at its
	 * curren pointer location.<br>
	 * 
	 * @param data
	 *            ID3V2 tag.
	 * @param ID3Flags
	 *            The flags of the tag header.
	 * @param version
	 *            Version Flag. (used to handle some version specific
	 *            implementations).
	 * @return An ID3V2 tag representation.
	 * @throws UnsupportedEncodingException
	 *             Thrown on charset conversions, if system does not support
	 *             them.
	 */
	public Id3v2Tag read(ByteBuffer data, boolean[] ID3Flags, byte version)
			throws UnsupportedEncodingException {
		// get the tagsize from the buffers size.
		int tagSize = data.limit();
		byte[] b;
		// Create a result object
		Id3v2Tag tag = new Id3v2Tag();
		// ---------------------------------------------------------------------
		// If the flags indicate an extended header to be present, read its
		// size and skip it. (It does not contain any useful information, maybe
		// CRC)
		if ((version == Id3v2Tag.ID3V23 || version == Id3v2Tag.ID3V24)
				&& ID3Flags[1]) {
			processExtendedHeader(data, version);
		}
		// ---------------------------------------------------------------------
		/*
		 * Now start the extraction of the text frames.
		 */
		// The frame names differ in lengths between version 2 to 3
		int specSize = (version == Id3v2Tag.ID3V22) ? 3 : 4;
		// As long as we have unread bytes...
		for (int a = 0; a < tagSize; a++) {
			// Create buffer taking the name of the frame.
			b = new byte[specSize];

			// Do we still have enough bytes for reading the name?
			if (data.remaining() <= specSize)
				break;

			// Read the Name
			data.get(b);

			// Convert the bytes (of the name) into a String.
			String field = new String(b);
			// If byte[0] is zero, we have invalid data
			if (b[0] == 0)
				break;

			// Now we read the length of the current frame
			int frameSize = readSize(data, version);

			// If the framesize is greater than the bytes we've left to read,
			// or the frame length is zero, abort. Invalid data
			if ((frameSize > data.remaining()) || frameSize <= 0) {
				// ignore empty frames
				System.err.println(field
						+ " Frame size error, skiping the rest of the tag:"
						+ frameSize);
				break;
			}

			b = new byte[frameSize
					+ ((version == Id3v2Tag.ID3V23 || version == Id3v2Tag.ID3V24) ? 2
							: 0)];
			// Read the complete frame into the byte array.
			data.get(b);

			// Check the frame name once more
			if (!"".equals(field)) {
				Id3Frame f = null;
				/*
				 * Now catch possible errors occuring in the data
				 * interpretation. Even if a frame is not valid regarding the
				 * spec, the rest of the tag could be read.
				 */
				try {
					// Create the Frame upon the byte array data.
					f = createId3Frame(field, b, version);
				} catch (UnsupportedEncodingException uee) {
					throw uee;
				} catch (Exception e) {
					e.printStackTrace();
				}
				// If the frame was successfully parsed, add it to the tag.
				if (f != null)
					tag.add(f);
			}
		}

		return tag;
	}

	/**
	 * This mehtod reads the data of an integer out of the given buffer.<br>
	 * Since different version of ID3V2 tags have a different size definiton,
	 * the version is needed.
	 * 
	 * @param bb
	 *            The buffer containing the integer.
	 * @param version
	 *            The ID3V2 version. {@link Id3v2Tag#ID3V22}.
	 * @return The integer value
	 */
	private int readSize(ByteBuffer bb, int version) {
		int value = 0;
		if (version == Id3v2Tag.ID3V24) {
			value = readSyncsafeInteger(bb);
		} else {
			if (version == Id3v2Tag.ID3V23)
				value += (bb.get() & 0xFF) << 24;
			value += (bb.get() & 0xFF) << 16;
			value += (bb.get() & 0xFF) << 8;
			value += (bb.get() & 0xFF);
		}
		return value;
	}

	/**
	 * This method reads the next 4 byte of the buffer and interprets them as a
	 * sync safe integer.<br>
	 * 
	 * @param buffer
	 *            Buffer to read from
	 * @return represented integer value.
	 */
	private int readSyncsafeInteger(ByteBuffer buffer) {
		int value = 0;
		value += (buffer.get() & 0xFF) << 21;
		value += (buffer.get() & 0xFF) << 14;
		value += (buffer.get() & 0xFF) << 7;
		value += buffer.get() & 0xFF;
		return value;
	}
}
