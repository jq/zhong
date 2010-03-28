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
package entagged.audioformats;

import java.util.Iterator;
import java.util.List;

import entagged.audioformats.generic.TagField;

/**
 * This interface represents the basic data structure for the default
 * audiolibrary functionality.<br>
 * <br>
 * Some audio file tagging systems allow to specify multiple values for one type
 * of information. The artist for example. Some songs may be a cooperation of
 * two or more artists. Sometimes a tagging user wants to specify them in the
 * tag without making one long text string.<br>
 * For that kind of fields, the <b>commonly</b> used fields have adequate
 * methods for adding values. But it is possible the underlying implementation
 * does not support that kind of storing multiple values.<br>
 * <br>
 * <b>Code Examples:</b><br>
 * 
 * <pre>
 * <code>
 * AudioFile file = AudioFileIO.read(new File(&quot;C:\\test.mp3&quot;));
 * 
 * Tag tag = file.getTag();
 * </code>
 * </pre>
 * 
 * @author Raphaël Slinckx
 */
public interface Tag {
	/**
	 * This final field contains all the tags that id3v1 supports. The list has
	 * the same order as the id3v1 genres. To be perfectly compatible (with
	 * id3v1) the genre field should match one of these genre (case ignored).
	 * You can also use this list to present a list of basic (modifiable)
	 * possible choices for the genre field.
	 */
	public static final String[] DEFAULT_GENRES = { "Blues", "Classic Rock",
			"Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop", "Jazz",
			"Metal", "New Age", "Oldies", "Other", "Pop", "R&B", "Rap",
			"Reggae", "Rock", "Techno", "Industrial", "Alternative", "Ska",
			"Death Metal", "Pranks", "Soundtrack", "Euro-Techno", "Ambient",
			"Trip-Hop", "Vocal", "Jazz+Funk", "Fusion", "Trance", "Classical",
			"Instrumental", "Acid", "House", "Game", "Sound Clip", "Gospel",
			"Noise", "AlternRock", "Bass", "Soul", "Punk", "Space",
			"Meditative", "Instrumental Pop", "Instrumental Rock", "Ethnic",
			"Gothic", "Darkwave", "Techno-Industrial", "Electronic",
			"Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy",
			"Cult", "Gangsta", "Top 40", "Christian Rap", "Pop/Funk", "Jungle",
			"Native American", "Cabaret", "New Wave", "Psychadelic", "Rave",
			"Showtunes", "Trailer", "Lo-Fi", "Tribal", "Acid Punk",
			"Acid Jazz", "Polka", "Retro", "Musical", "Rock & Roll",
			"Hard Rock", "Folk", "Folk-Rock", "National Folk", "Swing",
			"Fast Fusion", "Bebob", "Latin", "Revival", "Celtic", "Bluegrass",
			"Avantgarde", "Gothic Rock", "Progressive Rock",
			"Psychedelic Rock", "Symphonic Rock", "Slow Rock", "Big Band",
			"Chorus", "Easy Listening", "Acoustic", "Humour", "Speech",
			"Chanson", "Opera", "Chamber Music", "Sonata", "Symphony",
			"Booty Bass", "Primus", "Porn Groove", "Satire", "Slow Jam",
			"Club", "Tango", "Samba", "Folklore", "Ballad", "Power Ballad",
			"Rhythmic Soul", "Freestyle", "Duet", "Punk Rock", "Drum Solo",
			"A capella", "Euro-House", "Dance Hall" };

	/**
	 * Adds a tagfield to the structure.<br>
	 * It is not recommended to use this method for normal use of the
	 * audiolibrary. The developer will circumvent the underlying
	 * implementation. For example, if one adds a field with the field id
	 * &quot;TALB&quot; for an mp3 file, and the given {@link TagField}
	 * implementation does not return a text field compliant data with
	 * {@link TagField#getRawContent()} other software and the audio library
	 * won't read the file correctly, if they do read it at all. <br>
	 * So for short:<br>
	 * <uil>
	 * <li>The field is stored withoud validation</li>
	 * <li>No conversion of data is perfomed</li>
	 * </ul>
	 * 
	 * @param field
	 *            The field to add.
	 */
	public void add(TagField field);

	/**
	 * Adds an album to the tag.<br>
	 * 
	 * @param album
	 *            Album description
	 */
	public void addAlbum(String album);

	/**
	 * Adds an artist to the tag.<br>
	 * 
	 * @param artist
	 *            Artist's name
	 */
	public void addArtist(String artist);

	/**
	 * Adds a comment to the tag.<br>
	 * 
	 * @param comment
	 *            Comment.
	 */
	public void addComment(String comment);

	/**
	 * Adds a genre to the tag.<br>
	 * 
	 * @param genre
	 *            Genre
	 */
	public void addGenre(String genre);

	/**
	 * Adds a title to the tag.<br>
	 * 
	 * @param title
	 *            Title
	 */
	public void addTitle(String title);

	/**
	 * Adds a track to the tag.<br>
	 * 
	 * @param track
	 *            Track
	 */
	public void addTrack(String track);

	/**
	 * Adds a year to the Tag.<br>
	 * 
	 * @param year
	 *            Year
	 */
	public void addYear(String year);

	/**
	 * Returns a {@linkplain List list} of {@link TagField} objects whose &quot;{@linkplain TagField#getId() id}&quot;
	 * is the specified one.<br>
	 * 
	 * @param id
	 *            The field id.
	 * @return A list of {@link TagField} objects with the given &quot;id&quot;.
	 */
	public List get(String id);

	public List getAlbum();

	public List getArtist();

	public List getComment();

	public Iterator getFields();

	public String getFirstAlbum();

	public String getFirstArtist();

	public String getFirstComment();

	public String getFirstGenre();

	public String getFirstTitle();

	public String getFirstTrack();

	public String getFirstYear();

	public List getGenre();

	public List getTitle();

	public List getTrack();

	public List getYear();

	/**
	 * Returns <code>true</code>, if at least one of the contained
	 * {@linkplain TagField fields} is a common field ({@link TagField#isCommon()}).
	 * 
	 * @return <code>true</code> if a {@linkplain TagField#isCommon() common}
	 *         field is present.
	 */
	public boolean hasCommonFields();

	/**
	 * Determines whether the tag has at least one field with the specified
	 * &quot;id&quot;.
	 * 
	 * @param id
	 *            The field id to look for.
	 * @return <code>true</code> if tag contains a {@link TagField} with the
	 *         given {@linkplain TagField#getId() id}.
	 */
	public boolean hasField(String id);

	/**
	 * Determines whether the tag has no fields specified.<br>
	 * 
	 * @return <code>true</code> if tag contains no field.
	 */
	public boolean isEmpty();

	public void merge(Tag tag);

	public void set(TagField field);

	public void setAlbum(String s);

	public void setArtist(String s);

	public void setComment(String s);

	public boolean setEncoding(String enc);

	public void setGenre(String s);

	public void setTitle(String s);

	public void setTrack(String s);

	public void setYear(String s);

	public String toString();
}