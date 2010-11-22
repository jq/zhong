package music.threads;

import image.extract.ImageExactor;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JFrame;
import javax.swing.ProgressMonitor;

import music.gui.MyFrame;
import music.info.MusicInfo;
import util.Constants;


public class DownloadThread	implements Runnable
{	
	private MyFrame frame;
	private MusicInfo music;
	
	//  progress bar
	private ProgressMonitor pm;  
    //private JFrame poi;  
	public void initProgress()
	{
		 //poi = new JFrame();
         //poi.setLocation((int)(Math.random()*Constants.WINDOW_SIZE), (int)(Math.random()*Constants.WINDOW_SIZE));
		 //poi.setVisible(true);
		 
		 pm = new ProgressMonitor(null, "", "Downloading "+music.getTitle()+" ...", 1, 100);  
         
		 /*
		 pm.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
         pm.addWindowFocusListener(new WindowAdapter()
 		 {
         	public void windowClosing(WindowEvent e)
         	{
         		 music.doCancel();
         		 System.out.println("cancel");
         	}
 		});
 		*/
	}	
	
	
	
	public DownloadThread(MyFrame frm, MusicInfo info)
	{
		frame = frm;
		music = info;
	}
	

	
	public void run()
	{
		frame.showMessage("start downloading "+music.getTitle()+" ...");
        
		if(music.getDownloadUrl()!=null && downloadMusic(music.getDownloadUrl(), Constants.DOWNLOAD_DIR+music.getTitle()))
		{
			frame.showMessage(music.getTitle()+" music download success!");
			frame.changeStatus(music, Constants.MUSIC_DONE);
	        
			//  extract image here
			if(!ImageExactor.extract(music))
			{
				while(!music.isCanceled() && music.getImageName()==null)
	 			{
					try
					{
						Thread.sleep(2000);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
			
			if(music.isCanceled())
			{
				frame.showMessage("cancel donwloading "+music.getTitle());
				// remove file here
				removeMusic();
				
				return ;
			}
			
			frame.changeStatus(music, Constants.IMAGE_DONE);
			// start compose
			try
			{
				new ComposeThread(music, frame).run();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
	        
		}
		else if(music.getDownloadUrl()!=null)
		{
			if(pm != null)
				pm.close();
			removeMusic();
			frame.showMessage(music.getTitle()+" music download fail!");
		}
	}

	public void removeMusic()
	{
		// raw music
		if(music.getTitle() != null)
		{
			File file = new File(Constants.DOWNLOAD_DIR+music.getTitle());
			if(file.exists())
			{
				file.delete();
			}
		}
		// finished music
		if(music.getFilename() != null)
		{
			File file = new File(Constants.DOWNLOAD_DIR+music.getFilename());
			if(file.exists())
			{
				file.delete();
			}
		}
		// finished image
		if(music.getImageName()!= null && !music.getImageName().equals(Constants.DEFAULT_IMAGE))
		{
			File file = new File(Constants.DOWNLOAD_DIR+music.getImageName());
			if(file.exists())
			{
				file.delete();
			}
		}
	}
	
	// 下载音乐到本地
	public boolean downloadMusic(String link, String pathname)
	{
		int byteread=0,bytesum=0;
		InputStream inStream=null;
		FileOutputStream fs=null;
		
		initProgress();
		
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
	        int i=0;
	        byte[] src = new byte[3];
	    
	        while ((byteread = inStream.read(buffer)) != -1)
	        {
	        	if(i == 0)
	        	{
	        		src[0]=buffer[0]; src[1]=buffer[1]; src[2]=buffer[2];
	        		i ++ ;
	        	}
	        	if(pm.isCanceled())
	        	{
	        		music.doCancel();
	        		frame.showMessage("cancel downloading "+music.getTitle());
	        		return false;
	        	}
                bytesum += byteread;
                fs.write(buffer, 0, byteread);
                pm.setProgress(bytesum*95/music.getFilesize());
	        }
	        // add music format postfix 
	        music.setFileSize(bytesum);
	        File   file=new   File(pathname);
	        music.setFilename(music.getTitle() + ComposeThread.getType(src));
	        file.renameTo(new   File(Constants.DOWNLOAD_DIR+music.getFilename())); 
	        pm.setProgress(100);
	   
	        return true;
	    
		}
		catch (Exception e)
		{
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
	
}
