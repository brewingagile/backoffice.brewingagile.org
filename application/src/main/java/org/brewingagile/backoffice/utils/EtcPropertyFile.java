package org.brewingagile.backoffice.utils;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import com.hencjo.summer.migration.util.Resources;
import fj.data.Either;

public class EtcPropertyFile {
	private final Properties properties;

	private EtcPropertyFile(Properties properties) {
		this.properties = properties;
	}

	public static Either<IOException, EtcPropertyFile> from(Reader reader) {
		Properties properties = new Properties();
		try {
			properties.load(reader);
			return Either.right(new EtcPropertyFile(properties));
		} catch (IOException e) {
			return Either.left(e);
		}
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
//
//	public static <T> T killApplicationIfPropertiesAreMissing(Try<T> either) {
//		try {
//			return either.get();
//		} catch (Exception e) {
//			System.err.println("Could not read properties from properties file :(.");
//			either.exception().get().printStackTrace();
//			System.exit(1);
//			throw new RuntimeException("Never happens.");
//		}
//	}
}