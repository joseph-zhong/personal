package edu.washington.cs.grail.relative_size.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import edu.washington.cs.grail.relative_size.nlp.BasicNlpNeeds;

public class ObjectsNetworkPath extends ObjectsNetwork {
	private static final long serialVersionUID = 778165121822702435L;

	private static final Logger LOG = Logger.getLogger(ObjectsNetworkPath.class
			.getName());

	private ArrayList<ArrayList<Double>> distance = new ArrayList<ArrayList<Double>>();
	private ArrayList<ArrayList<Integer>> par = new ArrayList<ArrayList<Integer>>();

	public ObjectsNetworkPath(String... initialObjects) {
		super(initialObjects);

		resize(initialObjects.length);
	}

	public ObjectsNetworkPath(List<String> initialObjects) {
		this(initialObjects.toArray(new String[] {}));
	}
	
	public void addObjects(String... newObjects) {
		resize(newObjects.length + nodes.size());
		
		super.addObjects(newObjects);
	}

	public void updateSubgraph() {
		if (subgraphUpdated)
			return;

		updateSingleEdgePaths();
		floyd();
		updateRequiredEdges();
		checkConnectivity();
		subgraphUpdated = true;
	}

	public ArrayList<String> getOptimalPath(String object1, String object2) {
		Node v = str2node.get(object1);
		Node u = str2node.get(object2);
		ArrayList<String> res = new ArrayList<String>();
		ArrayList<Node> nodesPath = getOptimalPath(v, u);
		for (Node node : nodesPath)
			res.add(node.object);
		return res;
	}

	public double getDistance(String object1, String object2) {
		Node v = str2node.get(object1);
		Node u = str2node.get(object2);
		if (v == null || u == null)
			throw new RuntimeException("Object not found.");
		return distance.get(v.id).get(u.id);
	}

	public boolean isObservationSufficient() {
		for (int i = 0; i < nodes.size(); i++)
			for (int j = i + 1; j < nodes.size(); j++) {
				int parID = par.get(i).get(j);
				if (parID == -1)
					return false;

				if (observations.get(i).get(parID) == null
						|| observations.get(i).get(parID).getCount() == 0)
					return false;
			}
		return true;
	}

	@Override
	public double getEdgeScore(String object1, String object2) {
		Node v = str2node.get(object1);
		Node u = str2node.get(object2);
		return 1.0 / v.getEdgeWeight(u);
	}

	public Double getRelationalSize(String object1, String object2) {
		Node v = str2node.get(object1);
		Node u = str2node.get(object2);
		
		if (v == null || u == null)
			return null;
		
		ArrayList<Node> path = getOptimalPath(v, u);

		double res = 1;
		for (int i = 1; i < path.size(); i++) {
			v = path.get(i - 1);
			u = path.get(i);

			double val = observations.get(v.id).get(u.id).getAverageSize();
			if (val == 0)
				return null;
			res *= val;
		}
		return res;
	}

	public static ObjectsNetworkPath constructGraph(List<String> objects)
			throws IOException {
		ObjectsNetworkPath graph = new ObjectsNetworkPath(objects);
		graph.construct();

		return graph;
	}

	public static ObjectsNetworkPath constructGraph() throws IOException {
		BasicNlpNeeds nlp = BasicNlpNeeds.getInstance();
		List<String> objects = nlp.objectsWithSize();
		return ObjectsNetworkPath.constructGraph(objects);
	}

	@Override
	public ArrayList<Edge> getAllEdges() {
		ArrayList<Edge> res = new ArrayList<Edge>();
		for (int i = 0; i < nodes.size(); i++)
			for (int j = i + 1; j < nodes.size(); j++) {
				Node v = nodes.get(i);
				Node u = nodes.get(j);
				int parId = par.get(v.id).get(u.id);
				if (parId == -1)
					continue;
				Node parent = nodes.get(parId);
				if (parent.id == u.id) {
					res.add(new Edge(v.object, u.object));
				}
			}
		return res;
	}

	public void recalcSubgraphFromScratch() {
		distance.clear();
		resizeDistArr(distance, nodes.size(), Double.POSITIVE_INFINITY);
		subgraphUpdated = false;
		updateSubgraph();
	}

	@Override
	public void recalcSizesFromScratch() {
		// Nothing needs to be done here.
	}

	protected void checkConnectivity() {
		for (int i = 0; i < nodes.size(); i++) {
			for (int j = i + 1; j < nodes.size(); j++) {
				Node v = nodes.get(i);
				Node u = nodes.get(j);
				if (distance.get(v.id).get(u.id) == Double.POSITIVE_INFINITY) {
					LOG.info("No path between " + v.object + " and " + u.object
							+ ". The graph is not connected");
					break;
				}
			}
		}
	}

	private void updateRequiredEdges() {
		requiredEdges.clear();
		for (int i = 0; i < nodes.size(); i++)
			for (int j = i + 1; j < nodes.size(); j++) {
				Node v = nodes.get(i);
				Node u = nodes.get(j);
				int parId = par.get(v.id).get(u.id);
				if (parId == -1)
					continue;
				Node parent = nodes.get(parId);
				if (parent.id == u.id
						&& observations.get(v.id).get(u.id) == null) {
					requiredEdges.add(new Edge(v.object, u.object));
				}
			}
	}

	private void floyd() {
		for (Node m : nodes)
			for (Node u : nodes)
				for (Node v : nodes) {
					double vu = distance.get(v.id).get(u.id);
					double mu = distance.get(m.id).get(u.id);
					double mv = distance.get(m.id).get(v.id);

					double candidate = distanceFunction
							.getTotalDistance(mv, mu);
					if (vu > candidate) {
						distance.get(v.id).set(u.id, candidate);
						distance.get(u.id).set(v.id, candidate);

						par.get(v.id).set(u.id, par.get(v.id).get(m.id));
						par.get(u.id).set(v.id, par.get(u.id).get(m.id));
					}
				}
	}

	private void updateSingleEdgePaths() {
		for (Node u : nodes)
			for (Node v : nodes)
				if (distance.get(u.id).get(v.id) > u.getEdgeWeight(v)) {
					distance.get(u.id).set(v.id, u.getEdgeWeight(v));
					par.get(u.id).set(v.id, v.id);
				}
	}

	private ArrayList<Node> getOptimalPath(Node v, Node u) {
		if (v.id == u.id) {
			ArrayList<Node> res = new ArrayList<Node>();
			res.add(v);
			return res;
		}

		int parID = par.get(u.id).get(v.id);
		if (parID == -1)
			return null;

		Node parent = nodes.get(parID);
		ArrayList<Node> res = getOptimalPath(v, parent);
		res.add(u);

		return res;
	}

	private void resizeDistArr(ArrayList<ArrayList<Double>> arr, int n,
			double defaultValue) {
		arr.ensureCapacity(n);
		for (int i = arr.size(); i < n; i++)
			arr.add(new ArrayList<Double>());

		for (int i = 0; i < arr.size(); i++) {
			arr.get(i).ensureCapacity(n);
			for (int j = arr.get(i).size(); j < n; j++)
				arr.get(i).add(j == i ? 0 : defaultValue);
		}
	}

	private void resizeDistArr(ArrayList<ArrayList<Integer>> arr, int n,
			int defaultValue) {
		arr.ensureCapacity(n);
		for (int i = arr.size(); i < n; i++)
			arr.add(new ArrayList<Integer>());

		for (int i = 0; i < arr.size(); i++) {
			arr.get(i).ensureCapacity(n);
			for (int j = arr.get(i).size(); j < n; j++)
				arr.get(i).add(j == i ? 0 : defaultValue);
		}
	}

	private void resize(int n) {
		resizeDistArr(observations, n, null);
		resizeDistArr(distance, n, Double.POSITIVE_INFINITY);
		resizeDistArr(par, n, -1);
	}
}
