package edu.washington.cs.grail.relative_size.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

public class Config {
	private static final Logger LOG = Logger.getLogger(Config.class.getName());

	private static final String CONFIG_PATH = "relative-size.properties";
	private static Properties properties = new Properties();

	static {
		try {
			FileInputStream fis = new FileInputStream(CONFIG_PATH);
			properties.load(fis);
		} catch (FileNotFoundException e) {
			LOG.info("Config file not found. Proceeding with default configs.");
		} catch (IOException e) {
			LOG.info("Failed to read/parse the config file. Proceeding with default configs.");
		}
	}

	public static String getValue(String key, String defaultValue) {
		if (properties == null)
			return defaultValue;

		String value = properties.getProperty(key);
		if (value == null) {
			LOG.info(key
					+ " not found in the config file. Proceeding with the default value.");
			return defaultValue;
		}

		return value;
	}

	public static int getIntValue(String key, int defaultValue) {
		String value = getValue(key, null);
		if (value == null)
			return defaultValue;

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException nfe) {
			LOG.info("Format of "
					+ key
					+ " in the config file is not an integer. Proceeding with the default value.");
			return defaultValue;
		}
	}
	
	public static boolean getBooleanValue(String key, boolean defaultValue) {
		String value = getValue(key, null);
		if (value == null)
			return defaultValue;

		try {
			return Boolean.parseBoolean(value);
		} catch (NumberFormatException nfe) {
			LOG.info("Format of "
					+ key
					+ " in the config file is not a boolean. Proceeding with the default value.");
			return defaultValue;
		}
	}
	
	public static double getDoubleValue(String key, double defaultValue) {
		String value = getValue(key, null);
		if (value == null)
			return defaultValue;

		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException nfe) {
			LOG.info("Format of "
					+ key
					+ " in the config file is not n double. Proceeding with the default value.");
			return defaultValue;
		}
	}

	public static int[] getIntArray(String key, int[] defaultValue) {
		String value = getValue(key, null);
		if (value == null)
			return defaultValue;
		
		String[] strArr = value.split(",");
		int[] intArr = new int[strArr.length];
		
		for (int i=0 ; i<strArr.length ; i++){
			try {
				intArr[i] = Integer.parseInt(strArr[i]);
			} catch (NumberFormatException nfe) {
				LOG.info("Format of "
						+ key
						+ " in the config file is not an integer array. Proceeding with the default value.");
				return defaultValue;
			}
		}
		return intArr;
	}
}
