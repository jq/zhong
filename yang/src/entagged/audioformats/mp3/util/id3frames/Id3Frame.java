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
package entagged.audioformats.mp3.util.id3frames;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import entagged.audioformats.generic.TagField;
import entagged.audioformats.mp3.Id3v2Tag;
import entagged.audioformats.mp3.util.Id3v2TagCreator;

public abstract class Id3Frame implements TagField {
	protected byte[] flags;

	protected byte version;

	public Id3Frame() {
		this.version = Id3v2Tag.ID3V23;
		createDefaultFlags();
	}

	public Id3Frame(byte[] raw, byte version)
			throws UnsupportedEncodingException {
		byte[] rawNew;
		if (version == Id3v2Tag.ID3V23 || version == Id3v2Tag.ID3V24) {
			byte size = 2;

			if ((raw[1] & 0x80) == 0x80) {
				// Compression zlib, 4 bytes uncompressed size.
				size += 4;
			}

			if ((raw[1] & 0x80) == 0x40) {
				// Encryption method byte
				size += 1;
			}

			if ((raw[1] & 0x80) == 0x20) {
				// Group identity byte
				size += 1;
			}

			this.flags = new byte[size];
			for (int i = 0; i < size; i++)
				this.flags[i] = raw[i];
			rawNew = raw;
		} else {
			createDefaultFlags();
			rawNew = new byte[this.flags.length + raw.length];
			copy(this.flags, rawNew, 0);
			copy(raw, rawNew, this.flags.length);
		}

		this.version = version;

		populate(rawNew);
	}

	protected abstract byte[] build() throws UnsupportedEncodingException;

	/**
	 * (overridden)
	 * 
	 * @see java.lang.Object#clone()
	 */
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	protected void copy(byte[] src, byte[] dst, int dstOffset) {
		for (int i = 0; i < src.length; i++)
			dst[i + dstOffset] = src[i];
	}

	private void createDefaultFlags() {
		this.flags = new byte[2];
		this.flags[0] = 0;
		this.flags[1] = 0;
	}

	/**
	 * (overridden) For Id3Frame objects the comparison can be easily done by
	 * comparing their binary representation which can be retrieved by invoking
	 * {@link Id3Frame#build()}.<br>
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof Id3Frame) {
			Id3Frame other = (Id3Frame) obj;
			try {
				return Arrays.equals(this.build(), other.build());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	protected byte[] getBytes(String s, String encoding)
			throws UnsupportedEncodingException {
		byte[] result = null;
		if ("UTF-16".equalsIgnoreCase(encoding)) {
			result = s.getBytes("UTF-16LE");
			// 2 for BOM and 2 for terminal character
			byte[] tmp = new byte[result.length + 4];
			System.arraycopy(result, 0, tmp, 2, result.length);
			// Create the BOM
			tmp[0] = (byte) 0xFF;
			tmp[1] = (byte) 0xFE;
			result = tmp;
		} else {
			// this is encoding ISO-8859-1, for the time of this change.
			result = s.getBytes(encoding);
			int zeroTerm = 1;
			if ("UTF-16BE".equals(encoding)) {
				zeroTerm = 2;
			}
			byte[] tmp = new byte[result.length + zeroTerm];
			System.arraycopy(result, 0, tmp, 0, result.length);
			result = tmp;
		}
		return result;
	}

	public byte[] getFlags() {
		return this.flags;
	}

	public abstract String getId();

	protected byte[] getIdBytes() {
		return getId().getBytes();
	}

	public byte[] getRawContent() throws UnsupportedEncodingException {
		return build();
	}

	protected byte[] getSize(int size) {
		byte[] b = null;
		if (this.version == Id3v2Tag.ID3V24) {
			b = Id3v2TagCreator.getSyncSafe(size);
		} else {
			b = new byte[4];
			b[0] = (byte) ((size >> 24) & 0xFF);
			b[1] = (byte) ((size >> 16) & 0xFF);
			b[2] = (byte) ((size >> 8) & 0xFF);
			b[3] = (byte) (size & 0xFF);
		}
		return b;
	}

	protected String getString(byte[] b, int offset, int length, String encoding)
			throws UnsupportedEncodingException {
		String result = null;
		if ("UTF-16".equalsIgnoreCase(encoding)) {
			int zerochars = 0;
			// do we have zero terminating chars (old entagged did not)
			if (b[offset + length - 2] == 0x00
					&& b[offset + length - 1] == 0x00) {
				zerochars = 2;
			}
			if (b[offset] == (byte) 0xFE && b[offset + 1] == (byte) 0xFF) {
				result = new String(b, offset + 2, length - 2 - zerochars,
						"UTF-16BE");
			} else if (b[offset] == (byte) 0xFF && b[offset + 1] == (byte) 0xFE) {
				result = new String(b, offset + 2, length - 2 - zerochars,
						"UTF-16LE");
			} else {
				/*
				 * Now we have a little problem. The tag is not id3-spec
				 * conform. And since I don't have a way to see if its little or
				 * big endian, i decide for the windows default little endian.
				 */
				result = new String(b, offset, length - zerochars, "UTF-16LE");
			}
		} else {
			int zerochars = 0;
			if ("UTF-16BE".equals(encoding)) {
				if (b[offset + length - 2] == 0x00
						&& b[offset + length - 1] == 0x00) {
					zerochars = 2;
				}
			} else if (b[offset + length - 1] == 0x00) {
				zerochars = 1;
			}
			if (length == 0 || offset + length > b.length) {
				result = "";
			} else {
				result = new String(b, offset, length - zerochars, encoding);
			}
		}
		return result;
	}

	protected int indexOfFirstNull(byte[] b, int offset) {
		for (int i = offset; i < b.length; i++)
			if (b[i] == 0)
				return i;
		return -1;
	}

	public abstract boolean isBinary();

	public void isBinary(boolean b) { /* Never can choose if binary or not */
	}

	public abstract boolean isCommon();

	protected abstract void populate(byte[] raw)
			throws UnsupportedEncodingException;
}
