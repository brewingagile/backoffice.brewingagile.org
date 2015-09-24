package org.brewingagile.backoffice.integrations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import functional.Effect;
import functional.Either;
import org.brewingagile.backoffice.utils.JsonReaderWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

public class MailchimpSubscribeClient {
	private static final JsonReaderWriter jsonReaderWriter = new JsonReaderWriter();

	private final Client client;
	private final String endpoint;
	private final String apikey;

	public MailchimpSubscribeClient(Client client, String endpoint, String apikey) {
		this.client = client;
		this.endpoint = endpoint;
		this.apikey = apikey;
	}

	public Either<String, Effect> subscribe(String emailAddress) {
		ObjectNode request = request("da90a13118", emailAddress);
		try {
			return response(client.target(endpoint).path("lists/subscribe.json").request()
				.accept(MediaType.APPLICATION_JSON)
				.post(Entity.entity(jsonReaderWriter.serialize(request), MediaType.APPLICATION_JSON)).readEntity(String.class));
		} catch (WebApplicationException e) {
			return Either.left(e.getMessage());
		}
	}

	private Either<String,Effect> response(String json) {
		return jsonReaderWriter.jsonNode.apply(json).transform(MailchimpSubscribeClient::response).or(Either.left("Mandrill response did not contain JSON at all."));
	}

	private static Either<String, Effect> response(JsonNode jn) {
		if (!(jn.hasNonNull("email") && jn.hasNonNull("euid") && jn.hasNonNull("leid")))
			return Either.left("MailChimp responded with error: \"" + jn.path("error").asText() + "\".");
		return Either.right(Effect.Performed);
	}

	private ObjectNode request(String listId, String emailAddress) {
		return JsonNodeFactory.instance.objectNode()
			.put("apikey", apikey)
			.put("id", listId)
			.putPOJO("email", JsonNodeFactory.instance.objectNode()
				.put("email", emailAddress));
	}
}
