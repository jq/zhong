package image.extract;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Vector;

import javax.imageio.ImageIO;

import music.info.MusicInfo;

import org.cmc.music.metadata.IMusicMetadata;
import org.cmc.music.metadata.ImageData;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;

import util.Constants;

public class ImageExactor
{
	public static boolean extract(MusicInfo music)
	{
		try
		{
			File src = new File(Constants.DOWNLOAD_DIR+music.getFilename());
			if(!src.exists())	
			{
				System.out.println("file not exists");
				return false;
			}
			
			MusicMetadataSet src_set = new MyID3().read(src); // read metadata

			if (src_set == null) // perhaps no metadata
			{
				System.out.println("no metadata");
				return false;
			}
			
			IMusicMetadata metadata = src_set.getSimplified();
			Vector vector = metadata.getPictures();
			if(vector.size() == 0) 
			{
				System.out.println("no associated image");
				return false;
			}
			
			ByteArrayInputStream in = new ByteArrayInputStream(((ImageData)vector.get(0)).imageData);
			BufferedImage image = ImageIO.read(in);
			music.setImageName(music.getTitle()+"_img.jpg");
			ImageIO.write(image, "JPEG", new File(Constants.DOWNLOAD_DIR+music.getImageName()));
			return true;
			/*
			String artist = metadata.getArtist();  
			String album = metadata.getAlbum();  
			String song_title = metadata.getSongTitle(); 
			System.out.println(album);
			metadata.setArtist("Bob Marley");
			File dst = new File("");
			new MyID3().write(src, dst, src_set, metadata);  // write updated metadata
			 */
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}	
	}
}
