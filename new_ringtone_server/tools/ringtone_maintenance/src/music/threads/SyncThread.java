package music.threads;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import music.gui.MyFrame;
import music.info.MusicInfo;
import music.search.MusicSearcher;
import util.Constants;

public class SyncThread implements Runnable
{
	public final static String Mabilo = "http://www.mabilo.com/ringtones.htm";
	public final static String updateFile = "mabilo.txt";
	public final static String prefix = "http://www.mabilo.com/";
	
	public final static String EncodeStyle = "utf-8";
	private static final Pattern TOTAL_PATTERN = Pattern.compile(
								"New Ringtones</h4>(.*?)<h4>Categories", Pattern.DOTALL);
	private static final Pattern EACH_PATTERN = Pattern.compile(
			"src=\"(.*?)\".*?" 							// image 
		+	"title\"><a\\shref=\"(.*?)\">"	 			// ringtone
		+   "(.*?)</a>.*?"								// title						
		+   "date\">(.*?)</"							// date
			, Pattern.DOTALL);

	private static final Pattern MUSIC_PATTERN = Pattern.compile(
			"Artist:\\s<a.*?>(.*?)</a>.*?"	 		 // artist
		+	"Category:\\s<a.*?>(.*?)</a>.*?"		 //category
		+	"style=\"width:(.*?)%;\".*?"			 // rating
		+	"Downloads:</span>(.*?)<br.*?"			 // downloads
		+	"<a\\shref=\"(.*?)&title="				 // ringtone url
			, Pattern.DOTALL);
	
	public static final SimpleDateFormat sdf=new SimpleDateFormat("MMM dd yyyy");
	public static final String Download_Prefix = "http://music.mabilo.com/dl";
	private Date newDate = null;
	private MyFrame frame;
	
	public SyncThread(MyFrame frm)
	{
		frame = frm;
	}
	
	public Date getDate()
	{
		File file = new File(Constants.DOWNLOAD_DIR+updateFile);
		Date date = null;
		if(file.exists())
		{
			String buf;
			try
			{
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				if((buf=reader.readLine()) != null)
				{
					date = new Date(buf);
				}
				reader.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return date;
	}
	
	public void setDate()
	{
		File file = new File(Constants.DOWNLOAD_DIR+updateFile);
		if(!file.exists())
		{
			try
			{
				file.createNewFile();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		if(file.exists())
		{
			try
			{
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
				writer.write(newDate.toString());
				writer.flush();
				writer.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void run()
	{
		Date date = getDate();
		try
		{
			Matcher all = TOTAL_PATTERN.matcher(MusicSearcher.fetchHtmlPage(Mabilo, EncodeStyle));
			while(all.find())
			{
				Matcher each = EACH_PATTERN.matcher(all.group(1));
				while(each.find())
				{
				   String time = each.group(4);
				   int split = time.indexOf(',');
				   Date temp = sdf.parse(time.substring(0,split-2)+time.substring(split+1));
				   if(date==null || temp.after(date))
				   { // need to update
					   MusicInfo music = new MusicInfo();
					   music.setDate(temp);
					   music.setImageUrl(each.group(1));
					   music.setUrl(prefix+each.group(2));
					   music.setTitle(each.group(3));
					   music.inValide();
					   process(music);
					   
					   if(newDate==null || temp.after(newDate))
						   newDate = temp;
				   }
				}
				JOptionPane.showMessageDialog(frame, "Sync with Mabilo finish!");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		setDate();
	}
	
	public static String url2fileName(String url)
	{
		String[] que = url.split("/");
		return que[que.length-1];
	}
	
	public void process(MusicInfo music)
	{
		System.out.println("start process "+music.getTitle());
		music.setImageName(url2fileName(music.getImageUrl()));
		if(download(music.getImageUrl(), Constants.DOWNLOAD_DIR+music.getImageName()))
		{ // if image downloaded successfully 
			try
			{
				Matcher matcher = MUSIC_PATTERN.matcher(MusicSearcher.fetchHtmlPage(music.getUrl(), EncodeStyle));
				while(matcher.find())
				{
					music.setArtist(matcher.group(1).trim());
					music.setAlbum(matcher.group(2).trim());
					music.setmCounts(Integer.parseInt(matcher.group(3).trim()));
					music.setmScore(Integer.parseInt(matcher.group(4).trim()));
					music.setDownloadUrl(Download_Prefix+matcher.group(5).substring(matcher.group(5).indexOf(".php")));
					music.setRingName(matcher.group(5).substring(matcher.group(5).indexOf("file=")+5));
					if(download(music.getDownloadUrl(), Constants.DOWNLOAD_DIR+music.getRingName()))
					{
						ToS3Thread toS3Thread = new ToS3Thread(music, frame);
						toS3Thread.run();
					}
					break;
				}
				
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static boolean download(String link, String pathname)
	{
		int byteread=0,bytesum=0;
		InputStream inStream=null;
		FileOutputStream fs=null;
		
		try
		{
			URL url = new URL(link);
			URLConnection conn;
			conn = url.openConnection();
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(60000);
	        inStream = conn.getInputStream();
	        fs = new FileOutputStream(pathname);
	        byte[] buffer = new byte[1024];
	    
	        while ((byteread = inStream.read(buffer)) != -1)
	        {
	        	bytesum += byteread;
                fs.write(buffer, 0, byteread);
            }
	        fs.flush();
	        return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		finally
		{
			try
			{
				if(inStream != null)  inStream.close();
				if(fs != null) 		fs.close();
			}
			catch (Exception e2)
			{
				System.out.println("close error in MusicSearcher");
				e2.printStackTrace();
			}
		}
	}

	public static void main(String[] args)
	{
		//new Thread(new SyncThread(null)).start();
		/*
		 * Date date;
		try
		{
			String time = "November 23rd, 2010";
			int split = time.indexOf(',');
		    Date temp = sdf.parse(time.substring(0,split-2)+time.substring(split+1));
			  
			System.out.println(temp);
		}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		/*
		Matcher matcher;
		try
		{
			matcher = MUSIC_PATTERN.matcher(MusicSearcher.fetchHtmlPage("http://www.mabilo.com/239201-pnk-raiseyourglass.htm", EncodeStyle));
			while(matcher.find())
			{
				System.out.println(matcher.group(1));
				System.out.println(matcher.group(2));
				System.out.println(matcher.group(3));
				System.out.println(matcher.group(4));
				System.out.println(Download_Prefix+matcher.group(5).substring(matcher.group(5).indexOf(".php")));
				System.out.println(matcher.group(5).substring(matcher.group(5).indexOf("file=")+5));
			}			
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		*/
	}
}
