package edu.washington.cs.grail.relative_size.hybrid;

import java.util.Map;
import java.util.TreeMap;

import edu.washington.cs.grail.relative_size.SizeComparator;
import edu.washington.cs.grail.relative_size.graph.ObjectsNetworkConverge;
import edu.washington.cs.grail.relative_size.graph.models.Gaussian;
import edu.washington.cs.grail.relative_size.nlp.NlpSizeComparator;

public abstract class HybridComparator implements SizeComparator {
	protected ObjectsNetworkConverge vision;
	protected Map<String, Gaussian> nlpLogGaussians;
	
	protected HybridComparator(NlpSizeComparator nlp, ObjectsNetworkConverge vision) {
		this.vision = vision;

		nlpLogGaussians = new TreeMap<String, Gaussian>();
		double avgNlpLogSizes = 0;
		for (String object : vision.getNodes()) {
			Gaussian now = nlp.getLogAbsoluteSizeGaussian(object);
			nlpLogGaussians.put(object, now);

			avgNlpLogSizes += now.getMean();
		}
		avgNlpLogSizes /= vision.getNodes().size();

		for (Map.Entry<String, Gaussian> entry : nlpLogGaussians.entrySet()) {
			nlpLogGaussians.get(entry.getKey()).setMean(
					entry.getValue().getMean() - avgNlpLogSizes);
		}
	}
	
	public Gaussian getAbsoluteSizeGaussian(String object){
		Gaussian nlpGaussian = nlpLogGaussians.get(object);
		Gaussian visionGaussian = vision.getLogSizeGaussian(object);
		return Gaussian.average(nlpGaussian, visionGaussian);
	}

	public double getAbsoluteSize(String object) {
		return getAbsoluteSizeGaussian(object).getMean();
	}
	
	public abstract double compare(String A, String B);

}
