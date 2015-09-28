package org.brewingagile.backoffice.integrations;

import argo.jdom.JsonNode;
import argo.jdom.JsonNodeFactories;
import argo.jdom.JsonRootNode;
import fj.data.Either;
import functional.Effect;
import org.brewingagile.backoffice.utils.ArgoUtils;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import static argo.jdom.JsonNodeFactories.*;

public class MailchimpSubscribeClient {
	private final Client client;
	private final String endpoint;
	private final String apikey;

	public MailchimpSubscribeClient(Client client, String endpoint, String apikey) {
		this.client = client;
		this.endpoint = endpoint;
		this.apikey = apikey;
	}

	public Either<String, Effect> subscribe(String emailAddress) {
		JsonRootNode request = request("da90a13118", emailAddress);
		try {
			return response(client.target(endpoint).path("lists/subscribe.json").request()
				.accept(MediaType.APPLICATION_JSON)
				.post(Entity.entity(ArgoUtils.format(request), MediaType.APPLICATION_JSON)).readEntity(String.class));
		} catch (WebApplicationException e) {
			return Either.left(e.getMessage());
		}
	}

	private Either<String, Effect> response(String json) {
		return Either.joinRight(ArgoUtils.parseEither(json)
			.bimap(
				l -> "Mandrill response did not contain JSON at all.",
				MailchimpSubscribeClient::response
			));
	}

	private static Either<String, Effect> response(JsonNode jn) {
		if (!(jn.isStringValue("email") && jn.isStringValue("euid") && jn.isStringValue("leid"))) {
			JsonNode error = jn.getNode("error");
			return Either.left("MailChimp responded with error: \"" + ArgoUtils.format(error.getRootNode()) + "\".");
		}
		return Either.right(Effect.Performed);
	}

	private JsonRootNode request(String listId, String emailAddress) {
		return JsonNodeFactories.object(
			field("apikey", string(apikey)),
			field("id", string(listId)),
			field("email", object(
				field("email", string(emailAddress))
			))
		);
	}
}
