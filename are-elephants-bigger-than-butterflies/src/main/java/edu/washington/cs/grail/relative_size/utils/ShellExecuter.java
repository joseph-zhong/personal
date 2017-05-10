package edu.washington.cs.grail.relative_size.utils;

import java.io.IOException;
import java.util.Scanner;

public class ShellExecuter {
	private String command;

	public ShellExecuter(String command) {
		this.command = command;
	}

	public String execute() throws IOException, InterruptedException {
		Process proc = Runtime.getRuntime().exec(command);
		proc.waitFor();

		Scanner input = null;
		try {
			input = new Scanner(proc.getInputStream());
			StringBuilder sb = new StringBuilder();
			while (input.hasNextLine()) {
				sb.append(input.nextLine());
			}
			return sb.toString();
		} finally {
			if (input != null)
				input.close();
		}
	}
}
