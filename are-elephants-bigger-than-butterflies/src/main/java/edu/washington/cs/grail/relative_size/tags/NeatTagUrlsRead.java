package edu.washington.cs.grail.relative_size.tags;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import edu.washington.cs.grail.relative_size.tags.models.TagUrl;
import edu.washington.cs.grail.relative_size.utils.Config;

public class NeatTagUrlsRead implements TagUrlReader {
	private static final String NEAT_TAGS_PATH = Config.getValue(
			"tags-neat-path", "data/datasets/Flickr100tags/tags.txt");
	private Scanner fileScanner;

	public NeatTagUrlsRead() throws FileNotFoundException {
		fileScanner = new Scanner(new File(NEAT_TAGS_PATH));
	}

	public TagUrl nextTagUrl() {
		if (!fileScanner.hasNextLine()) {
			fileScanner.close();
			return null;
		}
		String[] tags = fileScanner.nextLine().split("\t");
		String url = fileScanner.nextLine();
		return new TagUrl(tags, url);
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
