package org.brewingagile.backoffice.integrations;

import argo.jdom.JsonNode;
import argo.saj.InvalidSyntaxException;
import fj.data.Either;
import okhttp3.*;
import org.brewingagile.backoffice.types.*;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.Http;

import java.io.IOException;
import java.math.BigInteger;

public class StripeChargeClient {
	private final OkHttpClient okHttpClient;
	private final HttpUrl endpoint = HttpUrl.parse("https://api.stripe.com/v1/charges");
	private final StripePrivateKey key;

	public StripeChargeClient(OkHttpClient okHttpClient, StripePrivateKey key) {
		this.okHttpClient = okHttpClient;
		this.key = key;
	}

	public Either<String, ChargeResponse> postCharge(
		String token,
		BigInteger amountInOre,
		ParticipantName participantName,
		ParticipantEmail participantEmail,
		RegistrationId registrationId
	) throws IOException, InvalidSyntaxException {
		RequestBody requestBody = new FormBody.Builder()
			.add("amount", amountInOre.toString())
			.add("currency", "sek")
			.add("statement_descriptor", "brewingagile.org")
			.add("source", token)
			.add("metadata[name]", participantName.value)
			.add("metadata[email]", participantEmail.value)
			.add("metadata[registration_id]", registrationId.value.toString())
			.build();

		Request httpRequest = new Request.Builder()
			.url(endpoint)
			.addHeader("Authorization", Http.basic(key.value, ""))
			.post(requestBody)
			.build();

		try (Response r = okHttpClient.newCall(httpRequest).execute()) {
			if (!r.isSuccessful()) {
				System.out.println(r.code());
				String bodyString = r.body().string();
				System.out.println(bodyString);
				String s = unjsonErrorMessage(ArgoUtils.parse(bodyString));
				return Either.left(s);
			}

			return Either.right(ChargeResponse.unjson(ArgoUtils.parse(r.body().string())));
		}
	}

	public static final class ChargeResponse {
		public final ChargeId id;

		public ChargeResponse(ChargeId id) {
			this.id = id;
		}

		public static ChargeResponse unjson(JsonNode x) {
			return new ChargeResponse(
				ChargeId.chargeId(x.getStringValue("id"))
			);
		}
	}

	public static String unjsonErrorMessage(JsonNode x) {
		return x.getStringValue("error", "message");
	}

	/*
	{
  "error": {
    "message": "Your card was declined.",
    "type": "card_error",
    "code": "card_declined",
    "decline_code": "generic_decline",
    "charge": "ch_1BEdrABG5kmo2d4yamtdIfV4"
  }
}
	 */
}
