package org.brewingagile.backoffice.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;


import com.google.common.base.Charsets;

public class GitPropertiesDescribeVersionNumberProvider {
	private static final String GIT_DESCRIBE = "git.commit.id.describe";

	private final String softwareVersion;

	public GitPropertiesDescribeVersionNumberProvider(Class<?> clazz, String propertiesFile) {
		Properties properties = fromResource(clazz, propertiesFile);
		String property = properties.getProperty(GIT_DESCRIBE);
		if (property == null) throw new RuntimeException("Could not read " + GIT_DESCRIBE + " from " + propertiesFile + ". Bug!");
		this.softwareVersion = property;
	}

	private static Properties fromResource(Class<?> clazz, String resource) {
		try(InputStream resourceAsStream = Resources2.resourceAsStream(clazz, resource);
				InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream, Charsets.UTF_8)) {
			Properties properties = new Properties();
			properties.load(inputStreamReader);
			return properties;
		} catch (IOException e) {
			throw new RuntimeException("Could not find software version from " + resource + " :(");
		}
	}

	public String softwareVersion() {
		return softwareVersion;		
	}
}
