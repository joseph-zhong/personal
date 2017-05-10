package edu.washington.cs.grail.relative_size.retreive;

import java.util.List;

import edu.washington.cs.grail.relative_size.retreive.models.Image;

public interface ImageRetreiver {
	
	public List<Image> getImages(String firstObject, String secondObject, int count);
}
