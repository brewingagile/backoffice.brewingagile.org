package org.brewingagile.backoffice.integrations;

import argo.jdom.JsonRootNode;
import fj.F;
import fj.data.Either;
import fj.data.Set;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.BillingMethod;
import org.brewingagile.backoffice.db.operations.TicketsSql;
import org.brewingagile.backoffice.utils.ArgoUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import static argo.jdom.JsonNodeFactories.*;

public class OutvoiceInvoiceClient {
	private final OkHttpClient okHttpClient;
	private final String endpoint;
	private final String apikey;

	public OutvoiceInvoiceClient(OkHttpClient okHttpClient, String endpoint, String apikey) {
		this.okHttpClient = okHttpClient;
		this.endpoint = endpoint;
		this.apikey = apikey;
	}
	
	public Either<String,UUID> postInvoice(
		UUID registrationId,
		BillingMethod deliveryMethod,
		String recipientEmailAddress,
		String recipient,
		String recipientBillingAddres,
		Set<TicketsSql.Ticket> tickets,
		String participantName) throws IOException {

		JsonRootNode jsonRequest = object(
			field("apiClientReference", string(registrationId.toString())),
			field("deliveryMethod", string(deliveryMethod.name())),
			field("recipientEmailAddress", string(recipientEmailAddress)),
			field("recipient", string(recipient)),
			field("recipientBillingAddress", string(recipientBillingAddres)),
			field("lines", tickets.toList().map(OutvoiceInvoiceClient.line("Brewing Agile 2017: ", participantName)).toJavaList().stream().collect(ArgoUtils.toArray()))
		);

		Request httpRequest = new Request.Builder()
			.url(endpoint)
			.addHeader("Accept", "application/json")
			.addHeader("X-API-KEY", apikey)
			.post(RequestBody.create(okhttp3.MediaType.parse("application/json"), ArgoUtils.format(jsonRequest)))
			.build();

		try (Response r = okHttpClient.newCall(httpRequest).execute()) {
			if (!(200 <= r.code() && r.code() < 300))
				return Either.left("While sending invoice: Received HTTP Status " + r.code());

			return Either.right(registrationId);
		}
	}

	private static F<TicketsSql.Ticket, JsonRootNode> line(String eventPrefix, String participantName) {
		return ticket -> line(eventPrefix + ticket.ticket.ticketName, ticket.productText + "\nAvser deltagare: " + participantName, ticket.price.multiply(BigDecimal.valueOf(0.8)), BigDecimal.ONE);
	}

	private static JsonRootNode line(String text, String description, BigDecimal price, BigDecimal qty) {
		return object(
			field("text", string(text)),
			field("description", string(description)),
			field("price", number(price)),
			field("quantity", number(qty)),
			field("vatRate", string("VAT_25"))
		);
	}
}
