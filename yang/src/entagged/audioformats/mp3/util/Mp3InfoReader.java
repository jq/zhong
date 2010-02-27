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

import entagged.audioformats.EncodingInfo;
import entagged.audioformats.exceptions.*;

import java.io.*;

public class Mp3InfoReader {

	public EncodingInfo read( RandomAccessFile raf ) throws CannotReadException, IOException {
		EncodingInfo encodingInfo = new EncodingInfo();
		
		//Begin info fetch-------------------------------------------
		if ( raf.length()==0 ) {
			//Empty File
			System.err.println("Error: File empty");
		
			throw new CannotReadException("File is empty");
		}
		
		int id3TagSize = 0;
		raf.seek( 0 );
	// skip id3v2 tag, because there may be long pictures inside with
	// slows reading and they can be not unsyncronized
		byte[] bbb = new byte[3];
			raf.read(bbb);
			raf.seek(0);
			String ID3 = new String(bbb);
			if (ID3.equals("ID3")) {
				raf.seek(6);
				id3TagSize = read_syncsafe_integer(raf);
				raf.seek(id3TagSize+10);
				//System.err.println("TagSize: "+tagSize);
			}

		MPEGFrame firstFrame = null;
		
		byte[] b = new byte[4];
		raf.read(b);
		
		// search for sync mark, but also for a right bitrate, samplerate and layer(that way you can
		// read corrupted but playable files)
		while ( !( (b[0]&0xFF)==0xFF  &&  (b[1]&0xE0)==0xE0 && (b[1]&0x06)!=0  && (b[2]&0xF0)!=0xF0  && (b[2]&0x0C)!=0x0C ) && raf.getFilePointer() < raf.length()-4) {
			//System.err.println(sync[0]+"|"+(sync[1]&0xE0)+"|"+((sync[1]&0xE0)==0xE0));
			raf.seek( raf.getFilePointer() - 3);
			//raf.read(sync);
			raf.read(b);
		}

		//raf.seek( raf.getFilePointer() - 2 );
		//System.err.println(raf.getFilePointer());
		//raf.read( b );
		firstFrame = new MPEGFrame( b );
		//System.err.println(b[0]+"|"+b[1]+"|"+b[2]+"|"+b[3]);
		//System.err.println("Frame at offset:"+new Long(raf.getFilePointer()-4)+firstFrame);

		if ( firstFrame == null || !firstFrame.isValid() || firstFrame.getSamplingRate() == 0 ) {
			//MP3File corrupted, no valid MPEG frames
			//System.err.println("Error: could not synchronize to first mp3 frame");
			
			throw new CannotReadException("Error: could not synchronize to first mp3 frame");
		}

		int firstFrameLength = firstFrame.getFrameLength();
		//----------------------------------------------------------------------------
		int skippedLength = 0;

		if ( firstFrame.getMPEGVersion() == MPEGFrame.MPEG_VERSION_1 && firstFrame.getChannelMode() == MPEGFrame.CHANNEL_MODE_MONO ) {
			raf.seek( raf.getFilePointer() + 17 );
			skippedLength += 17;
		}
		else if ( firstFrame.getMPEGVersion() == MPEGFrame.MPEG_VERSION_1 ) {
			raf.seek( raf.getFilePointer() + 32 );
			skippedLength += 32;
		}
		else if ( firstFrame.getMPEGVersion() == MPEGFrame.MPEG_VERSION_2 && firstFrame.getChannelMode() == MPEGFrame.CHANNEL_MODE_MONO ) {
			raf.seek( raf.getFilePointer() + 9 );
			skippedLength += 9;
		}
		else if ( firstFrame.getMPEGVersion() == MPEGFrame.MPEG_VERSION_2 ) {
			raf.seek( raf.getFilePointer() + 17 );
			skippedLength += 17;
		}
		int optionalFrameLength = 0;
		//System.err.println(mp3File);
		//System.err.println(raf.getFilePointer());
		byte[] xingPart1 = new byte[16];

		raf.read( xingPart1 );
		raf.seek( raf.getFilePointer() + 100 );

		byte[] xingPart2 = new byte[4];

		raf.read( xingPart2 );
		
		VbrInfoFrame vbrInfoFrame = new XingMPEGFrame( xingPart1, xingPart2 );
		if ( vbrInfoFrame.isValid() ) {
			optionalFrameLength += 120;
			byte[] lameHeader = new byte[36];
			raf.read( lameHeader );

			LameMPEGFrame currentLameFrame = new LameMPEGFrame( lameHeader );
			if ( !currentLameFrame.isValid() )
				raf.seek( raf.getFilePointer() - 36 ); //Skipping Lame frame reading
			else
				optionalFrameLength += 36; //Lame Frame read
			
			raf.seek( raf.getFilePointer() + firstFrameLength - ( skippedLength + optionalFrameLength + 4 ) );
		} else {
			//Skipping Xing frame reading
			raf.seek( raf.getFilePointer() - 120 - skippedLength + 32);  //120 Xing bytes, unused skipped bytes then go to vbri location

			// Try to read VBRI frame
			byte[] vbriHeader = new byte[18];
			raf.read( vbriHeader );
			
			vbrInfoFrame = new VBRIMPEGFrame(vbriHeader);
			
			raf.seek( raf.getFilePointer() - 18 - 4); //18 VBRI bytes and 4 mpeg info bytes
		}
		
		//----------------------------------------------------------------------------
		//Length computation
		double timePerFrame = ((double) firstFrame.getSampleNumber()) / firstFrame.getSamplingRate();		
		
		double lengthInSeconds;
		if (vbrInfoFrame.isValid()) {
		    //Preffered Method: extracts time length with the Xing Header (vbr:Xing or cbr:Info) or VBRI****************
		    lengthInSeconds = ( timePerFrame * vbrInfoFrame.getFrameCount() );
		    encodingInfo.setVbr(vbrInfoFrame.isVbr());
		    int fs = vbrInfoFrame.getFileSize();
		    
		    encodingInfo.setBitrate((int)( ( (fs==0 ? raf.length()-id3TagSize : fs) * 8 ) / ( timePerFrame * vbrInfoFrame.getFrameCount() * 1000 ) ));
		}
		else {
		    //Default Method: extracts time length using the file length and assuming CBR********************
		    int frameLength = firstFrame.getFrameLength();
			if (frameLength==0)
				throw new CannotReadException("Error while reading header(maybe file is corrupted, or missing first mpeg frame before xing header)");

		    lengthInSeconds =  timePerFrame * ((raf.length()-id3TagSize) / frameLength);
		    
		    encodingInfo.setVbr(false);
		    encodingInfo.setBitrate( firstFrame.getBitrate() );
		}
		
		//Populates encodingInfo----------------------------------------------------
		encodingInfo.setPreciseLength ((float)lengthInSeconds );
		encodingInfo.setChannelNumber( firstFrame.getChannelNumber() );
		encodingInfo.setSamplingRate( firstFrame.getSamplingRate() );
		encodingInfo.setEncodingType( firstFrame.MPEGVersionToString( firstFrame.getMPEGVersion() ) + " || " + firstFrame.layerToString( firstFrame.getLayerVersion() ) );
		encodingInfo.setExtraEncodingInfos( "" );
	
		return encodingInfo;
	}
	
	private int read_syncsafe_integer(RandomAccessFile raf)
		throws IOException {
		int value = 0;

		value += (raf.read()& 0xFF) << 21;
		value += (raf.read()& 0xFF) << 14;
		value += (raf.read()& 0xFF) << 7;
		value += (raf.read()& 0xFF);

		return value;
	}
}

