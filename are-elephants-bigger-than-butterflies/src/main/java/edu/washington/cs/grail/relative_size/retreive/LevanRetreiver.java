package edu.washington.cs.grail.relative_size.retreive;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import edu.washington.cs.grail.relative_size.retreive.models.Detector;
import edu.washington.cs.grail.relative_size.utils.Config;

public class LevanRetreiver implements DetectorRetreiver {

	private static final String urlFormat = Config.getValue("levan-url-format",
			"http://levan.cs.washington.edu/download_concept.php?concept=%s");
	private static final Logger LOG = Logger.getLogger(LevanRetreiver.class.getName());

	public Detector getDetector(String object) {
		URL url;
		try {
			url = new URL(String.format(urlFormat, object));
		} catch (MalformedURLException e) {
			LOG.severe("Error in code. Malformed url. " + e.getMessage());
			return null;
		}

		return new Detector(url, object);
	}
}
