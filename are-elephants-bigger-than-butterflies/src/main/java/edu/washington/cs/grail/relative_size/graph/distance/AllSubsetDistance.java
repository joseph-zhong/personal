package edu.washington.cs.grail.relative_size.graph.distance;

public class AllSubsetDistance extends DistanceFormulator {
	private static final long serialVersionUID = 6196718770620474853L;
	
	public AllSubsetDistance(Double alpha) {
		super(alpha);
	}
	
	public double getTotalDistance(double distance1, double distance2) {
		return distance1 + distance2 + distance1*distance2;
	}
	
}
