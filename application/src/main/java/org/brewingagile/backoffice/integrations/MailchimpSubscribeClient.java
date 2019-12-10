package org.brewingagile.backoffice.integrations;

import argo.jdom.JsonNode;
import fj.data.Either;
import functional.Effect;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.brewingagile.backoffice.types.ParticipantEmail;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.Http;
import org.brewingagile.backoffice.utils.Strings;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

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

	@EqualsAndHashCode
	@ToString
	public static final class ListUniqueId {
		public final String value;

		private ListUniqueId(String value) {
			this.value = Objects.requireNonNull(Strings.emptyToNull(value));
		}

		public static ListUniqueId listUniqueId(String x) {
			return new ListUniqueId(x);
		}
	}

	public Either<String, Effect> subscribe(ParticipantEmail participantEmail, ListUniqueId listUniqueId) {
		try {
			String email = participantEmail.value;
			Response post = client.target(endpoint).path("3.0/lists/" + listUniqueId.value + "/members/" + md5Crap(email.toLowerCase())).request()
				.header("Authorization", Http.basic("anystring", apikey))
				.accept(MediaType.APPLICATION_JSON)
				.put(Entity.entity(ArgoUtils.format(request(email)), MediaType.APPLICATION_JSON));
			return response(post.readEntity(String.class));
		} catch (WebApplicationException e) {
			return Either.left(e.getMessage());
		}
	}

	private static String md5Crap(String s) {
		try {
			return bytesToHex(MessageDigest.getInstance("MD5").digest(s.getBytes("UTF-8")));
		} catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

	private static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}

	private Either<String, Effect> response(String json) {
		return Either.joinRight(ArgoUtils.parseEither(json)
			.bimap(
				l -> "MailChimp response did not contain JSON at all.",
				(jn) -> {
					if (!(jn.isStringValue("email_address")))
						return Either.left("MailChimp responded with error: \"" + json + "\".");
					return Either.right(Effect.Performed);
				}
			));
	}

	private JsonNode request(String emailAddress) {
		return object(
			field("email_address", string(emailAddress)),
			field("status_if_new", string("subscribed")),
			field("status", string("subscribed"))
		);
	}
}
