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
import entagged.audioformats.generic.TagTextField;
import entagged.audioformats.mp3.Id3v2Tag;

public class TextId3Frame extends Id3Frame implements TagTextField {

	protected String content;

	protected byte encoding;

	protected String id;

	protected boolean common;

	/*
	 * 0,1| frame flags 2| encoding 3,..,(0x00(0x00))| text content
	 */

	public TextId3Frame(String id, String content) {
		this.id = id;
		checkCommon();
		this.content = content;
		setEncoding(Id3v2Tag.DEFAULT_ENCODING);
	}

	public TextId3Frame(String id, byte[] rawContent, byte version)
			throws UnsupportedEncodingException {
		super(rawContent, version);
		this.id = id;
		checkCommon();
	}

	private void checkCommon() {
		// TODO on renaming time field, change this too
		this.common = id.equals("TIT2") || id.equals("TALB")
				|| id.equals("TPE1") || id.equals("TCON") || id.equals("TRCK")
				|| id.equals("TDRC") || id.equals("COMM");
	}

	public String getEncoding() {
		if (encoding == 0)
			return "ISO-8859-1";
		else if (encoding == 1)
			return "UTF-16";
		else if (encoding == 2)
			return "UTF-16BE";
		else if (encoding == 3) {
			return "UTF-8";
		}
		return "ISO-8859-1";
	}

	public void setEncoding(String enc) {
		if ("ISO-8859-1".equals(enc))
			encoding = 0;
		else if ("UTF-16".equals(enc))
			encoding = 1;
		else if ("UTF-16BE".equals(enc))
			encoding = 2;
		else if ("UTF-8".equals(enc))
			encoding = 3;
		else
			encoding = 1;
	}

	public String getContent() {
		return content;
	}

	public boolean isBinary() {
		return false;
	}

	public String getId() {
		return this.id;
	}

	public boolean isCommon() {
		return this.common;
	}

	public void setContent(String s) {
		this.content = s;
	}

	public boolean isEmpty() {
		return this.content.equals("");
	}

	public void copyContent(TagField field) {
		if (field instanceof TextId3Frame) {
			this.content = ((TextId3Frame) field).getContent();
			setEncoding(((TextId3Frame) field).getEncoding());
		}
	}

	protected void populate(byte[] raw) throws UnsupportedEncodingException {
		this.encoding = raw[flags.length];
		if (this.encoding != 0 && this.encoding != 1 && this.encoding != 2
				&& this.encoding != 3)
			this.encoding = 0;

		this.content = getString(raw, flags.length + 1, raw.length
				- flags.length - 1, getEncoding());

		int i = this.content.indexOf("\u0000");

		if (i != -1)
			this.content = this.content.substring(0, i);
	}

	protected byte[] build() throws UnsupportedEncodingException {
		byte[] data = getBytes(this.content, getEncoding());
		// the return byte[]
		byte[] b = new byte[4 + 4 + flags.length + 1 + data.length];

		int offset = 0;
		copy(getIdBytes(), b, offset);
		offset += 4;
		copy(getSize(b.length - 10), b, offset);
		offset += 4;
		copy(flags, b, offset);
		offset += flags.length;

		b[offset] = this.encoding;
		offset += 1;

		copy(data, b, offset);

		return b;
	}

	public String toString() {
		return getContent();
	}

	/*
	 * public static void main(String[] args) throws Exception { byte[] bytes =
	 * "TeT\u0000TeT".getBytes("ISO-8859-1"); for(int i = 0; i<bytes.length;
	 * i++) System.out.print(Integer.toHexString(bytes[i]&0xFF)+"|");
	 * System.out.println(); System.out.println("TeT\u0000TeT".length());
	 * System.out.println(new String(bytes,"ISO-8859-1").length());
	 * 
	 * System.out.println("".equals(null));
	 * 
	 * byte[][] content = { {0x00, 0x00, 0x00, 'T','e','T'}, {0x00, 0x00, 0x00,
	 * 'T','e','T',0x00}, {0x00, 0x00, 0x00, 'T','e','T',0x00,'T','e'}, {0x00,
	 * 0x00, 0x01, (byte)0xFE, (byte)0xFF,0x00,'T',0x00,'e',0x00,'T'}, {0x00,
	 * 0x00, 0x01, (byte)0xFE, (byte)0xFF,0x00,'T',0x00,'e',0x00,'T',0x00,0x00},
	 * {0x00, 0x00, 0x01, (byte)0xFE,
	 * (byte)0xFF,0x00,'T',0x00,'e',0x00,'T',0x00,0x00,0x00,'t'}, {0x00, 0x00,
	 * 0x01, (byte)0xFF, (byte)0xFE,'T',0x00,'e',0x00,'T',0x00}, {0x00, 0x00,
	 * 0x01, (byte)0xFF, (byte)0xFE,'T',0x00,'e',0x00,'T',0x00,0x00,0x00},
	 * {0x00, 0x00, 0x01, (byte)0xFF,
	 * (byte)0xFE,'T',0x00,'e',0x00,'T',0x00,0x00,0x00,'T',0x00} };
	 * 
	 * for(int i = 0; i<content.length; i++) { TextId3Frame t = new
	 * TextId3Frame("TALB", content[i], Id3v2Tag.ID3V23);
	 * System.out.println("-------"); System.out.println(t.isBinary());
	 * System.out.println(t.getContent()); System.out.println(t.getEncoding());
	 * System.out.println(t.getId());
	 * 
	 * 
	 * bytes = t.build(); for(int j = 0; j<bytes.length; j++)
	 * System.out.print(Integer.toHexString(bytes[j]&0xFF)+"|"); } }
	 */
}
