package org.brewingagile.backoffice.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.stream.Collector;
import java.util.stream.Collectors;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import functional.Either;

public final class JsonReaderWriter {
	private final ObjectMapper objectMapper = objectMapperMaker();

	public JsonReaderWriter() {
	}

	private static ObjectMapper objectMapperMaker() {
		ObjectMapper mapper = new ObjectMapper();
		return mapper;
	}

	public String serialize(Object jurs) {
		try {
			return objectMapper.writeValueAsString(jurs);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public <T> T deserialize(String content, Class<T> clazz) {
		try {
			return objectMapper.readValue(content, clazz);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public <T> Either<String,T> deserializeEither(String content, Class<T> clazz) {
		try {
			return Either.right(objectMapper.readValue(content, clazz));
		} catch (IOException e) {
			return Either.left(e.getMessage());
		}
	}

	public <T> T deserialize(String content, TypeReference<T> typeReference) {
		try {
			return objectMapper.readValue(content, typeReference);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] serializeToByteArray(Object jurs) {
		try {
			return objectMapper.writeValueAsBytes(jurs); // According to JavaDoc this will use UTF-8 encoding
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public <T> T deserialize(byte[] content, Class<T> clazz) {
		try {
			return objectMapper.readValue(content, clazz);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public <T> T deserialize(byte[] content, TypeReference<T> typeReference) {
		try {
			return objectMapper.readValue(content, typeReference);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public JsonNode readTree(String content) {
		try {
			return objectMapper.readTree(content);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public final Function<Object, String> serialize = this::serialize;

	public final Function<String, Optional<JsonNode>> jsonNode = content -> {
		try {
			return Optional.of(objectMapper.readValue(content, JsonNode.class));
		} catch (IOException e) {
			return Optional.absent();
		}
	};

	public Either<String,JsonNode> jsonNodeEither(String content) {
		try {
			return Either.right(objectMapper.readValue(content, JsonNode.class));
		} catch (IOException e) {
			return Either.left(e.getMessage());
		}
	}

	public final <T> Function<String, Either<String, T>> deserializerFor(final Class<T> clazz) {
		return content -> {
			try {
				return Either.right(objectMapper.readValue(content, clazz));
			} catch (IOException e) {
				return Either.left("json deserialization error: " + e.getMessage());
			}
		};
	}

	public static Collector<JsonNode, ?, ArrayNode> toArrayNode() {
		return Collectors.collectingAndThen(Collectors.toList(), list -> {
			ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
			arrayNode.addAll(list);
			return arrayNode;
		});
	}
}
