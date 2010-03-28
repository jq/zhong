package com.macrohard.musicbug;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.commons.lang.StringEscapeUtils;

import android.text.TextUtils;

public class MP3Info {
  private String mName;  
  private String mArtist;  
  private String mAlbum;
  private long mSize; 
  private float mRate;    
  private String mLink;
  
  public String getName() {
	  return mName;
  }
  
  public void setName(String name) {
    try {
      mName = URLDecoder.decode(name, "gb2312");
    } catch (UnsupportedEncodingException e) {
    	e.printStackTrace();
    }
    name = StringEscapeUtils.unescapeHtml(name);
  }

  public String getArtist() {
	  return TextUtils.isEmpty(mArtist) ? "Unknown Artist" : mArtist;
  }
  
  public void setArtist(String artist) {
    mArtist = artist.replaceAll("\\<.*?>", "");
    mArtist = StringEscapeUtils.unescapeHtml(mArtist);
  }

  public void setAlbum(String album) {
    mAlbum = album.replaceAll("\\<.*?>", "");
    mAlbum = StringEscapeUtils.unescapeHtml(mAlbum);
  }
  
  public String getAlbum() {
	  return TextUtils.isEmpty(mAlbum) ? "Unknown Album" : mAlbum; 
  }
	  
  public void setSize(long size) {
    mSize = size;
  }
  
  public long getSize(){
	  return mSize;
  }
  
  public void setRate(float rate) {
    mRate = rate;
  }
  
  public float getRate() {
    return mRate;
  }
  
  public void setLink(String link) {
    mLink = link;
  }
  
  public String getLink() {
    return mLink;
  }
 }
