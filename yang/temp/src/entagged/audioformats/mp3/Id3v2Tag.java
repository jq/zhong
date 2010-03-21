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
package entagged.audioformats.mp3;

import java.util.List;
import java.util.Locale;

import entagged.audioformats.generic.AbstractTag;
import entagged.audioformats.generic.TagField;
import entagged.audioformats.mp3.util.id3frames.CommId3Frame;
import entagged.audioformats.mp3.util.id3frames.TextId3Frame;

/**
 * This class is the implementation of {@link entagged.audioformats.Tag} of the
 * ID3V2 tagging system used with MP3s.<br>
 * 
 * @author Rapha?l Slinckx , Christian Laireiter
 */
public class Id3v2Tag extends AbstractTag {
	/**
	 * This is the default encoding to use for new frames.<br>
	 * The code indirectly will choose the UTF-16LE variant with BOM.
	 */
	public static String DEFAULT_ENCODING = "UTF-16";

	/**
	 * This constant is used to identify the minor version 2 of the ID3V2 tag.
	 */
	public static byte ID3V22 = 0;

	/**
	 * This constant is used to identify the minor version 3 of the ID3V2 tag.
	 */
	public static byte ID3V23 = 1;

	/**
	 * This constant is used to identify the minor version 4 of the ID3V2 tag.
	 */
	public static byte ID3V24 = 2;

	/**
	 * If this flag is <code>true</code>, the file which contained this ID3V2
	 * tag also contains a ID3V1 Tag, whose values are merged into this tag
	 * object.<br>
	 */
	private boolean hasV1 = false;

	/**
	 * This field stores the ID3V2 version identifier to which the current tag
	 * belongs.<br>
	 * <ul>
	 * <li> {@link #ID3V22} </li>
	 * <li> {@link #ID3V23} </li>
	 * <li> {@link #ID3V24} </li>
	 * </ul>
	 */
	private byte representedVersion = ID3V23;

	/**
	 * Creates a default instance. <br>
	 * Tag version is {@link #ID3V22}.
	 */
	public Id3v2Tag() {
		// Nothing to do
	}

	/**
	 * Creates an instance.
	 * 
	 * @param version
	 *            The version to represent. {@link #representedVersion}.
	 */
	public Id3v2Tag(byte version) {
		this.representedVersion = version;
	}

	/**
	 * 
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#createAlbumField(java.lang.String)
	 */
	protected TagField createAlbumField(String content) {
		return new TextId3Frame("TALB", content);
	}

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#createArtistField(java.lang.String)
	 */
	protected TagField createArtistField(String content) {
		return new TextId3Frame("TPE1", content);
	}

	/**
	 * 
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#createCommentField(java.lang.String)
	 */
	protected TagField createCommentField(String content) {
		return new CommId3Frame(content);
	}

	/**
	 * 
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#createGenreField(java.lang.String)
	 */
	protected TagField createGenreField(String content) {
		return new TextId3Frame("TCON", content);
	}

	/**
	 * 
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#createTitleField(java.lang.String)
	 */
	protected TagField createTitleField(String content) {
		return new TextId3Frame("TIT2", content);
	}

	/**
	 * 
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#createTrackField(java.lang.String)
	 */
	protected TagField createTrackField(String content) {
		return new TextId3Frame("TRCK", content);
	}

	/**
	 * 
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#createYearField(java.lang.String)
	 */
	protected TagField createYearField(String content) {
		return new TextId3Frame("TDRC", content);
	}

	/**
	 * (overridden) Returns the frame id (name) for the album frame
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#getAlbumId()
	 */
	protected String getAlbumId() {
		return "TALB";
	}

	/**
	 * (overridden) Returns the frame id (name) for the artist frame
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#getArtistId()
	 */
	protected String getArtistId() {
		return "TPE1";
	}

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#getComment()
	 */
	public List getComment() {
		List comments = super.getComment();
		String currIso = Locale.getDefault().getISO3Language();
		CommId3Frame top = null;
		for (int i = 0; i < comments.size(); i++) {
			if (comments.get(i) instanceof CommId3Frame) {
				top = (CommId3Frame) comments.get(i);
				if (!top.getLangage().equals(currIso)) {
					top = null;
				} else {
					comments.remove(i);
					break;
				}
			}
		}
		if (top != null) {
			comments.add(0, top);
		}
		return comments;
	}

	/**
	 * (overridden) Returns the frame id (name) for the comment frame
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#getCommentId()
	 */
	protected String getCommentId() {
		return "COMM";
	}

	/**
	 * (overridden) Returns the frame id (name) for the genre frame
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#getGenreId()
	 */
	protected String getGenreId() {
		return "TCON";
	}

	/**
	 * Returns the Id3v2 minor version identifier, the tag represents.<br>
	 * Values are {@link #ID3V22}, {@link #ID3V23} and {@link #ID3V24}.<br>
	 * 
	 * @return Returns the Id3v2 version identifier of mp3 library part.
	 */
	public byte getRepresentedVersion() {
		return this.representedVersion;
	}

	/**
	 * (overridden) Returns the frame id (name) for the title frame
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#getTitleId()
	 */
	protected String getTitleId() {
		return "TIT2";
	}

	/**
	 * (overridden) Returns the frame id (name) for the track frame
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#getTrackId()
	 */
	protected String getTrackId() {
		return "TRCK";
	}

	/**
	 * (overridden) Returns the frame id (name) for the year frame
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#getYearId()
	 */
	protected String getYearId() {
		return "TDRC";
	}

	/**
	 * Determines whether the values of a parallel stored ID3V1 tag are merged
	 * whithin this object.<br>
	 * In fact no value may have made it into this, but there is an ID3V1 tag
	 * present in the original file.<br>
	 * 
	 * @return <code>true</code> if there was a ID3V1 tag present.
	 */
	public boolean hasId3v1() {
		return hasV1;
	}

	/**
	 * This method sets the ID3V1 tag present property.<br>
	 * 
	 * @see #hasId3v1()
	 * @param b
	 *            the value to set.
	 */
	protected void hasId3v1(boolean b) {
		this.hasV1 = b;
	}

	/**
	 * 
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#isAllowedEncoding(java.lang.String)
	 */
	protected boolean isAllowedEncoding(String enc) {
		boolean result = enc.equals("ISO-8859-1") || enc.startsWith("UTF-16");
		if (!result && this.representedVersion == ID3V24) {
			result = enc.equals("UTF-16BE") || enc.equals("UTF-8");
		}
		return result;
	}

	/**
	 * 
	 * (overridden)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Id3v2 " + super.toString();
	}

	/**
	 * @param representedVersion The representedVersion to set.
	 */
	protected void setRepresentedVersion(byte representedVersion) {
		this.representedVersion = representedVersion;
	}
}
