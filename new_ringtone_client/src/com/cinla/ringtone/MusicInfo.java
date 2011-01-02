package com.cinla.ringtone;

import java.io.Serializable;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class MusicInfo implements Serializable{

	private String mTitle;
	private String mUuid; 
	private String mArtist;
	private String mCategory;
	private double mRate;
	private int mDownloadCount;
	private String mImageUrl;
	private String mMp3Url;
	private long mSize;
	private String mDownloadedPath;
	private String mDownloadedUriString;
	
	public MusicInfo() { }
	
	public long getmSize() {
		return mSize;
	}
	public void setmSize(long mSize) {
		this.mSize = mSize;
	}
	public String getmTitle() {
		return mTitle;
	}
	public void setmTitle(String mTitle) {
		this.mTitle = mTitle;
	}
	public String getmUuid() {
		return mUuid;
	}
	public void setmUuid(String mUuid) {
		this.mUuid = mUuid;
	}
	public String getmArtist() {
		return mArtist;
	}
	public void setmArtist(String mArtist) {
		this.mArtist = mArtist;
	}
	public String getmCategory() {
		return mCategory;
	}
	public void setmCategory(String mCategory) {
		this.mCategory = mCategory;
	}
	public double getmRate() {
		return mRate;
	}
	public void setmRate(double mRate) {
		this.mRate = mRate;
	}
	public int getmDownloadCount() {
		return mDownloadCount;
	}
	public void setmDownloadCount(int mDownloadCount) {
		this.mDownloadCount = mDownloadCount;
	}
	public String getmImageUrl() {
		return mImageUrl;
	}
	public void setmImageUrl(String mImageUrl) {
		this.mImageUrl = mImageUrl;
	}
	public String getmMp3Url() {
		return mMp3Url;
	}
	public void setmMp3Url(String mMp3Url) {
		this.mMp3Url = mMp3Url;
	}
//	@Override
//	public int describeContents() {
//		return 0;
//	}
	
	private MusicInfo(Parcel in) {
		mArtist = in.readString();
		mCategory = in.readString();
		mImageUrl = in.readString();
		mMp3Url = in.readString();
		mTitle = in.readString();
		mUuid = in.readString();
		mDownloadCount = in.readInt();
		mRate = in.readDouble();
		mSize = in.readLong();
	} 
	
	public String getFilePath() {
		return Constant.sMusicDir+mTitle+'['+mArtist+']'+getExtName();
	}
	
	public String getObjFilePath() {
		return Constant.sObjDir+mTitle+'['+mArtist+']';
	}

	public String getmDownloadedPath() {
		return mDownloadedPath;
	}

	public void setmDownloadedPath(String mDownloadedPath) {
		this.mDownloadedPath = mDownloadedPath;
	}

	public String getmDownloadedUri() {
		return mDownloadedUriString;
	}

	public void setmDownloadedUri(String mDownloadedUriString) {
		this.mDownloadedUriString = mDownloadedUriString;
	}
	
//	@Override
//	public void writeToParcel(Parcel dest, int flags) {
//		dest.writeString(mArtist);
//		dest.writeString(mCategory);
//		dest.writeString(mImageUrl);
//		dest.writeString(mMp3Url);
//		dest.writeString(mTitle);
//		dest.writeString(mUuid);
//		dest.writeInt(mDownloadCount);
//		dest.writeDouble(mRate);
//		dest.writeLong(mSize);
//	}
//    public static final Parcelable.Creator<MusicInfo> CREATOR = new Parcelable.Creator<MusicInfo>() {
//    	public MusicInfo createFromParcel(Parcel in) {
//    		return new MusicInfo(in);
//    	}
//    	public MusicInfo[] newArray(int size) {
//    		return new MusicInfo[size];
//    	}
//    };
	
	//get .mp3 from mFileName field
	private String getExtName() {
		return mMp3Url.substring(mMp3Url.lastIndexOf('.'));
	}

}
