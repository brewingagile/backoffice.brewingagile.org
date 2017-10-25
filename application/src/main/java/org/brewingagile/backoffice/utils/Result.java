package org.brewingagile.backoffice.utils;

import argo.jdom.JsonRootNode;

import static argo.jdom.JsonNodeFactories.*;

public final class Result {
	public static JsonRootNode success2(String message) {
		return object(
			field("style", string("success")),
			field("message", string(message))
		);
	}
	public static JsonRootNode warning(String message) {
		return object(
			field("style", string("warning")),
			field("message", string(message))
		);
	}

	public static JsonRootNode success(String message) { return result(message, true); }
	public static JsonRootNode failure(String message) { return result(message, false); }

	private static JsonRootNode result(String message, boolean value) {
		return object(
			field("message", string(message)),
			field("success", booleanNode(value)),
			field("data", nullNode())
		);
	}
}