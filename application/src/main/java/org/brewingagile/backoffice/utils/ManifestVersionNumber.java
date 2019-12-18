package org.brewingagile.backoffice.utils;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ManifestVersionNumber {
	public String version(Class<ManifestVersionNumber> clazz) throws IOException {
		String className = clazz.getSimpleName() + ".class";
		String classPath = clazz.getResource(className).toString();
		if (!classPath.startsWith("jar"))
			throw new IllegalStateException(this.getClass().getName() + " only works when run as a JAR.");

		String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) +
			"/META-INF/MANIFEST.MF";
		Manifest manifest = new Manifest(new URL(manifestPath).openStream());
		Attributes attr = manifest.getMainAttributes();
		return attr.getValue("Implementation-Version");
	}
}
