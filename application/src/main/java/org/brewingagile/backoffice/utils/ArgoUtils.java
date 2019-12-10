package org.brewingagile.backoffice.utils;

import argo.format.CompactJsonFormatter;
import argo.format.JsonFormatter;
import argo.jdom.JdomParser;
import argo.jdom.JsonNode;
import argo.saj.InvalidSyntaxException;
import fj.data.Either;

import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static argo.jdom.JsonNodeFactories.*;

public final class ArgoUtils {
	private static final JsonFormatter COMPACT_FORMATTER = new CompactJsonFormatter();
	private static final JdomParser JDOM_PARSER = new JdomParser();

	public static String format(JsonNode json) {
		return COMPACT_FORMATTER.format(json);
	}

	public static Collector<JsonNode, ?, JsonNode> toArray() {
		return Collectors.collectingAndThen(Collectors.toList(), list -> array(list));
	}

	public static Collector<String, ?, JsonNode> toStringArray() {
		return Collectors.mapping(s -> string(s), toArray());
	}

	public static <V> Collector<V, ?, JsonNode> toObject(Function<V, String> keyFn, Function<V, JsonNode> nodeFn) {
		return Collectors.mapping(
			v -> field(keyFn.apply(v), nodeFn.apply(v)),
			Collectors.collectingAndThen(Collectors.toList(), fields -> object(fields))
		);
	}

	public static JsonNode parse(String s) throws InvalidSyntaxException {
		return JDOM_PARSER.parse(s);
	}

	public static Either<String, JsonNode> parseEither(String s) {
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
		if (body.isNullNode(f)) return "";
		if (!body.isStringValue(f)) return "";
		return body.getStringValue(f);
	}
}
