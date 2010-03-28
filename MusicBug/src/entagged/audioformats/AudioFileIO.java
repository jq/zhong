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

import java.io.File;
import java.util.Hashtable;
import java.util.Iterator;

//import entagged.audioformats.ape.MonkeyFileReader;
//import entagged.audioformats.ape.MonkeyFileWriter;
//import entagged.audioformats.asf.AsfFileReader;
//import entagged.audioformats.asf.AsfFileWriter;
import entagged.audioformats.exceptions.CannotReadException;
import entagged.audioformats.exceptions.CannotWriteException;
//import entagged.audioformats.flac.FlacFileReader;
//import entagged.audioformats.flac.FlacFileWriter;
import entagged.audioformats.generic.AudioFileModificationListener;
import entagged.audioformats.generic.AudioFileReader;
import entagged.audioformats.generic.AudioFileWriter;
import entagged.audioformats.generic.ModificationHandler;
import entagged.audioformats.generic.Utils;
import entagged.audioformats.mp3.Mp3FileReader;
import entagged.audioformats.mp3.Mp3FileWriter;
//import entagged.audioformats.mpc.MpcFileReader;
//import entagged.audioformats.mpc.MpcFileWriter;
//import entagged.audioformats.ogg.OggFileReader;
//import entagged.audioformats.ogg.OggFileWriter;
//import entagged.audioformats.wav.WavFileReader;
//import entagged.audioformats.wav.WavFileWriter;

/**
 * <p>
 * The main entry point for the Tag Reading/Writing operations, this class will
 * select the appropriate reader/writer for the given file.
 * </p>
 * <p>
 * It selects the appropriate reader/writer based on the file extension (case
 * ignored).
 * </p>
 * <p>
 * Here is an simple example of use:
 * </p>
 * <p>
 * <code>
 *		AudioFile audioFile = AudioFileIO.read(new File("audiofile.mp3")); //Reads the given file.<br/>
 *		int bitrate = audioFile.getBitrate(); //Retreives the bitrate of the file.<br/>
 *		String artist = audioFile.getTag().getArtist(); //Retreive the artist name.<br/>
 *		audioFile.getTag().setGenre("Progressive Rock"); //Sets the genre to Prog. Rock, note the file on disk is still unmodified.<br/>
 *		AudioFileIO.write(audioFile); //Write the modifications in the file on disk.
 *	</code>
 * </p>
 * <p>
 * You can also use the <code>commit()</code> method defined for
 * <code>AudioFile</code>s to achieve the same goal as
 * <code>AudioFileIO.write(File)</code>, like this:
 * </p>
 * <p>
 * <code>
 *		AudioFile audioFile = AudioFileIO.read(new File("audiofile.mp3"));<br/>
 *		audioFile.getTag().setGenre("Progressive Rock");<br/>
 *		audioFile.commit(); //Write the modifications in the file on disk.<br/>
 *	</code>
 * </p>
 * 
 * @author Raphael Slinckx
 * @version $Id: AudioFileIO.java,v 1.12 2005/09/18 11:12:46 liree Exp $
 * @since v0.01
 * @see AudioFile
 * @see Tag
 */
public class AudioFileIO {
	// !! Do not forget to also add new supported extensions to AudioFileFilter
	// !!

	/**
	 * This field contains the default instance for static use.
	 */
	private static AudioFileIO defaultInstance;

	/**
	 * <p>
	 * Delete the tag, if any, contained in the given file.
	 * </p>
	 * 
	 * @param f
	 *            The file where the tag will be deleted
	 * @exception CannotWriteException
	 *                If the file could not be written/accessed, the extension
	 *                wasn't recognized, or other IO error occured.
	 */
	public static void delete(AudioFile f) throws CannotWriteException {
		getDefaultAudioFileIO().deleteTag(f);
	}

	/**
	 * This method returns the default isntance for static use.<br>
	 * 
	 * @return The default instance.
	 */
	public static AudioFileIO getDefaultAudioFileIO() {
		if (defaultInstance == null) {
			defaultInstance = new AudioFileIO();
		}
		return defaultInstance;
	}

	/**
	 * <p>
	 * Read the tag contained in the given file.
	 * </p>
	 * 
	 * @param f
	 *            The file to read.
	 * @return The AudioFile with the file tag and the file encoding infos.
	 * @exception CannotReadException
	 *                If the file could not be read, the extension wasn't
	 *                recognized, or an IO error occured during the read.
	 */
	public static AudioFile read(File f) throws CannotReadException {
		return getDefaultAudioFileIO().readFile(f);
	}

	/**
	 * <p>
	 * Write the tag contained in the audiofile in the actual file on the disk.
	 * </p>
	 * 
	 * @param f
	 *            The AudioFile to be written
	 * @exception CannotWriteException
	 *                If the file could not be written/accessed, the extension
	 *                wasn't recognized, or other IO error occured.
	 */
	public static void write(AudioFile f) throws CannotWriteException {
		getDefaultAudioFileIO().writeFile(f);
	}

	/**
	 * This member is used to broadcast modification events to registered
	 * {@link entagged.audioformats.generic.AudioFileModificationListener}
	 */
	private final ModificationHandler modificationHandler;

	// These tables contains all the readers/writers associated with extension
	// as a key
	private Hashtable readers = new Hashtable();

	private Hashtable writers = new Hashtable();

	/**
	 * Creates an instance.
	 * 
	 */
	public AudioFileIO() {
		this.modificationHandler = new ModificationHandler();
		prepareReadersAndWriters();
	}

	/**
	 * Adds an listener for all file formats.
	 * 
	 * @param listener
	 *            listener
	 */
	public void addAudioFileModificationListener(
			AudioFileModificationListener listener) {
		this.modificationHandler.addAudioFileModificationListener(listener);
	}

	/**
	 * <p>
	 * Delete the tag, if any, contained in the given file.
	 * </p>
	 * 
	 * @param f
	 *            The file where the tag will be deleted
	 * @exception CannotWriteException
	 *                If the file could not be written/accessed, the extension
	 *                wasn't recognized, or other IO error occured.
	 */
	public void deleteTag(AudioFile f) throws CannotWriteException {
		String ext = Utils.getExtension(f);

		Object afw = writers.get(ext);
		if (afw == null)
			throw new CannotWriteException(
					"No Deleter associated to this extension: " + ext);

		((AudioFileWriter) afw).delete(f);
	}

	/**
	 * Creates the readers and writers.
	 */
	private void prepareReadersAndWriters() {
		// Tag Readers
		readers.put("mp3", new Mp3FileReader());
//		readers.put("ogg", new OggFileReader());
//		readers.put("flac", new FlacFileReader());
//		readers.put("wav", new WavFileReader());
//		readers.put("mpc", new MpcFileReader());
//		readers.put("mp+", readers.get("mpc"));
//		readers.put("ape", new MonkeyFileReader());
//		readers.put("wma", new AsfFileReader());

		// Tag Writers
		writers.put("mp3", new Mp3FileWriter());
//		writers.put("ogg", new OggFileWriter());
//		writers.put("flac", new FlacFileWriter());
//		writers.put("wav", new WavFileWriter());
//		writers.put("mpc", new MpcFileWriter());
//		writers.put("mp+", writers.get("mpc"));
//		writers.put("ape", new MonkeyFileWriter());
//		writers.put("wma", new AsfFileWriter());

		// Register modificationHandler
		Iterator it = writers.values().iterator();
		while (it.hasNext()) {
			AudioFileWriter curr = (AudioFileWriter) it.next();
			curr.setAudioFileModificationListener(this.modificationHandler);
		}
	}

	/**
	 * <p>
	 * Read the tag contained in the given file.
	 * </p>
	 * 
	 * @param f
	 *            The file to read.
	 * @return The AudioFile with the file tag and the file encoding infos.
	 * @exception CannotReadException
	 *                If the file could not be read, the extension wasn't
	 *                recognized, or an IO error occured during the read.
	 */
	public AudioFile readFile(File f) throws CannotReadException {
		String ext = Utils.getExtension(f);

		Object afr = readers.get(ext);
		if (afr == null)
			throw new CannotReadException(
					"No Reader associated to this extension: " + ext);

		return ((AudioFileReader) afr).read(f);
	}

	/**
	 * Removes an listener for all file formats.
	 * 
	 * @param listener
	 *            listener
	 */
	public void removeAudioFileModificationListener(
			AudioFileModificationListener listener) {
		this.modificationHandler.removeAudioFileModificationListener(listener);
	}

	/**
	 * <p>
	 * Write the tag contained in the audiofile in the actual file on the disk.
	 * </p>
	 * 
	 * @param f
	 *            The AudioFile to be written
	 * @exception CannotWriteException
	 *                If the file could not be written/accessed, the extension
	 *                wasn't recognized, or other IO error occured.
	 */
	public void writeFile(AudioFile f) throws CannotWriteException {
		String ext = Utils.getExtension(f);

		Object afw = writers.get(ext);
		if (afw == null)
			throw new CannotWriteException(
					"No Writer associated to this extension: " + ext);

		((AudioFileWriter) afw).write(f);
	}
}
