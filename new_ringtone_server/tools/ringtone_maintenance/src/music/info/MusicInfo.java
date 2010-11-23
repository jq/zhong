package music.info;

import java.util.Date;

public class MusicInfo
{
	
	private String 	mTitle;
	private String 	mType;
	private String  mFilename;		// music file name
	private String  mRingFilename;	// ring file name
	
	private String 	mArtist;
	private String 	mAlbum;
	private String 	mUrl;
	private String  mDownloadUrl;
	private String 	mDisplaySize;
	private int    	mFileSize;
	private Date    mAddDate;
	private String 	mImageUrl;
	private String  mImageFilename;
	private int     mCounts;
	
	public int getmCounts()
	{
		return mCounts;
	}

	public void setmCounts(int mCounts)
	{
		this.mCounts = mCounts;
	}

	public int getmScore()
	{
		return mScore;
	}

	public void setmScore(int mScore)
	{
		this.mScore = mScore;
	}

	private int 	mScore;
	
	

	private int mIndex;					// in which row of table
	private boolean  mIsValid = true;	// true if it's show in the table
	
	public void setDate(Date date)
	{
		mAddDate = date;
	}
	
	public Date getDate()
	{
		return mAddDate;
	}
	
	public void setIndex(int idx) 
	{
		mIndex = idx;
	}
	
	public int getIndex()
	{
		return mIndex;
	}
	
	private boolean  mCanceled = false;
	public boolean isCanceled()
	{
		return mCanceled;
	}
	public void doCancel()
	{
		mCanceled = true;
	}
	
	public boolean isValid()
	{
		return mIsValid;
	}
	public void inValide()
	{
		mIsValid = false;
	}
	public void setImageName(String name)
	{
		mImageFilename = name;
	}
	
	public String getImageName()
	{
		return mImageFilename;
	}
	
	public void setFilename(String name)
	{
		mFilename = name;
	}
	
	public void setRingName(String name)
	{
		mRingFilename = name;
	}
	
	public String getRingName()
	{
		return mRingFilename;
	}
	
	public String getFilename()
	{
		return mFilename;
	}
	
	public void setTitle(String title) {
		mTitle = title;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public void setDownloadUrl(String downloadUrl) {
		mDownloadUrl = downloadUrl;
	}
	
	public String getDownloadUrl() {
		return mDownloadUrl;
	}
	
	public void setArtist(String artist) {
		mArtist = artist;
	}
	
	public String getArtist() {
		return mArtist;
	}
	
	public void setAlbum(String album) {
		mAlbum = album;
	}
	
	public String getAlbum() {
		return mAlbum;
	}
	
	public void setUrl(String url) {
		mUrl = url;
	}
	
	public String getUrl() {
		return mUrl;
	}
	
	public void setFileSize(int size) {
		mFileSize = size;
	}
	
	public int getFilesize() {
		return mFileSize;
	}
	
	public void setDisplayFileSize(String displaySize) {
		mDisplaySize = displaySize;
	}
	
	public String getDisplayFileSize() {
		return mDisplaySize;
	}
	
	public void setImageUrl(String url) {
		mImageUrl = url;
	}
	
	public String getImageUrl() {
		return mImageUrl;
	}
	
	public void setType(String type) {
		mType = type;
	}
	
	public String getType() {
		return mType;
	}
	
	
	public String toString() {
		StringBuffer sb = new StringBuffer("title=" + mTitle);
		sb.append(",artist=" + mArtist);
		sb.append(",album=" + mAlbum);
		sb.append(",url=" + mUrl);
		/*sb.append(",displaysize=" + mDisplaySize);
		sb.append(",filesize=" + mFileSize);
		sb.append(",lyricurl=" + mImageUrl);
		sb.append(",type=" + mType); */
		return sb.toString();
	}

	public boolean equals(MusicInfo info)
	{
		return mTitle.equals(info.getTitle()) && mArtist.equals(info.getArtist());
	}
	
	
}
