package org.brewingagile.backoffice.integrations;

import argo.jdom.JsonRootNode;
import fj.F;
import fj.P2;
import fj.Unit;
import fj.data.Either;
import fj.data.List;
import fj.data.Option;
import fj.data.Set;
import fj.function.Strings;
import okhttp3.*;
import org.brewingagile.backoffice.pure.AccountLogic;
import org.brewingagile.backoffice.types.BillingMethod;
import org.brewingagile.backoffice.db.operations.TicketsSql;
import org.brewingagile.backoffice.types.ParticipantName;
import org.brewingagile.backoffice.types.TicketName;
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

	public Either<String, Unit> postInvoice(JsonRootNode jsonRequest) throws IOException {
		HttpUrl url = HttpUrl.parse(endpoint).newBuilder()
			.addEncodedPathSegment("invoices")
			.build();
		Request httpRequest = new Request.Builder()
			.url(url)
			.addHeader("Accept", "application/json")
			.addHeader("X-API-KEY", apikey)
			.post(RequestBody.create(okhttp3.MediaType.parse("application/json"), ArgoUtils.format(jsonRequest)))
			.build();

		try (Response r = okHttpClient.newCall(httpRequest).execute()) {
			if (!(200 <= r.code() && r.code() < 300))
				return Either.left("While sending invoice: Received HTTP Status " + r.code());

			return Either.right(Unit.unit());
		}
	}

	public static Option<JsonRootNode> mkAccountRequest(
		String accountKey,
		String billingEmail,
		String recipient,
		String recipientBillingAddres,
		AccountLogic.AccountStatement2 accountStatement,
		BigDecimal alreadyInvoicedAmountExVat
	) {
		BigDecimal total = AccountLogic.total(accountStatement.lines);
		if (total.compareTo(alreadyInvoicedAmountExVat) == 0) return Option.none();

		List<JsonRootNode> map = accountStatement.lines.map(x -> line(x.description, "Avser: Brewing Agile 2017", x.price, new BigDecimal(x.qty)))
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

	public static JsonRootNode mkParticipantRequest(
		UUID registrationId,
		BillingMethod deliveryMethod,
		String recipientEmailAddress,
		String recipient,
		String recipientBillingAddres,
		Set<TicketsSql.Ticket> tickets,
		String participantName
	) {
		return object(
			field("apiClientReference", string(registrationId.toString())),
			field("accountKey", string("brewingagile-" + registrationId.toString())),
			field("deliveryMethod", string(deliveryMethod.name())),
			field("recipientEmailAddress", string(recipientEmailAddress)),
			field("recipient", string(recipient)),
			field("recipientBillingAddress", string(recipientBillingAddres)),
			field("lines", tickets.toList().map(OutvoiceInvoiceClient.line("Brewing Agile 2017: ", participantName)).toJavaList().stream().collect(ArgoUtils.toArray()))
		);
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
