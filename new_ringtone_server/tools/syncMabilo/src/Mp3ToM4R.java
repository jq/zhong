import java.io.File;
import javazoom.jl.converter.Converter;

public class Mp3ToM4R {
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
			new Converter().convert(source, temp);
			
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
