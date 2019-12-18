package org.brewingagile.backoffice.integrations;

import argo.jdom.JsonNode;
import argo.saj.InvalidSyntaxException;
import fj.data.Either;
import fj.data.List;
import okhttp3.*;
import org.brewingagile.backoffice.db.operations.TicketsSql;
import org.brewingagile.backoffice.rest.json.ToJson;
import org.brewingagile.backoffice.types.ChargeId;
import org.brewingagile.backoffice.types.ParticipantEmail;
import org.brewingagile.backoffice.types.ParticipantName;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.postgresql.util.Base64;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;

import static argo.jdom.JsonNodeFactories.*;

public class OutvoiceReceiptClient {
	private final OkHttpClient okHttpClient;
	private final String endpoint;
	private final String apikey;

	public OutvoiceReceiptClient(OkHttpClient okHttpClient, String endpoint, String apikey) {
		this.okHttpClient = okHttpClient;
		this.endpoint = endpoint;
		this.apikey = apikey;
	}

	public Either<String, ReceiptResponse> post(JsonNode jsonRequest) throws IOException, InvalidSyntaxException {
		HttpUrl url = HttpUrl.parse(endpoint).newBuilder()
			.addPathSegment("receipts")
			.build();
		Request httpRequest = new Request.Builder()
			.url(url)
			.addHeader("Accept", "application/json")
			.addHeader("X-API-KEY", apikey)
			.post(RequestBody.create(MediaType.parse("application/json"), ArgoUtils.format(jsonRequest)))
			.build();

		try (Response r = okHttpClient.newCall(httpRequest).execute()) {
			if (!r.isSuccessful())
				return Either.left("While sending invoice: Received HTTP Status " + r.code());

			return Either.right(ReceiptResponse.unjson(r.body().string()));
		}
	}

	public static JsonNode mkParticipantRequest(
		ChargeId chargeId,
		Instant stripeTxTimestamp,
		ParticipantName buyerName,
		ParticipantEmail buyerEmail,
		List<TicketsSql.Ticket> tickets
	) {
		return object(
			field("chargeId", ToJson.chargeId(chargeId)),
			field("timestamp", ToJson.instant(stripeTxTimestamp)),
			field("buyerName", ToJson.participantName(buyerName)),
			field("buyerEmail", ToJson.participantEmail(buyerEmail)),
			field("lines", tickets.map(x -> object(
				field("text", string("Brewing Agile 2020: " + x.ticket.ticketName)),
				field("description", string(x.productText + "\nAvser deltagare: " + buyerName.value)),
				field("price", number(x.price.multiply(BigDecimal.valueOf(0.8)))),
				field("quantity", number(BigDecimal.ONE)),
				field("vatRate", string("VAT_25"))
			)).toJavaList().stream().collect(ArgoUtils.toArray()))
		);
	}

	public static final class ReceiptResponse {
		public final HttpUrl pdfUrl;
		public final byte[] pdfSource;

		public ReceiptResponse(HttpUrl pdfUrl, byte[] pdfSource) {
			this.pdfUrl = pdfUrl;
			this.pdfSource = pdfSource;
		}

		public static ReceiptResponse unjson(String json) throws InvalidSyntaxException {
			JsonNode parse = ArgoUtils.parse(json);
			return new ReceiptResponse(
				HttpUrl.parse(parse.getStringValue("pdfUrl")),
				Base64.decode(parse.getStringValue("pdfSource"))
			);
		}
	}
}