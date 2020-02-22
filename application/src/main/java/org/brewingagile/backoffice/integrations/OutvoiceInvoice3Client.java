package org.brewingagile.backoffice.integrations;

import argo.jdom.JsonNode;
import argo.saj.InvalidSyntaxException;
import fj.F;
import fj.Unit;
import fj.data.Either;
import fj.data.List;
import fj.data.Option;
import fj.data.Set;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import okhttp3.*;
import org.brewingagile.backoffice.db.operations.TicketsSql;
import org.brewingagile.backoffice.pure.AccountLogic;
import org.brewingagile.backoffice.types.ParticipantName;
import org.brewingagile.backoffice.types.RegistrationId;
import org.brewingagile.backoffice.utils.ArgoUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.UUID;

import static argo.jdom.JsonNodeFactories.*;

public class OutvoiceInvoice3Client {
	private final OkHttpClient okHttpClient;
	private final String endpoint;
	private final String apikey;

	public OutvoiceInvoice3Client(OkHttpClient okHttpClient, String endpoint, String apikey) {
		this.okHttpClient = okHttpClient;
		this.endpoint = endpoint;
		this.apikey = apikey;
	}

	public PostInvoiceResponse postInvoice(JsonNode jsonRequest) throws IOException, InvalidSyntaxException {
		HttpUrl url = HttpUrl.parse(endpoint).newBuilder()
			.addPathSegment("3")
			.addPathSegment("invoices")
			.build();
		Request httpRequest = new Request.Builder()
			.url(url)
			.addHeader("Accept", "application/json")
			.addHeader("X-API-KEY", apikey)
			.post(RequestBody.create(MediaType.parse("application/json"), ArgoUtils.format(jsonRequest)))
			.build();

		try (Response r = okHttpClient.newCall(httpRequest).execute()) {
			if (!r.isSuccessful())
				throw new IOException("While sending invoice: Received HTTP Status " + r.code());

			return responseUnjson(ArgoUtils.parse(r.body().string()));
		}
	}

	public static PostInvoiceResponse responseUnjson(JsonNode jsonNode) {
		return new PostInvoiceResponse(
			jsonNode.getStringValue("invoiceNumber"),
			Base64.getDecoder().decode(jsonNode.getStringValue("pdfBytes"))
		);
	}

	@EqualsAndHashCode
	@ToString
	public static final class PostInvoiceResponse {
		public final String invoiceNumber;
		public final byte[] pdf;

		public PostInvoiceResponse(String invoiceNumber, byte[] pdf) {
			this.invoiceNumber = invoiceNumber;
			this.pdf = pdf;
		}
	}

	public static Option<JsonNode> mkAccountRequest(
		String accountKey,
		String recipient,
		String recipientBillingAddres,
		AccountLogic.AccountStatement2 accountStatement,
		BigDecimal alreadyInvoicedAmountExVat
	) {
		BigDecimal total = AccountLogic.total(accountStatement.lines);
		if (total.compareTo(alreadyInvoicedAmountExVat) == 0) return Option.none();

		List<JsonNode> map = accountStatement.lines.map(x -> line(x.description, "Avser: Brewing Agile 2020", x.price, new BigDecimal(x.qty)))
			.append(
				alreadyInvoicedAmountExVat.equals(BigDecimal.ZERO)
				? List.list()
				: List.list(line("Avgår, redan fakturerat", "", alreadyInvoicedAmountExVat.negate(), BigDecimal.ONE))
			);

		return Option.some(object(
			field("apiClientReference", string(UUID.randomUUID().toString())),
			field("accountKey", string(accountKey)),
			field("recipient", string(recipient)),
			field("recipientBillingAddress", string(recipientBillingAddres)),
			field("lines", array(map))
		));
	}

	public static JsonNode mkParticipantRequest(
		RegistrationId registrationId,
		String recipient,
		String recipientBillingAddres,
		Set<TicketsSql.Ticket> tickets,
		ParticipantName participantName
	) {
		return object(
			field("apiClientReference", string(registrationId.value.toString())),
			field("accountKey", string("brewingagile-" + registrationId.value.toString())),
			field("recipient", string(recipient)),
			field("recipientBillingAddress", string(recipientBillingAddres)),
			field("lines", tickets.toList().map(OutvoiceInvoice3Client.line("Brewing Agile 2020: ", participantName)).toJavaList().stream().collect(ArgoUtils.toArray()))
		);
	}

	private static F<TicketsSql.Ticket, JsonNode> line(String eventPrefix, ParticipantName participantName) {
		return ticket -> line(eventPrefix + ticket.ticket.ticketName, ticket.productText + "\nAvser deltagare: " + participantName.value, ticket.price.multiply(BigDecimal.valueOf(0.8)), BigDecimal.ONE);
	}

	private static JsonNode line(String text, String description, BigDecimal price, BigDecimal qty) {
		return object(
			field("text", string(text)),
			field("description", string(description)),
			field("price", number(price)),
			field("quantity", number(qty)),
			field("vatRate", string("VAT_25"))
		);
	}
}
