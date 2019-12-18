package org.brewingagile.backoffice.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Http {
	public static String basic(String username, String password) {
		return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.ISO_8859_1));
	}
}
