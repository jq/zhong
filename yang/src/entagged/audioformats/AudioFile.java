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
package entagged.audioformats;

import java.io.File;

import entagged.audioformats.exceptions.CannotWriteException;
import entagged.audioformats.generic.GenericTag;

/**
 *	<p>This is the main object manipulated by the user representing an audiofile, its properties and its tag.</p>
 *	<p>The prefered way to obtain an <code>AudioFile</code> is to use the <code>AudioFileIO.read(File)</code> method.</p>
 *	<p>The <code>AudioFile</code> contains every properties associated with the file itself (no meta-data), like the bitrate, the sampling rate, the encoding infos, etc.</p>
 *	<p>To get the meta-data contained in this file you have to get the <code>Tag</code> of this <code>AudioFile</code></p>
 *
 *@author	Raphael Slinckx
 *@version	$Id: AudioFile.java,v 1.10 2005/09/01 12:19:37 liree Exp $
 *@since	v0.01
 *@see	AudioFileIO
 *@see	Tag
 */
public class AudioFile extends File {
	
	private EncodingInfo info;
	private Tag tag;
	
	/**
	 *	<p>These constructors are used by the different readers, users should not use them, but use the <code>AudioFileIO.read(File)</code> method instead !.</p>
	 *	<p>Create the AudioFile representing file f, the encodinginfos and containing an empty tag</p>
	 *
	 *@param	f	The file of the audiofile
	 *@param	info	the encoding infos over this file
	 */
	public AudioFile(File f, EncodingInfo info) {
		super(f.getAbsolutePath());
		this.info = info;
		this.tag = new GenericTag();
	}
	
	/**
	 *	<p>These constructors are used by the different readers, users should not use them, but use the <code>AudioFileIO.read(File)</code> method instead !.</p>
	 *	<p>Create the AudioFile representing file f, the encodinginfos and containing the tag</p>
	 *
	 *@param	f	The file of the audiofile
	 *@param	info	the encoding infos over this file
	 *@param	tag	the tag contained in this file
	 */
	public AudioFile(File f, EncodingInfo info, Tag tag) {
		super(f.getAbsolutePath());
		this.info = info;
		this.tag = tag;
	}
	
	/**
	 *	<p>These constructors are used by the different readers, users should not use them, but use the <code>AudioFileIO.read(File)</code> method instead !.</p>
	 *	<p>Create the AudioFile representing file denoted by pathname s, the encodinginfos and containing an empty tag</p>
	 *
	 *@param	s	The pathname of the audiofile
	 *@param	info	the encoding infos over this file
	 */
	public AudioFile(String s, EncodingInfo info) {
		super(s);
		this.info = info;
		this.tag = new GenericTag();
	}
	
	/**
	 *	<p>These constructors are used by the different readers, users should not use them, but use the <code>AudioFileIO.read(File)</code> method instead !.</p>
	 *	<p>Create the AudioFile representing file denoted by pathname s, the encodinginfos and containing the tag</p>
	 *
	 *@param	s	The pathname of the audiofile
	 *@param	info	the encoding infos over this file
	 *@param	tag	the tag contained in this file
	 */
	public AudioFile(String s, EncodingInfo info, Tag tag) {
		super(s);
		this.info = info;
		this.tag = tag;
	}
	
	/**
	 *	<p>Write the tag contained in this AudioFile in the actual file on the disk, this is the same as calling the <code>AudioFileIO.write(this)</code> method.</p>
	 *
	 *@see AudioFileIO
	 *@exception CannotWriteException	If the file could not be written/accessed, the extension wasn't recognized, or other IO error occured.
	 */
	public void commit() throws CannotWriteException {
		AudioFileIO.write(this);
	}

	/**
	 *	<p>Returns the bitrate of this AufioFile in kilobytes per second (KB/s). Example: 192 KB/s</p>
	 *
	 *@return	Returns the bitrate of this AufioFile
	 */
	public int getBitrate() {
		return info.getBitrate();
	}
	
	/**
	 *	<p>Returns the number of audio channels contained in this AudioFile, 2 for example means stereo</p>
	 *
	 *@return	Returns the number of audio channels contained in this AudioFile
	 */
	public int getChannelNumber() {
		return info.getChannelNumber();
	}

	/**
	 *	<p>Returns the encoding type of this AudioFile, this needs to be precisely specified in the future</p>
	 *
	 *@return	Returns the encoding type of this AudioFile
	 *@todo		This method needs to be fully specified
	 */
	public String getEncodingType() {
		return info.getEncodingType();
	}

	/**
	 *	<p>Returns the extra encoding infos of this AudioFile, this needs to be precisely specified in the future</p>
	 *
	 *@return	Returns the extra encoding infos of this AudioFile
	 *@todo		This method needs to be fully specified
	 */
	public String getExtraEncodingInfos() {
		return info.getExtraEncodingInfos();
	}

	/**
	 *	<p>Returns the length (duration) in seconds (s) of this AudioFile.Example: 241 seconds</p>
	 *
	 *@return	Returns the length (duration) of this AudioFile
	 */
	public int getLength() {
		return info.getLength();
	}
	
	/**
	 * Returns the length (duration) in seconds with fractions.<br>
	 * 
	 * @return The duration in seconds.
	 */
	public float getPreciseLength() {
		return info.getPreciseLength();
	}
	
	/**
	 *	<p>Returns the sampling rate of this AudioFile in Hertz (Hz). Example: 44100 Hz for most of the audio files</p>
	 *
	 *@return	Returns the sampling rate of this AudioFile
	 */
	public int getSamplingRate() {
		return info.getSamplingRate();
	}
	
	/**
	 *	<p>Returns the tag contained in this AudioFile, the <code>Tag</code> contains any useful meta-data, like artist, album, title, etc.</p>
	 *	<p>If the file does not contain any tag, a new empty tag is returned</p>
	 *
	 *@return	Returns the tag contained in this AudioFile, or a new one if file hasn't any tag.
	 */
	public Tag getTag() {
		return (tag == null) ? new GenericTag() : tag;
	}
	
	/**
	 * <p>Checks if this file is a VBR (variable bitrate) or a Constant Bitrate one</p>
	 * <p>True means VBR, false means CBR</p>
	 * <p>This has only meaning with MP3 and MPC files, other formats are always VBR
	 * 	since it offers a better compression ratio (and lossless compression is by nature VBR</p>
	 */
	public boolean isVbr() {
	    return info.isVbr();
	}
	
	/**
	 *	<p>Returns a multi-line string with the file path, the encoding informations, and the tag contents.</p>
	 *
	 *@return	A multi-line string with the file path, the encoding informations, and the tag contents.
	 *@todo		Maybe this can be changed ?
	 */
	public String toString() {
		return "AudioFile "+getAbsolutePath()+"  --------\n"+info.toString()+"\n"+ ( (tag == null) ? "" : tag.toString())+"\n-------------------";
	}
}
