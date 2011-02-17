
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


public class UploadToS3 {
	public static AmazonS3 s3 = null;
	public static void createS3() throws Exception{
		s3 = new AmazonS3Client(new PropertiesCredentials(
				UploadToS3.class.getResourceAsStream("AwsCredentials.properties")));
	}
	
	public static boolean upload(MusicInfo music) {
		String key = "";
		try {
			// upload  mp3 ring 
			key = music.getUUID() + music.getRingName();
			File file = new File(Consts.NEW_DOWNLOAD_DIR+music.getRingName());
			s3.putObject(new PutObjectRequest(Consts.AMAZON_RING_BUCKET, key, file));      			// upload ring    	
			s3.setObjectAcl(Consts.AMAZON_RING_BUCKET, key, CannedAccessControlList.PublicRead);	// set access 		
			/*
			// upload m4r ring
			String m4r = music.getRingName().replace(".mp3", ".m4r");
			key = music.getUUID() + m4r;
			file = new File(Consts.NEW_DOWNLOAD_DIR+m4r);
			s3.putObject(new PutObjectRequest(Consts.AMAZON_M4R_BUCKET, key, file));      			// upload ring    	
			s3.setObjectAcl(Consts.AMAZON_M4R_BUCKET, key, CannedAccessControlList.PublicRead);		// set access 		
			*/
			
			// upload image
			key = music.getUUID() + music.getImageName();
			file = new File(Consts.NEW_DOWNLOAD_DIR+music.getImageName());
			s3.putObject(new PutObjectRequest(Consts.AMAZON_IMAGE_BUCKET, key, file));      		// upload image    	
			s3.setObjectAcl(Consts.AMAZON_IMAGE_BUCKET, key, CannedAccessControlList.PublicRead);	// set access
			
			return true;
		} catch (Exception e) {
			System.out.println("upload to S3 err");
			e.printStackTrace();
			return false;
		}
	}
}
