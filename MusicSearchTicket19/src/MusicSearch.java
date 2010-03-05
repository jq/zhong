import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MusicSearch {

	public ArrayList<MP3Info> mSongs;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MusicSearch ms = new MusicSearch();
		//ms.doDownload("http://mp3.baidu.com/m?f=3&rf=idx&tn=baidump3&ct=134217728&lf=&rn=&word=%CE%D2%D6%BB%CA%C7%B8%F6%B4%AB%CB%B5&lm=-1&oq=wo&rsp=0");
		ms.doDownload("http://mp3.sogou.com/music.so?query=jolin+%B2%CC%D2%C0%C1%D5&as=false&st=&ac=1&pf=&_asf=mp3.sogou.com&_ast=1267805032&w=02009900&p=&class=1");
		ms.displayMp3();

	}
	
	private Integer doDownload(String urlStr){
		//初始化歌曲列表
		mSongs = new ArrayList<MP3Info>();
        try {
            
            
        	URL url = new URL(urlStr);
        	HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
        	urlConn.setRequestProperty("User-Agent", "Apache-HttpClient/UNAVAILABLE (java 1.4)");
        	urlConn.setConnectTimeout(12000);
        	urlConn.connect();
        	
        	InputStream stream = urlConn.getInputStream();
			
        	StringBuilder builder = new StringBuilder(8*1024);
			
        	char[] buff = new char[4096];
        	//必须在此指定编码，否则后面toString会导致乱码
        	InputStreamReader is = new InputStreamReader(stream,"gb2312");
			
			int len;
			while ((len = is.read(buff, 0, 4096)) > 0) {
				builder.append(buff, 0, len);
			}
			urlConn.disconnect();
			String httpresponse = builder.toString();
			
			//Pattern pattern = Pattern.compile("<td class=d><a href=\"([\\s\\S]*?)\" title=\"");
			Pattern pattern = Pattern.compile("<a pb=t class=mr style=");
			Matcher matcher = pattern.matcher(httpresponse);

			//Pattern pattern_artist = Pattern.compile("&si=.*?;;(.*?);;");
			int count = 0;
			while(matcher.find()) {
				
				MP3Info mp3 = new MP3Info();
				//获取歌曲名
				int nameStartPos = httpresponse.indexOf("\" action=\"listen\">", matcher.start())+"\" action=\"listen\">".length();
				int nameEndPos = httpresponse.indexOf('<', nameStartPos);
				mp3.setName(httpresponse.substring(nameStartPos, nameEndPos));
				
				//获取歌手名
				int artistStartPos = httpresponse.indexOf("underline;\">", matcher.start())+"underline;\">".length();
				int artistEndPos = httpresponse.indexOf("<",artistStartPos);
				mp3.setArtist(httpresponse.substring(artistStartPos, artistEndPos));
			
				//获取专辑名称
				int albumStartPos = httpresponse.indexOf("target=_blank>", artistEndPos) + "target=_blank>".length();
				int albumEndPos = httpresponse.indexOf("<", albumStartPos);
				mp3.setAlbum(httpresponse.substring(albumStartPos, albumEndPos));
				
				
				//获取文件大小
				int sizeStartPos = httpresponse.indexOf("<td align=center>", albumEndPos)+"<td align=center>".length();
				int sizeEndPos = httpresponse.indexOf('<', sizeStartPos);
				mp3.setFSize(httpresponse.substring(sizeStartPos, sizeEndPos));
				
				//获取链接
				int linkStartPos = httpresponse.indexOf("window.open('", sizeEndPos)+"window.open('".length();
				int linkEndPos = httpresponse.indexOf("&ac=1&c", linkStartPos)+"&ac=1&c".length();
				String request = httpresponse.substring(linkStartPos, linkEndPos);
				mp3.setLink(getLink(request));
				
				//获取连接速度
				int spdStartPos = httpresponse.indexOf("span class=\"spd", sizeEndPos)+"span class=\"spd".length();
				int spdEndPos = spdStartPos+1;
				mp3.setRate(httpresponse.substring(spdStartPos, spdEndPos));
				
				mSongs.add(mp3);

                count++;
                if(count >= 26)
                    break;
			}
			
			if((mSongs!=null)&&(!mSongs.isEmpty())){
				//免费版添加提示信息，Tao版会添加下一页的link
				MP3Info mp3Tip = new MP3Info();
				mp3Tip.bNull = true;
				mSongs.add(mp3Tip);
			}
        } catch (Exception e) {
	        //ShowToastMessage("Network can not connect, please try again.");
        	return null;
		}
        return 1;
	}
	
	private void displayMp3() {
		System.out.println("Total songs:" + mSongs.size());
		for (MP3Info mp3 : mSongs) {
			System.out.println(mp3.name +" "+mp3.album+" "+mp3.artist+" "+mp3.fsize+ " "+mp3.rate+" "+mp3.link);
		}
	}
	
	private String getLink(String request) throws IOException {
		request = "http://mp3.sogou.com" + request;
    	URL url = new URL(request);
    	HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
    	urlConn.setRequestProperty("User-Agent", "Apache-HttpClient/UNAVAILABLE (java 1.4)");
    	urlConn.setConnectTimeout(12000);
    	urlConn.connect();
    	
    	InputStream stream = urlConn.getInputStream();
		
    	StringBuilder builder = new StringBuilder(8*1024);
		
    	char[] buff = new char[4096];
    	//必须在此指定编码，否则后面toString会导致乱码
    	InputStreamReader is = new InputStreamReader(stream,"gb2312");
		
		int len;
		while ((len = is.read(buff, 0, 4096)) > 0) {
			builder.append(buff, 0, len);
		}
		urlConn.disconnect();
		String httpresponse = builder.toString();
		int linkStartPos = httpresponse.indexOf("下载歌曲\" href=\"")+"下载歌曲\" href=\"".length();
		int linkEndPos = httpresponse.indexOf('>', linkStartPos)-1;
		return httpresponse.substring(linkStartPos, linkEndPos);
	}
}
