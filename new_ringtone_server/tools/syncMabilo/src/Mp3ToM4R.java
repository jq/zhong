import java.io.File;
import javazoom.jl.converter.Converter;
import javazoom.jl.converter.Converter.ProgressListener;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.Obuffer;

public class Mp3ToM4R{
	public static final String GenTempCmd = "mplayer souPath -ao pcm:file=\"tempPath\"";
	public static final String GenTarCmd = "faac tempPath -o tarPath -w";
	public static final String RmTempCmd = "rm -rf tempPath";
	
	public static boolean convert(MusicInfo music) {
		String source = Consts.NEW_DOWNLOAD_DIR+music.getRingName();
		File file = new File(source);
		if(!file.exists()) {
			return false;
		}

		String temp = Consts.NEW_DOWNLOAD_DIR+"temp"+((int)(Math.random()*100))+".wav";
		String target = source.replace(".mp3", ".m4r");
		try {
			//Process proc1 = Runtime.getRuntime().exec(GenTempCmd.replace("souPath", source).replace("tempPath", temp));
			//Thread.sleep(1000);
			//if(proc1.exitValue() != 0) return false;
			
			CPListener cpl = new CPListener();
			new Converter().convert(source, temp, cpl);
			while (cpl.isNotCompleted());
			
			Process proc2 = Runtime.getRuntime().exec(GenTarCmd.replace("tempPath", temp).replace("tarPath", target));
			Thread.sleep(1000);
			if(proc2.exitValue() != 0) return false;
			
			Process proc3 = Runtime.getRuntime().exec(RmTempCmd.replace("tempPath", temp));
			Thread.sleep(1000);
			if(proc3.exitValue() == 0) 
				return true;
			else 
				return false;
			
		} catch (Exception e) {
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
