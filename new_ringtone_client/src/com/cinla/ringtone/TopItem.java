package com.cinla.ringtone;

public class TopItem {
	
	private String mTitle;
	private String mImageUrl;
	
	public TopItem(String mTitle, String mImageUrl) {
		super();
		this.mTitle = mTitle;
		this.mImageUrl = mImageUrl;
	}

	public String getTitle() {
		return mTitle;
	}
	
	public void setTitle(String title) {
		this.mTitle = title;
	}
	
	public String getImageUrl() {
		return mImageUrl;
	}
	
	public void setImageUrl(String imageUrl) {
		this.mImageUrl = imageUrl;
	}
	
}
