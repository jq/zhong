import java.io.File;

import javazoom.jl.converter.Converter;
import javazoom.jl.converter.Converter.ProgressListener;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.Obuffer;

public class Mp3ToM4R{
	public static String Mac_GenTarCmd = "/opt/local/var/macports/software/faac/1.28_2/opt/local/bin/faac tempPath -o tarPath -w";
	public static String GenTarCmd = "faac tempPath -o tarPath -w";
	
	public static boolean convert(MusicInfo music) {
		String source = Consts.NEW_DOWNLOAD_DIR+music.getRingName();
		File file = new File(source);
		if(!file.exists()) {
			return false;
		}

		//String temp = Consts.NEW_DOWNLOAD_DIR+"temp"+((int)(Math.random()*100))+".wav";
		String temp = source.replace(".mp3", ".wav");
		String target = source.replace(".mp3", ".m4r");
		try {
			CPListener cpl = new CPListener();
			new Converter().convert(source, temp, cpl);
			while (cpl.isNotCompleted());
			System.out.println(temp + " is generated!");
			
			String osName = System.getProperty("os.name");
			System.out.println(osName);
			Process proc2 = null;
			
			if (osName.indexOf("Mac") != -1) {
				proc2 = Runtime.getRuntime().exec(Mac_GenTarCmd.replace("tempPath", temp).replace("tarPath", target));
			} else {
				proc2 = Runtime.getRuntime().exec(GenTarCmd.replace("tempPath", temp).replace("tarPath", target));
			}
			
  			Thread.sleep(2000);
  			if(proc2.exitValue() != 0) return false;
						
			return new File(temp).delete();
			
		} catch (Exception e) {
			System.out.println(e);
			return false;
		}
	}

	/*
	public static void main(String[] args) {
		MusicInfo music = new MusicInfo();
		music.setRingName("a.mp3");
		System.out.println(convert(music));
	}*/
	
}

class CPListener implements ProgressListener {
	boolean notCompleted = true;

	@Override
	public boolean converterException(Throwable arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void converterUpdate(int arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		notCompleted = (arg0 != UPDATE_CONVERT_COMPLETE); 
	}

	@Override
	public void decodedFrame(int arg0, Header arg1, Obuffer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void parsedFrame(int arg0, Header arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void readFrame(int arg0, Header arg1) {
		// TODO Auto-generated method stub
		
	}
	
	boolean isNotCompleted() {
		return notCompleted;
	}
	
}
