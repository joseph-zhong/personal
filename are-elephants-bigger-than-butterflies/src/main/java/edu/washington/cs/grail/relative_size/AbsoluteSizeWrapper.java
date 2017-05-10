package edu.washington.cs.grail.relative_size;

import java.util.Map;

import edu.washington.cs.grail.relative_size.graph.models.Gaussian;

public class AbsoluteSizeWrapper implements SizeComparator {
	private Map<String, Gaussian> absSize;
	
	public AbsoluteSizeWrapper(Map<String, Gaussian> absSize) {
		this.absSize = absSize;
	}
	
	public double compare(String A, String B) {
		Gaussian gA = absSize.get(A);
		Gaussian gB = absSize.get(B);
		
		return gA.subtract(gB).getNonZeroProbability();
	}

}
