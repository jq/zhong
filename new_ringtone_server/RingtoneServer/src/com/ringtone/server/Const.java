package com.ringtone.server;

import java.util.ArrayList;
import java.util.HashMap;


public class Const {
	public static final String ID = "id";
	public static final String UUID = "uuid";
	public static final String TITILE = "title";
	public static final String ARTIST = "artist";
	public static final String CATEGORY = "category";
	public static final String DOWNLOAD_COUNT = "download_count";
	public static final String AVG_RATE = "avg_rate";
	public static final String SIZE = "size";
	public static final String FILE_NAME = "file_name";
	public static final String IMAGE = "image";
	public static final String S3URL = "s3url";
	public static final String RECORD = "record";
	public static final String ADD_DATE = "add_date";
	public static final String RATE = "rate";
	
	public static final String QUERY = "q";
	public static final String START = "start";
	public static final String JSON = "json";
	public static final String KEY = "key";
	
	public static final String TYPE = "type";
	
	public static final int MAX_RESULTS_PER_QUERY = 10;
	
	public static final String CATE_CHRISTIAN = "Christian";
	public static final String CATE_METAL = "Metal";
	public static final String CATE_HOLIDAY = "Holiday";
	public static final String CATE_RANDB = "R&B";
	public static final String CATE_WORLD_MUSIC = "World Music";
	public static final String CATE_POP = "Pop";
	public static final String CATE_ROCK = "Rock";
	public static final String CATE_GAMES = "GAMES";
	public static final String CATE_DANCE = "Dance";
	public static final String CATE_RAP = "Rap";
	public static final String CATE_JAZZ = "Jazz";
	public static final String CATE_HIP_HOP = "Hip-Hop";
	public static final String CATE_GOSPEL = "Gospel";
	public static final String CATE_TV = "TV";
	public static final String CATE_HARD_ROCK = "Hard Rock";
	public static final String CATE_ELECTRONIC = "Electronic";
	public static final String CATE_LATIN_MUSIC = "Latin Music";
	public static final String CATE_BLUES = "Blues";
	public static final String CATE_SOUND_EFFECTES = "Sound Effects";
	public static final String CATE_CLASSICAL = "Classical";
	public static final String CATE_COMEDY = "Comedy";
	public static final String CATE_COUNTRY = "Country";
	
	public static final String S3_PREFIX_M4R = "https://s3.amazonaws.com/ringtone_m4r/";
	
	public static final String[] CATEGORIES_NAME = { "Christian and Gospel", "Metal", "Holiday", "R&B Soul",
													 "Techno", "Pop", "Rock", "Video Games", "Jazz", "International",
													 "Hip-Hop", "Movies and TV", "Latin", "Blues", "Sound Effects",
													 "Classical", "Comedy", "Country", "Reggae"};
	
	public static final String[] CATEGORIES_VALUE = { "Christian and Gospel", "Metal", "Holiday", "Rnb_Soul",
													  "Techno", "Pop", "Rock", "Vedio Games", "Jazz", "Internation",
													  "Hip-Hop", "Movies and TV", "Latin", "Blues", "Sound Effects", 
													  "Classical", "Comedy", "Country", "Reggae"};

 	
	public static final String[] HOME_CATEGORIES = { "Country", "Hip-Hop", "Rock", "Pop"};
	
	public static final String EMAIL = "email";
	public static final String DOWNLOAD_LINK = "download_link";
	public static final String FROM_EMAIL = "support@ringdroid.me";
	public static final String EMAIL_BODY1 =   "Hello, you have requested to download a ringtone ";
	public static final String EMAIL_BODY2 = " using Ringtones for iPhone. Follow these instructions to install:\n"
											+ "1. On your computer, click ";
	public static final String EMAIL_BODY3 = " and save the file.\n"
											+ "2. Double click on the file to launch iTunes. The file will automatically be placed in the \"Ringtone\" folder.\n"
											+ "3. Make sure your iPhone is connected with your computer and sync your iPhone with iTunes.\n"
											+ "4. On your iPhone, choose Settings > Sounds > Ringtone, then set this as the new ringtone.\n"
											+ "If you need more help, please contact support@ringdroid.me";
	public static final String EMAIL_SUBJECT = "Download Ringtones For Your iPhone ";
}
