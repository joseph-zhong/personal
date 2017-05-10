package edu.washington.cs.grail.relative_size.graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import edu.washington.cs.grail.relative_size.SizeComparator;
import edu.washington.cs.grail.relative_size.dataset.ObjectPair;
import edu.washington.cs.grail.relative_size.dataset.SizePairsDataset;
import edu.washington.cs.grail.relative_size.graph.ObjectsNetwork.Edge;
import edu.washington.cs.grail.relative_size.graph.models.Gaussian;
import edu.washington.cs.grail.relative_size.graph.models.Observation;
import edu.washington.cs.grail.relative_size.graph.models.ObservationList;
import edu.washington.cs.grail.relative_size.hybrid.HybridComparator;
import edu.washington.cs.grail.relative_size.hybrid.HybridComparatorProbabilityThreshold;
import edu.washington.cs.grail.relative_size.hybrid.HybridComparatorSubtractAverages;
import edu.washington.cs.grail.relative_size.nlp.NlpSizeComparator;
import edu.washington.cs.grail.relative_size.utils.Config;

public class LoadAndTestObjectsNetwork {
	public static final String TEST_GRAPH_FILE_PATH = Config.getValue(
			"graph-test-file", "data/graphs/41words");
	private static final String NODES_CSV_OUTPUT = "data/visualization/nodes.csv";
	private static final String EDGES_CSV_OUTPUT = "data/visualization/edges.csv";

	private static double probabilityThreshold = Config.getDoubleValue(
			"probability-threshold", 0);

	private ObjectsNetwork graph;

	public LoadAndTestObjectsNetwork(String filePath) throws IOException {
		graph = ObjectsNetwork.load(filePath);
		// graph.recalcSizesFromScratch();
	}

	public void printOptimalPath(String object1, String object2) {
		if (!(graph instanceof ObjectsNetworkPath)) {
			System.out
					.println("Optimal path only defined for path-based graphs.");
			return;
		}

		ArrayList<String> path = ((ObjectsNetworkPath) graph).getOptimalPath(
				object1, object2);
		System.out.println("The optimal path between " + object1 + " and "
				+ object2 + ":");
		for (String node : path) {
			System.out.print(node);
			System.out.println(" ");
		}
	}

	public void printRequiredEdges() {
		Set<Edge> edges = graph.getRequiredEdges();
		System.out.println("There are " + edges.size() + " edges required:");
		for (Edge e : edges)
			System.out.println(e.getV() + " " + e.getU());
	}

	public void compareObjects(String object1, String object2) {
		double prob = graph.compare(object1, object2);
		System.out.println("According to the graph, " + object1
				+ (prob > 0.5 ? " > " : " < ") + object2);
	}

	public void setDistanceFormulator(String distanceFunc) {
		try {
			graph.setDistanceFormulator("edu.washington.cs.grail.relative_size.graph.distance."
					+ distanceFunc);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Failed to set distance function "
					+ distanceFunc);
		}
	}

	public void setAlpha(double alpha) {
		try {
			graph.setDistanceAlpha(alpha);
			graph.updateSubgraph();
		} catch (Exception e) {
			System.err.println("Failed to set distance alpha " + alpha);
		}
	}

	public void printNodes() {
		System.out.println("Here is the list of nodes in the graph:");
		Set<String> nodes = graph.getNodes();
		for (String node : nodes) {
			System.out.print(node + " ");
		}
		System.out.println();
	}

	public void recalc() {
		graph.recalcSubgraphFromScratch();
	}

	public void printAllEdges() {
		ArrayList<Edge> edges = graph.getAllEdges();
		System.out.println("List of all " + edges.size() + " edges:");
		for (Edge e : edges)
			System.out.println(e);
	}

	public void saveGraph(String destFolder) {
		try {
			graph.save(destFolder);
		} catch (IOException e) {
			System.out.println("Failed to write on file. " + e.getMessage());
		}
	}

	public void loadGraph(String path) {
		try {
			graph = ObjectsNetwork.load(path);
		} catch (IOException e) {
			System.out.println("Failed to laod graph.");
		}
	}

	public void printObservations(String object1, String object2) {
		ObservationList obs = graph.getObservationList(object1, object2);
		if (obs == null) {
			System.out.println("We haven't observed this edge.");
		} else {
			System.out.println("We have had " + obs.getCount()
					+ " observations.");
			for (Observation size : obs.getObservations())
				System.out.println(object1 + " / " + object2 + " = "
						+ size.getValue() + " " + size.getScore());
			System.out.println("Average: " + obs.getAverageSize());

		}
	}

	public void exportCsv() {
		PrintStream nps;
		PrintStream eps;
		try {
			nps = new PrintStream(NODES_CSV_OUTPUT);
			eps = new PrintStream(EDGES_CSV_OUTPUT);
		} catch (FileNotFoundException e) {
			System.out.println("Could not open output files.");
			return;
		}

		System.out.println("Exporting nodes to " + NODES_CSV_OUTPUT);
		Set<String> nodes = graph.getNodes();
		Map<String, Integer> str2id = new TreeMap<String, Integer>();

		int nid = 1;
		nps.println("Id,Label");
		for (String node : nodes) {
			nps.println(nid + "," + node);
			str2id.put(node, nid);
			nid++;
		}
		nps.close();

		ArrayList<Edge> edges = graph.getAllEdges();

		System.out.println("Exporting edges to " + EDGES_CSV_OUTPUT);
		int eid = 1;
		eps.println("Source,Target,Type,Id,Label,Weight");
		for (Edge edge : edges) {
			int v = str2id.get(edge.getV());
			int u = str2id.get(edge.getU());

			eps.println(v + "," + u + "," + "Undirected" + "," + eid + ","
					+ "," + graph.getEdgeScore(edge.getV(), edge.getU()));
		}
		eps.close();
	}

	public void reportScore() {
		graph.recalcSizesFromScratch();
		AccuracyResult accuracy = getAccuracyResults();
		System.out.println("Precision: " + accuracy.getPrecisionReport());
	}

	private AccuracyResult getAccuracyResults(SizeComparator comparator) {
		SizePairsDataset sizeDataset = new SizePairsDataset();

		if (comparator instanceof ObjectsNetwork) {
			ObjectsNetwork network = (ObjectsNetwork) comparator;

			if (!network.isObservationSufficient())
				return new AccuracyResult(0, sizeDataset.getTotalSize(), 0);
		}

		int correct = 0, wrong = 0, unknown = 0;
		for (ObjectPair pair : sizeDataset) {
			double prob = comparator.compare(pair.getBiggerObject(),
					pair.getSmallerObject());
			if (prob >= 0.5 + probabilityThreshold)
				correct++;
			else if (prob < 0.5 - probabilityThreshold)
				wrong++;
			else
				unknown++;
			System.out.println(pair.getBiggerObject() + " "
					+ pair.getSmallerObject() + " " + prob);
		}
		return new AccuracyResult(correct, wrong, unknown);
	}

	private AccuracyResult getAccuracyResults() {
		return getAccuracyResults(graph);
	}

	private SizePairsDataset getErrorLabels(SizeComparator comparator) {
		if (comparator instanceof ObjectsNetwork) {
			ObjectsNetwork network = (ObjectsNetwork) comparator;

			if (!network.isObservationSufficient())
				return new SizePairsDataset();
		}

		SizePairsDataset sizeDataset = new SizePairsDataset();
		ArrayList<ObjectPair> errs = new ArrayList<ObjectPair>();
		for (ObjectPair pair : sizeDataset) {
			double prob = comparator.compare(pair.getBiggerObject(),
					pair.getSmallerObject());
			if (prob < 0.5 - probabilityThreshold)
				errs.add(pair);
		}
		return new SizePairsDataset(errs);
	}

	private SizePairsDataset getErrorLabels() {
		return getErrorLabels(graph);
	}

	public ModelParameters reportBestThreshold() {
		return reportBestThreshold(true);
	}

	public ModelParameters reportBestThreshold(boolean verbose) {
		SizePairsDataset dataset = new SizePairsDataset();

		final double MAXIMUM_COEFF = 1;
		final double MINIMUM_COEFF = 1;
		final double STEP_COEFF = 0.01;

		final double MAXIMUM_THRESH = 1;
		final double MINIMUM_THRESH = -1.2;
		final double STEP_THRESH = 0.01;

		double origThreshold = ObservationList.getScoreThreshold();
		double origCoeff = ObservationList.getThreshCoefficient();

		int bestCorrect = 0;
		double bestThreshold = 0;
		double bestCoeff = 0;

		for (double thresh = MINIMUM_THRESH; thresh <= MAXIMUM_THRESH; thresh += STEP_THRESH) {
			ObservationList.setScoreThreshold(thresh);
			ObservationList.setThresholdCoefficient(MAXIMUM_COEFF);
			if (!graph.isObservationSufficient()) {
				break;
			}

			for (double coeff = MAXIMUM_COEFF; coeff >= MINIMUM_COEFF; coeff -= STEP_COEFF) {
				ObservationList.setThresholdCoefficient(coeff);
				if (!graph.isObservationSufficient()) {
					break;
				}
				graph.recalcSizesFromScratch();

				int currentCorrect = getAccuracyResults().getCorrect();
				if (currentCorrect >= bestCorrect) {
					bestCorrect = currentCorrect;
					bestThreshold = thresh;
					bestCoeff = coeff;
				}
			}
		}
		if (verbose) {
			System.out.println("The best score achieved with tresh="
					+ bestThreshold + ", coeff=" + bestCoeff);
			System.out.println("The score is " + bestCorrect + "/"
					+ dataset.getTotalSize() + "=" + 1.0 * bestCorrect
					/ dataset.getTotalSize());
		}

		ObservationList.setScoreThreshold(origThreshold);
		ObservationList.setThresholdCoefficient(origCoeff);

		return new ModelParameters(bestThreshold, bestCoeff,
				(double) bestCorrect / dataset.getTotalSize());
	}

	public ModelParameters reportSmallestThreshold() {
		return reportSmallestThreshold(true);
	}

	public ModelParameters reportSmallestThreshold(boolean verbose) {
		SizePairsDataset dataset = new SizePairsDataset();

		final double MAXIMUM_COEFF = 1;
		final double MINIMUM_COEFF = 1;
		final double STEP_COEFF = 0.01;

		final double MAXIMUM_THRESH = 1;
		final double MINIMUM_THRESH = -1.2;
		final double STEP_THRESH = 0.01;

		double origThreshold = ObservationList.getScoreThreshold();
		double origCoeff = ObservationList.getThreshCoefficient();

		int smallestCorrect = 0;
		double smallestThreshold = 0;
		double smallestCoeff = 0;

		for (double thresh = MINIMUM_THRESH; thresh <= MAXIMUM_THRESH; thresh += STEP_THRESH) {
			ObservationList.setScoreThreshold(thresh);
			ObservationList.setThresholdCoefficient(MAXIMUM_COEFF);
			if (!graph.isObservationSufficient()) {
				break;
			}

			for (double coeff = MAXIMUM_COEFF; coeff >= MINIMUM_COEFF; coeff -= STEP_COEFF) {
				ObservationList.setThresholdCoefficient(coeff);
				if (!graph.isObservationSufficient()) {
					break;
				}
				smallestThreshold = thresh;
				smallestCoeff = coeff;
			}
		}

		ObservationList.setScoreThreshold(smallestThreshold);
		ObservationList.setThresholdCoefficient(smallestCoeff);
		graph.recalcSizesFromScratch();
		smallestCorrect = getAccuracyResults().getCorrect();

		if (verbose) {
			System.out.println("The smallest tresh=" + smallestThreshold
					+ ", coeff=" + smallestCoeff);
			System.out.println("The score is " + smallestCorrect + "/"
					+ dataset.getTotalSize() + "=" + 1.0 * smallestCorrect
					/ dataset.getTotalSize());
		}

		ObservationList.setScoreThreshold(origThreshold);
		ObservationList.setThresholdCoefficient(origCoeff);

		return new ModelParameters(smallestThreshold, smallestCoeff,
				(double) smallestCorrect / dataset.getTotalSize());
	}

	public void reportErrors() {
		SizePairsDataset all = new SizePairsDataset();
		SizePairsDataset errs = getErrorLabels();

		printErrorsStatistics(all, errs);

	}

	public void addNewObjects(ArrayList<String> objects) {
		graph.addObjects(objects.toArray(new String[] {}));
	}

	public void reportAllGraphs() throws IOException {
		System.out.format("%-23s%-13s%-13s%-13s%-13s%-13s%-13s%-13s%-13s\n",
				"Model", "Edges", "thr=conf", "thr=-1", "thr=-0.8", "thr=thr*",
				"thr*", "thr=thr'", "thr'");

		final String FORMAT = "%-23s%-13d%-13f%-13f%-13f%-13f%-13f%-13f%-13f\n";
		File testGraph = new File(TEST_GRAPH_FILE_PATH);
		File dir = testGraph.getParentFile();

		for (File graphFile : dir.listFiles()) {
			if (!graphFile.isFile() || !graphFile.getName().endsWith("Full"))
				continue;

			LoadAndTestObjectsNetwork graphWrapper = new LoadAndTestObjectsNetwork(
					graphFile.getAbsolutePath());

			SizePairsDataset sizeDataset = new SizePairsDataset();

			double curThresh = ObservationList.getScoreThreshold();

			String model = graphFile.getName();
			int edges = graphWrapper.graph.getAllEdges().size();
			double score = (double) graphWrapper.getAccuracyResults()
					.getCorrect() / sizeDataset.getTotalSize();
			ObservationList.setScoreThreshold(-1);
			double score1 = (double) graphWrapper.getAccuracyResults()
					.getCorrect() / sizeDataset.getTotalSize();
			ObservationList.setScoreThreshold(-0.8);
			double score08 = (double) graphWrapper.getAccuracyResults()
					.getCorrect() / sizeDataset.getTotalSize();
			// ParametersTuning bestConfig = graphWrapper
			// .reportBestThreshold(false);
			double bestScore = 0;// bestConfig.getScore();
			double bestThresh = 0;// bestConfig.getThreshold();

			// ParametersTuning smallestConfig = graphWrapper
			// .reportSmallestThreshold(false);
			double smallestScore = 0;// smallestConfig.getScore();
			double smallestThresh = 0;// smallestConfig.getThreshold();

			System.out.format(FORMAT, model, edges, score, score1, score08,
					bestScore, bestThresh, smallestScore, smallestThresh);

			ObservationList.setScoreThreshold(curThresh);

		}

	}

	public void reportNlpScore() {
		NlpSizeComparator nlp = new NlpSizeComparator();
		AccuracyResult accuracy = getAccuracyResults(nlp);
		System.out.println("Precision: " + accuracy.getPrecisionReport());
	}

	public void reportNlpErrors() {
		SizePairsDataset all = new SizePairsDataset();
		SizePairsDataset errs = getErrorLabels(new NlpSizeComparator());

		printErrorsStatistics(all, errs);
	}

	private void printErrorsStatistics(SizePairsDataset all,
			SizePairsDataset errs) {
		for (ObjectPair pair : errs) {
			System.out.println("ERROR: " + pair);
		}

		Map<String, Integer> errMap = errs.getObjectRate();
		Map<String, Integer> bigErrMap = errs.getBiggerObjectRate();
		Map<String, Integer> smallErrMap = errs.getSmallerObjectRate();
		Map<String, Integer> allMap = all.getObjectRate();

		for (Map.Entry<String, Integer> entry : allMap.entrySet()) {
			Integer errRate = errMap.get(entry.getKey());
			if (errRate == null)
				errRate = 0;
			Integer bigErrRate = bigErrMap.get(entry.getKey());
			if (bigErrRate == null)
				bigErrRate = 0;
			Integer smallErrRate = smallErrMap.get(entry.getKey());
			if (smallErrRate == null)
				smallErrRate = 0;

			System.out.println(entry.getKey() + "\t"
					+ (1.0 * errRate / entry.getValue()) + "\t(" + bigErrRate
					+ "," + smallErrRate + ") / " + entry.getValue());
		}
	}

	public void reportAbsoluteSizes() {
		if (!(graph instanceof ObjectsNetworkConverge)) {
			System.out.println("GRAPH does not compute absolute size.");
			return;
		}
		ObjectsNetworkConverge graphConverge = (ObjectsNetworkConverge) graph;
		for (String object : graphConverge.getNodes()) {
			Gaussian now = graphConverge.getLogSizeGaussian(object);
			System.out.println(object + "\t" + Math.exp(now.getMean()) + "\t"
					+ now.getVariance());
		}
	}

	public void reportNlpAbsoluteSizes() {
		NlpSizeComparator nlp = new NlpSizeComparator();

		double avg = 0;
		for (String object : graph.getNodes())
			avg += nlp.getLogAbsoluteSizeGaussian(object).getMean();
		avg /= graph.getNodes().size();

		for (String object : graph.getNodes()) {
			Gaussian now = nlp.getLogAbsoluteSizeGaussian(object);
			System.out.println(object + "\t" + Math.exp(now.getMean() - avg)
					+ "\t" + now.getVariance());
		}
	}

	public void reportPrecisionThreshold(SizeComparator comparator) {
		AccuracyResult accuracy = getAccuracyResults(comparator);
		SizePairsDataset sizeDataset = new SizePairsDataset();

		ArrayList<Double> probabilities = new ArrayList<Double>();
		for (ObjectPair pair : sizeDataset) {
			double prob = comparator.compare(pair.getBiggerObject(),
					pair.getSmallerObject());
			probabilities.add(prob);
		}
		probabilities.sort(new Comparator<Double>() {
			public int compare(Double p1, Double p2) {
				return (int) Math.signum(Math.abs(p1 - 0.5)
						- Math.abs(p2 - 0.5));
			}
		});

		int correct = accuracy.getCorrect(), wrong = accuracy.getWrong(), unknown = accuracy
				.getUnknown();
		for (int i = 0; i < probabilities.size(); i++) {
			double prob = probabilities.get(i);
			double declarationRate = (double) (probabilities.size() - i)
					/ probabilities.size();

			AccuracyResult currectAcc = new AccuracyResult(correct, wrong,
					unknown);
			System.out.println(currectAcc.getPrecision() + "\t"
					+ declarationRate);
			if (prob >= 0.5) {
				correct--;
			} else {
				wrong--;
			}
			unknown++;
		}
	}

	public void reportPrecisionThreshold() {
		reportPrecisionThreshold(graph);
	}

	public void reportPrecisionThresholdNlp() {
		NlpSizeComparator nlp = new NlpSizeComparator();
		reportPrecisionThreshold(nlp);
	}

	public void reportPrecisionThresholdHybrid() {
		HybridComparator hybrid = getHybridModel();
		if (hybrid == null)
			return;
		reportPrecisionThreshold(hybrid);
	}

	public void reportHybridScore() {
		HybridComparator hybrid = getHybridModel();
		if (hybrid == null)
			return;

		AccuracyResult accuracy = getAccuracyResults(hybrid);
		System.out.println("Precision: " + accuracy.getPrecisionReport()
				+ ". Recall: " + accuracy.getRecallReport());
	}

	public void reportHybridErrors() {
		HybridComparator hybrid = getHybridModel();
		if (hybrid == null)
			return;

		SizePairsDataset all = new SizePairsDataset();
		SizePairsDataset errs = getErrorLabels(hybrid);

		printErrorsStatistics(all, errs);
	}

	public void reportHybridAbsoluteSizes() {
		HybridComparator hybrid = getHybridModel();
		if (hybrid == null)
			return;

		for (String object : graph.getNodes()) {
			Gaussian g = hybrid.getAbsoluteSizeGaussian(object);
			System.out.println(object + "\t" + g.getMean() + "\t"
					+ g.getVariance());
		}
	}

	public void injectObjectSize(String object, Double size) {
		if (!(graph instanceof ObjectsNetworkConverge)) {
			System.out
					.println("This is feature is just for converge model. Abort.");
			return;
		}
		ObjectsNetworkConverge graphConverge = (ObjectsNetworkConverge) graph;
		graphConverge.injectObjectSize(object, size);
	}

	private HybridComparator getHybridModel() {
		if (!(graph instanceof ObjectsNetworkConverge)) {
			System.out
					.println("Graph doesn't have absolute distance. Cannot combine.");
			return null;
		}
		// if (!graph.isObservationSufficient()) {
		// System.out
		// .println("Graph does not have sufficient observations in it.");
		// return null;
		// }
		ObjectsNetworkConverge graphConverge = (ObjectsNetworkConverge) graph;

		NlpSizeComparator nlp = new NlpSizeComparator();
		HybridComparator hybrid = new HybridComparatorSubtractAverages(nlp,
				graphConverge);
		return hybrid;
	}

	public void reportHybridThresholdPlot() {
		final double START_THR = 0;
		final double END_THR = 0.5;
		final double THR_STEP = 0.01;

		HybridComparator hybrid = getHybridModel();
		if (hybrid == null) {
			return;
		} else if (!(hybrid instanceof HybridComparatorProbabilityThreshold)) {
			System.out
					.println("The current Hybrid model does not support threshold. Abort.");
			return;
		}

		HybridComparatorProbabilityThreshold hybridThresh = (HybridComparatorProbabilityThreshold) hybrid;
		double EPS = 1e-7;
		for (double thr = START_THR; thr <= END_THR + EPS; thr += THR_STEP) {
			hybridThresh.setThreshold(thr);
			AccuracyResult accuracy = getAccuracyResults(hybridThresh);

			System.out.println(String.format("%.2f", thr) + "\t"
					+ accuracy.getPrecision());
		}
	}

	public void autoInjectFromNlp(int count) {
		if (!(graph instanceof ObjectsNetworkConverge)) {
			System.out.println("Graph is not converge model, cannot inject.");
			return;
		}
		ObjectsNetworkConverge graphConverge = (ObjectsNetworkConverge) graph;
		graphConverge.autoInjectObjectsFromNLP(count);
	}

	public static void main(String[] args) throws IOException {
		LoadAndTestObjectsNetwork instance = new LoadAndTestObjectsNetwork(
				TEST_GRAPH_FILE_PATH);
		Scanner input = new Scanner(System.in);
		while (true) {
			System.out.print("> ");
			String command = input.next().toUpperCase();
			if (command.equals("PATH")) {
				// Finds the optimal path between two nodes, only for object
				// path graphs.
				String object1 = input.next().toLowerCase();
				String object2 = input.next().toLowerCase();
				instance.printOptimalPath(object1, object2);
			} else if (command.equals("EDGES")) {
				// Prints all of the edges of the graph.
				instance.printAllEdges();
			} else if (command.equals("COMPARE")) {
				// Compares the size of two objects.
				String object1 = input.next().toLowerCase();
				String object2 = input.next().toLowerCase();
				instance.compareObjects(object1, object2);
			} else if (command.equals("REQ")) {
				// Prints list of edges for which we yet need observations.
				instance.printRequiredEdges();
			} else if (command.equals("OBS")) {
				// Prints the list of binary observations for an edge.
				String object1 = input.next().toLowerCase();
				String object2 = input.next().toLowerCase();
				instance.printObservations(object1, object2);
			} else if (command.equals("DFUNC")) {
				// Sets the distance function for the baseline that finds the
				// optimal path.
				String distanceFunc = input.next();
				instance.setDistanceFormulator(distanceFunc);
			} else if (command.equals("RECALC")) {
				// Re-optimizes the parameters of log-normal distributions.
				instance.recalc();
			} else if (command.equals("NODES")) {
				// Prints the list of the nodes in the graph.
				instance.printNodes();
			} else if (command.equals("SAVE")) {
				// Save the graph into a file.
				String destFolder = input.next();
				instance.saveGraph(destFolder);
			} else if (command.equals("LOAD")) {
				// Loads the graph from a file.
				String path = input.next();
				instance.loadGraph(path);
			} else if (command.equals("CSV")) {
				// Exports graph into a csv file for visualization.
				instance.exportCsv();
			} else if (command.equals("ERRORS")) {
				// Prints the errors based on the comparisons in the dataset.
				instance.reportErrors();
			} else if (command.equals("EVALUATE")) {
				// Evaluates model on the test dataset.
				instance.reportScore();
			} else if (command.equals("THRESHOLD")) {
				// Finds the best threshold for the object detector.
				instance.reportBestThreshold();
			} else if (command.equals("INJECT")) {
				// Fixes a the size of an object with a value.
				String object = input.next();
				Double size = input.nextDouble();
				instance.injectObjectSize(object, size);
			} else if (command.equals("AUTO_INJECT")) {
				// Injects the size of objects from NLP in a greedy fashion to
				// get the highest accuracy.
				int cnt = input.nextInt();
				instance.autoInjectFromNlp(cnt);
			} else if (command.equals("ADD")) {
				// Add a list of objects to the graph.
				String line = input.nextLine();
				ArrayList<String> objects = new ArrayList<String>();

				Scanner objectScanner = new Scanner(line);
				while (objectScanner.hasNext()) {
					objects.add(objectScanner.next());
				}
				objectScanner.close();

				instance.addNewObjects(objects);
			} else if (command.equals("DISTRIBUTIONS")) {
				// Reports the distributions learned for all the objects.
				instance.reportAbsoluteSizes();
			} else if (command.equals("NLP_DISTRIBUTIONS")) {
				// Reports the distributions learned from language only for all
				// the objects.
				instance.reportNlpAbsoluteSizes();
			} else if (command.equals("EVALUATE_HYBRID")) {
				// Evaluate the "Hybrid" model. Hybrid is a model that is not
				// reported in the paper, because it's a little hacky, but it
				// gets a little better accuracy.
				instance.reportHybridScore();
			} else if (command.equals("HYBRID_ERRORS")) {
				// Print the errors of the hybrid model.
				instance.reportHybridErrors();
			} else if (command.equals("NLP_ERRORS")) {
				// Print the errors of language only model.
				instance.reportNlpErrors();
			} else if (command.equals("EVALUATE_NLP")) {
				// Evaluate the language only model.
				instance.reportNlpScore();
			} else if (command.equals("HYBRID_DISTIBUTIONS")) {
				// Prints the distributions that the hybrid model has learned. 
				instance.reportHybridAbsoluteSizes();
			} else if (command.equals("QUIT")) {
				break;
			} else {
				System.out.println("Unrecognized command: " + command);
				input.nextLine();
			}
		}
		input.close();
	}

	public static class ModelParameters {
		private double threshold;
		private double coefficient;
		private double score;

		public ModelParameters(double threshold, double coefficient,
				double score) {
			this.threshold = threshold;
			this.coefficient = coefficient;
			this.score = score;
		}

		public double getThreshold() {
			return threshold;
		}

		public double getCoefficient() {
			return coefficient;
		}

		public double getScore() {
			return score;
		}
	}

	public static class AccuracyResult {
		private int correct;
		private int wrong;
		private int unknown;

		public AccuracyResult(int correct, int wrong, int unknown) {
			this.correct = correct;
			this.wrong = wrong;
			this.unknown = unknown;
		}

		public int getCorrect() {
			return correct;
		}

		public int getWrong() {
			return wrong;
		}

		public int getUnknown() {
			return unknown;
		}

		public double getPrecision() {
			if (correct + wrong == 0)
				return 0;
			else
				return 1.0 * correct / (correct + wrong);
		}

		public double getRecall() {
			if (correct + wrong + unknown == 0)
				return 0;
			else
				return 1.0 * correct / (correct + wrong + unknown);
		}

		public String getRecallReport() {
			return correct + "/" + (correct + wrong + unknown) + " = "
					+ getRecall();
		}

		public String getPrecisionReport() {
			return correct + "/" + (correct + wrong) + " = " + getPrecision();
		}
	}

}