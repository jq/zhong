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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import entagged.audioformats.AudioFile;
import entagged.audioformats.Tag;
import entagged.audioformats.exceptions.CannotWriteException;
import entagged.audioformats.exceptions.ModifyVetoException;

/**
 * This abstract class is the skeleton for tag writers. It handles the
 * creation/closing of the randomaccessfile objects and then call the subclass
 * method writeTag or deleteTag. These two method have to be implemented in the
 * subclass.
 * 
 * @author Raphael Slinckx
 * @version $Id: AudioFileWriter.java,v 1.10 2005/09/18 11:12:45 liree Exp $
 * @since v0.02
 */
public abstract class AudioFileWriter {

	/**
	 * If not <code>null</code>, this listener is used to notify the listener
	 * about modification events.<br>
	 */
	private AudioFileModificationListener modificationListener = null;

	/**
	 * Delete the tag (if any) present in the given file
	 * 
	 * @param f
	 *            The file to process
	 * @exception CannotWriteException
	 *                if anything went wrong
	 */
	public synchronized void delete(AudioFile f) throws CannotWriteException {
		if (!f.canWrite())
			throw new CannotWriteException("Can't write to file \""
					+ f.getAbsolutePath() + "\"");

		if (f.length() <= 150)
			throw new CannotWriteException("Less than 150 byte \""
					+ f.getAbsolutePath() + "\"");

		RandomAccessFile raf = null;
		RandomAccessFile rafTemp = null;
		File tempF = null;
		// Will be set to true on VetoException, causing the finally block to
		// discard
		// the tempfile.
		boolean revert = false;
		try {

			tempF = File.createTempFile("entagged", ".tmp", f.getParentFile());
			rafTemp = new RandomAccessFile(tempF, "rw");
			raf = new RandomAccessFile(f, "rw");
			raf.seek(0);
			rafTemp.seek(0);

			try {
				if (this.modificationListener != null) {
					this.modificationListener.fileWillBeModified(f, true);
				}
				deleteTag(raf, rafTemp);
				if (this.modificationListener != null) {
					this.modificationListener.fileModified(f, tempF);
				}
			} catch (ModifyVetoException veto) {
				throw new CannotWriteException(veto);
			}

		} catch (Exception e) {
			revert = true;
			throw new CannotWriteException("\"" + f.getAbsolutePath() + "\" :"
					+ e, e);
		} finally {
			// will be set to the remaining file.
			File result = f;
			try {
				if (raf != null)
					raf.close();
				if (rafTemp != null)
					rafTemp.close();

				if (tempF.length() > 0 && !revert) {
					f.delete();
					tempF.renameTo(f);
					result = tempF;
				} else {
					tempF.delete();
				}
			} catch (Exception ex) {
				System.err.println("AudioFileWriter:113:\""
						+ f.getAbsolutePath() + "\" or \""
						+ tempF.getAbsolutePath() + "\" :" + ex);
			}
			// Notify listener
			if (this.modificationListener != null) {
				this.modificationListener.fileOperationFinished(result);
			}
		}
	}

	/**
	 * Delete the tag (if any) present in the given randomaccessfile, and do not
	 * close it at the end.
	 * 
	 * @param raf
	 *            The source file, already opened in r-write mode
	 * @param tempRaf
	 *            The temporary file opened in r-write mode
	 * @exception CannotWriteException
	 *                if anything went wrong
	 */
	public synchronized void delete(RandomAccessFile raf,
			RandomAccessFile tempRaf) throws CannotWriteException, IOException {
		raf.seek(0);
		tempRaf.seek(0);
		deleteTag(raf, tempRaf);
	}

	/**
	 * Same as above, but delete tag in the file.
	 * 
	 * @exception IOException
	 *                is thrown when the RandomAccessFile operations throw it
	 *                (you should never throw them manually)
	 * @exception CannotWriteException
	 *                when an error occured during the deletion of the tag
	 */
	protected abstract void deleteTag(RandomAccessFile raf,
			RandomAccessFile tempRaf) throws CannotWriteException, IOException;

	/**
	 * This method sets the {@link AudioFileModificationListener}.<br>
	 * There is only one listener allowed, if you want more instances to be
	 * supported, use the {@link ModificationHandler} to broadcast those events.<br>
	 * 
	 * @param listener
	 *            The listener. <code>null</code> allowed to deregister.
	 */
	public synchronized void setAudioFileModificationListener(
			AudioFileModificationListener listener) {
		this.modificationListener = listener;
	}

	/**
	 * Write the tag (if not empty) present in the AudioFile int the associated
	 * File
	 * 
	 * @param af
	 *            The file we want to process
	 * @exception CannotWriteException
	 *                if anything went wrong
	 */
	public synchronized void write(AudioFile af) throws CannotWriteException {
		// Preliminary checks
		if (af.getTag().isEmpty()) {
			delete(af);
			return;
		}

		if (!af.canWrite())
			throw new CannotWriteException("Can't write to file \""
					+ af.getAbsolutePath() + "\"");

		if (af.length() <= 150)
			throw new CannotWriteException("Less than 150 byte \""
					+ af.getAbsolutePath() + "\"");

		RandomAccessFile raf = null;
		RandomAccessFile rafTemp = null;
		File tempF = null;
		// Will be set to true in exception block to not replace original file.
		boolean cannotWrite = false;
		try {

			tempF = File.createTempFile("entagged", ".tmp", af.getParentFile());
			rafTemp = new RandomAccessFile(tempF, "rw");
			raf = new RandomAccessFile(af, "rw");
			raf.seek(0);
			rafTemp.seek(0);
			try {
				if (this.modificationListener != null) {
					this.modificationListener.fileWillBeModified(af, false);
				}
				writeTag(af.getTag(), raf, rafTemp);
				if (this.modificationListener != null) {
					this.modificationListener.fileModified(af, tempF);
				}
			} catch (ModifyVetoException veto) {
				throw new CannotWriteException(veto);
			}
		} catch (Exception e) {
			cannotWrite = true;
			throw new CannotWriteException("\"" + af.getAbsolutePath() + "\" :"
					+ e);
		} finally {
			File result = af;
			try {
				if (raf != null)
					raf.close();
				if (rafTemp != null)
					rafTemp.close();

				if (!cannotWrite && tempF.length() > 0) {
					af.delete();
					tempF.renameTo(af);
					result = tempF;
				} else {
					tempF.delete();
				}

			} catch (Exception ex) {
				System.err.println("AudioFileWriter:165:\""
						+ af.getAbsolutePath() + "\" or \""
						+ tempF.getAbsolutePath() + "\" :" + ex);
			}
			if (this.modificationListener != null) {
				this.modificationListener.fileOperationFinished(result);
			}
		}
	}

	/**
	 * This is called when a tag has to be written in a file. Three parameters
	 * are provided, the tag to write (not empty) Two randomaccessfiles, the
	 * first points to the file where we want to write the given tag, and the
	 * second is an empty temporary file that can be used if e.g. the file has
	 * to be bigger than the original.
	 * 
	 * If something has been written in the temporary file, when this method
	 * returns, the original file is deleted, and the temporary file is renamed
	 * the the original name
	 * 
	 * If nothing has been written to it, it is simply deleted.
	 * 
	 * This method can assume the raf, rafTemp are pointing to the first byte of
	 * the file. The subclass must not close these two files when the method
	 * returns.
	 * 
	 * @exception IOException
	 *                is thrown when the RandomAccessFile operations throw it
	 *                (you should never throw them manually)
	 * @exception CannotWriteException
	 *                when an error occured during the generation of the tag
	 */
	protected abstract void writeTag(Tag tag, RandomAccessFile raf,
			RandomAccessFile rafTemp) throws CannotWriteException, IOException;

}
