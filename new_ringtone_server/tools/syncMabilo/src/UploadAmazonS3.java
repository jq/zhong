
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;


public class UploadAmazonS3 {
	private MusicInfo music;
	public UploadAmazonS3(MusicInfo info) {
		this.music = info;
	}
	
	public static AmazonS3 s3 = null;
	
	public static void createS3() throws Exception{
		s3 = new AmazonS3Client(new PropertiesCredentials(
				UploadAmazonS3.class.getResourceAsStream("AwsCredentials.properties")));
	}
	
	public void run() {
		String key = "";
		String uuid =  UUID.randomUUID().toString();
		try {
			// upload  mp3 ring 
			key = uuid + music.getRingName();
			File file = new File(Consts.NEW_DOWNLOAD_DIR+music.getRingName());
			s3.putObject(new PutObjectRequest(Consts.AMAZON_RING_BUCKET, key, file));      			// upload ring    	
			s3.setObjectAcl(Consts.AMAZON_RING_BUCKET, key, CannedAccessControlList.PublicRead);	// set access 		
		
			// upload m4r ring
			String m4r = music.getRingName().replace(".mp3", ".m4r");
			key = uuid + m4r;
			file = new File(Consts.NEW_DOWNLOAD_DIR+m4r);
			s3.putObject(new PutObjectRequest(Consts.AMAZON_M4R_BUCKET, key, file));      			// upload ring    	
			s3.setObjectAcl(Consts.AMAZON_M4R_BUCKET, key, CannedAccessControlList.PublicRead);	// set access 		
			
			
			// upload image
			key = uuid + music.getImageName();
			file = new File(Consts.NEW_DOWNLOAD_DIR+music.getImageName());
			s3.putObject(new PutObjectRequest(Consts.AMAZON_IMAGE_BUCKET, key, file));      		// upload image    	
			s3.setObjectAcl(Consts.AMAZON_IMAGE_BUCKET, key, CannedAccessControlList.PublicRead);	// set access
			
			// store xml file
			storeMusicInfo(uuid);
		} catch (Exception e) {
			System.out.println("transmit to S3 err");
			e.printStackTrace();
		}
	}
	
	public  void storeMusicInfo(String uuid) {
		int number = SyncMabilo.getNumber();
		File file = new File(Consts.NEW_DOWNLOAD_DIR+number+".xml");
		if(file.exists()) {
			System.out.println(file.getName()+" already exist");
			return ;
		}
		
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(file));
			out.write("<Record>\n");
			out.write("<UUID>" + uuid + "</UUID>\n");
			out.write("<Title>" + music.getTitle() + "</Title>\n");
			out.write("<Artist>" + music.getArtist() + "</Artist>\n");
			out.write("<Category>" + music.getAlbum() + "</Category>\n");
			out.write("<Mark>" + music.getmScore() + "</Mark>\n");
			out.write("<Size>" + music.getSize() + "</Size>\n");
			out.write("<Downloads>" + music.getmCounts() + "</Downloads>\n");
			out.write("<Ring>" + music.getRingName() + "</Ring>\n");
			out.write("<Image>" + music.getImageName() + "</Image>\n");
			out.write("</Record>");
			out.flush();
		}catch (IOException e) {
			System.out.println(file.getName() + " write err");
			e.printStackTrace();
		}finally {
			if(out != null) 
				try {
					out.close();
				} catch (Exception e2) {}
		}
	}
}
