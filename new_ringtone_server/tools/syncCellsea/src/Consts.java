import java.text.SimpleDateFormat;
import java.util.Locale;


public class Consts {
	// local file system
	public static final String SYNC_DIR = "d:/ringtone/";
	public static final String SYNC_TIME_FILE = "last_sync_time.txt";
	public static final String NEW_DOWNLOAD_DIR = SYNC_DIR+"download/";
	public static final String LOG_FILE = NEW_DOWNLOAD_DIR+"log";
	public final static String DEFAULT_IMGNAME = "0.jpg";
	public static final SimpleDateFormat SDF=new SimpleDateFormat("MMM_dd_yyyy", Locale.US);
	public static final int DEFAULT_SYNC_FROM = 3;  // sync from 3 day ago
	
	// amazon s3
	public static final String AMAZON_S3_URL = "http://s3.amazonaws.com/";
	public static final String AMAZON_RING_BUCKET = "ringtone_ring";
	public static final String AMAZON_IMAGE_BUCKET = "ringtone_image";
	public static final String AMAZON_M4R_BUCKET = "ringtone_m4r";
	
	// app engine
	public static final String  GAE_URL_PREFIX = "http://bingliu630.appspot.com/ringtoneserver/insertsong?";
	
	// sync with
	public static final String MABILO = "mabilo";
	public static final String CELLSEA = "cellsea";
	public static final String SYNC_WITH = CELLSEA;
}
