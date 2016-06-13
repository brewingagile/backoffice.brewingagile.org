package org.brewingagile.backoffice.integrations;

import java.math.BigDecimal;
import java.util.UUID;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static argo.jdom.JsonNodeFactories.*;

import argo.jdom.JsonRootNode;
import fj.F;
import fj.data.Either;
import fj.data.Set;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.BillingMethod;
import org.brewingagile.backoffice.utils.ArgoUtils;

public class OutvoiceInvoiceClient {
	private final Client client;
	private final String endpoint;
	private final String apikey;

	public OutvoiceInvoiceClient(Client client, String endpoint, String apikey) {
		this.client = client;
		this.endpoint = endpoint;
		this.apikey = apikey;
	}
	
	public Either<String,UUID> postInvoice(
		UUID registrationId,
		BillingMethod deliveryMethod,
		String recipientEmailAddress,
		String recipient,
		String recipientBillingAddres,
		Set<String> tickets,
		String participantName) {

		JsonRootNode request = object(
			field("apiClientReference", string(registrationId.toString())),
			field("deliveryMethod", string(deliveryMethod.name())),
			field("recipientEmailAddress", string(recipientEmailAddress)),
			field("recipient", string(recipient)),
			field("recipientBillingAddress", string(recipientBillingAddres)),
			field("lines", tickets.toList().map(OutvoiceInvoiceClient.line(participantName)).toJavaList().stream().collect(ArgoUtils.toArray()))
		);

		Response post;
		try {
			post = client.target(endpoint).request()
				.accept(MediaType.APPLICATION_JSON)
				.header("X-API-KEY", apikey)
				.post(Entity.entity(ArgoUtils.format(request), MediaType.APPLICATION_JSON));
		} catch (ProcessingException | WebApplicationException e) {
			return Either.left(e.getMessage());
		}

		if (!(200 <= post.getStatus() && post.getStatus() < 300))
			return Either.left("While sending invoice: Received HTTP Status " + post.getStatus());

		return Either.right(registrationId);
	}

	private static F<String, JsonRootNode> line(String participantName) {
		return ticket -> {
			switch (ticket) {
				case "conference": return line("Brewing Agile 2016: Konferens", "Avser konferens 3-4 november.\nAvser deltagare: " + participantName, BigDecimal.valueOf(960), BigDecimal.ONE);
				case "workshop1": return line("Brewing Agile 2016: Workshop 1", "Avser workshop 2 november.\nAvser deltagare: " + participantName, BigDecimal.valueOf(2800), BigDecimal.ONE);
				case "workshop2": return line("Brewing Agile 2016: Workshop 2", "Avser workshop 3 november.\nAvser deltagare: " + participantName, BigDecimal.valueOf(1400), BigDecimal.ONE);
				default: throw new IllegalArgumentException("Unknown ticket: " + ticket+ ".");
			}
		};
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
