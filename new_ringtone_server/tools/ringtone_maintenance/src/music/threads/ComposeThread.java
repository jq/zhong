package music.threads;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import ring.extract.CheapSoundFile;
import util.Constants;

import music.gui.MyFrame;
import music.info.MusicInfo;



public class ComposeThread implements Runnable
{
	private CheapSoundFile mSoundFile = null;
	private MusicInfo music;
	private MyFrame frame;
	
	public static String getType(byte[] src)
	{
		 StringBuilder stringBuilder = new StringBuilder();     
         if (src == null || src.length <= 0) {     
             return null;     
         }     
         for (int i = 0; i < src.length; i++) {     
             int v = src[i] & 0xFF;     
             String hv = Integer.toHexString(v);     
             if (hv.length() < 2) {     
                 stringBuilder.append(0);     
             }     
             stringBuilder.append(hv);     
         }     
         String type = stringBuilder.toString();
         if(type.equals("494433"))  
        	 return ".mp3";
         else  if(type.equals("524946")) 
        	 return ".wav";
         else  if(type.equals("FFD8FF"))
        	 return ".jpg";
         else if (type.equals("47494638"))
        	 return ".gif";
         else 
        	 return null;
	}
	/*
	public static void main(String []args)
	{
		MusicInfo music = new MusicInfo();
		music.setFilename("/home/liutao/test/Love Story.mp3");
		new Thread(new ComposeThread(music, null)).start();
	}*/
	
	public ComposeThread(MusicInfo mInfo, MyFrame frm) throws IOException
	{
			frame = frm;
			music = mInfo;
			mSoundFile = CheapSoundFile.create(Constants.DOWNLOAD_DIR+music.getFilename(), null);
	}
	
	public boolean compose() 
	{		
		
	      int numFrames = mSoundFile.getNumFrames();
	      int[] frameGains = mSoundFile.getFrameGains();
	      //trim frameGains
	      for(int i = frameGains.length-1; i > -1; i--) {
	        if(frameGains[i] == 0)
	          numFrames--;
	        else
	          break;
	      }
	      if(numFrames < 1)
	        return false;
	      
	      final int NUM_OF_FRAMES_TO_COMBINE = 5;
	      int numFramesReduced = numFrames/NUM_OF_FRAMES_TO_COMBINE;
	      int[] gains = new int[numFramesReduced];
	      for(int i = 0; i < numFramesReduced; i++) {
//	        gains[i] = 0;
//	        for(int j = 0; j < NUM_OF_FRAMES_TO_COMBINE; j++) {
//	          int index = i * NUM_OF_FRAMES_TO_COMBINE + j;
//	          if(index < numFrames)
//	            gains[i] += frameGains[index];
//	        }
//	        gains[i] = gains[i] / NUM_OF_FRAMES_TO_COMBINE; 
	        gains[i] = frameGains[i * NUM_OF_FRAMES_TO_COMBINE];
	        
	      }
	      //compute similarity matrix
	      /*
	      int[][] similarityMatrix = new int[numFramesReduced][numFramesReduced];
	      for(int i = 0; i < numFramesReduced; i++) {
	        for(int j = 0; j < i; j++) {
	          similarityMatrix[i][j] = (Math.abs(gains[j] - gains[i]));
	        }
	      }
	      */
	      //compute number of frames per 20 second
	      //double lastSeconds = Double.parseDouble(formatTime(mWaveformView.maxPos()));
	      double lastSeconds = mSoundFile.getNumFrames() * 0.026;  // 26ms per frame for mp3
	      int numFramesSelect = lastSeconds > 100.0 ? (int) (numFramesReduced * 100 / lastSeconds): numFramesReduced;
	      int ratio = (int) (numFramesSelect * 0.5);
	      //compute column sum of similarity matrix
	      int[] similarityX = new int[numFramesReduced];
	      for(int i = 0; i < numFramesReduced; i++) {
	        int sum =0;
	        for(int j = i + ratio; j < numFramesReduced; j++) {
	          int diff = gains[j] - gains[i];      
	          if(diff < 0)
	            diff = -diff;
	          sum += diff;
	        }
	        for(int j = 0; j < i - ratio; j++) {
	          int diff = gains[j] - gains[i];      
	          if(diff < 0)
	            diff = -diff;
	          sum += diff;
	        }
	        similarityX[i] = sum;
	      }
	      //compute summary scores
	      long minScore = 0;
	      for(int i = 0; i < numFramesSelect; i++) {
	        minScore += similarityX[i];
	      }
	      int index = 0;
	      long sum = minScore;
	      for(int i = numFramesSelect; i < numFramesReduced; i++) {
	        sum = sum + similarityX[i] - similarityX[i-numFramesSelect];
	        if(sum < minScore) {
	          minScore = sum;
	          index = i - numFramesSelect;
	        }
	      }
	      //move
	     // double startSecond = lastSeconds * index / numFramesReduced;
	      /*
	      mStartPos = mWaveformView.secondsToPixels(startSecond);
	      mEndPos = mWaveformView.secondsToPixels(startSecond + 20.0);
	      return mStartPos;
	    	*/
	      
	    try
		{
	    	music.setRingName("new_"+music.getFilename());
	    	mSoundFile.WriteFile(new File(Constants.DOWNLOAD_DIR+music.getRingName()), 0, numFramesSelect);
	    	
	    	return true;
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	      
	      
	 }


	@Override
	public void run()
	{
		if(compose())	
		{
			//frame.showMessage(music.getTitle()+" compose finish!");
			frame.changeStatus(music, Constants.COMP_DONE);
			new ToS3Thread(music, frame).run();
		}
		else 
			frame.showMessage(music.getTitle()+" compose error!");
	}
}
