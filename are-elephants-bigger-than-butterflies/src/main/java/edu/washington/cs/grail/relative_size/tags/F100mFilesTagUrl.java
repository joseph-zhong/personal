package edu.washington.cs.grail.relative_size.tags;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.logging.Logger;

import edu.washington.cs.grail.relative_size.tags.models.TagUrl;

public class F100mFilesTagUrl implements TagUrlReader {
	private static final int F100M_FILES_COUNT = 10;
	private static final int PHOTO_VIDEO_INDEX = 22;
	private static final int USER_TAGS_INDEX = 8;
	private static final int CONTENT_URL_INDEX = 14;
	private static final Logger LOG = Logger.getLogger(F100mFilesTagUrl.class.getName());
	
	private int cnt = 0;
	private String pathPrefix;
	private Scanner fileScanner;
	protected String[] infos;
	
	public F100mFilesTagUrl(String pathPrefix) throws FileNotFoundException {
		this.pathPrefix = pathPrefix;
		fileScanner = new Scanner(new File(pathPrefix + (cnt++)));
	}

	public TagUrl nextTagUrl() {
		if (!fileScanner.hasNextLine()){
			fileScanner.close();
			if (cnt >= F100M_FILES_COUNT)
				return null;
			try {
				fileScanner = new Scanner(new File(pathPrefix + (cnt++)));
			} catch (FileNotFoundException e) {
				LOG.warning("File " + (pathPrefix + (cnt++)) + " doesn't exist.");
				return null;
			}
		}
		
		infos = fileScanner.nextLine().split("\t");
		if (!infos[PHOTO_VIDEO_INDEX].equals("0"))
			return nextTagUrl();
		
		String userTagsString = infos[USER_TAGS_INDEX];
		String url = infos[CONTENT_URL_INDEX];
		
		if (userTagsString.isEmpty())
			return new TagUrl(new String[]{}, url);
		else
			return new TagUrl(userTagsString.split(","), url);
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			super.finalize();
		} finally {
			fileScanner.close();
		}
	}
}
