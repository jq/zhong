package music.search;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

import music.info.MusicInfo;


public abstract class MusicSearcher
{
	private String prefix;
	private String encodeStyle;
	
	public MusicSearcher(String pre, String encodeSty)
	{
		prefix = pre;
		encodeStyle = encodeSty;
	}
	
	// 获得结果页的内容，一般有多条结果
	public ArrayList<MusicInfo> search(String keyword) throws Exception
	{
		String listPage = fetchHtmlPage(prefix+URLEncoder.encode(keyword), encodeStyle);
		return getMusicList(listPage);
	}
	
	// 解析搜索页
	public abstract ArrayList<MusicInfo> getMusicList(String listPage) throws Exception;	
	// 解析获得需要下载的歌的地址
	public abstract void 	getDownloadUrl(String downloadPage, MusicInfo info);			
	
	// 获得需要下载的歌的url
	public void fetchDownloadUrl(MusicInfo info) throws Exception
	{
		if(info.getDownloadUrl()==null || info.getDownloadUrl().equals(""))
		{
			try
			{
				getDownloadUrl(fetchHtmlPage(info.getUrl(), encodeStyle), info);
			}
			catch (IOException e)
			{
				System.out.println("fetchHtmlPage err in MusicSearcher");
				e.printStackTrace();
				throw new Exception();
			}	
		}	
	}
	
	
	// 判断一首歌是否在list中
	public static boolean inList(ArrayList<MusicInfo> list, MusicInfo music)
	{
		for(MusicInfo info: list)
			if(music.equals(info))
				return true;
		return false;
	}
	
	// 获得link的网页内容
	public static String fetchHtmlPage(String link, String coding) throws IOException 
	{ 
		URL url = new URL(link);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.3) Gecko/20100401 Firefox/3.6.3");
		connection.setRequestProperty("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
		connection.setRequestProperty("Accept-Language", "en-us");
		connection.setRequestProperty("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
		connection.setRequestProperty("Keep-Alive", "300");
		connection.setRequestProperty("Connection", "keep-alive");
		
		connection.setConnectTimeout(20000);
		connection.connect();
		
		//String cookie = connection.getHeaderField("Set-Cookie");

		StringBuilder builder = new StringBuilder(1024*4);

		InputStreamReader is = coding != null ? new InputStreamReader(connection.getInputStream(), coding) :
			new InputStreamReader(connection.getInputStream());
		
		BufferedReader reader = new BufferedReader(is);

		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line + "\n");
		}
		try
		{
			is.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return builder.toString();
	}
}
