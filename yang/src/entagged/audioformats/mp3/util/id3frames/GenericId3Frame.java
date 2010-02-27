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

public class GenericId3Frame extends Id3Frame {
	
	private byte[] data;
	private String id;
	
	/*
	 * 0,1| frame flags
	 * 2,..,0X00| Owner ID
	 * xx,...| identifier (binary)
	 */
	
	public GenericId3Frame(String id, byte[] raw, byte version) throws UnsupportedEncodingException {
		super(raw, version);
		this.id = id;
	}
	
	public String getId() {
		return this.id;
	}
	
	public boolean isBinary() {
		return true;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public boolean isCommon() {
		return false;
	}
	
	public void copyContent(TagField field) {
	    if(field instanceof GenericId3Frame)
	        this.data = ((GenericId3Frame)field).getData();
	}
	
	public boolean isEmpty() {
	    return this.data.length == 0;
	}
	
	protected void populate(byte[] raw) {
		this.data = new byte[raw.length - flags.length];
		for(int i = 0; i<data.length; i++)
			data[i] = raw[i + flags.length];
	}
	
	protected byte[] build() {
		byte[] b = new byte[4 + 4 + data.length + flags.length];
		
		int offset = 0;
		copy(getIdBytes(), b, offset);        offset += 4;
		copy(getSize(b.length-10), b, offset); offset += 4;
		copy(flags, b, offset);               offset += flags.length;
		
		copy(data, b, offset);
		
		return b;
	}
	
	public String toString() {
		return this.id+" : No associated view";
	}
	
	/*
	public static void main(String[] args) throws Exception {
		byte[][] content = {
				{0x00, 0x00, 'T','e','T',0x00,0x01,0x02},
				{0x00, 0x00, 'T','e','T',0x00}
		};
		
		for(int i = 0; i<content.length; i++) {
		    GenericId3Frame t = new GenericId3Frame("APIC",content[i], Id3v2Tag.ID3V23);
			System.out.println("-------");
			System.out.println(t.isBinary());
			byte[] id = t.getData();
			for(int j = 0; j<id.length; j++)
				System.out.print(Integer.toHexString(id[j]&0xFF)+"|");
			System.out.println();
			System.out.println(t.getId());
			
			id = t.build();
			for(int j = 0; j<id.length; j++)
				System.out.print(Integer.toHexString(id[j]&0xFF)+"|");
		}
	}
	*/
}
