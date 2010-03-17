package com.trans.music.search;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class MusicUtil {
  // pn= start number, rn is the load number
  //private final static String Search_Url = "http://221.195.40.183/m?f=ms&tn=baidump3&ct=134217728&rn=15&lm=0&word=";

  public static ArrayList<MP3Info> getBiduMp3(String urlStr){
    //初始化歌曲列表
    ArrayList<MP3Info> songs = new ArrayList<MP3Info>();
    try {
        Log.e("MusicSearch ", "onSearchRequested: " + urlStr);
        
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
    
      Pattern pattern = Pattern.compile("<td class=d><a href=\"([\\s\\S]*?)\" title=\"");
      Matcher matcher = pattern.matcher(httpresponse);

      Pattern pattern_title = Pattern.compile("&si=(.*?);;.*?;;");
      Pattern pattern_title_2 = Pattern.compile("&tn=baidusg,(.*?)&si=");

      //Pattern pattern_artist = Pattern.compile("&si=.*?;;(.*?);;");
      while(matcher.find()) {
        
        MP3Info mp3 = new MP3Info();
        int pos2 = httpresponse.indexOf("</tr>",matcher.start());
        
        //获取歌手名
        int artistStartPos = httpresponse.indexOf("<td>",matcher.start());
        int artistEndPos = httpresponse.indexOf("</td>",artistStartPos);
        if((artistStartPos>0)&&(artistStartPos<artistEndPos))
        {
          artistStartPos = httpresponse.indexOf(">",artistStartPos+12);
          int artistEndPos2 = httpresponse.indexOf("</a>",artistStartPos);
          if((artistEndPos>0)&&(artistEndPos2<artistEndPos))
            mp3.setArtist(httpresponse.substring(artistStartPos+1,artistEndPos2));
        }
        //获取连接速度
        int gifpos = httpresponse.lastIndexOf(".gif",pos2);
        if((gifpos>0)&&(gifpos<pos2))
        {
          mp3.setRate(httpresponse.substring(gifpos-1, gifpos));
        }
        //获取文件尺寸
        int sizePos = httpresponse.lastIndexOf(" M</td>",gifpos);
        if((sizePos>0)&&(sizePos<pos2))
        {
          int sizePos2 = httpresponse.indexOf(">",sizePos-6);
          mp3.setFSize(httpresponse.substring(sizePos2+1,sizePos));
        }
        //获取专辑名称
        int albumPos = httpresponse.indexOf("<td class=al><a",matcher.start());
        if((albumPos>0)&&(albumPos<pos2))
        {
          albumPos = httpresponse.indexOf(">",albumPos+16);
          int albumPos2 = httpresponse.indexOf("</a",albumPos);
          if((albumPos2>0)&&(albumPos2<pos2))
            mp3.setAlbum(httpresponse.substring(albumPos+1,albumPos2));
        }
        
        String link = matcher.group(1);
        Matcher matcher_title = pattern_title.matcher(link);
        matcher_title.find();
        
        if(matcher_title.group(1).length() == 0){
          matcher_title = pattern_title_2.matcher(link);
          matcher_title.find();
        }
        
        //Matcher matcher_artist = pattern_artist.matcher(link);
        //matcher_artist.find();
        
      
        mp3.setName(matcher_title.group(1));
      //  mp3.setArtist(matcher_artist.group(1));
        mp3.setLink(link);
        songs.add(mp3);
      }
      /*
      if((mSongs!=null)&&(!mSongs.isEmpty())){
        //免费版添加提示信息，Tao版会添加下一页的link
        MP3Info mp3Tip = new MP3Info();
        mp3Tip.bNull = true;
        mSongs.add(mp3Tip);
      }
      */
    } catch (Exception e) {
      //ShowToastMessage("Network can not connect, please try again.");
      return null;
    }
    return songs;
  }
  
  public static final String SogouSearchBase = "http://mp3.sogou.com/music.so?pf=mp3&as=&st=&ac=1&w=02009900&query=";
  //http://mp3.sogou.com/music.so?pf=&as=&st=&ac=1&w=02009900&query=
  // "http://mp3.sogou.com/music.so?pf=mp3&query=";
  public static String getSogouLinks(String key) {
    return SogouSearchBase + key;
  }
  
  public static String getSogouLinks(String url, int page) {
    return url + "&page=" + page;
  }
  
  public static ArrayList<MP3Info> getSogoMp3(String urlStr, int limit){
		//初始化歌曲列表
    int cnt = 0;
	  ArrayList<MP3Info> songs = new ArrayList<MP3Info>();
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
  			int nameStartPos = httpresponse.indexOf(" title=\"", matcher.start())+" title=\"".length();
  			int nameEndPos = httpresponse.indexOf('"', nameStartPos);
  			mp3.setName(httpresponse.substring(nameStartPos, nameEndPos).trim());
  			
  			//获取歌手名
  			int artistStartPos = httpresponse.indexOf("class=mr target=_blank>", nameEndPos)+"class=mr target=_blank>".length();
  			int artistEndPos = httpresponse.indexOf('<',artistStartPos);
  			if ((artistEndPos-artistStartPos) < 2) {
  				artistStartPos = httpresponse.indexOf("text-decoration:underline;\">", artistStartPos)+"text-decoration:underline;\">".length();
  				artistEndPos = httpresponse.indexOf('<', artistStartPos);
  			}
  			mp3.setArtist(httpresponse.substring(artistStartPos, artistEndPos).trim());
  		
  			//获取专辑名称
  			int albumStartPos = httpresponse.indexOf("class=mr target=_blank>", artistEndPos) + "class=mr target=_blank>".length();
  			int albumEndPos = httpresponse.indexOf('<', albumStartPos);
  			if ((albumEndPos-albumStartPos) < 2) {
  				albumStartPos = httpresponse.indexOf("text-decoration:underline;\">", albumStartPos)+"text-decoration:underline;\">".length();
  				albumEndPos = httpresponse.indexOf('<', albumStartPos);
  			}
  			mp3.setAlbum(httpresponse.substring(albumStartPos, albumEndPos).trim());
  			
  			
  			//获取文件大小
  			int sizeStartPos = httpresponse.indexOf("<td align=center>", albumEndPos)+"<td align=center>".length();
  			int sizeEndPos = httpresponse.indexOf('<', sizeStartPos);
  			mp3.setFSize(httpresponse.substring(sizeStartPos, sizeEndPos).trim());
  			
  			//获取链接
  			int linkStartPos = httpresponse.indexOf("window.open('", sizeEndPos)+"window.open('".length();
  			int linkEndPos = httpresponse.indexOf("&ac=", linkStartPos)+"&ac=".length();
  			String request = httpresponse.substring(linkStartPos, linkEndPos);
  			mp3.setLink(request.trim());
  			
  			//获取连接速度
  			int spdStartPos = httpresponse.indexOf("span class=\"spd", sizeEndPos)+"span class=\"spd".length();
  			int spdEndPos = spdStartPos+1;
  			mp3.setRate(httpresponse.substring(spdStartPos, spdEndPos).trim());
  			
  			songs.add(mp3);
  			cnt++;
  			if (limit > 0 && cnt >= limit) {
  			  break;
  			}
  
  		}
		
/*			if((Songs!=null)&&(!Songs.isEmpty())){
				//免费版添加提示信息，Tao版会添加下一页的link
				MP3Info mp3Tip = new MP3Info();
				mp3Tip.bNull = true;
				mSongs.add(mp3Tip);
			}*/
    } catch (Exception e) {
        //ShowToastMessage("Network can not connect, please try again.");
    	return null;
		}
      return songs;
  }

	private static String getLink(String request) throws IOException {
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
