package org.brewingagile.backoffice.utils;

import argo.jdom.JsonNode;

import static argo.jdom.JsonNodeFactories.*;

public final class Result {
	public static JsonNode success2(String message) {
		return object(
			field("style", string("success")),
			field("message", string(message))
		);
	}
	public static JsonNode warning(String message) {
		return object(
			field("style", string("warning")),
			field("message", string(message))
		);
	}

	public static JsonNode success(String message) { return result(message, true); }
	public static JsonNode failure(String message) { return result(message, false); }

	private static JsonNode result(String message, boolean value) {
		return object(
			field("message", string(message)),
			field("success", booleanNode(value)),
			field("data", nullNode())
		);
	}
}