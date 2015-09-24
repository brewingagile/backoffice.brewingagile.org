package org.brewingagile.backoffice.utils;

import java.io.InputStream;

public class Resources2 {
	public static InputStream resourceAsStream(Class<?> clazz, String resource) {
		InputStream resourceAsStream = clazz.getResourceAsStream(resource);
		if (resourceAsStream == null) throw new IllegalArgumentException(String.format("resource %s relative to %s not found.", resource, clazz.getName()));
		return resourceAsStream;
	}
}
