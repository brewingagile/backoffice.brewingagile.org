package org.brewingagile.backoffice.utils;

import org.glassfish.jersey.internal.util.Base64;

public class Http {
	public static String basic(String username, String password) {
		return "Basic " + Base64.encodeAsString(username + ":" + password);
	}
}
