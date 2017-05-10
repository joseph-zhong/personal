package edu.washington.cs.grail.relative_size.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import edu.washington.cs.grail.relative_size.graph.models.Gaussian;
import edu.washington.cs.grail.relative_size.graph.models.Observation;
import edu.washington.cs.grail.relative_size.graph.models.ObservationList;
import edu.washington.cs.grail.relative_size.nlp.BasicNlpNeeds;
import edu.washington.cs.grail.relative_size.nlp.NlpSizeComparator;
import edu.washington.cs.grail.relative_size.utils.Config;

public class ObjectsNetworkConverge extends ObjectsNetwork {
	private static final long serialVersionUID = -3127604530724210633L;
	private static final int PRIM_COUNT = Config.getIntValue(
			"graph-prim-count", 2);
	private static final int MINIMUM_EDGES = Config.getIntValue(
			"graph-min-edges", 200);
	private static final double CONVERGE_THRESHOLD = Config.getDoubleValue(
			"graph-converge-threshold", 0.01);
	private static final Logger LOG = Logger
			.getLogger(ObjectsNetworkConverge.class.getName());
	public static boolean OPTIMIZE_VARIANCE = Config.getBooleanValue(
			"graph-update-variance", true);
	public static boolean INCORPORATE_NLP_UNARY_POTENTIALS = Config
			.getBooleanValue("graph-unary-potential", true);
	public static boolean INITILIZE_VARIANCE_FROM_NLP = Config.getBooleanValue(
			"graph-init-from-nlp", true);

	private boolean sizeConverged = false;
	private ArrayList<Gaussian> sizes = new ArrayList<Gaussian>();
	private ArrayList<Set<Integer>> nei = new ArrayList<Set<Integer>>();
	public Set<Integer> injectedNodes = new TreeSet<Integer>();

	public ObjectsNetworkConverge(String... initialObjects) {
		super(initialObjects);
		resize(initialObjects.length);
	}

	public ObjectsNetworkConverge(List<String> initialObjects) {
		this(initialObjects.toArray(new String[] {}));
	}

	public void addObjects(String... newObjects) {
		resize(newObjects.length + nodes.size());

		super.addObjects(newObjects);
	}

	@Override
	public double getEdgeScore(String object1, String object2) {
		int v = str2node.get(object1).id;
		int u = str2node.get(object2).id;
		
		return observations.get(v).get(u).getObservationScore();
	}

	@Override
	public void recalcSubgraphFromScratch() {
		subgraphUpdated = false;
		updateSubgraph();
	}

	@Override
	public void recalcSizesFromScratch() {
		sizeConverged = false;
		computeSizes();
	}

	@Override
	public void updateSubgraph() {
		if (subgraphUpdated)
			return;

		resetStructures();
		for (int i = 0; i < PRIM_COUNT; i++)
			LOG.info("Prim size = " + prim());
		satisfyMinimumEdgeRequirement();
		updateRequiredEdges();
		checkConnectivity();
		subgraphUpdated = true;
	}

	@Override
	public double compare(String A, String B) {
		Gaussian gaussianA = getLogSizeGaussian(A);
		Gaussian gaussianB = getLogSizeGaussian(B);
		
		Gaussian difference = gaussianA.subtract(gaussianB);

		return difference.getNonZeroProbability();
	}

	public void autoInjectObjectsFromNLP(int count) {
		final class NlpObjectSize implements Comparable<NlpObjectSize> {
			private String object;
			private Gaussian size;

			public NlpObjectSize(String object, Gaussian size) {
				this.object = object;
				this.size = size;
			}

			public int compareTo(NlpObjectSize o) {
				return (int) Math.signum(- size.getVariance()
						+ o.size.getVariance());
			}
		}

		ArrayList<NlpObjectSize> predictions = new ArrayList<NlpObjectSize>();
		if (nlp == null) {
			nlp = new NlpSizeComparator();
		}
		for (String object : this.getNodes()) {
			Gaussian nlpSize = nlp.getLogAbsoluteSizeGaussian(object);
			Gaussian thisSize = this.getLogSizeGaussian(object);
			
			Gaussian size = new Gaussian(nlpSize.getMean(), thisSize.getVariance());
			predictions.add(new NlpObjectSize(object, size));
		}

		Collections.sort(predictions);
		for (int i = 0; i < count && i < predictions.size(); i++) {
			injectObjectSize(predictions.get(i).object,
					Math.exp(predictions.get(i).size.getMean()));
			LOG.info("Injecting " + predictions.get(i).object);
		}
	}

	public void removeAllInjections() {
		injectedNodes.clear();
	}

	public void removeInjection(String object) {
		Node v = str2node.get(object);
		if (v == null) {
			LOG.info("Object does not exist in database. Abort.");
			return;
		}

		injectedNodes.remove(v.id);
	}

	public void injectObjectSize(String object, Double size) {
		Node v = str2node.get(object);
		if (v == null) {
			LOG.info("Object does not exist in database. Abort.");
			return;
		}

		injectedNodes.add(v.id);
		sizes.get(v.id).setMean(Math.log(size));
	}

	private void satisfyMinimumEdgeRequirement() {
		int edgeCount = 0;
		for (int i = 0; i < nodes.size(); i++)
			edgeCount += nei.get(i).size();
		edgeCount /= 2;

		if (edgeCount >= MINIMUM_EDGES)
			return;

		ArrayList<WeightedEdge> edges = new ArrayList<WeightedEdge>();
		for (int i = 0; i < nodes.size(); i++)
			for (int j = i + 1; j < nodes.size(); j++) {
				Node v = nodes.get(i);
				Node u = nodes.get(j);
				edges.add(new WeightedEdge(v, u, v.getEdgeWeight(u)));
			}
		Collections.sort(edges);

		for (int i = 0; i < edges.size() && edgeCount < MINIMUM_EDGES; i++) {
			WeightedEdge cur = edges.get(i);
			int v = cur.v.id;
			int u = cur.u.id;
			if (!nei.get(v).contains(u)) {
				nei.get(v).add(u);
				nei.get(u).add(v);
				edgeCount++;
			}
		}
	}

	private void resetStructures() {
		for (int i = 0; i < nodes.size(); i++)
			nei.get(i).clear();
		requiredEdges.clear();
	}

	private double prim() {
		// The goal of prim here is to find a bottleneck tree, and not an MST
		// We do prim in O(n^2) rather than O(elge) since the graph is complete.

		Map<Node, Node> par = new TreeMap<Node, Node>();
		for (Node v : nodes)
			par.put(v, null);

		Node curNode = nodes.get(0);

		double maximumWeight = 0;

		while (curNode != null) {
			par.remove(curNode);

			Node nextNode = null;
			double bestDist = Double.POSITIVE_INFINITY;
			for (Node v : nodes) {
				if (nei.get(curNode.id).contains(v.id)) {
					// The subgraph already has the edge
					continue;
				}

				if (par.containsKey(v)) {
					Node curPar = par.get(v);
					double curDist = curPar == null ? Double.POSITIVE_INFINITY
							: curPar.getEdgeWeight(v);
					double curEdge = curNode.getEdgeWeight(v);

					if (curEdge < curDist) {
						curDist = curEdge;
						par.put(v, curNode);
					}

					if (curDist < bestDist) {
						bestDist = curDist;
						nextNode = v;
					}
				}
			}

			if (nextNode != null) {
				for (Node v : nodes) {
					if (!par.containsKey(v)) {
						if (v.getEdgeWeight(nextNode) <= bestDist
								&& !nei.get(v.id).contains(nextNode.id)) {
							nei.get(v.id).add(nextNode.id);
							nei.get(nextNode.id).add(v.id);
							break;
						}
					}
				}
				if (bestDist > maximumWeight)
					maximumWeight = bestDist;

			}
			curNode = nextNode;
		}

		return maximumWeight;
	}

	private void dfs(int v, Set<Integer> mark, boolean obsEdges) {
		if (mark.contains(v))
			return;
		mark.add(v);
		for (Integer j : nei.get(v)) {
			if (obsEdges && observations.get(v).get(j).getCount() == 0)
				continue;

			dfs(j, mark, obsEdges);
		}
	}

	private void checkConnectivity() {
		Set<Integer> mark = new TreeSet<Integer>();
		dfs(0, mark, false);

		int cnt = 0;
		for (Integer i : mark)
			if (i != cnt) {
				System.out.println(nodes.get(cnt).object);
				break;
			} else
				cnt++;
		if (mark.size() != nodes.size())
			LOG.warning("The graph is not connected!");

	}

	public boolean isObservationSufficient() {
		Set<Integer> mark = new TreeSet<Integer>();
		dfs(0, mark, true);

		return (mark.size() == nodes.size());
	}

	private void updateRequiredEdges() {
		for (int i = 0; i < nodes.size(); i++) {
			for (int j : nei.get(i)) {
				Node v = nodes.get(i);
				Node u = nodes.get(j);

				if (observations.get(i).get(j) == null) {
					requiredEdges.add(new Edge(v.object, u.object));
					sizeConverged = false;
				}
			}
		}
	}

	@Override
	public ArrayList<Edge> getAllEdges() {
		ArrayList<Edge> res = new ArrayList<Edge>();
		for (int i = 0; i < nodes.size(); i++)
			for (int j = i + 1; j < nodes.size(); j++) {
				Node v = nodes.get(i);
				Node u = nodes.get(j);
				if (nei.get(v.id).contains(u.id))
					res.add(new Edge(u.object, v.object));
			}
		return res;
	}

	@Override
	public void setEdgeObservations(String object1, String object2,
			ObservationList observations) {
		if (observations == null)
			observations = new ObservationList(0.0, 0.0, new Double[0],
					new Double[0], new Double[0], new Double[0]);
		super.setEdgeObservations(object1, object2, observations);
		sizeConverged = false;
	}

	public Double getSize(String object) {
		return Math.exp(getLogSizeGaussian(object).getMean());
	}

	public Gaussian getLogSizeGaussian(String object) {
		computeSizes();

		Node v = str2node.get(object);

		if (v == null || sizes.get(v.id) == null)
			return null;
		else
			return sizes.get(v.id).clone();
	}

	@Override
	public Double getRelationalSize(String object1, String object2) {
		computeSizes();

		Node v = str2node.get(object1);
		Node u = str2node.get(object2);

		if (v == null || u == null || sizes.get(v.id) == null
				|| sizes.get(u.id) == null)
			return null;
		else
			return Math.exp(sizes.get(v.id).getMean()
					- sizes.get(u.id).getMean());
	}

	public static ObjectsNetworkConverge constructGraph(List<String> objects)
			throws IOException {
		ObjectsNetworkConverge graph = new ObjectsNetworkConverge(objects);
		graph.construct();
		return graph;
	}

	public static ObjectsNetworkConverge constructGraph() throws IOException {
		BasicNlpNeeds nlp = BasicNlpNeeds.getInstance();
		List<String> objects = nlp.objectsWithSize();
		return ObjectsNetworkConverge.constructGraph(objects);
	}

	private double logLikelihood(Node v) {
		double loglike = 0;
		int i = v.id;

		for (Integer j : nei.get(i)) {
			double neiLogSize = sizes.get(j).getMean();
			double neiLogLike = 0;
			for (Observation obs : observations.get(i).get(j)
					.getLogObservations()) {

				neiLogLike -= obs.getScore()
						* (neiLogSize + obs.getValue() - sizes.get(i).getMean())
						* (neiLogSize + obs.getValue() - sizes.get(i).getMean());
			}
			neiLogLike /= 2 * (sizes.get(i).getVariance() + sizes.get(j)
					.getVariance());
			neiLogLike -= observations.get(i).get(j).getObservationScore()
					* Math.log(Math.sqrt(sizes.get(i).getVariance()
							+ sizes.get(j).getVariance()));

			loglike += neiLogLike;
		}

		if (INCORPORATE_NLP_UNARY_POTENTIALS) {
			double nlpLogLike = 0;
			if (nlp == null)
				nlp = new NlpSizeComparator();
			List<Double> nlpObs = nlp.getAbsoluteObservations(v.object);
			for (double obs : nlpObs) {
				nlpLogLike -= (obs - sizes.get(i).getMean())
						* (obs - sizes.get(i).getMean());
			}
			nlpLogLike /= 2 * (sizes.get(i).getVariance());
			nlpLogLike -= nlpObs.size() * Math.log(sizes.get(i).getVariance());

			loglike += nlpLogLike;
		}
		return loglike;
	}

	private double blankedLogLikelihood(Node v) {
		double res = logLikelihood(v);
		for (Integer j : nei.get(v.id))
			res += logLikelihood(nodes.get(j));
		return res;
	}

	private void computeSizes() {
		if (!subgraphUpdated)
			LOG.warning("Computing sizes while the subgraph is not updated.");
		else if (!requiredEdges.isEmpty()) {
			LOG.warning("Computing sizes while the required edges are not set.");
		}

		if (sizeConverged)
			return;

		initLogSizes();

		if (INITILIZE_VARIANCE_FROM_NLP) {
			if (nlp == null) {
				nlp = new NlpSizeComparator();
			}
			for (int i = 0; i < nodes.size(); i++) {
				if (injectedNodes.contains(nodes.get(i).id))
					continue;
				
				Gaussian nlpPrediction = nlp.getLogAbsoluteSizeGaussian(nodes
						.get(i).object);
				sizes.get(i).setMean(nlpPrediction.getMean());
				sizes.get(i).setVariance(nlpPrediction.getVariance());
			}
		}

		double totalLogLike = 0;
		for (int i = 0; i < nodes.size(); i++)
			totalLogLike += logLikelihood(nodes.get(i));

		while (!sizeConverged) {
			// for (int t = 0; t < 100; t++) {
			sizeConverged = true;

			double totalLogSize = 0;

			for (int i = 0; i < nodes.size(); i++) {

				double newLogSize = getOptimalMean(nodes.get(i));
				sizes.get(i).setMean(newLogSize);
				totalLogSize += newLogSize;
			}

			if (!INCORPORATE_NLP_UNARY_POTENTIALS) {
				for (int i = 0; i < sizes.size(); i++) {
					if (injectedNodes.contains(i))
						continue;
					sizes.get(i)
							.setMean(
									sizes.get(i).getMean()
											- (totalLogSize / (sizes.size() - injectedNodes
													.size())));
				}
			}
			
			if (OPTIMIZE_VARIANCE) {
				for (int j = 0; j < sizes.size(); j++) {
					double newLogVar = getOptimalVariance(nodes.get(j));
					sizes.get(j).setVariance(newLogVar);
				}
			}
			
			double newTotalLogLike = 0;
			for (int i = 0; i < nodes.size(); i++)
				newTotalLogLike += logLikelihood(nodes.get(i));
			
			// System.out.println(maximumChange);
			// System.out.println("TOTAL_SUM = " + sumTotal);
			//
			// System.out.println(maximumChange);
			// System.out.println();
			// System.out.println();
			// System.out.println();
			if ((newTotalLogLike - totalLogLike) > CONVERGE_THRESHOLD)
				sizeConverged = false;
			totalLogLike = newTotalLogLike;

		}

		double totalLogSize = 0;

		for (int i = 0; i < nodes.size(); i++) {
			double thisLogSize = sizes.get(i).getMean();
			totalLogSize += thisLogSize;
		}
		
		for (int i = 0; i < sizes.size(); i++) {
			if (injectedNodes.contains(i))
				continue;
			sizes.get(i).setMean(
					sizes.get(i).getMean()
							- (totalLogSize / (sizes.size() - injectedNodes
									.size())));
		}

		// System.out.println();
		// for (int i=0 ; i<sizes.size() ; i++)
		// System.out.println(nodes.get(i).object + " -> " + sizes.get(i));
		// System.out.println("DONE");
	}

	private double getOptimalVariance(Node v) {
		final double EPS = 1e-3;
		final int i = v.id;

		// double varGradient = 0;
		// for (Integer j : nei.get(i)) {
		// double neiLogSize = sizes.get(j).getMean();
		// double neiVarGradient = 0;
		// for (Observation obs : observations.get(i).get(j)
		// .getLogObservations()) {
		//
		// neiVarGradient += obs.getScore()
		// * (neiLogSize + obs.getValue() - sizes.get(i).getMean())
		// * (neiLogSize + obs.getValue() - sizes.get(i).getMean());
		// }
		// neiVarGradient -= observations.get(i).get(j).getObservationScore()
		// * (sizes.get(i).getVariance() + sizes.get(j).getVariance());
		// neiVarGradient /= (sizes.get(i).getVariance() + sizes.get(j)
		// .getVariance())
		// * (sizes.get(i).getVariance() + sizes.get(j).getVariance());
		// varGradient += neiVarGradient;
		//
		// }
		//
		// if (Math.abs(varGradient) < EPS)
		// return sizes.get(i).getVariance();
		//
		// double gradientDirection = Math.signum(varGradient);

		// double factors[] = { 0.0001, 0.0003, 0.001, 0.003, 0.01, 0.03, 0.1,
		// 0.3, 1, 3 };
		// double curLogVar = sizes.get(i).getVariance();
		// double bestLogVar = sizes.get(i).getVariance();
		// double bestLogLike = logLikelihood(v);
		//
		// for (int f = 0; f < factors.length; f++) {
		// double newLogVar = curLogVar + factors[f] * gradientDirection;
		// sizes.get(i).setVariance(newLogVar);
		// double newLogLike = logLikelihood(v);
		//
		// if (newLogLike > bestLogLike + EPS) {
		// bestLogVar = newLogVar;
		// bestLogLike = newLogLike;
		// }
		// }
		// sizes.get(i).setVariance(curLogVar);
		// return bestLogVar;
		final double ETA = 0.01;

		double curLogVar = sizes.get(i).getVariance();
		double curLogLike = blankedLogLikelihood(v);
		double newLogDev = Math.sqrt(curLogVar);
		double newLogLike = curLogLike;
		double bestLogDev = 0;
		double bestLogLike = 0;
		do {
			bestLogDev = newLogDev;
			bestLogLike = newLogLike;

			newLogDev -= ETA;
			sizes.get(i).setVariance(newLogDev * newLogDev);
			newLogLike = blankedLogLikelihood(v);
			if (newLogLike <= bestLogLike + EPS) {
				newLogDev += 2 * ETA;
				sizes.get(i).setVariance(newLogDev * newLogDev);
				newLogLike = blankedLogLikelihood(v);
			}
		} while (newLogLike > bestLogLike + EPS);
		sizes.get(i).setVariance(curLogVar);
		return bestLogDev * bestLogDev;
	}

	private transient NlpSizeComparator nlp = new NlpSizeComparator();

	private double getOptimalMean(Node v) {
		if (injectedNodes.contains(v.id))
			return sizes.get(v.id).getMean();

		final int i = v.id;
		double newLogSize = 0;
		double totalScore = 0;

		double totalObs = 0, obsCount = 0;
		for (Integer j : nei.get(i)) {
			double neiLogSize = sizes.get(j).getMean();
			Double logRatio = observations.get(i).get(j).getAverageLogSize();
			// int numObservations =
			// observations.get(i).get(j).getCount();
			// double edgeScore = observations.get(i).get(j)
			// .getObservationScore();
			double edgeScore = observations.get(i).get(j).getObservationScore()
					/ (sizes.get(j).getVariance() + sizes.get(i).getVariance());
			totalObs += observations.get(i).get(j).getObservationScore();
			obsCount += observations.get(i).get(j).getCount();

			if (logRatio == null) {
				// LOG.warning("NULL HERE: " + nodes.get(i).object +
				// " - " + nodes.get(j).object);
				// throw new NullPointerException();
				continue;
			}

			// Node v = nodes.get(i);
			// Node u = nodes.get(j);
			// System.out.println(u.object + " : " + v.object + " = "
			// + (logRatio + neiLogSize));
			// newLogSize += numObservations * (logRatio + neiLogSize);
			// totalObservation += numObservations;
			newLogSize += edgeScore * (logRatio + neiLogSize);
			totalScore += edgeScore;
		}
		if (totalObs == 0)
			totalObs = 1;

		if (INCORPORATE_NLP_UNARY_POTENTIALS) {
			newLogSize *= (obsCount / totalObs);
			totalScore *= (obsCount / totalObs);

			if (nlp == null) {
				nlp = new NlpSizeComparator();
			}
			Gaussian nlpObservation = nlp.getLogAbsoluteSizeGaussian(v.object);
			double nlpObservationCount = nlp.getObservationCount(v.object);

			double nlpScore = (double) nlpObservationCount
					/ nlpObservation.getVariance();
			newLogSize += nlpScore * (nlpObservation.getMean());
			totalScore += nlpScore;
		}
		// if (totalObservation == 0){
		// LOG.warning("ISOLATED VERTEX: " + nodes.get(i).object);
		// totalObservation = 1;
		// // throw new NullPointerException();
		// }
		// newLogSize /= totalObservation;
		if (totalScore == 0) {
			// LOG.warning("ISOLATED VERTEX: " + nodes.get(i).object);
			totalScore = 1;
			// throw new NullPointerException();
		}
		newLogSize /= totalScore;

		return newLogSize;
	}

	private void initLogSizes() {
		for (int i = 0; i < sizes.size(); i++) {
			if (injectedNodes.contains(i))
				continue;
			sizes.set(i, new Gaussian());
		}
	}

	private void resize(int n) {
		resizeDistArr(observations, n, null);

		sizes.ensureCapacity(n);
		while (sizes.size() < n) {
			sizes.add(new Gaussian());
		}

		nei.ensureCapacity(n);
		while (nei.size() < n) {
			nei.add(new TreeSet<Integer>());
		}
	}

	private class WeightedEdge implements Comparable<WeightedEdge> {
		private Node v, u;
		private double weight;

		public WeightedEdge(Node v, Node u, double weight) {
			this.v = v;
			this.u = u;
			this.weight = weight;
		}

		public int compareTo(WeightedEdge e) {
			return (int) Math.signum((this.weight - e.weight));
		}
	}
}
