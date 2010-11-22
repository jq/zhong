package music.threads;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import util.Constants;

import music.gui.ImageDialog;
import music.gui.MyFrame;
import music.info.MusicInfo;
import music.search.MusicSearcher;


public class ImageThread implements Runnable
{ 
	public final static String prefix = "http://image.youdao.com/search?q=";
	public final static String sufix = "&keyfrom=image.top&size=s";
	private final static Pattern PATTERN = Pattern.compile(
			"image0\"\\ssrc=\"(.*?)\".*?"			// image 0
		+   "image1\"\\ssrc=\"(.*?)\".*?"			// image 1
		+ 	"image2\"\\ssrc=\"(.*?)\""				// image 2
		,	Pattern.DOTALL);
	
	
	private  MusicInfo  music;
	private  MyFrame  frame;
	
	public ImageThread(MyFrame frm, MusicInfo msc)
	{
		music = msc;
		frame = frm;
	}
	
	public void run()
	{
		Image[] images = new Image[Constants.IMAGE_NUM];

		if(music.getArtist()!=null && searchAndDownload(music.getAlbum()+" "+music.getArtist(), images)) 
		{
			new ImageDialog(frame, true, images, music);
		}
		else 
		{
			// image download fail, then use ni.jpg instead
			music.setImageName(Constants.DEFAULT_IMAGE);
			frame.showMessage(music.getTitle()+" image download fail!");	
		}
	}
	
	public static boolean  searchAndDownload(String keyword, Image[] images)
	{
		String link = prefix+URLEncoder.encode(keyword)+sufix;
		String[] urls = new String[Constants.IMAGE_NUM];
		
		//System.out.println(link);
		try
		{
			String content = MusicSearcher.fetchHtmlPage(link, "utf-8");
			Matcher matcher = PATTERN.matcher(content);
			
			while(matcher.find())
			{
				for(int j=0; j<Constants.IMAGE_NUM; j++)
					urls[j] = matcher.group(j+1);
			}
			if(urls[0] == null)  return false;
			for(int i=0; i<Constants.IMAGE_NUM; i++)
			{
				if(urls[i] != null)
				{
					URL url = new URL(urls[i]);
					images[i] = ImageIO.read(url);
				}
			}
			return true;
			//System.out.println("no");
		}	
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean storeImage(Image image, String pathname)
	{ // store image use jpg format
		try
		{
			int width = image.getWidth(null);
			int height= image.getHeight(null);
			BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); 
			bi.getGraphics().drawImage(image, 0, 0, width, height, null);
			ImageIO.write(bi, "JPEG", new File(pathname));
			
			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
}
