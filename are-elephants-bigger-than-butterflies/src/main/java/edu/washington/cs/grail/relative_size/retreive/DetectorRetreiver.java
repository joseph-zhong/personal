package edu.washington.cs.grail.relative_size.retreive;

import edu.washington.cs.grail.relative_size.retreive.models.Detector;

public interface DetectorRetreiver {
	public Detector getDetector(String object);
}
