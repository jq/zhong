package com.cinla.ringtone;

import java.util.ArrayList;

import de.umass.lastfm.Chart;
import de.umass.lastfm.ImageSize;
import de.umass.lastfm.MusicEntry;
import de.umass.lastfm.PaginatedResult;
import de.umass.lastfm.Track;

public class HypedTracksFetcher implements ITopChartFetcher {

	@Override
	public ArrayList<TopItem> getTopItemList() {
		Utils.D("In get TopItemList method.");
		ArrayList<TopItem> topItemList = new ArrayList<TopItem>();
		try {
			PaginatedResult<Track> result = Chart.getHypedTracks(Constant.LASTFM_API_KEY);
			if (result == null) {
				Utils.D("get lastfm list size: "+"null");

			} else {
				Utils.D("get lastfm list size: "+result.getTotalPages());
			}
			for (MusicEntry musicEntry : result.getPageResults()) {
				TopItem topItem = new TopItem(musicEntry.getName(), musicEntry.getImageURL(ImageSize.SMALL));
				topItemList.add(topItem);
			}
		} catch (Exception e) {
			Utils.D("Exception in TopChartFetcher: " + e.getMessage());
			return null;
		}
		return topItemList;
	}

}
