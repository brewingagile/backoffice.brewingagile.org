package org.brewingagile.backoffice.utils;

import com.hencjo.summer.migration.util.Charsets;

import java.io.UnsupportedEncodingException;

public class URLEncoder {
	public static String encode(String s) {
		try {
			return java.net.URLEncoder.encode(s, Charsets.UTF8.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}