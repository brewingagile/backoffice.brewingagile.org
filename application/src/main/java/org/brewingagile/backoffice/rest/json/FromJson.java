package org.brewingagile.backoffice.rest.json;

import argo.jdom.JsonNode;

public class FromJson {
	public static JsonNode getNullableNode(JsonNode node, String f) {
		if (node.isNullNode(f)) return null;
		return node.getNode(f);
	}
}
