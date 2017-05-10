package edu.washington.cs.grail.relative_size.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.logging.Logger;

public class Downloader {
	private static final Logger LOG = Logger.getLogger(Downloader.class
			.getName());

	public static void download(URL url, String filePath) throws IOException {
		InputStream is;
		try {
			is = url.openStream();
		} catch (IOException e) {
			LOG.warning("Failed to open url stream. " + e.getMessage());
			throw e;
		}
		OutputStream os;
		try {
			os = new FileOutputStream(filePath);
		} catch (FileNotFoundException e) {
			LOG.warning("Failed to open image file to write. " + e.getMessage());
			throw e;
		}

		byte[] b = new byte[2048];
		int length;

		try {
			while ((length = is.read(b)) != -1) {
				os.write(b, 0, length);
			}
		} catch (IOException e) {
			LOG.warning("Failed to read or write. " + e.getMessage());
			throw e;
		}

		try {
			is.close();
		} catch (IOException e) {
			LOG.warning("Failed to close input. " + e.getMessage());
		}
		try {
			os.close();
		} catch (IOException e) {
			LOG.warning("Failed to close output. " + e.getMessage());
		}
	}

	public static String download(URL url) throws IOException {
		InputStream is;
		try {
			is = url.openStream();
		} catch (IOException e) {
			LOG.warning("Failed to open url stream. " + e.getMessage());
			throw e;
		}

		Scanner urlScanner = null;

		try {
			urlScanner = new Scanner(is);
			urlScanner.useDelimiter("\\A");
			return urlScanner.next();
		} finally {
			if (urlScanner != null)
				urlScanner.close();
		}
	}
}
