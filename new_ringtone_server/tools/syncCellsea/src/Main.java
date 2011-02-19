import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {
	private Date mLastSyncDate=null; 
	
	private boolean getLastSyncDate() {
		File file = new File(Consts.SYNC_TIME_FILE);
		if(file.exists()) {
			try  {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(new FileInputStream(file)));
				String line = reader.readLine(); 
				if(line != null) {
					mLastSyncDate = new Date(line);
					Calendar cal=Calendar.getInstance();
					cal.setTime(mLastSyncDate);
					cal.add(Calendar.DATE, -1);
					mLastSyncDate = cal.getTime();
				}
				reader.close();
				return true;
			}catch (Exception e)  {
				System.out.println("get last time err");
				return false;
			}
		}else {
			// default 10 days ago
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, -Consts.DEFAULT_SYNC_FROM);
			mLastSyncDate = calendar.getTime();
			return true;
		}
	}
	
	private boolean allConditionOK() {
		// get last sync time
		if(!getLastSyncDate()) return false;

		try {
			UploadToS3.createS3();
		} catch (Exception e) {
			System.out.println("S3 object fail");
			return false;
		}
		// check download directory 
		File downDir = new File(Consts.NEW_DOWNLOAD_DIR);
		if(!downDir.exists()) {
			downDir.mkdir();
			if(!new File(Consts.NEW_DOWNLOAD_DIR+Consts.DEFAULT_IMGNAME).exists()) {
				Utils.copy(Consts.SYNC_DIR+Consts.DEFAULT_IMGNAME, 
						Consts.NEW_DOWNLOAD_DIR+Consts.DEFAULT_IMGNAME);
			}
		}
		return true;
	}
	
	public Main() {
		// pre process
		if(!allConditionOK())  return ;
		if(Consts.SYNC_WITH.equals(Consts.CELLSEA)) {
			CellseaCategory.sync(mLastSyncDate);
		}
		
		// update gae database 
		UpdateDB.update();
		
		// post process
		postProcess();
	}
	
	// update last sync time
	public static void postProcess() {
		Date now = new Date();
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(
					new FileWriter(new File(Consts.SYNC_TIME_FILE)));
			writer.write(now.toString());
			writer.flush();
		} catch (Exception e) {
			System.out.println("write to sync time file err");
			e.printStackTrace();
		} finally {
			if(writer != null) {
				try {
					writer.close();
				} catch (Exception e2) { }
			}
		}
		
		String destPath = Consts.SYNC_DIR+Consts.SDF.format(now)+"/";
		new File(destPath).mkdir();
		Utils.moveAllFiles(Consts.NEW_DOWNLOAD_DIR, destPath);
		Utils.delAllFiles(Consts.NEW_DOWNLOAD_DIR);
	}
	
	public static void main(String[] args) {
		new Main();
		//Utils.delAllFiles(Consts.NEW_DOWNLOAD_DIR);
	}
}
