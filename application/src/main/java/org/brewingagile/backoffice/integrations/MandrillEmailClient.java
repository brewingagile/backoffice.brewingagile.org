package org.brewingagile.backoffice.integrations;

import argo.jdom.JsonRootNode;
import com.google.common.base.Strings;
import fj.data.Either;
import org.brewingagile.backoffice.utils.ArgoUtils;

import static argo.jdom.JsonNodeFactories.*;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

public class MandrillEmailClient {
	private final Client client;
	private final String endpoint;
	private final String apikey;

	public MandrillEmailClient(Client client, String endpoint, String apikey) {
		this.client = client;
		this.endpoint = endpoint;
		this.apikey = apikey;
	}

	public Either<String,String> sendRegistrationReceived(String emailAddress) {
		JsonRootNode request = request(emailAddress, "registrationconfirmationemail");

		try {
			return response(client.target(endpoint).path("messages/send-template.json").request()
				.accept(MediaType.APPLICATION_JSON)
				.post(Entity.entity(ArgoUtils.format(request), MediaType.APPLICATION_JSON)).readEntity(String.class));
		} catch (WebApplicationException e) {
			return Either.left(e.getMessage());
		}
	}

	private static Either<String, String> response(String json) {
		return ArgoUtils.parseEither(json)
			.left().map(l -> "Mandrill response did not contain JSON at all." )
			.right().bind(MandrillEmailClient::response)
			.left().map(l -> "Payload: " + json);
	}

	private static Either<String, String> response(JsonRootNode jn) {
		if (!jn.isArrayNode()) return Either.left("Expected array at top-level.");
		if (jn.getArrayNode().isEmpty()) return Either.left("Expected non-empty array at top-level.");
		argo.jdom.JsonNode jsonNode = jn.getArrayNode().get(0);

		String status = ArgoUtils.stringOrEmpty(jsonNode, "status");
		String messageId = ArgoUtils.stringOrEmpty(jsonNode, "_id");

		if (!"sent".equals(status)) return Either.left("Mandrill Response Status was not expected 'sent' but '" + status + "'.");
		if (Strings.isNullOrEmpty(messageId)) return Either.left("Mandrill Message Id was not present. That's odd.");
		return Either.right(messageId);
	}

	private JsonRootNode request(String emailAddress, String templateName) {
		return object(
			field("async", booleanNode(false)),
			field("key", string(apikey)),
			field("template_content", array()),
			field("message", object(
				field("to", to(emailAddress)),
				field("inline_css", booleanNode(true))
			)),
			field("template_name", string(templateName))
		);
	}

	private static JsonRootNode to(String emailAddress) {
		return array(object(
			field("email", string(emailAddress))
		));
	}
}
