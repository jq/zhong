/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Rapha�l Slinckx <raphael@slinckx.net>
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
package entagged.audioformats.generic;

/**
 * This is a complete impelementation of
 * {@link entagged.audioformats.generic.AbstractTag}.<br>
 * The identifiers of commonly used fields is defined by {@link #keys}.<br>
 * 
 * @author Rapha�l Slinckx
 */
public class GenericTag extends AbstractTag {

	/**
	 * Implementations of {@link TagTextField} for use with
	 * &quot;ISO-8859-1&quot; strings.
	 * 
	 * @author Rapha�l Slinckx
	 */
	private class GenericTagTextField implements TagTextField {

		/**
		 * Stores the string.
		 */
		private String content;

		/**
		 * Stores the identifier.
		 */
		private final String id;

		/**
		 * Creates an instance.
		 * 
		 * @param fieldId
		 *            The identifier.
		 * @param initialContent
		 *            The string.
		 */
		public GenericTagTextField(String fieldId, String initialContent) {
			this.id = fieldId;
			this.content = initialContent;
		}

		/**
		 * (overridden)
		 * 
		 * @see entagged.audioformats.generic.TagField#copyContent(entagged.audioformats.generic.TagField)
		 */
		public void copyContent(TagField field) {
			if (field instanceof TagTextField) {
				this.content = ((TagTextField) field).getContent();
			}
		}

		/**
		 * (overridden)
		 * 
		 * @see entagged.audioformats.generic.TagTextField#getContent()
		 */
		public String getContent() {
			return this.content;
		}

		/**
		 * (overridden)
		 * 
		 * @see entagged.audioformats.generic.TagTextField#getEncoding()
		 */
		public String getEncoding() {
			return "ISO-8859-1";
		}

		/**
		 * (overridden)
		 * 
		 * @see entagged.audioformats.generic.TagField#getId()
		 */
		public String getId() {
			return id;
		}

		/**
		 * (overridden)
		 * 
		 * @see entagged.audioformats.generic.TagField#getRawContent()
		 */
		public byte[] getRawContent() {
			return this.content == null ? new byte[] {} : this.content
					.getBytes();
		}

		/**
		 * (overridden)
		 * 
		 * @see entagged.audioformats.generic.TagField#isBinary()
		 */
		public boolean isBinary() {
			return false;
		}

		/**
		 * (overridden)
		 * 
		 * @see entagged.audioformats.generic.TagField#isBinary(boolean)
		 */
		public void isBinary(boolean b) {
			/* not supported */
		}

		/**
		 * (overridden)
		 * 
		 * @see entagged.audioformats.generic.TagField#isCommon()
		 */
		public boolean isCommon() {
			return true;
		}

		/**
		 * (overridden)
		 * 
		 * @see entagged.audioformats.generic.TagField#isEmpty()
		 */
		public boolean isEmpty() {
			return this.content.equals("");
		}

		/**
		 * (overridden)
		 * 
		 * @see entagged.audioformats.generic.TagTextField#setContent(java.lang.String)
		 */
		public void setContent(String s) {
			this.content = s;
		}

		/**
		 * (overridden)
		 * 
		 * @see entagged.audioformats.generic.TagTextField#setEncoding(java.lang.String)
		 */
		public void setEncoding(String s) {
			/* Not allowed */
		}

		/**
		 * (overridden)
		 * 
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return getId() + " : " + getContent();
		}
	}

	/**
	 * Index for the &quot;album&quot;-identifier in {@link #keys}.
	 */
	public static final int ALBUM = 1;

	/**
	 * Index for the &quot;artist&quot;-identifier in {@link #keys}.
	 */
	public static final int ARTIST = 0;

	/**
	 * Index for the &quot;comment&quot;-identifier in {@link #keys}.
	 */
	public static final int COMMENT = 6;

	/**
	 * Index for the &quot;genre&quot;-identifier in {@link #keys}.
	 */
	public static final int GENRE = 5;

	/**
	 * Stores the generic identifiers of commonly used fields.
	 */
	private final static String[] keys = { "ARTIST", "ALBUM", "TITLE", "TRACK",
			"YEAR", "GENRE", "COMMENT", };

	/**
	 * Index for the &quot;title&quot;-identifier in {@link #keys}.
	 */
	public static final int TITLE = 2;

	/**
	 * Index for the &quot;track&quot;-identifier in {@link #keys}.
	 */
	public static final int TRACK = 3;

	/**
	 * Index for the &quot;year&quot;-identifier in {@link #keys}.
	 */
	public static final int YEAR = 4;

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#createAlbumField(java.lang.String)
	 */
	protected TagField createAlbumField(String content) {
		return new GenericTagTextField(keys[ALBUM], content);
	}

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#createArtistField(java.lang.String)
	 */
	protected TagField createArtistField(String content) {
		return new GenericTagTextField(keys[ARTIST], content);
	}

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#createCommentField(java.lang.String)
	 */
	protected TagField createCommentField(String content) {
		return new GenericTagTextField(keys[COMMENT], content);
	}

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#createGenreField(java.lang.String)
	 */
	protected TagField createGenreField(String content) {
		return new GenericTagTextField(keys[GENRE], content);
	}

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#createTitleField(java.lang.String)
	 */
	protected TagField createTitleField(String content) {
		return new GenericTagTextField(keys[TITLE], content);
	}

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#createTrackField(java.lang.String)
	 */
	protected TagField createTrackField(String content) {
		return new GenericTagTextField(keys[TRACK], content);
	}

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#createYearField(java.lang.String)
	 */
	protected TagField createYearField(String content) {
		return new GenericTagTextField(keys[YEAR], content);
	}

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#getAlbumId()
	 */
	protected String getAlbumId() {
		return keys[ALBUM];
	}

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#getArtistId()
	 */
	protected String getArtistId() {
		return keys[ARTIST];
	}

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#getCommentId()
	 */
	protected String getCommentId() {
		return keys[COMMENT];
	}

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#getGenreId()
	 */
	protected String getGenreId() {
		return keys[GENRE];
	}

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#getTitleId()
	 */
	protected String getTitleId() {
		return keys[TITLE];
	}

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#getTrackId()
	 */
	protected String getTrackId() {
		return keys[TRACK];
	}

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#getYearId()
	 */
	protected String getYearId() {
		return keys[YEAR];
	}

	/**
	 * (overridden)
	 * 
	 * @see entagged.audioformats.generic.AbstractTag#isAllowedEncoding(java.lang.String)
	 */
	protected boolean isAllowedEncoding(String enc) {
		return true;
	}
}
