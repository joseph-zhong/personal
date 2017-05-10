package edu.washington.cs.grail.relative_size.nlp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.tartarus.martin.Stemmer;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISenseEntry;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.washington.cs.grail.relative_size.utils.Config;

public class BasicNlpNeeds {
	private static final String WORDNET_DIRECTORY = Config.getValue("wordnet-path", "data/datasets/wordnet");
	private static final int[] OBJECTS_SENSES = Config.getIntArray(
			"wordnet-senses", new int[] { 6 });
	private static final int OBJECTS_MINIMUM_FREQUENCY = Config.getIntValue(
			"wordnet-min-freq", 12);

	private static final BasicNlpNeeds instance = new BasicNlpNeeds();

	public static BasicNlpNeeds getInstance() {
		return instance;
	}

	private BasicNlpNeeds() {
	}

	private Stemmer stemmer = new Stemmer();

	public ArrayList<String> objectsWithSize() throws IOException {
		ArrayList<String> res = new ArrayList<String>();

		IDictionary dict = new Dictionary(new File(WORDNET_DIRECTORY));
		dict.open();

		Iterator<IIndexWord> it = dict.getIndexWordIterator(POS.NOUN);
		while (it.hasNext()) {
			IWordID wordId = it.next().getWordIDs().get(0);
			IWord word = dict.getWord(wordId);
			int sense = word.getSenseKey().getLexicalFile().getNumber();
			if (!containsElement(OBJECTS_SENSES, sense))
				continue;
			ISenseEntry senseEntry = dict.getSenseEntry(word.getSenseKey());

			if (senseEntry.getTagCount() < OBJECTS_MINIMUM_FREQUENCY)
				continue;
			res.add(word.getLemma());
		}
		return res;
	}

	public String stem(String word) {
		stemmer.add(word.toCharArray(), word.length());
		stemmer.stem();
		return stemmer.toString();
	}

	public boolean areEqual(String object1, String object2) {
		return stem(object1).equals(stem(object2));
	}

	private boolean containsElement(int[] arr, int element) {
		for (int a : arr)
			if (a == element)
				return true;
		return false;
	}

	public static void main(String[] args) throws IOException {
		BasicNlpNeeds nlp = BasicNlpNeeds.getInstance();
		System.out.println(nlp.objectsWithSize());
	}

}
