package edu.washington.cs.grail.relative_size.hybrid;

import edu.washington.cs.grail.relative_size.graph.ObjectsNetworkConverge;
import edu.washington.cs.grail.relative_size.graph.models.Gaussian;
import edu.washington.cs.grail.relative_size.nlp.NlpSizeComparator;

public class HybridComparatorProbabilityThreshold extends HybridComparator {
	private static final PrimaryModel primaryModel = PrimaryModel.VISION;
	protected double threshold;
	
	public HybridComparatorProbabilityThreshold(NlpSizeComparator nlp,
			ObjectsNetworkConverge vision, double threshold) {
		super(nlp, vision);
		this.threshold = threshold;
	}

	@Override
	public double compare(String A, String B) {
		Gaussian nlpGaussianA = nlpLogGaussians.get(A);
		Gaussian visionGaussianA = vision.getLogSizeGaussian(A);
		Gaussian nlpGaussianB = nlpLogGaussians.get(B);
		Gaussian visionGaussianB = vision.getLogSizeGaussian(B);

		Gaussian nlpDifference = nlpGaussianA.subtract(nlpGaussianB);
		Gaussian visionDifference = visionGaussianA.subtract(visionGaussianB);

		double nlpProbability = nlpDifference.getNonZeroProbability();
		double visionProbability = visionDifference.getNonZeroProbability();
		
		double primaryProb = primaryModel == PrimaryModel.NLP ? nlpProbability : visionProbability;
		double secondaryProb = primaryModel == PrimaryModel.NLP ? visionProbability : nlpProbability;
		
		if (Math.abs(primaryProb - 0.5) > threshold)
			return primaryProb;
		else
			return secondaryProb;
	}
	
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
}

enum PrimaryModel{
	VISION,
	NLP
}