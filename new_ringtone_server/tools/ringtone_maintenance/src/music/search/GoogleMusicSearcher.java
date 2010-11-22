package music.search;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import music.info.MusicInfo;



public class GoogleMusicSearcher extends MusicSearcher
{
	private static final Pattern ROW_PATTERN = Pattern.compile("<tbody(.*?)</tbody>", Pattern.DOTALL);
	private static final Pattern PATTERN = Pattern.compile(
			//"<td\\sclass=\"Title\".*?\">(.*?)</a>\\s</td>.*?" +	// Title
			"<td\\sclass=\"Title.*?\"><a.*?\">(.*?)</a>\\s</td>.*?" + 
			"(\\(.*?\\))</a></td>.*?" +						// Artist
			//"Album\"><a\\shref=\"(.*?)&amp.*?" +			
			"《(.*?)》.*?"+									// Album
			"(http://g.top.*?).{2}26resnum" 				// url
			,Pattern.DOTALL);
	
	private static final Pattern MUTIL_ARTIST_PATTERN = Pattern.compile("\\((.*?)\\)", Pattern.DOTALL);
	
	private static final Pattern DOWN_PATTERN = Pattern.compile(
			"src=\"(http://lh.*?)\".*?" +			// ignore
			"td-size\">([0-9].*?)</td>.*?" +		// size
			"q=(.*?)&amp",							// ring url
			Pattern.DOTALL);
	
	private static final String googleString = "http://www.google.cn";
	private static final String downldString = "http://www.google.cn/music/top100/musicdownload?";
	
	public GoogleMusicSearcher()
	{
		super("http://www.google.cn/music/search?q=","utf-8");
	}

	@Override
	public ArrayList<MusicInfo> getMusicList(String listPage) throws Exception
	{
		ArrayList<MusicInfo> musicList = new ArrayList<MusicInfo>();
		Matcher matcherSingle = ROW_PATTERN.matcher(listPage);
		
		while(matcherSingle.find() )
		{
			String content = matcherSingle.group(1);
			//System.out.println(content);
			
			Matcher music = PATTERN.matcher(content);	
			while(music.find())
			{ 
				MusicInfo info = new MusicInfo();
				//System.out.println(music.group(1));
				info.setTitle(procString(music.group(1)));
				info.setArtist(procArtist(music.group(2)));
				info.setAlbum(procString(music.group(3)));
				info.setUrl(URLDecoder.decode(downldString+music.group(4).substring(music.group(4).indexOf("id"))));
				//System.out.println(info.getUrl());
				
				if(!inList(musicList, info))
					musicList.add(info);
				
			}
		}
	
		return musicList;
	}
	
	public static String procArtist(String orig) throws Exception
	{
		StringBuffer buffer = new StringBuffer();
		Matcher artist = MUTIL_ARTIST_PATTERN.matcher(orig);	
		boolean flag = true;
		while(artist.find())
		{
			//System.out.println(artist.group(1));
			if(flag)
			{
				buffer.append(artist.group(1));
				flag = false ;
			}
			else 
				buffer.append(","+artist.group(1));
		}
		
		return changeCharset(buffer.toString());
	}
	
	public static String procString(String orig) throws Exception
	{
		return changeCharset(URLDecoder.decode(orig.replace("<b>"," ").replace("</b>"," ").trim()));//.replace("&#39", "'");
	}
	
	
	@Override
	public void getDownloadUrl(String downloadPage, MusicInfo info)
	{
	
		Matcher matcher = DOWN_PATTERN.matcher(downloadPage);
		while(matcher.find())
		{		
			// info.setImageUrl(matcher.group(1));
			info.setFileSize(procFileSize(matcher.group(2)));
			info.setDownloadUrl(URLDecoder.decode(matcher.group(3)));
			
			/*
			System.out.println(matcher.group(1));
			System.out.println(URLDecoder.decode(matcher.group(2)));
			*/
			break;
		}
	}
	
	public static int procFileSize(String sizeString)
	{
		 double size = Double.parseDouble(sizeString.substring(0,sizeString.indexOf('&')));
		 if(sizeString.endsWith("MB"))
			 size *= 1000000;
		 else if(sizeString.endsWith("KB"))
			 size *= 1000;
		 return (int)size;
	}
	
	public static String changeCharset(String raw) throws UnsupportedEncodingException {
        //String raw="&#22696;&#23572;&#26412;&#21578;&#21035;&#24033;&#22238;&#28436;&#21809;&#20250;";
        //raw=raw.replaceAll("&#","");
        String[] pre=raw.split(";");
        StringBuffer r=new StringBuffer();
        int i,start,stop;
        for(String s:pre)
        {
        	if((start=s.indexOf("&#")) != -1)
        	{
        		r.append(s.substring(0, start));
        		for(stop=start+2; stop<s.length()&&Character.isDigit(s.charAt(stop)); stop++) ;
            	i=Integer.parseInt(s.substring(start+2, stop));
            	r.append((char)i);
            	r.append(s.substring(stop));
        	}
        	else 
        		r.append(s);
        }
        return r.toString();
    }

}
