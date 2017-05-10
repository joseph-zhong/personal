package edu.washington.cs.grail.relative_size.retreive;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.SearchParameters;
import com.flickr4java.flickr.tags.Tag;

import edu.washington.cs.grail.relative_size.retreive.models.Image;
import edu.washington.cs.grail.relative_size.utils.CollectionParallelComputation;
import edu.washington.cs.grail.relative_size.utils.Config;

public class FlickrApiRetreiver implements ImageRetreiver {
	private static final int RETREIVE_FACTOR = Config.getIntValue("flickrapi-retreive-factor", 50);
	private static final int MAXIMUM_PER_PAGE = Config.getIntValue("flickrapi-maximum-page", 500);
	
	public static final String API_KEY = Config.getValue("flickr-api-key", "5d6934acb6a5966f79efdae0bbdefa56");
	public static final String SHARED_SECRET = Config.getValue("flickr-shared-secret", "aa98f5d5d8774e86");
	
	private Flickr flickr;
	private static final Logger LOG = Logger.getLogger(FlickrApiRetreiver.class.getName());

	public FlickrApiRetreiver() {
		flickr = new Flickr(API_KEY, SHARED_SECRET, new REST());
	}

	public List<Image> getImages(String firstObject, String secondObject,
			int count) {
		
		ArrayList<Image> available = new Image(firstObject, secondObject).getAllImages();
		
		ArrayList<Image> images = new ArrayList<Image>();
		for (int i=0 ; i<count && i<available.size() ; i++)
			images.add( available.get(i) );
		count -= images.size();
		if (count == 0){
			LOG.info("Sufficient images already exist for " + firstObject + " and " + secondObject);
			return images;
		}
			
		
		ArrayList<Photo> flickrPhotos = getFlickrPhotos(firstObject,
				secondObject, count);

		

		for (Photo photo : flickrPhotos) {
			URL url;
			try {
				url = new URL(photo.getSmallUrl());
			} catch (MalformedURLException e) {
				LOG.warning("Photo url is malformed. " + e.getMessage());
				continue;
			}
			Image current;
			try {
				current = new Image(url, firstObject, secondObject);
			} catch (IOException e) {
				LOG.info("Failed to write an image for " + firstObject + " - "
						+ secondObject + ". " + e.getMessage());
				continue;
			}

			images.add(current);
		}
		return images;
	}

	private ArrayList<Photo> getFlickrPhotos(String firstObject,
			String secondObject, int count) {
		SearchParameters searchParams = new FlickrSearchParameters(firstObject,
				secondObject);

		int samples = RETREIVE_FACTOR * count;
		ArrayList<Photo> sampleList = new ArrayList<Photo>(samples);

		for (int page = 1; (page - 1) * MAXIMUM_PER_PAGE < samples; page++) {
			try {
				int thisPageSize = Math.min(MAXIMUM_PER_PAGE, samples
						- sampleList.size());
				sampleList.addAll(flickr.getPhotosInterface().search(
						searchParams, thisPageSize, page));
			} catch (FlickrException e) {
				LOG.warning("Flickr photo search failed with error code "
						+ e.getErrorCode());
			}
		}
		new CollectionParallelComputation<Photo>(sampleList) {
			@Override
			public void run(Photo element) {
				try {
					element.setTags(flickr.getTagsInterface()
							.getListPhoto(element.getId()).getTags());
					element.setComments(FlickrApiRetreiver.this.hashCode(element
							.getTags()));
				} catch (FlickrException e) {
					LOG.warning("Failed to get info for photo "
							+ element.getId() + ". Error code: "
							+ e.getErrorCode());
				}
			}
		}.start().join();
		
		Collections.sort(sampleList, new PhotoCompare());

		ArrayList<Photo> results = new ArrayList<Photo>();
		for (int i = 0; i < sampleList.size() && results.size() < count; i++) {
			Photo current = sampleList.get(i);

			if ((i == 0 || sampleList.get(i - 1).getComments() != current
					.getComments())
					&& (i == sampleList.size() - 1 || sampleList.get(i + 1)
							.getComments() != current.getComments())) {
				results.add(current);
			}
		}

		return results;
	}

	private int hashCode(Collection<Tag> tags) {
		int result = 0;
		int prime = 1000000007;
		for (Tag tag : tags) {
			result = result * prime + tag.getValue().hashCode();
		}
		return result;
	}

	private class PhotoCompare implements Comparator<Photo> {
		public int compare(Photo p1, Photo p2) {
			if (p1.getTags().size() != p2.getTags().size()) {
				return p1.getTags().size() - p2.getTags().size();
			} else {
				return p1.getComments() - p2.getComments();
			}
		}
	}
}

class FlickrSearchParameters extends SearchParameters {
	private Logger logger = Logger.getLogger("Flickr Params");

	public FlickrSearchParameters(String firstObject, String secondObject) {
		setTags(new String[] { firstObject, secondObject });
		setTagMode("all");
		setSort(RELEVANCE);
		try {
			setMedia("photos");
		} catch (FlickrException e) {
			logger.severe("Error in code. Failed to set content type.");
		}
	}

}
