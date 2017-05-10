package edu.washington.cs.grail.relative_size.nlp.search;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;
import java.util.logging.Logger;

import com.google.gson.Gson;

import edu.washington.cs.grail.relative_size.nlp.search.models.GoogleSearchResponse;
import edu.washington.cs.grail.relative_size.nlp.search.models.SearchFailedException;
import edu.washington.cs.grail.relative_size.nlp.search.models.WebSearchResults;
import edu.washington.cs.grail.relative_size.utils.Config;
import edu.washington.cs.grail.relative_size.utils.Downloader;

public class GoogleSearch implements WebSearcher {
	private static final long serialVersionUID = 1816807170862167366L;

	private static final Logger LOG = Logger.getLogger(GoogleSearch.class
			.getName());

	private static final String GOOGLE_API_KEY = Config.getValue(
			"google-api-key", "");
	private static final String GOOGLE_SEARCH_ID = Config.getValue(
			"google-search-id", "");
	private static final String GOOGLE_SEARCH_URL_FORMAT = "https://www.googleapis.com/customsearch/v1?key="
			+ GOOGLE_API_KEY + "&cx=" + GOOGLE_SEARCH_ID + "&q=%s";
	private static final String GOOGLE_SEARCH_CORPUS_FORMAT = "data/searches/google/%s";

	private Gson gson = new Gson();

	public WebSearchResults search(String query) throws SearchFailedException {
		String jsonResponse = getJsonReponse(query);
		return gson.fromJson(jsonResponse, GoogleSearchResponse.class);
	}

	@Override
	public String toString() {
		return "Google Search Engine with search-id=" + GOOGLE_SEARCH_ID
				+ " and api-key=" + GOOGLE_API_KEY;
	}

	private String readFromCorpus(String query) {
		try {
			String filePath = String.format(GOOGLE_SEARCH_CORPUS_FORMAT, query.hashCode());
			Scanner fileScanner = null;

			try {
				fileScanner = new Scanner(new File(filePath));
				fileScanner.useDelimiter("\\A");
				return fileScanner.next();
			} finally {
				if (fileScanner != null)
					fileScanner.close();
			}
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	private void writeToCorpus(String query, String jsonResponse) {
		try {
			String filePath = String.format(GOOGLE_SEARCH_CORPUS_FORMAT, query.hashCode());
			PrintStream ps = null;
			try {
				ps = new PrintStream(filePath);
				ps.println(jsonResponse);
			} finally {
				if (ps != null)
					ps.close();
			}
		} catch (FileNotFoundException e) {
			LOG.warning("Failed to write search result to file. Abort: "
					+ e.getMessage());
		}
	}

	private String readFromServer(String query) throws SearchFailedException {
		System.out.println(query);
		String jsonResponse = null;
		try {
			jsonResponse = Downloader.download(new URL(
					String.format(GOOGLE_SEARCH_URL_FORMAT,
							URLEncoder.encode(query, "UTF-8"))));
		} catch (IOException e) {
			throw new SearchFailedException(e.getMessage());
		}

		writeToCorpus(query, jsonResponse);
		return jsonResponse;
	}

	private String getJsonReponse(String query) throws SearchFailedException {
		String jsonResponse = readFromCorpus(query);

		if (jsonResponse == null)
			jsonResponse = readFromServer(query);

		return jsonResponse;
	}
	
}
