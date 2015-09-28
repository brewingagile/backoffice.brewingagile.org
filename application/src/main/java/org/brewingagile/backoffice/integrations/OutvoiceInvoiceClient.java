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
import fj.data.Either;
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
		String invoiceTemplate,
		String participantName) {

		JsonRootNode request = object(
			field("apiClientReference", string(registrationId.toString())),
			field("deliveryMethod", string(deliveryMethod.name())),
			field("recipientEmailAddress", string(recipientEmailAddress)),
			field("recipient", string(recipient)),
			field("recipientBillingAddress", string(recipientBillingAddres)),
			field("lines", lines(invoiceTemplate, participantName))
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

	private static JsonRootNode lines(String invoiceTemplate, String participantName) {
		JsonRootNode conference = line("Brewing Agile 2015: Konferens", "Avser konferens 16-17 oktober.\nAvser deltagare: " + participantName, BigDecimal.valueOf(960), BigDecimal.ONE);
		switch (invoiceTemplate) {
			case "conference": return array(conference);
			case "conference+workshop": return array(
				conference,
				line("Brewing Agile 2015: Workshop 1", "Avser workshop 16 oktober.\nAvser deltagare: " + participantName, BigDecimal.valueOf(800), BigDecimal.ONE)
			);
			case "conference+workshop2": return array(
				conference,
				line("Brewing Agile 2015: Workshop 2", "Avser workshop 16 oktober.\nAvser deltagare: " + participantName, BigDecimal.valueOf(800), BigDecimal.ONE)
			);
			default: throw new IllegalArgumentException("Unknown template: " + invoiceTemplate + ".");
		}
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
