package com.cinla.ringtone;

import java.util.ArrayList;

public class TopChartFactory {
	
	public static ITopChartFetcher getTopChartFetcher(int id) {
		switch (id) {
		case Constant.CHART_TYPE_TOPTRACKS:
			return new TopTracksFetcher();
		case Constant.CHART_TYPE_TOPARTISTS:
			return new TopArtistsFetcher();
		case Constant.CHART_TYPE_LOVEDTRACKS:
			return new LovedTracksFetcher();
		case Constant.CHART_TYPE_TOPTAGS:
			return new TopTagsFetcher();
		case Constant.CHART_TYPE_HYPEDTRACKS:
			return new HypedTracksFetcher();
		case Constant.CHART_TYPE_HYPEDARTISTS:
			return new HypedArtistsFetcher();
		default:
			break;
		}
		return null;
	}
}
