package edu.washington.cs.grail.relative_size.graph.distance;

import java.io.Serializable;

public abstract class DistanceFormulator implements Serializable {
	private static final long serialVersionUID = 5231836359690876485L;
	
	protected double alpha;
	
	protected DistanceFormulator(double alpha) {
		this.alpha = alpha;
	}
	
	public double getCoOccuranceValue(int tagsCount) {
		return alpha/tagsCount;
	}
	
	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	public double getEdgeNewWeight(Double oldWeight, double newPairValue) {
		if (oldWeight == null)
			return 1.0 / newPairValue;
		else
			return 1.0 / ( (1.0 / oldWeight) + newPairValue);
	}
	
	public abstract double getTotalDistance(double distance1, double distance2);

	public double getAlpha() {
		return alpha;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		return this.getAlpha() == ((DistanceFormulator)obj).getAlpha();
	}
}
