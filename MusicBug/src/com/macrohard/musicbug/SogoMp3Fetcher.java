package com.macrohard.musicbug;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;


// This class is not thread safe.
public class SogoMp3Fetcher implements IMp3Fetcher {
	private static final String BASE_URL = "http://mp3.sogou.com/music.so?pf=mp3&query=";
	private static final String SOGOU_MP3_URL = "http://mp3.sogou.com";
	private static final String TAG = Debug.TAG;

	private String mKeyWords;
	private final String mLink;
	private int mNextPage;
	private boolean mDone;

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
	}

	private String getNextUrl() {
		return mNextPage == 1 ? mLink : mLink + "&page=" + mNextPage;
	}

	// TODO(zyu): Rewrite the following code.
	// Consider using cache?
	private ArrayList<MP3Info> getListBatchByUrl(String urlStr, int limit) {
		Debug.D("mp3 url = " + urlStr);

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

			Pattern PATTERN_ROW = Pattern.compile("<tr(.*?)</tr>", Pattern.DOTALL);
			Pattern PATTERN = Pattern.compile(
			"<td.*?\\btitle=\"([^\"]*)\".*?" +   // 1
			"<td.*?\\bsinger=\"([^\"]*)\".*?" +   // 2
			"<td.*?\\btitle=\"([^\"]*)\".*?" +   // 3
			"<td.*?</td>.*?" +  // Ignore
			"<td.*?</td>.*?" +  // Ignore
			"<td.*?\'(/down.so.*?)\'.*?" +  // 6
			"<td.*?href=\"([^\"]*)\".*?" +  // 7
			"<td.*?</td>.*?" +  // Ignore
			"<td.*?>([^<]*)<.*?" +   // 9
			"<td.*?>([^<]*)<" +   // 10
			""
			, Pattern.DOTALL);
			
			
			Matcher matcherRow = PATTERN_ROW.matcher(httpresponse);
			while (matcherRow.find()) {
				Matcher m = PATTERN.matcher(matcherRow.group(1));
				while (m.find()) {
					MP3Info info = new MP3Info();
					info.setName(m.group(1).trim());
					info.setArtist(URLDecoder.decode(m.group(2), "gb2312").trim());
					info.setAlbum(m.group(3).trim());
					info.setLink(m.group(4).trim());
					info.setSize(Utils.sizeFromStr(m.group(6).trim()));
					
					songs.add(info);
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
	public String getDownloadLink(MP3Info mp3) throws Mp3FetcherException {
		try {
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
		} catch (IOException e) {
			e.printStackTrace();
			throw new Mp3FetcherException("Failure in getting mp3 download link: " + mp3.getLink());
		}
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
