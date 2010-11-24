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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.crypto.Data;

import music.gui.MyFrame;
import music.info.MusicInfo;
import music.search.MusicSearcher;
import util.Constants;

public class SyncThread implements Runnable
{
	//public final static String Mabilo = "http://www.mabilo.com/ringtones.htm";
	public final static String updateFile = "mabilo";
	public final static String prefix = "http://www.mabilo.com/";
	
	public final static String EncodeStyle = "utf-8";
	/*private static final Pattern TOTAL_PATTERN = Pattern.compile(
								"New Ringtones</h4>(.*?)<h4>Categories", Pattern.DOTALL);
	*/
	private static final Pattern TOTAL_PATTERN = Pattern.compile(
			"row2.*?src=\"(.*?)\".*?"		// image 
		+	"href=\"(.*?)\">"				// url
		+	"(.*?)</a>.*?"					// title
		+	"Artist.*?>(.*?)</a>.*?"		// artist
		+	"Category.*?>(.*?)</a>.*?"		// category
		+	"style=\"width:(.*?)%;.*?"		// rating
		+	"<span>(.*?)\\sdownloads.*?"	// downloads
		+	"Added:\\s(.*?)</span>"			// date
			, Pattern.DOTALL);
	
	private static final Pattern EACH_PATTERN = Pattern.compile(
			"det2.*?<a\\shref=\"(.*?)&title="				 // ringtone url
			, Pattern.DOTALL);
	
	/*
	private static final Pattern EACH_PATTERN = Pattern.compile(
			"src=\"(.*?)\".*?" 							// image 
		+	"title\"><a\\shref=\"(.*?)\">"	 			// ringtone
		+   "(.*?)</a>.*?"								// title						
		+   "date\">(.*?)</"							// date
			, Pattern.DOTALL);
	*/
	
	public static final SimpleDateFormat sdf=new SimpleDateFormat("MMM dd yyyy");
	public static final String Download_Prefix = "http://music.mabilo.com/dl";
	private Date newDate = null;
	private MyFrame frame;
	
	private static final String proceeding = "http://www.mabilo.com/search/All-";
	private static final String exceeding = "-da.htm";
	private ExecutorService pool;
	
	public SyncThread(MyFrame frm)
	{
		frame = frm;
		pool = Executors.newFixedThreadPool(3);
		
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
		
		if(date == null)
		{
			date = new Date();
			date.setDate(date.getDate()-1);
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
		// get last sync time
		Date date = getDate();	
		newDate = date;
		boolean running = true;
		int page = 1;
		
		
		while(running)
		{
			try
			{
				Matcher all = TOTAL_PATTERN.matcher(MusicSearcher.fetchHtmlPage(proceeding+page+exceeding, EncodeStyle));
				while(all.find())
				{
					String time = all.group(8);
					int split = time.indexOf(',');
					Date temp = sdf.parse(time.substring(0,split-2)+time.substring(split+1));
					//System.out.println(all.group(3));
					if(temp.after(date))
					{
						MusicInfo music = new MusicInfo();
						music.setDate(temp);
						music.setImageUrl(all.group(1));
						music.setUrl(prefix+all.group(2));
						music.setTitle(all.group(3));
						music.setArtist(all.group(4));
						music.setAlbum(all.group(5));
						music.setmScore(Integer.parseInt(all.group(6)));
						music.setmCounts(Integer.parseInt(all.group(7)));
						music.setDate(temp);
						music.inValide();
						pool.execute(new MabiloThread(music));
						
						// update lastest time
						if(temp.after(newDate))
							newDate = temp;
					}
					else 
					{
						running = false;
						break;
					}
				}
				if(running)  page++;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
		}
		setDate();
		JOptionPane.showMessageDialog(frame, "sync with mabilo done!");
	}
	
	public static String url2fileName(String url)
	{
		String[] que = url.split("/");
		return que[que.length-1];
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
			conn.setConnectTimeout(15000);
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
	/*
	public static void main(String[] args)
	{
		new Thread(new SyncThread(null)).start();
	}
	*/
	
	
	// Inner class
	// process every valid ringtone: download, upload to S3
	class MabiloThread implements Runnable
	{
		private MusicInfo music;
	
		public MabiloThread(MusicInfo msc)
		{
			music = msc;
		}
		
		public void run()
		{
			//System.out.println("start process "+music.getTitle());
			frame.showMessage("start process "+music.getTitle()+" from mabilo");
			
			music.setImageName(url2fileName(music.getImageUrl()));
			if(download(music.getImageUrl(), Constants.DOWNLOAD_DIR+music.getImageName()))
			{ // if image downloaded successfully 
				try
				{
					Matcher matcher = EACH_PATTERN.matcher(MusicSearcher.fetchHtmlPage(music.getUrl(), EncodeStyle));
					while(matcher.find())
					{
						music.setDownloadUrl(Download_Prefix+matcher.group(1).substring(matcher.group(1).indexOf(".php")));
						music.setRingName(matcher.group(1).substring(matcher.group(1).indexOf("file=")+5));
						//System.out.println(matcher.group(1));
						
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
	}
}


