package org.brewingagile.backoffice.utils;

public class Strings {
	public static String emptyToNull(String x) {
		if (x.isEmpty()) return null;
		return x;
	}
}
