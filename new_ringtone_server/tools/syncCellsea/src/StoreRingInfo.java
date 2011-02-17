import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class StoreRingInfo {
	public static boolean storeMusicInfo(MusicInfo music) {
		File file = new File(Consts.NEW_DOWNLOAD_DIR+music.getIndex()+".xml");
		if(file.exists()) {
			System.out.println(file.getName()+" already exist when store");
			return false;
		}
		
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(file));
			/*
			out.write("<Record>\n");
			out.write("<UUID>" + music.getUUID() + "</UUID>\n");
			out.write("<Title>" + music.getTitle() + "</Title>\n");
			out.write("<Artist>" + music.getArtist() + "</Artist>\n");
			out.write("<Category>" + music.getCategory() + "</Category>\n");
			out.write("<Mark>" + music.getMark() + "</Mark>\n");
			out.write("<Downloads>" + music.getDownloads() + "</Downloads>\n");
			out.write("<Size>" + music.getSize() + "</Size>\n");
			out.write("<Ring>" + music.getRingName() + "</Ring>\n");
			out.write("<Image>" + music.getImageName() + "</Image>\n");
			out.write("</Record>");
			*/
			out.write("UUID:" + music.getUUID() + "\n");
			out.write("Title:" + music.getTitle() + "\n");
			out.write("Artist:" + music.getArtist() + "\n");
			out.write("Category:" + music.getCategory() + "\n");
			out.write("Mark:" + music.getMark() + "\n");
			out.write("Downloads:" + music.getDownloads() + "\n");
			out.write("Size:" + music.getSize() + "\n");
			out.write("Ring:" + music.getRingName() + "\n");
			out.write("Image:" + music.getImageName() + "\n");
		
			out.flush();
			return true;
		}catch (IOException e) {
			System.out.println(file.getName() + " store err");
			e.printStackTrace();
			return false;
		}finally {
			if(out != null) 
				try {
					out.close();
				} catch (Exception e2) {}
		}
	}
}
