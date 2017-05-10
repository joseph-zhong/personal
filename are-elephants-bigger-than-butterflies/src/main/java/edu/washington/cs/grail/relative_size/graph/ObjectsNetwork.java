package edu.washington.cs.grail.relative_size.graph;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import edu.washington.cs.grail.relative_size.SizeComparator;
import edu.washington.cs.grail.relative_size.graph.distance.BasicSumDistance;
import edu.washington.cs.grail.relative_size.graph.distance.DistanceFormulator;
import edu.washington.cs.grail.relative_size.graph.models.ObservationList;
import edu.washington.cs.grail.relative_size.retreive.url.FlickrTwoObjectsUrl;
import edu.washington.cs.grail.relative_size.tags.NeatTagUrlsRead;
import edu.washington.cs.grail.relative_size.tags.models.TagUrl;
import edu.washington.cs.grail.relative_size.utils.Config;

public abstract class ObjectsNetwork implements Serializable, SizeComparator {
	private static final long serialVersionUID = 6417955736904691855L;

	protected static final double DISTANCE_ALPHA = Config.getDoubleValue(
			"graph-distance-alpha", 100.0);
	private static final DateFormat SAVING_DATE_FORMAT = new SimpleDateFormat(
			"_yyyy-MM-dd@HH:mm:ss");
	private static final Logger LOG = Logger.getLogger(ObjectsNetwork.class
			.getName());

	protected DistanceFormulator distanceFunction;
	protected boolean subgraphUpdated = false;
	protected Map<String, Node> str2node = new TreeMap<String, Node>();
	protected ArrayList<Node> nodes = new ArrayList<Node>();
	protected ArrayList<ArrayList<ObservationList>> observations = new ArrayList<ArrayList<ObservationList>>();
	protected Set<Edge> requiredEdges = new TreeSet<Edge>();

	public ObjectsNetwork(String... initialObjects) {
		addNewObjects(initialObjects);
		
		try {
			setDistanceFormulator(
					Config.getValue("graph-distance-function",
							"edu.washington.cs.grail.relative_size.graph.distance.BasicSumDistance"),
					DISTANCE_ALPHA);
		} catch (Exception e) {
			distanceFunction = new BasicSumDistance(DISTANCE_ALPHA);
		}
	}

	public void addObjects(String... newObjects) {
		addNewObjects(newObjects);
		try {
			construct();
		} catch (FileNotFoundException e) {
			LOG.severe("Could not find tagUrl dataset.");
		}
	}

	public Set<String> getNodes() {
		return str2node.keySet();
	}

	public void addClique(TagUrl tagUrl) {
		String[] tags = tagUrl.getTags();
		ArrayList<Node> tagNodes = new ArrayList<Node>();
		for (String tag : tags) {
			Node cur = str2node.get(tag);
			if (cur != null)
				tagNodes.add(cur);
		}

		double coValue = distanceFunction.getCoOccuranceValue(tags.length);

		for (int i = 0; i < tagNodes.size(); i++)
			for (int j = i + 1; j < tagNodes.size(); j++) {
				subgraphUpdated = false;
				Node v = tagNodes.get(i);
				Node u = tagNodes.get(j);

				v.addEdge(u, coValue);
				u.addEdge(v, coValue);
			}
	}

	public void setDistanceAlpha(double alpha) {
		double prevAlpha = distanceFunction.getAlpha();
		if (prevAlpha != alpha) {
			distanceFunction.setAlpha(alpha);
			recalcSubgraphFromScratch();
		}
	}

	public abstract boolean isObservationSufficient();

	public abstract double getEdgeScore(String object1, String object2);

	public abstract void recalcSubgraphFromScratch();

	public abstract void updateSubgraph();

	public abstract Double getRelationalSize(String object1, String object2);

	public abstract ArrayList<Edge> getAllEdges();

	public abstract void recalcSizesFromScratch();
	
	public double compare(String A, String B) {
		Double relSize = getRelationalSize(A, B);
		if (relSize == null){
			LOG.info("Asked for comparing non-existent objects. Return half.");
			return 0.5;
		}
		if (relSize == Double.NaN){
			LOG.warning("Relative size returned NaN. Continuing with half probability.");
			return 0.5;
		}
		if (relSize > 1)
			return 1;
		else if (relSize < 1)
			return 0;
		else
			return 0.5;
	}
	
	public void setDistanceFormulator(String formulatorClass)
			throws ClassNotFoundException, NoSuchMethodException,
			SecurityException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		setDistanceFormulator(formulatorClass, distanceFunction.getAlpha());
	}

	public void setDistanceFormulator(String formulatorClass, double alpha)
			throws ClassNotFoundException, NoSuchMethodException,
			SecurityException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {

		Class<?> clazz = Class.forName(formulatorClass);
		Constructor<?> constructor = clazz.getConstructor(Double.class);
		DistanceFormulator newDistance = (DistanceFormulator) constructor
				.newInstance(alpha);

		DistanceFormulator prevDistance = this.distanceFunction;
		this.distanceFunction = newDistance;

		if (prevDistance != null && !newDistance.equals(prevDistance)) {
			recalcSubgraphFromScratch();
		}
	}
	
	public void setEdgeObservations(String object1, String object2,
			ObservationList observation) {
		if (observation == null || !observation.isValid())
			return;

		requiredEdges.remove(new Edge(object1, object2));

		int v = str2node.get(object1).id;
		int u = str2node.get(object2).id;

		this.observations.get(v).set(u, observation);
		this.observations.get(u).set(v, observation.getReverse());

	}

	public ObservationList getObservationList(String object1, String object2) {
		int i = str2node.get(object1).id;
		int j = str2node.get(object2).id;

		return observations.get(i).get(j);
	}

	public double getEdgeWeight(String object1, String object2) {
		Node v = str2node.get(object1);
		Node u = str2node.get(object2);

		return v.getEdgeWeight(u);
	}

	protected void construct() throws FileNotFoundException {
		List<String> objects = new ArrayList<String>(this.getNodes());
		FlickrTwoObjectsUrl urlBase = new FlickrTwoObjectsUrl(objects);

		NeatTagUrlsRead read = new NeatTagUrlsRead();
		int cnt = 0;
		while (true) {
			TagUrl tagUrl = read.nextTagUrl();
			if (tagUrl == null)
				break;
			this.addClique(tagUrl);
			urlBase.addClique(tagUrl);

			if ((++cnt) % 1000000 == 0)
				LOG.info(cnt + " tags group added.");
		}
		this.updateSubgraph();
		urlBase.writeAllUrls();
	}

	public Set<Edge> getRequiredEdges() {
		return requiredEdges;
	}

	public void save(String path) throws IOException {
		File file = new File(path);
		String filePath = path;

		if (file.exists() && file.isDirectory()) {
			filePath = path + File.separator + nodes.size()
					+ SAVING_DATE_FORMAT.format(new Date());
		}

		FileOutputStream fos = new FileOutputStream(filePath);
		ObjectOutputStream oos = new ObjectOutputStream(fos);

		this.updateSubgraph();

		try {
			oos.writeObject(this);
		} finally {
			oos.close();
			fos.close();
			LOG.info("Graph saved at " + filePath);
		}
	}

	public static ObjectsNetwork load(String filePath) throws IOException {
		FileInputStream fis = new FileInputStream(filePath);
		ObjectInputStream ois = new ObjectInputStream(fis);
		try {
			ObjectsNetwork result = (ObjectsNetwork) ois.readObject();
			result.updateSubgraph();
			return result;
		} catch (ClassCastException e) {
			throw new IOException(filePath + " File format is invalid "
					+ e.getMessage());
		} catch (ClassNotFoundException e) {
			throw new IOException(filePath + " File format is invalid "
					+ e.getMessage());
		} finally {
			fis.close();
			ois.close();
		}
	}

	protected void resizeDistArr(ArrayList<ArrayList<ObservationList>> arr,
			int n, ObservationList defaultValue) {
		arr.ensureCapacity(n);
		for (int i = arr.size(); i < n; i++)
			arr.add(new ArrayList<ObservationList>());

		for (int i = 0; i < arr.size(); i++) {
			arr.get(i).ensureCapacity(n);
			for (int j = arr.get(i).size(); j < n; j++)
				arr.get(i).add(j == i ? null : defaultValue);
		}
	}
	
	private void addNewObjects(String[] initialObjects) {
		for (int i = 0; i < initialObjects.length; i++) {
			String object = initialObjects[i];

			Node cur = new Node(object, i);
			str2node.put(object, cur);
			nodes.add(cur);
		}
		
		resize(str2node.size());
	}

	private void resize(int n) {
		resizeDistArr(observations, n, null);
	}

	protected class Node implements Comparable<Node>, Serializable {
		private static final long serialVersionUID = -2236849360636030288L;

		protected int id;
		protected String object;

		private Map<Node, Double> edges = new TreeMap<Node, Double>();

		public Node(String object, int id) {
			this.object = object;
			this.id = id;
		}

		public Double getEdgeWeight(Node v) {
			Double weight = edges.get(v);
			return (weight == null ? Double.POSITIVE_INFINITY : weight);
		}

		public int compareTo(Node o) {
			return this.id - o.id;
		}

		public void addEdge(Node v, double value) {
			edges.put(v, distanceFunction.getEdgeNewWeight(edges.get(v), value));
		}

	}

	public static class Edge implements Serializable, Comparable<Edge> {
		private static final long serialVersionUID = -9032442846390894873L;

		private String v, u;

		public Edge(String v, String u) {
			if (v.compareTo(u) < 0) {
				this.v = u;
				this.u = v;
			} else {
				this.v = v;
				this.u = u;
			}
		}

		@Override
		public String toString() {
			return v + " - " + u;
		}

		public String getV() {
			return v;
		}

		public String getU() {
			return u;
		}

		public int compareTo(Edge o) {
			if (!v.equals(o.v))
				return v.compareTo(o.v);
			else
				return u.compareTo(o.u);
		}
	}

}
