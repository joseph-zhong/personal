package edu.washington.cs.grail.relative_size.graph.distance;


public class BasicSumDistance extends DistanceFormulator {
	private static final long serialVersionUID = -9140851651753127963L;
	
	public BasicSumDistance(Double alpha) {
		super(alpha);
	}
	
	public double getTotalDistance(double distance1, double distance2) {
		return distance1 + distance2;
	}

}
