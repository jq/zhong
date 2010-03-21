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
import java.util.Locale;

import entagged.audioformats.generic.TagField;
import entagged.audioformats.generic.TagTextField;

public class CommId3Frame extends TextId3Frame implements TagTextField {

	private String shortDesc;

	private String lang;

	/*
	 * 0,1| frame flags 2| encoding 3,4,5| lang 6,..,0x00(0x00)| short descr
	 * x,..| actual comment
	 */

	public CommId3Frame(String content) {
		super("COMM", content);

		this.shortDesc = "";
		// this.lang = "eng";
		this.lang = Locale.getDefault().getISO3Language();
	}

	public CommId3Frame(byte[] rawContent, byte version)
			throws UnsupportedEncodingException {
		super("COMM", rawContent, version);
	}

	public String getLangage() {
		return this.lang;
	}

	protected void populate(byte[] raw) throws UnsupportedEncodingException {
		this.encoding = raw[flags.length];
		if (flags.length + 1 + 3 > raw.length - 1) {
			this.lang = "XXX";
			content = "";
			shortDesc = "";
			return;
		}
		this.lang = new String(raw, flags.length + 1, 3);

		int commentStart = getCommentStart(raw, flags.length + 4, getEncoding());

		this.shortDesc = getString(raw, flags.length + 4, commentStart
				- flags.length - 4, getEncoding());
		this.content = getString(raw, commentStart, raw.length - commentStart,
				getEncoding());
		assert lang != null && this.shortDesc != null && this.content != null;
	}

	/**
	 * This methods interprets content to be a valid comment section. where
	 * first comes a short comment directly after that the comment section. This
	 * method searches for the terminal character of the short description, and
	 * return the index of the first byte of the fulltext comment.
	 * 
	 * @param content
	 *            The comment data.
	 * @param offset
	 *            The offset where the short descriptions is about to start.
	 * @param encoding
	 *            the encoding of the field.
	 * @return the index (including given offset) for the first byte of the
	 *         fulltext commen.
	 */
	public int getCommentStart(byte[] content, int offset, String encoding) {
		int result = 0;
		if ("UTF-16".equals(encoding)) {
			for (result = offset; result < content.length; result += 2) {
				if (content[result] == 0x00 && content[result + 1] == 0x00) {
					result += 2;
					break;
				}
			}
		} else {
			for (result = offset; result < content.length; result++) {
				if (content[result] == 0x00) {
					result++;
					break;
				}
			}
		}
		return result;
	}

	protected byte[] build() throws UnsupportedEncodingException {
		byte[] shortDescData = getBytes(this.shortDesc, getEncoding());
		byte[] contentData = getBytes(this.content, getEncoding());
		byte[] data = new byte[shortDescData.length + contentData.length];
		System.arraycopy(shortDescData, 0, data, 0, shortDescData.length);
		System.arraycopy(contentData, 0, data, shortDescData.length,
				contentData.length);
		byte[] lan = this.lang.getBytes();

		// the return byte[]
		byte[] b = new byte[4 + 4 + flags.length + 1 + 3 + data.length];

		int offset = 0;
		copy(getIdBytes(), b, offset);
		offset += 4;
		copy(getSize(b.length - 10), b, offset);
		offset += 4;
		copy(flags, b, offset);
		offset += flags.length;

		b[offset] = this.encoding;
		offset += 1;

		copy(lan, b, offset);
		offset += lan.length;

		copy(data, b, offset);

		return b;
	}

	public String getShortDescription() {
		return shortDesc;
	}

	public boolean isEmpty() {
		return this.content.equals("") && this.shortDesc.equals("");
	}

	public void copyContent(TagField field) {
		super.copyContent(field);
		if (field instanceof CommId3Frame) {
			this.shortDesc = ((CommId3Frame) field).getShortDescription();
			this.lang = ((CommId3Frame) field).getLangage();
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("[").append(getLangage()).append("] ").append("(").append(
				getShortDescription()).append(") ").append(getContent());
		return sb.toString();
	}

	/*
	 * public static void main(String[] args) throws Exception { byte[][]
	 * content = { {0x00, 0x00, 0x00, 'e','n','g', 'S','h',0x00,'C','o'}, {0x00,
	 * 0x00, 0x00, 'e','n','g', 'S','h',0x00}, {0x00, 0x00, 0x00, 'e','n','g',
	 * 'S','h'}, {0x00, 0x00, 0x01, 'e','n','g', (byte)0xFE,
	 * (byte)0xFF,0x00,'S',0x00,'h',0x00,0x00,0x00,'T'}, };
	 * 
	 * for(int i = 0; i<content.length; i++) { CommId3Frame t = new
	 * CommId3Frame(content[i], Id3v2Tag.ID3V23); System.out.println("-------");
	 * System.out.println(t.isBinary()); System.out.println(t.getContent());
	 * System.out.println(t.getLangage());
	 * System.out.println(t.getShortDescription());
	 * System.out.println(t.getEncoding()); System.out.println(t.getId());
	 * 
	 * 
	 * byte[] bytes = t.build(); for(int j = 0; j<bytes.length; j++)
	 * System.out.print(Integer.toHexString(bytes[j]&0xFF)+"|"); } }
	 */
}
