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


/**
 *  Creates a representation of a Lame Mpeg frame $Id: LameMPEGFrame.java,v 1.3 2005/05/19 16:00:15 kikidonk Exp $
 *
 * @author     Raphaï¿½l Slinckx (KiKiDonK)
 * @version    v0.03
 */
public class LameMPEGFrame {

	/**  contains the Bitrate of this frame */
	private int bitrate;

	/**  Flag indicating if bitset contains a Lame Frame */
	private boolean containsLameMPEGFrame;

	/**  Contains the Filesize in bytes of the frame's File */
	private int fileSize;

	/**  Flag indicating if this is a correct Lame Frame */
	private boolean isValidLameMPEGFrame = false;

	/**  Contains the Lame Version number of this frame */
	private String lameVersion;

	/**  Contains the bitset representing this Lame Frame */
	private boolean containsLameFrame = false;


	/**
	 *  Creates a Lame Mpeg Frame and checks it's integrity
	 *
	 * @param  lameHeader  a byte array representing the Lame frame
	 */
	public LameMPEGFrame( byte[] lameHeader ) {
		String xing = new String( lameHeader, 0, 4 );

		if ( xing.equals( "LAME" ) ) {
			isValidLameMPEGFrame = true;

			int[] b = u( lameHeader );

			containsLameFrame = ( (b[9]&0xFF) == 0xFF  );

			byte[] version = new byte[5];

			version[0] = lameHeader[4];
			version[1] = lameHeader[5];
			version[2] = lameHeader[6];
			version[3] = lameHeader[7];
			version[4] = lameHeader[8];
			lameVersion = new String( version );

			containsLameMPEGFrame = containsLameMPEGFrame();

			if ( containsLameMPEGFrame ) {
				bitrate = b[20];
				fileSize = b[28] * 16777215 + b[29] * 65535 + b[30] * 255 + b[31];
			}
		}
		else
			//Pas de frame VBR MP3 Lame
			isValidLameMPEGFrame = false;

	}
	
	private int[] u(byte[] b) {
		int[] i = new int[b.length];
		for(int j = 0; j<i.length; j++)
			i[j] = b[j] & 0xFF;
		return i;
	}


	/**
	 *  Gets the valid attribute of the LameMPEGFrame object
	 *
	 * @return    The valid value
	 */
	public boolean isValid() {
		return isValidLameMPEGFrame;
	}


	/**
	 *  Create a string representation of this frame
	 *
	 * @return    the string representation of this Lame Frame
	 */
	public String toString() {
		String output;

		if ( isValidLameMPEGFrame ) {
			output = "\n----LameMPEGFrame--------------------\n";
			output += "Lame" + lameVersion;
			if ( containsLameMPEGFrame )
				output += "\tMin.Bitrate:" + bitrate + "\tLength:" + fileSize;
			output += "\n--------------------------------\n";
		}
		else
			output = "\n!!!No Valid Lame MPEG Frame!!!\n";
		return output;
	}


	/**
	 *  Checks wether this frame is a Lame Frame or Not
	 *
	 * @return    true if this frame contains a Lame Frame
	 */
	private boolean containsLameMPEGFrame() {
		return containsLameFrame;
	}
}

