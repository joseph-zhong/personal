package edu.washington.cs.grail.relative_size.nlp.search;

import java.io.Serializable;

import edu.washington.cs.grail.relative_size.nlp.search.models.SearchFailedException;
import edu.washington.cs.grail.relative_size.nlp.search.models.WebSearchResults;

public interface WebSearcher extends Serializable {
	WebSearchResults search(String query) throws SearchFailedException;
}
