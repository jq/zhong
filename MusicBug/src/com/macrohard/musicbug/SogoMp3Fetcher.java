package com.macrohard.musicbug;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;


// This class is not thread safe.
public class SogoMp3Fetcher implements Mp3FetcherInterface {
	private static final String BASE_URL = "http://mp3.sogou.com/music.so?pf=mp3&query=";
	private static final String SOGOU_MP3_URL = "http://mp3.sogou.com";
	private static final String TAG = Debug.TAG;

	private static int[] FILE_KINDS =
		new int[]{
		Const.FILE_KIND_RINGTONE,
		Const.FILE_KIND_NOTIFICATION,
		Const.FILE_KIND_ALARM};

	private String mKeyWords;
	private final String mLink;
	private int mNextPage;
	private boolean mDone;
	private Context mContext;

	private String getLink(String key) {
		String reqString = null;
		try {
			reqString = URLEncoder.encode(key, "GB2312");
		} catch (UnsupportedEncodingException e) {
			reqString = URLEncoder.encode(key);
		}
		return BASE_URL + reqString;
	}


	public SogoMp3Fetcher(Context context, String keyWords) {
		mKeyWords = keyWords;
		mLink = getLink(mKeyWords);  // Base link.
		mNextPage = 1;
		mDone = false;
		mContext = context;
	}

	private String getNextUrl() {
		return mNextPage == 1 ? mLink : mLink + "&page=" + mNextPage;
	}

	// TODO(zyu): Rewrite the following code.
	// Consider using cache?
	private ArrayList<MP3Info> getListBatchByUrl(String urlStr, int limit) {
		Debug.D("mp3 url = " + urlStr);

		int cnt = 0;
		ArrayList<MP3Info> songs = new ArrayList<MP3Info>();
		String httpresponse = null;
		try {
			URL url = new URL(urlStr);
			HttpURLConnection urlConn = (HttpURLConnection) url
			.openConnection();
			urlConn.setRequestProperty("User-Agent",
					"Apache-HttpClient/UNAVAILABLE (java 1.4)");
			urlConn.setConnectTimeout(12000);
			urlConn.connect();

			InputStream stream = urlConn.getInputStream();

			StringBuilder builder = new StringBuilder(8 * 1024);

			char[] buff = new char[4096];

			InputStreamReader is = new InputStreamReader(stream, "gb2312");

			int len;
			while ((len = is.read(buff, 0, 4096)) > 0) {
				builder.append(buff, 0, len);
			}
			urlConn.disconnect();
			httpresponse = builder.toString();

			// Pattern pattern =
				// Pattern.compile("<td class=d><a href=\"([\\s\\S]*?)\" title=\"");
			Pattern pattern = Pattern.compile("<a pb=t class=mr style=");
			Matcher matcher = pattern.matcher(httpresponse);

			// Pattern pattern_artist = Pattern.compile("&si=.*?;;(.*?);;");
			while (matcher.find()) {
				MP3Info mp3 = new MP3Info();

				int nameStartPos = httpresponse.indexOf(" title=\"", matcher
						.start())
						+ " title=\"".length();
				int nameEndPos = httpresponse.indexOf('"', nameStartPos);

				String value = httpresponse.substring(nameStartPos, nameEndPos).trim();

				mp3.setName(value);

				Debug.D("Title = " + value);

				String singer = "";
				int artistStartPos = httpresponse.indexOf(
						"class=mr target=_blank>", nameEndPos)
						+ "class=mr target=_blank>".length();
				int artistEndPos = httpresponse.indexOf("</a>", artistStartPos);
				singer = httpresponse.substring(artistStartPos, artistEndPos);
				singer.replaceAll("<*>", " ");

				//Debug.D("Artist = " + singer);

				mp3.setArtist(singer.trim());

				int albumStartPos = httpresponse.indexOf(
						"class=mr target=_blank>", artistEndPos)
						+ "class=mr target=_blank>".length();
				int albumEndPos = httpresponse.indexOf('<', albumStartPos);
				if ((albumEndPos - albumStartPos) < 2) {
					albumStartPos = httpresponse.indexOf(
							"text-decoration:underline;\">", albumStartPos)
							+ "text-decoration:underline;\">".length();
					albumEndPos = httpresponse.indexOf('<', albumStartPos);
				}
				mp3.setAlbum(httpresponse.substring(albumStartPos, albumEndPos)
						.trim());

				int sizeStartPos = httpresponse.indexOf(
						"<td align=center>", albumEndPos) + "<td align=center>".length();
				int sizeEndPos = httpresponse.indexOf('<', sizeStartPos);

				String sizeStr = httpresponse.substring(sizeStartPos, sizeEndPos).trim();

				mp3.setSize(Utils.sizeFromStr(sizeStr));

				int linkStartPos = httpresponse.indexOf("window.open('",
						sizeEndPos) + "window.open('".length();
				int linkEndPos = httpresponse.indexOf("&ac=", linkStartPos)
				+ "&ac=".length();
				String request = httpresponse.substring(linkStartPos,
						linkEndPos);
				mp3.setLink(request.trim());

				int spdStartPos = httpresponse.indexOf(
						"span class=\"spd", sizeEndPos) + "span class=\"spd".length();
				int spdEndPos = spdStartPos + 1;

				String rateStr = httpresponse.substring(spdStartPos, spdEndPos).trim();

				mp3.setRate(Float.valueOf(rateStr));

				songs.add(mp3);
				cnt++;
				if (limit > 0 && cnt >= limit) {
					break;
				}

			}
		} catch (Exception e) {
			// ShowToastMessage("Network can not connect, please try again.");
			e.printStackTrace();
			return null;
		}

		Debug.D("songs = " + songs);

		if (songs != null) {
			Log.e(TAG, "song size: " + songs.size());
		}
		return songs;
	}


	// Given the link to each individual mp3, get the corresponding link by which we can actually download the music.
	// TODO: Make this method private in the future. It should only be used by downloadMp3.
	public static String getDownloadLink(MP3Info mp3) throws IOException {
		String request = SOGOU_MP3_URL + mp3.getLink();
		URL url = new URL(request);
		HttpURLConnection urlConn = (HttpURLConnection)url.openConnection();
		urlConn.setRequestProperty("User-Agent", "Apache-HttpClient/UNAVAILABLE (java 1.4)");
		urlConn.setConnectTimeout(12000);
		urlConn.connect();

		InputStream stream = urlConn.getInputStream();

		StringBuilder builder = new StringBuilder(8 * 1024);

		char[] buff = new char[4096];
		//必须在此指定编码，否则后面toString会导致乱码
		InputStreamReader is = new InputStreamReader(stream,"gb2312");

		int len;
		while ((len = is.read(buff, 0, 4096)) > 0) {
			builder.append(buff, 0, len);
		}
		urlConn.disconnect();
		String httpresponse = builder.toString();
		int linkStartPos = httpresponse.indexOf("下载歌曲\" href=\"") + "下载歌曲\" href=\"".length();

		int linkEndPos = httpresponse.indexOf('>', linkStartPos)-1;
		return httpresponse.substring(linkStartPos, linkEndPos);
	}


	/*
    @Override
    public void downloadMp3(MP3Info mp3, String saveFile, DefaultDownloadListener listener)
            throws Mp3FetcherException {
    	// TODO(zyu): Use DownloadFile in feebelib.
    }
	 */

	@Override
	public ArrayList<MP3Info> getNextListBatch() throws Mp3FetcherException {
		if (mDone) {
			return null;
		}
		ArrayList<MP3Info> songs = getListBatchByUrl(getNextUrl(), -1);

		if (songs != null) {
			if (songs.size() == 0) {
				mDone = true;
			} else {
				++mNextPage;
			}
		} else {
			// Some error occurred.
		}

		return songs;
	}

	@Override
	public boolean listDone() {
		return mDone;
	}

	@Override
	public void resetList() {
		mDone = false;
		mNextPage = 1;
	}

}
