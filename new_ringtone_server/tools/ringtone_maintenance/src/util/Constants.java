package util;
import java.util.Random;


public class Constants
{
	public static  String DOWNLOAD_DIR = "/home/liutao/test/";
	public static  String RECORD_FILE = "record";
	public static  String DEFAULT_IMAGE = "default.jpg";
	public static  int IMAGE_NUM = 3;
	public static  int WINDOW_SIZE = 500;
	
	
	public static int BLANK = 0;
	public static int MUSIC_DONE = 1;
	public static int IMAGE_DONE = 2;
	public static int COMP_DONE = 3;
	public static int FINISH = 4;
	
	public static  String[] music_status = {
		"blank",				  
		"download done", 		
		"image done",			
		"compose done", 		
		"finish!!!"
		};
	
}
