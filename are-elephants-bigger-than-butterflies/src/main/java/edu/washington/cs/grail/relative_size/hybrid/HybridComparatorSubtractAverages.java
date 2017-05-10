package edu.washington.cs.grail.relative_size.hybrid;

import edu.washington.cs.grail.relative_size.graph.ObjectsNetworkConverge;
import edu.washington.cs.grail.relative_size.graph.models.Gaussian;
import edu.washington.cs.grail.relative_size.nlp.NlpSizeComparator;

public class HybridComparatorSubtractAverages extends HybridComparator {

	public HybridComparatorSubtractAverages(NlpSizeComparator nlp,
			ObjectsNetworkConverge vision) {
		super(nlp, vision);
	}

	@Override
	public double compare(String A, String B) {
		Gaussian nlpGaussianA = nlpLogGaussians.get(A);
		Gaussian visionGaussianA = vision.getLogSizeGaussian(A);
		Gaussian nlpGaussianB = nlpLogGaussians.get(B);
		Gaussian visionGaussianB = vision.getLogSizeGaussian(B);

		Gaussian avgA = Gaussian.average(nlpGaussianA, visionGaussianA);
		Gaussian avgB = Gaussian.average(nlpGaussianB, visionGaussianB);

		Gaussian differenceAvg = avgA.subtract(avgB);

		return differenceAvg.getNonZeroProbability();
	}

}
