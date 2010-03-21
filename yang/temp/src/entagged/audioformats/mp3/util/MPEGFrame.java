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
 *  Create a MPEG Frame object that represent a Mpeg frame in a mp3 file !! Contains an exception that should be modified !! $Id: MPEGFrame.java,v 1.7 2005/05/19 16:00:15 kikidonk Exp $
 *
 * @author     Raphaï¿½l Slinckx (KiKiDonK)
 * @version    v0.03
 */
public class MPEGFrame {
	
	private byte[] mpegBytes;

	/**  The version of this MPEG frame (see the constants) */
	private int MPEGVersion;

	/**  Bitrate of this frame */
	private int bitrate;

	/**  Channel Mode of this Frame (see constants) */
	private int channelMode;

	/**  Emphasis mode string */
	private String emphasis;

	/**  Flag indicating if this frame has padding byte */
	private boolean hasPadding;

	/**  Flag indicating if this frame contains copyrighted material */
	private boolean isCopyrighted;

	/**  Flag indicating if this frame contains original material */
	private boolean isOriginal;

	/**  Flag indicating if this frame is protected */
	private boolean isProtected;

	/**  Flag indicating if this is a valid MPEG Frame */
	private boolean isValid;

	/**  Contains the mpeg layer of this frame (see constants) */
	private int layer;

	/**  Mode Extension of this frame */
	private String modeExtension;

	/**  Sampling rate of this frame in kbps */
	private int samplingRate;

	/**  Constant holding the Dual Channel Stereo Mode */
	public final static int CHANNEL_MODE_DUAL_CHANNEL = 2;

	/**  Constant holding the Joint Stereo Mode */
	public final static int CHANNEL_MODE_JOINT_STEREO = 1;

	/**  Constant holding the Mono Mode */
	public final static int CHANNEL_MODE_MONO = 3;

	/**  Constant holding the Stereo Mode */
	public final static int CHANNEL_MODE_STEREO = 0;

	/**  Constant holding the Layer 1 value Mpeg frame */
	public final static int LAYER_I = 3;

	/**  Constant holding the Layer 2 value Mpeg frame */
	public final static int LAYER_II = 2;

	/**  Constant holding the Layer 3 value Mpeg frame */
	public final static int LAYER_III = 1;

	/**  Constant holding the Reserved Layer value Mpeg frame */
	public final static int LAYER_RESERVED = 0;

	/**  Constant holding the mpeg frame version 1 */
	public final static int MPEG_VERSION_1 = 3;

	/**  Constant holding the mpeg frame version 2 */
	public final static int MPEG_VERSION_2 = 2;

	/**  Constant holding the mpeg frame version 2.5 */
	public final static int MPEG_VERSION_2_5 = 0;

	/**  Constant holding the reserved mpeg frame */
	public final static int MPEG_VERSION_RESERVED = 1;

	/**  Constant table holding the different Mpeg versions allowed */
	private final static int[] MPEGVersionTable =
			{MPEG_VERSION_2_5, MPEG_VERSION_RESERVED, MPEG_VERSION_2, MPEG_VERSION_1};

	/**  Constant table holding the different Mpeg versions allowed in a string representation  */
	private final static String[] MPEGVersionTable_String =
			{"MPEG Version 2.5", "reserved", "MPEG Version 2 (ISO/IEC 13818-3)", "MPEG Version 1 (ISO/IEC 11172-3)"};

	/**  Constant 3ple table that holds the bitrate in kbps for the given layer, mode and value  */
	private final static int[][][] bitrateTable =
			{  //table
			{  //V1
			{0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, -1},   //LI
			{0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, -1},   //LII
			{0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, -1}  //LIII
			},
			{  //V2
			{0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256, -1},   //LI
			{0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, -1},   //LII
			{0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, -1}  //LIII
			}
			};

	/**  Constant table holding the channel modes allowed */
	/*private final static int[] channelModeTable = ;Not currently used
			{CHANNEL_MODE_STEREO, CHANNEL_MODE_JOINT_STEREO, CHANNEL_MODE_DUAL_CHANNEL, CHANNEL_MODE_MONO};
			*/
	
	/**  Constant table holding the channel modes allowed in a string representation */
	private final static String[] channelModeTable_String =
			{"Stereo", "Joint stereo (Stereo)", "Dual channel (2 mono channels)", "Single channel (Mono)"};

	/**  Constant table holding the names of the emphasis modes in a string representation */
	private final static String[] emphasisTable =
			{"none", "50/15 ms", "reserved", "CCIT J.17"};

	/**  Constant table holding the Layer descriptions allowed */
	private final static int[] layerDescriptionTable =
			{LAYER_RESERVED, LAYER_III, LAYER_II, LAYER_I};

	/**  Constant table holding the Layer descriptions allowed in a string representation */
	private final static String[] layerDescriptionTable_String =
			{"reserved", "Layer III", "Layer II", "Layer I"};

	/**  Constant table holding the mode extensions for a given layer in a string representation  */
	private final static String[][] modeExtensionTable =
			{
			{"4-31", "8-31", "12-31", "16-31"},   //LI , LII
			{"off-off", "on-off", "off-on", "on-on"}  //"intensity Stereo - MS Stereo" //LIII
			};

	/**  Constant table holding the sampling rate in Hz for a given Mpeg version */
	private final static int[][] samplingRateTable =
			{  //table
			{44100, 48000, 32000, 0},   //V1
			{22050, 24000, 16000, 0},   //V2
			{11025, 12000, 8000, 0}  //V3
			};

	private final static int[] SAMPLE_NUMBERS = {-1, 1152, 1152, 384};
	
	/**
	 *  Creates a new MPEG frame with the given bytre array and decodes its contents
	 *
	 * @param  b  the array of bytes representing this mpeg frame
	 */
	public MPEGFrame( byte[] b ) {
		this.mpegBytes = b;
		
		if ( isMPEGFrame() ) {
			MPEGVersion = MPEGVersion();
			layer = layerDescription();
			isProtected = isProtected();
			bitrate = bitrate();
			samplingRate = samplingRate();
			hasPadding = hasPadding();
			//privateBit();
			channelMode = channelMode();
			modeExtension = modeExtension();
			isCopyrighted = isCopyrighted();
			isOriginal = isOriginal();
			emphasis = emphasis();
			isValid = true;

		}
		else
			//Ce n'est pas un frame MPEG
			isValid = false;
		this.mpegBytes = null;

	}


	/**
	 *  Gets the bitrate attribute of the MPEGFrame object
	 *
	 * @return    The bitrate value
	 */
	public int getBitrate() {
		return bitrate;
	}


	/**
	 *  Gets the channelMode attribute of the MPEGFrame object
	 *
	 * @return    The channelMode value
	 */
	public int getChannelNumber() {
		switch(channelMode) {
			case CHANNEL_MODE_DUAL_CHANNEL: return 2;
			case CHANNEL_MODE_JOINT_STEREO: return 2;
			case CHANNEL_MODE_MONO: return 1;
			case CHANNEL_MODE_STEREO: return 2;
		}
		return 0;
	}
	
	public int getChannelMode() {
		return channelMode;
	}


	/**
	 *  Gets the layerVersion attribute of the MPEGFrame object
	 *
	 * @return    The layerVersion value
	 */
	public int getLayerVersion() {
		return layer;
	}


	/**
	 *  Gets the mPEGVersion attribute of the MPEGFrame object
	 *
	 * @return    The mPEGVersion value
	 */
	public int getMPEGVersion() {
		return MPEGVersion;
	}


	/**
	 *  Gets the paddingLength attribute of the MPEGFrame object
	 *
	 * @return    The paddingLength value
	 */
	public int getPaddingLength() {
		if ( hasPadding && layer != LAYER_I)
			return 1;
		if ( hasPadding && layer == LAYER_I)
		    return 4;
		
		return 0;
	}


	/**
	 *  Gets the samplingRate attribute of the MPEGFrame object
	 *
	 * @return    The samplingRate value
	 */
	public int getSamplingRate() {
		return samplingRate;
	}


	/**
	 *  Verify if this frame is a valid one
	 *
	 * @return    The isValid value
	 */
	public boolean isValid() {
		return isValid;
	}
	
	/*
	 * Gets this frame length in bytes
	 * 
	 * @return the length in bytes of this frame
	 */
	public int getFrameLength() {
	    if (layer == LAYER_I) {
	        return (12 * (getBitrate() * 1000) / getSamplingRate() + getPaddingLength()) * 4;
	    }
	    
	    return 144 * (getBitrate() * 1000) / getSamplingRate() + getPaddingLength();	
	}

	
	public int getSampleNumber() {
	    int sn = SAMPLE_NUMBERS[layer];
	    
	    //if ( ( MPEGVersion == MPEGFrame.MPEG_VERSION_2 ) || ( MPEGVersion == MPEGFrame.MPEG_VERSION_2_5 ) && (layer == LAYER_III))
			//sn = sn/2;
	    
	    return sn;
	}

	/**
	 *  The Mpeg version of this frame in a string representation
	 *
	 * @param  i  the int constant of the version
	 * @return    the string representation of the version
	 */
	public String MPEGVersionToString( int i ) {
		return MPEGVersionTable_String[i];
	}


	/**
	 *  get a string representation of the channel mode of this frame
	 *
	 * @param  i  the constant holding the channel mode
	 * @return    the string representation of this mode
	 */
	public String channelModeToString( int i ) {
		return channelModeTable_String[i];
	}


	/**
	 *  Get the string representation of the layer version given the constant representing it
	 *
	 * @param  i  the constant holding the layer information
	 * @return    the string representation of this layer version
	 */
	public String layerToString( int i ) {
		return layerDescriptionTable_String[i];
	}


	/**
	 *  Creates a string representation of this mpeg frame
	 *
	 * @return    the string representing this frame
	 */
	public String toString() {
		String output = "\n----MPEGFrame--------------------\n";

		output += "MPEG Version: " + MPEGVersionToString( MPEGVersion ) + "\tLayer: " + layerToString( layer ) + "\n";
		output += "Bitrate: " + bitrate + "\tSamp.Freq.: " + samplingRate + "\tChan.Mode: " + channelModeToString( channelMode ) + "\n";
		output += "Mode Extension: " + modeExtension + "\tEmphasis: " + emphasis + "\n";
		output += "Padding? " + hasPadding + "\tProtected? " + isProtected + "\tCopyright? " + isCopyrighted + "\tOriginal? " + isOriginal + "\n";
		output += "--------------------------------";
		return output;
	}


	/**
	 *  Gets the copyrighted attribute of the MPEGFrame object
	 *
	 * @return    The copyrighted value
	 */
	private boolean isCopyrighted() {
		return ( (mpegBytes[3]&0x08) == 0x08);
	}


	/**
	 *  Gets the mPEGFrame attribute of the MPEGFrame object
	 *
	 * @return    The mPEGFrame value
	 */
	private boolean isMPEGFrame() {
		return ( (mpegBytes[0]&0xFF) == 0xFF ) && ( (mpegBytes[1]&0xE0) == 0xE0 );
	}


	/**
	 *  Gets the original attribute of the MPEGFrame object
	 *
	 * @return    The original value
	 */
	private boolean isOriginal() {
		return (mpegBytes[3]&0x04) == 0x04;
	}


	/**
	 *  Gets the protected attribute of the MPEGFrame object
	 *
	 * @return    The protected value
	 */
	private boolean isProtected() {
		return (mpegBytes[1]&0x01) == 0x00;
	}


	/**
	 *  get the Mpeg version of this frame as an int value (see constants)
	 *
	 * @return    the int value describing the Mpeg version
	 */
	private int MPEGVersion() {
		//System.err.println("V.:"+mpegBytes[1]+"|"+(mpegBytes[1]&0x18)+"|"+((mpegBytes[1]&0x18) >>> 3));
		int index = ((mpegBytes[1]&0x18) >>> 3);
		//System.err.println(MPEGVersionTable[index]);

		return MPEGVersionTable[index];
	}


	/**
	 *  get the bitrate of this frame
	 *
	 * @return    the bitrate in kbps
	 */
	private int bitrate() {
		int index3 = ((mpegBytes[2]&0xF0) >>> 4);
		int index1 = ( MPEGVersion == MPEG_VERSION_1 ) ? 0 : 1;
		int index2;

		if ( layer == LAYER_I )
			index2 = 0;
		else if ( layer == LAYER_II )
			index2 = 1;
		else
			index2 = 2;
		return bitrateTable[index1][index2][index3];
	}


	/**
	 *  get the Mpeg channel mode of this frame as a constant (see constants)
	 *
	 * @return    the constant holding the channel mode
	 */
	private int channelMode() {
		int index = ((mpegBytes[3]&0xC0) >>> 6);

		return index;
	}


	/**
	 *  Get the emphasis mode of this frame in a string representation
	 *
	 * @return    the emphasis mode
	 */
	private String emphasis() {
		int index = (mpegBytes[3]&0x03);

		return emphasisTable[index];
	}


	/**
	 *  Check wether this frame uses padding bytes
	 *
	 * @return    a boolean indicating if this frame uses padding
	 */
	private boolean hasPadding() {
		return (mpegBytes[2]&0x02) == 0x02;
	}


	/**
	 *  Get the layer version of this frame as a constant int value (see constants)
	 *
	 * @return    the layer version constant
	 */
	private int layerDescription() {
		int index = ((mpegBytes[1]&0x06) >>> 1);

		return layerDescriptionTable[index];
	}


	/**
	 *  Gets the string representation of the mode extension of this frame
	 *
	 * @return    mode extension of this frame
	 */
	private String modeExtension() {
		int index2 = ((mpegBytes[3]&0x30) >>> 4);
		int index1 = ( layer == LAYER_III ) ? 1 : 0;

		return modeExtensionTable[index1][index2];
	}


	/**
	 *  get the sampling rate in Hz of this frame
	 *
	 * @return    the sampling rate in Hz of this frame
	 */
	private int samplingRate() {
		int index2 = ((mpegBytes[2]&0x0c) >>> 2);
		int index1;

		if ( MPEGVersion == MPEG_VERSION_1 )
			index1 = 0;
		else if ( MPEGVersion == MPEG_VERSION_2 )
			index1 = 1;
		else
			index1 = 2;
		return samplingRateTable[index1][index2];
	}

}

