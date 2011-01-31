import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;




public class SyncMabilo {
	private Date date;
	private Date now = null;
	public static int number = 1;
	
	public Date getLastSyncDate() {
		File file = new File(Consts.SYNC_DIR+Consts.LAST_SYNC_TIME_FILENAME);
		Date lastDate = null;
		if(file.exists()) {
			try  {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(new FileInputStream(file)));
				String line = reader.readLine(); 
				if(line != null) {
					lastDate = new Date(line);
				}
				reader.close();
			}catch (Exception e)  {
				System.out.println("get last time err");
			}
		}else {
			// default 10 days ago
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, -10);
			lastDate = calendar.getTime();
		}
		return lastDate;
	}
	
	public boolean allConditionOK() {
		// get last sync time
		date = getLastSyncDate();
		if(date == null) return false;
		
		try {
			UploadAmazonS3.createS3();
		} catch (Exception e) {
			System.out.println("S3 object fail");
		}
		// check download directory 
		File downDir = new File(Consts.NEW_DOWNLOAD_DIR);
		if(!downDir.exists()) {
			downDir.mkdir();
		}
		return true;
	}
	
	public SyncMabilo() {
		if(!allConditionOK())  return ;
		System.out.println("last update: "+date);
		
		System.out.println("start sync");
		FetchDataFromMabilo fetchDateThread = new FetchDataFromMabilo(date);
		fetchDateThread.start();
		try {
			fetchDateThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		now = fetchDateThread.getThisDate();
		System.out.println("1. fetch data from Mabilo and upload to Amazon S3 finish!");
			
		UpdateDB updateDBThread = new UpdateDB(Consts.NEW_DOWNLOAD_DIR);
		updateDBThread.start();
		try {
			updateDBThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("2. update database in GAE finish!");
		
		postProcess();
		System.out.println("3. postprocess finish!");
		System.out.println("sync done!");
		
	}
	
	
	// update sync time and move files from newdownload to local datastore
	public void postProcess() {
		if(now == null)  return ;
		try {
			BufferedWriter writer = new BufferedWriter(
					new FileWriter(new File(Consts.SYNC_DIR+Consts.LAST_SYNC_TIME_FILENAME)));
			writer.write(now.toString());
			writer.flush();
			writer.close();
		} catch (Exception e) {
			System.out.println("update this sync time err");
			e.printStackTrace();
		}
		
		String dirName = Consts.SYNC_DIR+Consts.SDF.format(now);
		File dir = new File(dirName);
		if(dir.exists()) {
			System.out.println(dirName+" exist err");
			return ;
		}
		File old = new File(Consts.NEW_DOWNLOAD_DIR);
		old.renameTo(dir);
		old.mkdir();
	}
	
	public synchronized static int getNumber() {
		return number++;
	}
	
	public static void main(String[] args) {
		new SyncMabilo();
	}
}
