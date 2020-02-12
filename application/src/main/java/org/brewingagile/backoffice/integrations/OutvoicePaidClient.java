package org.brewingagile.backoffice.integrations;

import argo.jdom.JsonNode;
import argo.saj.InvalidSyntaxException;
import fj.P;
import fj.P2;
import fj.data.Array;
import fj.data.Option;
import okhttp3.*;
import org.brewingagile.backoffice.utils.ArgoUtils;

import java.io.IOException;
import java.util.UUID;

public class OutvoicePaidClient {
	private final OkHttpClient okHttpClient;
	private final String endpoint;
	private final String apikey;

	public OutvoicePaidClient(OkHttpClient okHttpClient, String endpoint, String apikey) {
		this.okHttpClient = okHttpClient;
		this.endpoint = endpoint;
		this.apikey = apikey;
	}

	public String get() throws IOException {
		HttpUrl url = HttpUrl.parse(endpoint).newBuilder()
			.addEncodedPathSegment("2")
			.addEncodedPathSegment("invoices")
			.addEncodedPathSegment("paid")
			.build();
		Request request = new Request.Builder()
			.url(url)
			.header("X-API-KEY", apikey)
			.cacheControl(CacheControl.FORCE_NETWORK)
			.build();
		try (Response execute = okHttpClient.newCall(request).execute()) {
			if (!execute.isSuccessful() || execute.isRedirect())
				throw new IOException("Call to " + url + " failed unexpectedly: " + execute);
			return execute.body().string();
		}
	}

	public static Array<P2<String, Option<UUID>>> parse(String json) throws InvalidSyntaxException {
		return Array.iterableArray(ArgoUtils.parse(json).getArrayNode()).map(OutvoicePaidClient::unjson);
	}

	private static P2<String, Option<UUID>> unjson(JsonNode x) {
		return P.p(
			x.getStringValue("invoiceNumber"),
			Option.fromNull(x.getNullableStringValue("apiClientReference")).map(y -> UUID.fromString(y))
		);
	}

	public static void main(String[] args) throws IOException, InvalidSyntaxException {
		OutvoicePaidClient outvoicePaidClient = new OutvoicePaidClient(new OkHttpClient(), "http://localhost:9060/api/2/invoices/", "simplekey");
		Array<P2<String, Option<UUID>>> parse = outvoicePaidClient.parse(outvoicePaidClient.get());
		System.out.println(parse);
	}
}
