package org.brewingagile.backoffice.utils;

public class Hex {
	public static String encode(byte[] buf) {
		return javax.xml.bind.DatatypeConverter.printHexBinary(buf);
	}

	public static byte[] decode(String s) {
		return javax.xml.bind.DatatypeConverter.parseHexBinary(s);
	}
}
