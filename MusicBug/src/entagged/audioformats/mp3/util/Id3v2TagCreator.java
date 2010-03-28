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

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import entagged.audioformats.Tag;
import entagged.audioformats.generic.AbstractTagCreator;
import entagged.audioformats.mp3.Id3v2Tag;

public class Id3v2TagCreator extends AbstractTagCreator {
	/**
	 * Default Padding size, when the tag is created from scratch (the old
	 * cannot be replaced)
	 */
	public final static int DEFAULT_PADDING = 4000;

	/**
	 * This method will return a so called syncsafe integer with the value of
	 * the given <code>tempSize</code>.<br>
	 * 
	 * @param value
	 *            The value to be converted.
	 * @return A SyncSafe integer as 32-bit byte array.
	 */
	public static byte[] getSyncSafe(int value) {
		assert value >= 0;
		byte[] result = new byte[4];
		for (int i = 0; i < 4; i++)
			result[i] = (byte) (value >> ((3 - i) * 7) & 0x7f);
		assert ((result[0] & 0x80) == 0) && ((result[1] & 0x80) == 0)
				&& ((result[2] & 0x80) == 0) && ((result[3] & 0x80) == 0);
		return result;
	}

	public void create(Tag tag, ByteBuffer buf, List fields, int tagSize,
			int paddingSize) {
		byte[] b;

		// ID3------------------
		buf.put((byte) 73).put((byte) 68).put((byte) 51); // ID3
		// ----------------------------------------------------------------------------
		// When generation new tag
		// Version of tag ID3v2.xx.xx
		buf.put((byte) 4);
		buf.put((byte) 0);
		// ----------------------------------------------------------------------------
		// Create certain flags
		boolean[] ID3Flags = new boolean[4];

		ID3Flags[0] = false; // unsyncronization is not done
		ID3Flags[1] = false; // Extended header is useless
		ID3Flags[2] = false;
		ID3Flags[3] = false;
		buf.put(createID3Flags(ID3Flags));
		// ----------------------------------------------------------------------------
		// On ecrit la taille du nouveau tag ID3
		// Tag length
		int tempSize = (tagSize - 10) + paddingSize;
		// Here we need to use sync-safe integers, so conversion must be done
		buf.put(getSyncSafe(tempSize));
		// ----------------------------------------------------------------------------
		// Ecriture de l'Header Etendu si le flag est==true (A COMPLETER)
		if (ID3Flags[1] == true) {
			// Create the ID3 Extended Header (not used)
			// This may need to be implemented
			// createExtendedHeader();
		}
		// ----------------------------------------------------------------------------
		// Ecriture des champs de texte
		Iterator it = fields.iterator();
		while (it.hasNext()) {
			buf.put((byte[]) it.next());
		}

		// Fill the rest with \0 (padding)
		for (int i = 0; i < paddingSize; i++)
			buf.put((byte) 0);
	}

	// Create a byte representing ID3 Flags using the given array
	private byte createID3Flags(boolean[] flag) {
		byte b = 0;

		if (flag[0] == true)
			b += 128;
		if (flag[1] == true)
			b += 64;
		if (flag[2] == true)
			b += 32;
		if (flag[3] == true)
			b += 16;
		return b;
	}

	protected Tag getCompatibleTag(Tag tag) {
		if (!(tag instanceof Id3v2Tag)) {
			Id3v2Tag id3Tag = new Id3v2Tag();
			id3Tag.merge(tag);
			return id3Tag;
		}
		return tag;
	}

	protected int getFixedTagLength(Tag tag) {
		return 10;
	}
}
