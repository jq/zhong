package music.threads;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import javax.sound.midi.MidiDevice.Info;

import util.Constants;

import music.gui.MyFrame;
import music.info.MusicInfo;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;




public class ToS3Thread	implements Runnable
{
	MusicInfo music;
	MyFrame frame;
	
	public static String bucketName = "ringtone_test_2010";
	
	public ToS3Thread(MusicInfo mInfo, MyFrame frm)
	{
		music = mInfo;
		frame = frm;
	}
	
	public boolean upload()
	{
		try
		{
			final AmazonS3 s3 = new AmazonS3Client(new PropertiesCredentials(
					ToS3Thread.class.getResourceAsStream("AwsCredentials.properties")));
		
			frame.showMessage("start sending "+music.getTitle()+" to S3...");
			String key;
			String uuid =  UUID.randomUUID().toString();
			
			// upload  ring 
			key = uuid + music.getRingName();
			File file = new File(Constants.DOWNLOAD_DIR+music.getRingName());
			s3.putObject(new PutObjectRequest(bucketName, key, file));      		// 上传文件    	
			s3.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead);	// 设置权限		
		
			// upload image
			key = uuid + music.getImageName();
			file = new File(Constants.DOWNLOAD_DIR+music.getImageName());
			s3.putObject(new PutObjectRequest(bucketName, key, file));      		// 上传文件    	
			s3.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead);	// 设置权限		
		
			// record result
			record(uuid);
			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public synchronized void record(String uuid)
	{
		File file = new File(Constants.DOWNLOAD_DIR+Constants.RECORD_FILE);
		if(!file.exists())
		{
			try
			{
				file.createNewFile();
			}
			catch (IOException e)
			{
				System.out.println("record file create error!");
				e.printStackTrace();
			}
		}
		
		if(file.exists())
		{
			try
			{
				FileWriter out = new FileWriter(file, true);
				out.write("<Record>\n");
				out.write("<Title>"+music.getTitle()+"</Title>\n");
				out.write("<Artist>"+music.getArtist()+"</Artist>\n");
				out.write("<Album>"+music.getAlbum()+"</Album>\n");
				out.write("<Score>"+music.getmScore()+"</Score>\n");
				out.write("<Count>"+music.getmCounts()+"</Count>\n");
				out.write("<UUID>"+uuid+"</UUID>\n");
				out.write("<Ring>"+music.getRingName()+"</Ring>\n");
				out.write("<Image>"+music.getImageName()+"</Image>\n");
				out.write("</Record>\n\n");
				out.flush();
				out.close();
			}
			catch (IOException e)
			{
				frame.showMessage("add to record file error!");
				e.printStackTrace();
			}
		}
	}
	
	/*
	public static void main(String[] args)
	{
		MusicInfo music = new MusicInfo();
		music.setFilename("/home/liutao/test/a.mp3");
		new Thread(new ToS3Thread(music, null)).start();
	}
	*/
	
	@Override
	public void run()
	{
		if(upload()) 
		{
			frame.showMessage(music.getTitle()+" to S3 success!");
			frame.changeStatus(music, Constants.FINISH);
		}
		else 
			frame.showMessage(music.getTitle()+" to S3 fail!");
	}
	
}
