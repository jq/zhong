
public class MusicInfo {
	private String mCategory;
	private String mArtist;
	private String  mSize;  // like:58kb
	private String mTitle;
	private String mUUID;
	private String mFormat;	// like: ".mp3"
	private String mRingName;
	private String mImgName;
	private int mIndex;
	private int mMark;      // [0, 100]
	private int mDownloads; 
	
	public String getRingName() {
		return mRingName;
	}
	
	public String getUUID() {
		return mUUID;
	}
	
	public String getImageName() {
		return mImgName;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getArtist() {
		return mArtist;
	}
	
	public String getSize() {
		return mSize;
	}
	
	public String getCategory() {
		return mCategory;
	}
	
	public int getIndex() {
		return mIndex;
	}
	
	public int getMark() {
		return mMark;
	}
	
	public int getDownloads() {
		return mDownloads;
	}
	
	public String getFormat() {
		return mFormat;
	}
	
	public void setImgName(String imgName) {
		mImgName = imgName;
	}
	
	public void setRingName(String ringName) {
		mRingName = ringName;
	}
	
	public void setUUID(String uuid) {
		mUUID = uuid;
	}
	
	public MusicInfo(int index, String category, String title, 
			String artist, String format, String size) {
		mIndex = index;
		mCategory = category;
		mTitle = title;
		mArtist = artist;
		mSize = size;
		mFormat = format;
		mMark = 80;
		mDownloads = 5;
	}
	
}
