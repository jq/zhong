import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FetchDataFromMabilo extends Thread{
	private Date date;
	private Date now;
	private int numThreadAlive;
	
	public FetchDataFromMabilo(Date date) {
		this.date = date;	// time of last update
		this.now = date;	// time of today
		numThreadAlive = 0;
	}
	public Date getThisDate() {
		return now;
	}
	
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
			"Size:</span>(.*?)<br.*?"							 // ring size
		+	"det2.*?<a\\shref=\"(.*?)&title="				 // ringtone url
			, Pattern.DOTALL);
	
	private static final String PROCEED = "http://www.mabilo.com/search/All-";
	private static final String EXCEED = "-da.htm";
	public static final String Ring_Download_Prefix = "http://music.mabilo.com/dl";
	
	
	public void run() {
		int page = 1;
		String url = "";
		boolean running = true;
		ExecutorService pool = Executors.newFixedThreadPool(3);
			
		while(running) {
			try {
				url = PROCEED+page+EXCEED;
				Matcher all = TOTAL_PATTERN.matcher(Consts.fetchHtmlPage(url));
				while(all.find())  {
					String time = all.group(8);
					int split = time.indexOf(',');
					Date temp = Consts.SDF.parse(time.substring(0,split-2)+time.substring(split+1));
					//System.out.println(temp.toString());
					if(temp.after(date)) {
						MusicInfo music = new MusicInfo();
						music.setImageUrl(all.group(1));
						music.setUrl(Consts.MABILO_BASE+all.group(2));
						music.setTitle(all.group(3));
						music.setArtist(all.group(4).trim());
						music.setAlbum(all.group(5).replace("&", "n").replace("/", "_"));
						music.setmScore(Integer.parseInt(all.group(6)));
						music.setmCounts(Consts.String2Int(all.group(7)));
						pool.execute(new ItemThread(music));
						
						if(temp.after(now))
							now = temp;
					} else {
						running = false;
						break;
					}
				}
				if(running)  page++;
			} catch (Exception e) {
				page ++;
				System.out.println(url+" get err");
				e.printStackTrace();
			}
		}
		pool.shutdown(); 
		
		while(numThreadAlive != 0) {
			try {
				sleep(60000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}


	// get every music info, including: ring, image, download_counts, title...
	class ItemThread implements Runnable {
		private MusicInfo music;
		
		public ItemThread(MusicInfo msc) {
			this.music = msc;
		}
		
		public void run() {
			numThreadAlive ++;
			
			music.setImageName(getFilenameFromURL(music.getImageUrl()));
			if(Consts.downloadTryMulTimes(music.getImageUrl(), Consts.NEW_DOWNLOAD_DIR+music.getImageName())) { 
				// if image downloaded successfully 
				try {
					Matcher matcher = EACH_PATTERN.matcher(Consts.fetchHtmlPage(music.getUrl()));
					while(matcher.find()) {
						music.setSize(matcher.group(1).trim());
						music.setDownloadUrl(Ring_Download_Prefix+matcher.group(2).substring(matcher.group(2).indexOf(".php")));
						music.setRingName(matcher.group(2).substring(matcher.group(2).indexOf("file=")+5));
						
						// download ring here 
						if(Consts.downloadTryMulTimes(music.getDownloadUrl(), Consts.NEW_DOWNLOAD_DIR+music.getRingName())) {
							System.out.println(music.getTitle() + "  download  success");
							// convert from mp3 to m4r
							if(Mp3ToM4R.convert(music)) {
								UploadAmazonS3 uploadAmazonS3 = new UploadAmazonS3(music);
								uploadAmazonS3.run();
							}else {
								System.out.println(music.getRingName()+" convert err");
							}
						}
						break;
					}
				} catch (IOException e)	{
					e.printStackTrace();
				}
			}		
			numThreadAlive --;
		}
	}
	
	public static String getFilenameFromURL(String url)  {
		String[] que = url.split("/");
		return que[que.length-1];
	}
}
