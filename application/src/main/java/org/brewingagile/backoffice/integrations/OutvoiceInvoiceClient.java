package org.brewingagile.backoffice.integrations;

import argo.jdom.JsonNode;
import fj.F;
import fj.Unit;
import fj.data.Either;
import fj.data.List;
import fj.data.Option;
import fj.data.Set;
import fj.function.Strings;
import okhttp3.*;
import org.brewingagile.backoffice.db.operations.TicketsSql;
import org.brewingagile.backoffice.pure.AccountLogic;
import org.brewingagile.backoffice.rest.json.ToJson;
import org.brewingagile.backoffice.types.BillingMethod;
import org.brewingagile.backoffice.types.ParticipantEmail;
import org.brewingagile.backoffice.types.ParticipantName;
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

	public Either<String, Unit> postInvoice(JsonNode jsonRequest) throws IOException {
		HttpUrl url = HttpUrl.parse(endpoint).newBuilder()
			.addPathSegment("invoices")
			.build();
		Request httpRequest = new Request.Builder()
			.url(url)
			.addHeader("Accept", "application/json")
			.addHeader("X-API-KEY", apikey)
			.post(RequestBody.create(okhttp3.MediaType.parse("application/json"), ArgoUtils.format(jsonRequest)))
			.build();

		try (Response r = okHttpClient.newCall(httpRequest).execute()) {
			if (!r.isSuccessful())
				return Either.left("While sending invoice: Received HTTP Status " + r.code());

			return Either.right(Unit.unit());
		}
	}

	public static Option<JsonNode> mkAccountRequest(
		String accountKey,
		String billingEmail,
		String recipient,
		String recipientBillingAddres,
		AccountLogic.AccountStatement2 accountStatement,
		BigDecimal alreadyInvoicedAmountExVat
	) {
		BigDecimal total = AccountLogic.total(accountStatement.lines);
		if (total.compareTo(alreadyInvoicedAmountExVat) == 0) return Option.none();

		List<JsonNode> map = accountStatement.lines.map(x -> line(x.description, "Avser: Brewing Agile 2019", x.price, new BigDecimal(x.qty)))
			.append(
				alreadyInvoicedAmountExVat.equals(BigDecimal.ZERO)
				? List.list()
				: List.list(line("Avgår, redan fakturerat", "", alreadyInvoicedAmountExVat.negate(), BigDecimal.ONE))
			);

		BillingMethod deliveryMethod =
			Strings.isNullOrBlank.f(billingEmail)
			? BillingMethod.SNAILMAIL
			: BillingMethod.EMAIL;

		return Option.some(object(
			field("apiClientReference", string(UUID.randomUUID().toString())),
			field("accountKey", string(accountKey)),
			field("deliveryMethod", string(deliveryMethod.name())),
			field("recipientEmailAddress", string(billingEmail)),
			field("recipient", string(recipient)),
			field("recipientBillingAddress", string(recipientBillingAddres)),
			field("lines", array(map))
		));
	}

	public static JsonNode mkParticipantRequest(
		UUID registrationId,
		BillingMethod deliveryMethod,
		ParticipantEmail recipientEmailAddress,
		String recipient,
		String recipientBillingAddres,
		Set<TicketsSql.Ticket> tickets,
		ParticipantName participantName
	) {
		return object(
			field("apiClientReference", string(registrationId.toString())),
			field("accountKey", string("brewingagile-" + registrationId.toString())),
			field("deliveryMethod", string(deliveryMethod.name())),
			field("recipientEmailAddress", ToJson.participantEmail(recipientEmailAddress)),
			field("recipient", string(recipient)),
			field("recipientBillingAddress", string(recipientBillingAddres)),
			field("lines", tickets.toList().map(OutvoiceInvoiceClient.line("Brewing Agile 2019: ", participantName)).toJavaList().stream().collect(ArgoUtils.toArray()))
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
