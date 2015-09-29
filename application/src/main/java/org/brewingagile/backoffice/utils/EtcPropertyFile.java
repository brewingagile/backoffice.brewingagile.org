package org.brewingagile.backoffice.utils;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import com.hencjo.summer.migration.util.Resources;
import fj.data.Either;

public class EtcPropertyFile {
	private final Properties properties;

	private EtcPropertyFile(Properties properties) {
		this.properties = properties;
	}

	public static Either<String,EtcPropertyFile> from(String propsFilename) {
		Properties properties = new Properties();
		try {
			properties.load(new FileReader(propsFilename));
		} catch (IOException e) {
			return Either.left("Could not read/find application property file '" + propsFilename + "'.");
		}
		return Either.right(new EtcPropertyFile(properties));
	}

	public int integer(String key) {
		String property = properties.getProperty(key);
		if (property == null) throw new RuntimeException("Missing property: " + key);
		try {
			return Integer.parseInt(property);
		} catch (NumberFormatException e) {
			throw new RuntimeException("Illegal value for integer property: " + key + ": " + property);
		}
	}

	public String string(String key) {
		String property = properties.getProperty(key);
		if (property == null) throw new RuntimeException("Missing property: " + key);
		return property;
	}
	
	public boolean bool(String key) {
		String property = properties.getProperty(key);
		if (property == null) throw new RuntimeException("Missing property: " + key);
		if ("true".equalsIgnoreCase(property)) return true;
		if ("false".equalsIgnoreCase(property)) return false;
		throw new RuntimeException("Illegal value for boolean property: " + key + ": " + property);
	}

	public static <T> T killApplicationIfPropertiesAreMissing(Either<String, T> either) {
		try {
			return either.right().value();
		} catch (Exception e) {
			System.err.println("Could not read properties from properties file :(.");
			System.err.println(either.left());
			System.exit(1);
			throw new RuntimeException("Never happens.");
		}
	}

	public static Either<String,EtcPropertyFile> discover() {
		return from(Resources.getResource("backend.conf").toExternalForm());
	}
}