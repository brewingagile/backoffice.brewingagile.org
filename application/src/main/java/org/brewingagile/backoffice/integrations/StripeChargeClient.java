package org.brewingagile.backoffice.integrations;

import argo.jdom.JsonRootNode;
import argo.saj.InvalidSyntaxException;
import fj.data.Either;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import okhttp3.*;
import org.brewingagile.backoffice.types.ChargeId;
import org.brewingagile.backoffice.types.StripePrivateKey;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.Http;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public class StripeChargeClient {
	private final OkHttpClient okHttpClient;
	private final HttpUrl endpoint = HttpUrl.parse("https://api.stripe.com/v1/charges");
	private final StripePrivateKey key;

	public StripeChargeClient(OkHttpClient okHttpClient, StripePrivateKey key) {
		this.okHttpClient = okHttpClient;
		this.key = key;
	}

	public Either<String, Charge> postCharge(
		String token,
		BigInteger amountInOre
	) throws IOException, InvalidSyntaxException {
		RequestBody requestBody = new FormBody.Builder()
			.add("amount", amountInOre.toString())
			.add("currency", "sek")
			.add("statement_descriptor", "brewingagile.org")
			.add("source", token)
			.build();

		Request httpRequest = new Request.Builder()
			.url(endpoint)
			.addHeader("Authorization", Http.basic(key.value, ""))
			.post(requestBody)
			.build();

		try (Response r = okHttpClient.newCall(httpRequest).execute()) {
			if (!r.isSuccessful()) {
				System.out.println(r.code());
				System.out.println(r.body().string());
				return Either.left("While sending invoice: Received HTTP Status " + r.code());
			}

			return Either.right(Charge.unjson(ArgoUtils.parse(r.body().string())));
		}
	}

	@EqualsAndHashCode
	@ToString
	public static final class Charge {
		public final ChargeId id;

		public Charge(ChargeId id) {
			this.id = id;
		}

		public static Charge unjson(JsonRootNode x) {
			return new Charge(
				ChargeId.chargeId(x.getStringValue("id"))
			);
		}
	}
}
