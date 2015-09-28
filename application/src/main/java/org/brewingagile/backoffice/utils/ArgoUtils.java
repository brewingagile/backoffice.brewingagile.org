package org.brewingagile.backoffice.utils;

import argo.format.CompactJsonFormatter;
import argo.format.JsonFormatter;
import argo.jdom.JdomParser;
import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.saj.InvalidSyntaxException;
import fj.data.Either;

import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static argo.jdom.JsonNodeFactories.*;

public final class ArgoUtils {
	private static final JsonFormatter COMPACT_FORMATTER = new CompactJsonFormatter();
	private static final JdomParser JDOM_PARSER = new JdomParser();

	public static String format(JsonRootNode json) {
		return COMPACT_FORMATTER.format(json);
	}

	public static Collector<JsonNode, ?, JsonRootNode> toArray() {
		return Collectors.collectingAndThen(Collectors.toList(), list -> array(list));
	}

	public static Collector<String, ?, JsonRootNode> toStringArray() {
		return Collectors.mapping(s -> string(s), toArray());
	}

	public static <V> Collector<V, ?, JsonRootNode> toObject(Function<V, String> keyFn, Function<V, JsonNode> nodeFn) {
		return Collectors.mapping(
			v -> field(keyFn.apply(v), nodeFn.apply(v)),
			Collectors.collectingAndThen(Collectors.toList(), fields -> object(fields))
		);
	}

	public static JsonRootNode parse(String s) throws InvalidSyntaxException {
		return JDOM_PARSER.parse(s);
	}

	public static Either<String, JsonRootNode> parseEither(String s) {
		try {
			return Either.right(JDOM_PARSER.parse(s));
		} catch (InvalidSyntaxException ex) {
			return Either.left(ex.getMessage());
		}
	}

	public static Either<String, String> stringValue(JsonNode dom, String path) {
		if (!dom.isStringValue(path)) return Either.left(path + " not found/is not a string.");
		return Either.right(dom.getStringValue(path));
	}

	public static String stringOrEmpty(JsonNode body, String f) {
		if (!body.isStringValue(f)) return "";
		return body.getStringValue(f);
	}
}
