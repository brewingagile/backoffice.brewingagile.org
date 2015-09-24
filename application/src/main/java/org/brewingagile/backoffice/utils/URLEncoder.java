package org.brewingagile.backoffice.utils;

import java.io.UnsupportedEncodingException;
import com.google.common.base.Charsets;

public class URLEncoder {
	public static String encode(String s) {
		try {
			return java.net.URLEncoder.encode(s, Charsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}