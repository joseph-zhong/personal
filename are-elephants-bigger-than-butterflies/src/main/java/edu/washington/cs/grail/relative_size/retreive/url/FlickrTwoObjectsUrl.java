package edu.washington.cs.grail.relative_size.retreive.url;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Logger;

import edu.washington.cs.grail.relative_size.tags.models.TagUrl;
import edu.washington.cs.grail.relative_size.utils.Config;

public class FlickrTwoObjectsUrl {
	private static Logger LOG = Logger.getLogger(FlickrTwoObjectsUrl.class
			.getName());
	private static String URLS_DIRECTORY = Config.getValue(
			"flickr-url-directory", "data/urls");
	private static int URLS_COUNT_EACH_PAIR = Config.getIntValue(
			"flickr-url-pair-count", 100);

	private Map<String, PriorityQueue<UrlValue>> urlHeap = new TreeMap<String, PriorityQueue<UrlValue>>();

	public FlickrTwoObjectsUrl(List<String> objects) {
		for (int i = 0; i < objects.size(); i++)
			for (int j = i + 1; j < objects.size(); j++) {
				String obj1 = objects.get(i);
				String obj2 = objects.get(j);

				String pair = getPairString(obj1, obj2);
				
				if (!urlFileExist(pair)){
					urlHeap.put(pair, new PriorityQueue<UrlValue>());
				}

			}
	}

	/**
	 * This constructor can be used only for getting the urls and not for adding
	 * urls to the base.
	 */
	public FlickrTwoObjectsUrl() {
	}

	public void addClique(TagUrl tagUrl) {
		String[] tags = tagUrl.getTags();
		UrlValue urlValue = new UrlValue(tagUrl.getUrl(), -tags.length);

		for (int i = 0; i < tags.length; i++)
			for (int j = i + 1; j < tags.length; j++) {
				String pair = getPairString(tags[i], tags[j]);

				PriorityQueue<UrlValue> curHeap = urlHeap.get(pair);

				if (curHeap != null) {
					if (curHeap.size() < URLS_COUNT_EACH_PAIR) {
						curHeap.add(urlValue);
					} else if (curHeap.peek().getValue() < urlValue.getValue()) {
						curHeap.remove();
						curHeap.add(urlValue);
					}
				}
			}
	}

	public void writeAllUrls() {
		for (Map.Entry<String, PriorityQueue<UrlValue>> urlEntry : urlHeap
				.entrySet()) {
			String path = getPathToUrlFile(urlEntry.getKey());
			PrintStream ps = null;
			try {
				ps = new PrintStream(path);
				PriorityQueue<UrlValue> urlHeap = urlEntry.getValue();
				for (UrlValue urlValue : urlHeap) {
					ps.println(urlValue.getUrl());
				}
			} catch (FileNotFoundException e) {
				LOG.warning("Could not create/modify file " + path
						+ " to add urls.");
				continue;
			} finally {
				if (ps != null)
					ps.close();
			}
		}

	}

	public Scanner getUrlScanner(String obj1, String obj2) {
		String pair = getPairString(obj1, obj2);
		String path = getPathToUrlFile(pair);
		try {
			return new Scanner(new File(path));
		} catch (FileNotFoundException e) {
			return null;
		}
	}
	
	public int getUrlsCount(String obj1, String obj2) {
		String pair = getPairString(obj1, obj2);
		String path = getPathToUrlFile(pair);
		try {
			return countLines(path);
		} catch (IOException e) {
			return 0;
		}
	}
	
	public static int countLines(String filename) throws IOException {
	    InputStream is = new BufferedInputStream(new FileInputStream(filename));
	    try {
	        byte[] c = new byte[1024];
	        int count = 0;
	        int readChars = 0;
	        boolean empty = true;
	        while ((readChars = is.read(c)) != -1) {
	            empty = false;
	            for (int i = 0; i < readChars; ++i) {
	                if (c[i] == '\n') {
	                    ++count;
	                }
	            }
	        }
	        return (count == 0 && !empty) ? 1 : count;
	    } finally {
	        is.close();
	    }
	}

	private boolean urlFileExist(String subject) {
		String path = getPathToUrlFile(subject);
		return new File(path).exists();
	}

	private String getPathToUrlFile(String subject) {
		return URLS_DIRECTORY + File.separator + subject;
	}

	private String getPairString(String obj1, String obj2) {
		if (obj1.compareTo(obj2) < 0) {
			String tmp = obj1;
			obj1 = obj2;
			obj2 = tmp;
		}
		return obj1 + "-" + obj2;
	}

	private static class UrlValue implements Comparable<UrlValue> {
		private String url;
		private int value;

		public UrlValue(String url, int value) {
			this.url = url;
			this.value = value;
		}

		public int compareTo(UrlValue o) {
			return this.value - o.value;
		}

		public int getValue() {
			return value;
		}

		public String getUrl() {
			return url;
		}

	}
}
