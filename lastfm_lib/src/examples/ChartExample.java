package examples;

import java.util.Collection;

import de.umass.lastfm.Artist;
import de.umass.lastfm.Chart;
import de.umass.lastfm.ImageSize;
import de.umass.lastfm.PaginatedResult;
import de.umass.lastfm.Track;

public class ChartExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String key = "b25b959554ed76058ac220b7b2e0a026"; //this is the key used in the last.fm API examples online.
		PaginatedResult<Artist> topArtist = Chart.getHypedArtists(key);
		System.out.println("Top Artists: ");
		System.out.println("Total pages: "+topArtist.getTotalPages());
		for (Artist artist : topArtist.getPageResults()) {
			System.out.println(topArtist.getPage());
			System.out.println(artist.getName());
			System.out.println(artist.getImageURL(ImageSize.SMALL));
//			System.out.printf("%s (%d plays)%n", track.getName(), track.getPlaycount());
		}
	}

}
