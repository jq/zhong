package com.ringtone.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.apache.lucene.LucenePackage;

import com.google.appengine.api.datastore.DatastoreNeedIndexException;
import com.google.appengine.api.datastore.DatastoreTimeoutException;

public class SearchJanitor {
	
	private static final Logger log = Logger.getLogger(SearchJanitor.class.getName());
	
	public static final int MAXIMUM_NUMBER_OF_WORDS_TO_SEARCH = 10;
	
	public static final int MAX_NUMBER_OF_WORDS_TO_PUT_IN_INDEX = 200;
	
	public static final int RESULTS_PER_PAGE = 10;

	public static List<SongEntry> searchSongEntries(
			String queryString, 
			PersistenceManager pm, 
			int start) {

		StringBuffer queryBuffer = new StringBuffer();

		queryBuffer.append("SELECT FROM " + SongEntry.class.getName() + " WHERE ");

		Set<String> queryTokens = SearchJanitorUtils
				.getTokensForIndexingOrQuery(queryString,
						MAXIMUM_NUMBER_OF_WORDS_TO_SEARCH);

		List<String> parametersForSearch = new ArrayList<String>(queryTokens);

		StringBuffer declareParametersBuffer = new StringBuffer();

		int parameterCounter = 0;

		while (parameterCounter < queryTokens.size()) {

			queryBuffer.append("fts == param" + parameterCounter);
			declareParametersBuffer.append("String param" + parameterCounter);

			if (parameterCounter + 1 < queryTokens.size()) {
				queryBuffer.append(" && ");
				declareParametersBuffer.append(", ");

			}

			parameterCounter++;

		}

		System.out.println("QueryBuffer: "+queryBuffer.toString());
		Query query = pm.newQuery(queryBuffer.toString());

		query.setRange(start, start+RESULTS_PER_PAGE);
		
		if (parameterCounter <=3) {
			query.setOrdering("download_count desc");
		}
//		query.setOrdering("download_count desc");

		query.declareParameters(declareParametersBuffer.toString());

		List<SongEntry> result = null;
		
		try {
			result = (List<SongEntry>) query.executeWithArray(parametersForSearch
				.toArray());
		
		} catch (DatastoreTimeoutException e) {
			log.severe(e.getMessage());
			log.severe("datastore timeout at: " + queryString);// + " - timestamp: " + discreteTimestamp);
		} catch(DatastoreNeedIndexException e) {
			log.severe(e.getMessage());
			log.severe("datastore need index exception at: " + queryString);// + " - timestamp: " + discreteTimestamp);
		}

		return result;

	}

	public static void updateFTSStuffForSongEntry(
			SongEntry songEntry) {

		StringBuffer sb = new StringBuffer();
		
		sb.append(songEntry.getContent());
		
		Set<String> new_ftsTokens = SearchJanitorUtils.getTokensForIndexingOrQuery(
				sb.toString(),
				MAX_NUMBER_OF_WORDS_TO_PUT_IN_INDEX);
		
		
		Set<String> ftsTokens = songEntry.getFts();
	
			ftsTokens.clear();

			for (String token : new_ftsTokens) {
				ftsTokens.add(token);

			}		
	}
}
