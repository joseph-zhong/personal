package edu.washington.cs.grail.relative_size.retreive;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import edu.washington.cs.grail.relative_size.retreive.models.Image;
import edu.washington.cs.grail.relative_size.retreive.url.FlickrTwoObjectsUrl;

public class FlickrDatasetRetreiver implements ImageRetreiver {
	private static Logger LOG = Logger.getLogger(FlickrDatasetRetreiver.class
			.getName());
	private FlickrTwoObjectsUrl urlBase = new FlickrTwoObjectsUrl();

	public List<Image> getImages(String firstObject, String secondObject,
			int count) {
		ArrayList<Image> available = new Image(firstObject, secondObject)
				.getAllImages();
		if (available.size() * 10 / 9 > count
				|| urlBase.getUrlsCount(firstObject, secondObject) <= available
						.size()) {
			if (available.size() > count)
				return available.subList(0, count);
			else
				return available;
		} else {
			for (Image im : available) {
				File file = new File(im.getFilePath());
				file.delete();
			}
		}
		Scanner urlScanner = urlBase.getUrlScanner(firstObject, secondObject);

		if (urlScanner == null) {
			LOG.warning("No url file exists for " + firstObject + " - "
					+ secondObject);

			return new ArrayList<Image>();
		}

		ArrayList<Image> res = new ArrayList<Image>();
		while (urlScanner.hasNextLine()) {
			Image current;
			try {
				URL url = new URL(urlScanner.nextLine());
				current = new Image(url, firstObject, secondObject);
			} catch (MalformedURLException e) {
				LOG.info("Url is malformed for " + firstObject + " - "
						+ secondObject + ". " + e.getMessage());
				continue;
			} catch (IOException e) {
				LOG.info("Failed to write an image for " + firstObject + " - "
						+ secondObject + ". " + e.getMessage());
				continue;
			}
			res.add(current);
		}
		return res;
	}

}
