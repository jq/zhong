package com.ringtone.server;


import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.EmbeddedOnly;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType=IdentityType.APPLICATION)
public class SongEntry {
	
    @PrimaryKey
	@Persistent 
	private String uuid;
	
	@Persistent
	private String title;
	
	@Persistent
	private String artist;
	
	@Persistent
	private String category;
	
	@Persistent
	private int download_count;
	
	@Persistent
	private float avg_rate;
	
	@Persistent
	private int rate_count;
	
	@Persistent
	private long size;
	
	@Persistent
	private Date add_date;
	
	@Persistent
	private String file_name;
	
	@Persistent
	private String image;
	
	@Persistent 
	private String s3_url;
	
	private String content;
	
    @Persistent
    private Set<String> fts;

	public SongEntry(String uuid, String title, String artist, String category,
			int downloadCount, float avgRate, long size, String fileName, 
			String image, String s3Url) {
		super();
		this.uuid = uuid;
		this.title = title;
		this.artist = artist;
		this.category = category;
		download_count = downloadCount;
		avg_rate = avgRate;
		this.size = size;
		file_name = fileName;
		this.image = image;
		s3_url = s3Url;
		content = title + " " + artist + " " + category;
        this.fts = new HashSet<String>();
        SearchJanitor.updateFTSStuffForSongEntry(this);
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public int getDownload_count() {
		return download_count;
	}

	public void setDownload_count(int downloadCount) {
		download_count = downloadCount;
	}

	public float getAvg_rate() {
		return avg_rate;
	}

	public void setAvg_rate(float avgRate) {
		avg_rate = avgRate;
	}

	public int getRate_count() {
		return rate_count;
	}

	public void setRate_count(int rateCount) {
		rate_count = rateCount;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public Date getAdd_date() {
		return add_date;
	}

	public void setAdd_date(Date addDate) {
		add_date = addDate;
	}

	public String getFile_name() {
		return file_name;
	}

	public void setFile_name(String fileName) {
		file_name = fileName;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getS3_url() {
		return s3_url;
	}

	public void setS3_url(String s3Url) {
		s3_url = s3Url;
	}
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public void setFts(Set<String> fts) {
		this.fts = fts;
	}

	public Set<String> getFts() {
		return fts;
	}
	
	public String getContent() {
		return content;
	}
}
