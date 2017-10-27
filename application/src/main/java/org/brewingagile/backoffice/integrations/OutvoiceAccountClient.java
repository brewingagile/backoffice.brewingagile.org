package org.brewingagile.backoffice.integrations;

import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.saj.InvalidSyntaxException;
import fj.P;
import fj.P2;
import fj.data.Array;
import fj.data.Option;
import okhttp3.*;
import org.brewingagile.backoffice.utils.ArgoUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

public class OutvoiceAccountClient {
	private final OkHttpClient okHttpClient;
	private final String endpoint;
	private final String apikey;

	public OutvoiceAccountClient(OkHttpClient okHttpClient, String endpoint, String apikey) {
		this.okHttpClient = okHttpClient;
		this.endpoint = endpoint;
		this.apikey = apikey;
	}

	public String get(String invoiceAccountKey) throws IOException {
		HttpUrl url = HttpUrl.parse(endpoint).newBuilder()
			.addEncodedPathSegment("account")
			.addEncodedPathSegment(invoiceAccountKey)
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
		return Array.iterableArray(ArgoUtils.parse(json).getArrayNode()).map(OutvoiceAccountClient::unjson);
	}

	private static P2<String, Option<UUID>> unjson(JsonNode x) {
		return P.p(
			x.getStringValue("invoiceNumber"),
			Option.fromNull(x.getNullableStringValue("apiClientReference")).map(y -> UUID.fromString(y))
		);
	}

	public static void main(String[] args) throws IOException, InvalidSyntaxException {
		OutvoiceAccountClient c = new OutvoiceAccountClient(new OkHttpClient(), "http://localhost:9060/api/2/", "simplekey");
		Array<P2<String, Option<UUID>>> parse = c.parse(c.get("brewingagile-Pro%20Agile"));
		System.out.println(parse);
	}

	public static BigDecimal invoiceAmountExVat(String s) throws InvalidSyntaxException {
		return new BigDecimal(ArgoUtils.parse(s).getNumberValue("invoicedExVat"));
	}
}
