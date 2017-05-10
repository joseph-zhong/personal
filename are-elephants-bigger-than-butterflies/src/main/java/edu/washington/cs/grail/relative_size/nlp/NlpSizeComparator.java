package edu.washington.cs.grail.relative_size.nlp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Logger;

import edu.washington.cs.grail.relative_size.SizeComparator;
import edu.washington.cs.grail.relative_size.graph.models.Gaussian;
import edu.washington.cs.grail.relative_size.nlp.search.GoogleSearch;
import edu.washington.cs.grail.relative_size.nlp.search.WebSearcher;
import edu.washington.cs.grail.relative_size.nlp.search.models.SearchFailedException;
import edu.washington.cs.grail.relative_size.nlp.search.models.SearchResult;
import edu.washington.cs.grail.relative_size.nlp.search.models.WebSearchResults;

public class NlpSizeComparator implements SizeComparator, Serializable {
	private static final long serialVersionUID = -1550066817945693885L;
	private static final Logger LOG = Logger.getLogger(NlpSizeComparator.class
			.getName());
	private static final String RELATIVE_TEMPLATES_FILE = "data/searches/relativeTemplates";
	private static final String ABSOLUTE_TEMPLATES_FILE = "data/searches/absoluteTemplates";
	private static final int QUERY_WORDS_LIMIT = 32;
	private static AverageMethod METHOD = AverageMethod.LOG_SIZE;
	
	private List<String> relativeTemplates, absoluteTempates;
	private List<Double> unitScales;
	private Map<String, List<Double>> observationCache = new TreeMap<String, List<Double>>();
	private WebSearcher searcher = new GoogleSearch();

	public NlpSizeComparator() {
		relativeTemplates = new ArrayList<String>();
		Scanner fileScanner = null;
		try {
			fileScanner = new Scanner(new File(RELATIVE_TEMPLATES_FILE));
		} catch (FileNotFoundException e) {
			LOG.severe("Search templates file not found! " + e.getMessage());
			return;
		}

		while (fileScanner.hasNextLine()) {
			String template = fileScanner.nextLine().trim();
			if (!template.isEmpty())
				relativeTemplates.add(template);
		}
		fileScanner.close();

		absoluteTempates = new ArrayList<String>();
		unitScales = new ArrayList<Double>();
		fileScanner = null;
		try {
			fileScanner = new Scanner(new File(ABSOLUTE_TEMPLATES_FILE));
		} catch (FileNotFoundException e) {
			LOG.severe("Search templates file not found! " + e.getMessage());
			return;
		}
		while (fileScanner.hasNextLine()) {
			String line = fileScanner.nextLine().trim();
			if (line.isEmpty())
				continue;
			String[] parts = line.split("&");
			absoluteTempates.add(parts[0]);
			unitScales.add(Double.parseDouble(parts[1]));
		}
		fileScanner.close();
	}

	public double compare(String A, String B) {
		A = A.replace('_', ' ');
		B = B.replace('_', ' ');
		
		Gaussian gaussianA = getLogAbsoluteSizeGaussian(A);
		Gaussian gaussianB = getLogAbsoluteSizeGaussian(B);
		
		Double size1 = getAbsoluteSize(A);
		Double size2 = getAbsoluteSize(B);

		if (size1 == null || size2 == null) {
			return getRelationalQueriesComparison(A, B);
		} else {
			Gaussian difference = gaussianA.subtract(gaussianB);
			return difference.getNonZeroProbability();
		}
	}
	
	public Double getAbsoluteSize(String object){
		Gaussian g = getLogAbsoluteSizeGaussian(object);
		if (g == null)
			return null;
		else
			return METHOD == AverageMethod.SIZE ? g.getMean() : Math.exp(g.getMean());
	}

	public Gaussian getLogAbsoluteSizeGaussian(String object) {
		List<Double> observations = getAbsoluteObservations(object);
		if (observations.isEmpty()) {
			return null;
		} else {
			double size = getAverageSize(observations);
			double var = 0;
			for(double observation: observations)
				var += (observation - size) * (observation - size);
			var /= observations.size();
			return new Gaussian(size, var);
		}
	}

	public List<Double> getAbsoluteObservations(String object) {
		List<Double> observations = observationCache.get(object);
		if (observations != null)
			return observations;
		else
			observations = new ArrayList<Double>();
		
		for (int i = 0; i < absoluteTempates.size(); i++) {
			String query = String.format(absoluteTempates.get(i), object);
			String quotedQuery = "\"" + query + "\"";
			double scale = unitScales.get(i);

			try {
				WebSearchResults searchResult = searcher.search(quotedQuery);
				for (SearchResult result : searchResult.getTopResults()) {
					for (String match : result.getMatches(query)) {
						Scanner parser = new Scanner(match);
						parser.useDelimiter("[^\\d|\\.]");
						if (!parser.hasNext()) {
							parser.close();
							continue;
						}

						double avg = 0;
						int cnt = 0;
						while (parser.hasNext()) {
							String token = parser.next();
							if (token.isEmpty())
								continue;
							try {
								double val = Double.parseDouble(token);
								if (val == 0)
									continue;
								avg += Math.log(scale * val);
								cnt++;
							} catch (NumberFormatException nfe) {
							}
						}
						parser.close();
						if (cnt != 0) {
							avg /= cnt;
							observations.add( METHOD == AverageMethod.SIZE ? Math.exp(avg) : avg);
							break;
						}
					}
				}
			} catch (SearchFailedException e) {
				LOG.warning("Search Failed. " + e.getMessage());
			}
		}
		Collections.sort(observations);
		final int ignore = 0;
		observations = observations.subList(ignore, observations.size() - ignore);
		observationCache.put(object, observations);
		return observations;
	}
	
	private double getAverageSize(List<Double> observations) {
		double sum = 0 ;
		for (double obs: observations)
			sum += obs;
		return sum / observations.size();
	}
	
	private double getRelationalQueriesComparison(String object1, String object2) {
		ArrayList<ArrayList<String>> queries2Bigger = getSearchQueriesList(
				object2, object1);
		ArrayList<ArrayList<String>> queries1Bigger = getSearchQueriesList(
				object1, object2);

		long totalResults1 = getTotalResults(queries1Bigger);
		long totalResults2 = getTotalResults(queries2Bigger);
		if (totalResults1 > totalResults2)
			return 1.0;
		else if (totalResults2 > totalResults1)
			return 0.0;
		else
			return 0.5;
	}

	private long getTotalResults(ArrayList<ArrayList<String>> allQueries) {
		long total = 0;
		for (ArrayList<String> queries : allQueries) {
			StringBuilder query = new StringBuilder();
			if (queries.size() > 0)
				query.append(queries.get(0));
			for (int i = 1; i < queries.size(); i++) {
				query.append("|");
				query.append(queries.get(i));
			}
			try {
				Long curResults = searcher.search(query.toString())
						.getNumResults();
				if (curResults == null)
					throw new SearchFailedException("Reponse contains error.");
				total += curResults;
			} catch (SearchFailedException e) {
				LOG.warning("Search Failed. " + e.getMessage());
			}
		}
		return total;
	}

	private ArrayList<ArrayList<String>> getSearchQueriesList(
			String biggerObject, String smallerObject) {
		ArrayList<ArrayList<String>> queries = new ArrayList<ArrayList<String>>();

		ArrayList<String> limitedQueries = new ArrayList<String>();
		int totalWords = 0;

		for (String template : relativeTemplates) {
			String query = String.format(template, biggerObject, smallerObject);
			int words = countWords(query);
			if (words > QUERY_WORDS_LIMIT)
				LOG.severe("Query words limit exceeded by query: " + query);

			if (words + totalWords > QUERY_WORDS_LIMIT) {
				queries.add(limitedQueries);
				limitedQueries = new ArrayList<String>();
				totalWords = 0;
			}
			limitedQueries.add("\"" + query + "\"");
			totalWords += words;
		}
		queries.add(limitedQueries);

		return queries;
	}

	private int countWords(String str) {
		str = str.trim();
		if (str.isEmpty())
			return 0;
		return str.split("\\s+").length;
	}

	public double getObservationCount(String object) {
		return getAbsoluteObservations(object).size();
	}
}

enum AverageMethod{
	SIZE,
	LOG_SIZE
}
