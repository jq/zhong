package com.trans.music.search;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.commons.lang.StringEscapeUtils;

public class MP3Info{
  public boolean bNull = false;
  public String name="";  
  public String artist="";  
  public String album="";
  public String fsize=""; 
  public String rate="";    
  public String link="";
  public String speed="";
  public boolean bisLinkValid = false;
  void setName(String n)
  {
    try {
      name = URLDecoder.decode(n, "gb2312");
    } catch (Exception e) {
    	name = n;
    }
    name = StringEscapeUtils.unescapeHtml(name);
  }
  public String getName(){return name;} 
  void setArtist(String a)
  {
    artist = a.replaceAll("\\<.*?>","");
    artist = StringEscapeUtils.unescapeHtml(artist);
  }
  public String getArtist(){
    if(artist.length()>0)
      return artist;
    return "Unknown Artist";
  }
  void setAlbum(String a)
  {
    album = a.replaceAll("\\<.*?>","");
    album = StringEscapeUtils.unescapeHtml(album);
  }
  public String getAlbum(){
    if(album.length()>0)
      return album;
    return "Unknown Album"; 
  }
  void setFSize(String f)
  {
    fsize = f;
  }
  public String getFSize(){
    if(fsize.length()>0)
      return fsize;
    else
      return "0M";
  }
  void setRate(String r)
  {
    rate = r;
  }
  public String getRate(){return rate;}
  void setLink(String l)
  {
    link = l;
  }
  public String getLink(){return link;}
  void setSpeed(String s)
  {
    speed = s;
  }
  public String getSpeed(){return speed;}
 }
