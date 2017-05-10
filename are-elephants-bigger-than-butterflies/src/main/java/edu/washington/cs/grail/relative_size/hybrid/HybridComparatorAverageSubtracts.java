package edu.washington.cs.grail.relative_size.hybrid;

import edu.washington.cs.grail.relative_size.graph.ObjectsNetworkConverge;
import edu.washington.cs.grail.relative_size.graph.models.Gaussian;
import edu.washington.cs.grail.relative_size.nlp.NlpSizeComparator;

public class HybridComparatorAverageSubtracts extends HybridComparator {

	public HybridComparatorAverageSubtracts(NlpSizeComparator nlp,
			ObjectsNetworkConverge vision) {
		super(nlp, vision);
	}
	
	@Override
	public double compare(String A, String B) {
		Gaussian nlpGaussianA = nlpLogGaussians.get(A);
		Gaussian visionGaussianA = vision.getLogSizeGaussian(A);
		Gaussian nlpGaussianB = nlpLogGaussians.get(B);
		Gaussian visionGaussianB = vision.getLogSizeGaussian(B);

		Gaussian nlpDifference = nlpGaussianA.subtract(nlpGaussianB);
		Gaussian visionDifference = visionGaussianA.subtract(visionGaussianB);
		
		Gaussian avgDifference = Gaussian.average(nlpDifference, visionDifference);
		
		return avgDifference.getNonZeroProbability();
	}

}
