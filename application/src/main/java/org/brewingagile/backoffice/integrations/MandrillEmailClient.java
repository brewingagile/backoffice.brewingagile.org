package org.brewingagile.backoffice.integrations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import functional.Either;
import org.brewingagile.backoffice.utils.JsonReaderWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

public class MandrillEmailClient {
	private static final JsonReaderWriter jsonReaderWriter = new JsonReaderWriter();
	
	private final Client client;
	private final String endpoint;
	private final String apikey;

	public MandrillEmailClient(Client client, String endpoint, String apikey) {
		this.client = client;
		this.endpoint = endpoint;
		this.apikey = apikey;
	}

	public Either<String,String> sendRegistrationReceived(String emailAddress) {
		JsonNode templateContent = JsonNodeFactory.instance.arrayNode();
		ObjectNode request = request(emailAddress, "registrationconfirmationemail", templateContent);

		try {
			return response(client.target(endpoint).path("messages/send-template.json").request()
				.accept(MediaType.APPLICATION_JSON)
				.post(Entity.entity(jsonReaderWriter.serialize(request), MediaType.APPLICATION_JSON)).readEntity(String.class));
		} catch (WebApplicationException e) {
			return Either.left(e.getMessage());
		}
	}

	private Either<String,String> response(String json) {
		return jsonReaderWriter.jsonNode.apply(json).transform(MandrillEmailClient::response).or(Either.left("Mandrill response did not contain JSON at all."));
	}

	private static Either<String, String> response(JsonNode jn) {
		String status = jn.path(0).path("status").asText();
		String messageId = jn.path(0).path("_id").asText();
		if (!"sent".equals(status))
			return Either.left("Mandrill Response Status was not expected 'sent' but '" + status + "'.");
		if (Strings.isNullOrEmpty(messageId)) return Either.left("Mandrill Message Id was not present. That's odd.");
		return Either.right(messageId);
	}

	private ObjectNode request(String emailAddress, String templateName, JsonNode templateContent) {
		ObjectNode message = JsonNodeFactory.instance.objectNode()
			.putPOJO("to", to(emailAddress))
			.put("inline_css", true);

		return JsonNodeFactory.instance.objectNode()
			.put("async", false)
			.put("key", apikey)
			.putPOJO("template_content", templateContent)
			.putPOJO("message", message)
			.put("template_name", templateName);
	}

	private static ArrayNode to(String emailAddress) {
		ObjectNode email = JsonNodeFactory.instance.objectNode().put("email", emailAddress);
		return JsonNodeFactory.instance.arrayNode().add(email);
	}
}
