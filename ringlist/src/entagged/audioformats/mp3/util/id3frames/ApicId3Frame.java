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

import entagged.audioformats.generic.TagField;

public class ApicId3Frame extends TextId3Frame {

	private byte[] data;

	private String mime;

	private byte pictureType;

	private boolean unsupportedState = false;

	public ApicId3Frame(byte[] rawContent, byte version)
			throws UnsupportedEncodingException {
		super("APIC", rawContent, version);
	}

	public ApicId3Frame(String description, String mime, byte pictureType,
			byte[] data) {
		super("APIC", description);

		this.mime = mime;
		this.pictureType = pictureType;
		this.data = data;
	}

	protected byte[] build() throws UnsupportedEncodingException {
		if (unsupportedState) {
			return this.data;
		}
		byte[] contentB = getBytes(this.content, getEncoding());
		byte[] mimeB = getBytes(this.mime, "ISO-8859-1");

		byte[] b = new byte[4 + 4 + flags.length + 1 + mimeB.length + 1
				+ contentB.length + data.length];

		int offset = 0;
		copy(getIdBytes(), b, offset);
		offset += 4;
		copy(getSize(b.length - 10), b, offset);
		offset += 4;
		copy(flags, b, offset);
		offset += flags.length;

		b[offset] = this.encoding;
		offset += 1;

		copy(mimeB, b, offset);
		offset += mimeB.length;

		b[offset] = this.pictureType;
		offset += 1;

		copy(contentB, b, offset);
		offset += contentB.length;
		copy(data, b, offset);
		offset += data.length;

		return b;
	}

	public void copyContent(TagField field) {
		super.copyContent(field);

		if (field instanceof ApicId3Frame) {
			if (!((ApicId3Frame) field).unsupportedState) {
				this.mime = ((ApicId3Frame) field).getMimeType();
				this.pictureType = ((ApicId3Frame) field).getPictureType();
				this.data = ((ApicId3Frame) field).getData();
			} else {
				this.data = ((ApicId3Frame) field).data;
				this.unsupportedState = true;
			}
		}
	}

	public byte[] getData() {
		return data;
	}

	public String getMimeType() {
		return mime;
	}

	public byte getPictureType() {
		return pictureType;
	}

	public String getPictureTypeAsString() {
		switch (pictureType & 0xFF) {
		case 0x00:
			return "Other";
		case 0x01:
			return "32x32 pixels file icon";
		case 0x02:
			return "Other file icon";
		case 0x03:
			return "Cover (front)";
		case 0x04:
			return "Cover (back)";
		case 0x05:
			return "Leaflet page";
		case 0x06:
			return "Media (e.g. lable side of CD)";
		case 0x07:
			return "Lead artist/lead performer/soloist";
		case 0x08:
			return "Artist/performer";
		case 0x09:
			return "Conductor";
		case 0x0A:
			return "Band/Orchestra";
		case 0x0B:
			return "Composer";
		case 0x0C:
			return "Lyricist/text writer";
		case 0x0D:
			return "Recording Location";
		case 0x0E:
			return "During recording";
		case 0x0F:
			return "During performance";
		case 0x10:
			return "Movie/video screen capture";
		case 0x11:
			return "A bright coloured fish";
		case 0x12:
			return "Illustration";
		case 0x13:
			return "Band/artist logotype";
		case 0x14:
			return "Publisher/Studio logotype";
		}

		return "Unknown";
	}

	public boolean isBinary() {
		return true;
	}

	public boolean isEmpty() {
		return super.isEmpty() && data.length == 0 && mime.equals("");
	}

	/*
	 * Text encoding $xx MIME type <text string> $00 Picture type $xx
	 * Description <text string according to encoding> $00 (00) Picture data
	 * <binary data>
	 */

	protected void populate(byte[] raw) throws UnsupportedEncodingException {
		// Will create empty data, because there are multiple conditions under
		// which
		// no data is available. Hoewever the implementation of this class
		// relies on the
		// fact, that data is never null
		this.data = new byte[0];
		this.encoding = raw[flags.length];
		if (this.encoding < 0 || this.encoding > 3)
			this.encoding = 0;

		int offset = indexOfFirstNull(raw, flags.length + 1);
		this.mime = getString(raw, flags.length + 1, offset - flags.length - 1,
				"ISO-8859-1");
		if (this.mime == null) {
			// Set state to Invalid
			return;
		}
		if (this.mime.trim().equals("-->")) {
			// Now the Picture Data represents a URL to the real picutre
			// For now unsupported
			unsupportedState = true;
			this.data = raw;
			return;
		}
		this.pictureType = raw[offset + 1];

		int nextoffset = indexOfFirstNull(raw, offset + 2);
		this.content = getString(raw, offset + 2, nextoffset - offset - 2,
				getEncoding());

		// Again the encoding
		if (this.encoding == 2 || this.encoding == 3) {
			nextoffset++;
		}
		nextoffset++;

		if (raw.length > nextoffset) {
			this.data = new byte[raw.length - nextoffset];
			System.arraycopy(raw, nextoffset, data, 0, data.length);
		} else {
			System.err
					.println("ApicId3Frame-> No space for picture data left.");
		}
	}

	public String toString() {
		return "[" + mime + " (" + getPictureTypeAsString() + ")] "
				+ super.toString();
	}
}
