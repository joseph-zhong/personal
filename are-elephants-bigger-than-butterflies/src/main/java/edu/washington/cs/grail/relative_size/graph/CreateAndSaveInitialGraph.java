package edu.washington.cs.grail.relative_size.graph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CreateAndSaveInitialGraph {
	private static final String GRAPHS_DIRECTORY = "data/graphs";

	static {
		File graphDirectory = new File(GRAPHS_DIRECTORY);
		graphDirectory.mkdirs();
	}

	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		String graphType = ask("Which graph?", "CONVERGE", "path");

		ObjectsNetwork graph = null;
		String hasObjects = ask("Do you have the list of objects?", "Y", "n");

		if (hasObjects.equals("Y")) {
			System.out.println("Enter your objects all in one line:");
			String objects = input.nextLine();

			Scanner objectParser = new Scanner(objects);
			List<String> objectsList = new ArrayList<String>();
			while (objectParser.hasNext()) {
				objectsList.add(objectParser.next());
			}
			objectParser.close();

			if (graphType.equals("PATH"))
				graph = ObjectsNetworkPath.constructGraph(objectsList);
			else
				graph = ObjectsNetworkConverge.constructGraph(objectsList);

		} else if (hasObjects.equals("N")) {
			if (graphType.equals("PATH"))
				graph = ObjectsNetworkPath.constructGraph();
			else
				graph = ObjectsNetworkConverge.constructGraph();
		}
		graph.save(GRAPHS_DIRECTORY);
		input.close();
	}

	private static Scanner input = new Scanner(System.in);

	private static String ask(String question, String defaultOption,
			String... otherOptions) {

		while (true) {
			System.out.print(question);
			System.out.print(" [" + defaultOption.toUpperCase());
			for (String option : otherOptions) {
				System.out.print("," + option.toLowerCase());
			}
			System.out.print("] ");

			String answer = input.nextLine().trim().toUpperCase();
			if (answer.isEmpty())
				answer = defaultOption.toUpperCase();

			if (answer.equalsIgnoreCase(defaultOption))
				return answer;
			for (String option : otherOptions)
				if (answer.equalsIgnoreCase(option))
					return answer;

		}
	}
}
