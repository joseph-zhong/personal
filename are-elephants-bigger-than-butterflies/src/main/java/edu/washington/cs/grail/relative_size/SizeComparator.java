package edu.washington.cs.grail.relative_size;

public interface SizeComparator {
	/**
	 * Compares two objects sizes and returns the probability an instance of
	 * object A is bigger than an instance of object B.
	 */
	double compare(String A, String B);
}
