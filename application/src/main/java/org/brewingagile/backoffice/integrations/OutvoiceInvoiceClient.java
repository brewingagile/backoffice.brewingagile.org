package org.brewingagile.backoffice.integrations;

import java.math.BigDecimal;
import java.util.UUID;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.node.ArrayNode;
import functional.Either;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.BillingMethod;
import org.brewingagile.backoffice.utils.JsonReaderWriter;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class OutvoiceInvoiceClient {
	private static final JsonReaderWriter jsonReaderWriter = new JsonReaderWriter();
	
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

		ArrayNode lines = lines(invoiceTemplate, participantName);

		ObjectNode request = JsonNodeFactory.instance.objectNode()
			.put("apiClientReference", registrationId.toString())
			.put("deliveryMethod", deliveryMethod.name())
			.put("recipientEmailAddress", recipientEmailAddress)
			.put("recipient", recipient)
			.put("recipientBillingAddress", recipientBillingAddres)
			.putPOJO("lines", lines);

		Response post;
		try {
			post = client.target(endpoint).request()
				.accept(MediaType.APPLICATION_JSON)
				.header("X-API-KEY", apikey)
				.post(Entity.entity(jsonReaderWriter.serialize(request), MediaType.APPLICATION_JSON));
		} catch (ProcessingException | WebApplicationException e) {
			return Either.left(e.getMessage());
		}

		if (!(200 <= post.getStatus() && post.getStatus() < 300))
			return Either.left("While sending invoice: Received HTTP Status " + post.getStatus());

		return Either.right(registrationId);
	}

	private static ArrayNode lines(String invoiceTemplate, String participantName) {
		ObjectNode conference = line("Brewing Agile 2015: Konferens", "Avser konferens 16-17 oktober.\nAvser deltagare: " + participantName, BigDecimal.valueOf(960), BigDecimal.ONE);
		switch (invoiceTemplate) {
			case "conference": return JsonNodeFactory.instance.arrayNode().add(conference);
			case "conference+workshop": return JsonNodeFactory.instance.arrayNode().add(conference).add(
				line("Brewing Agile 2015: Workshop 1", "Avser workshop 16 oktober.\nAvser deltagare: " + participantName, BigDecimal.valueOf(800), BigDecimal.ONE)
			);
			case "conference+workshop2": return JsonNodeFactory.instance.arrayNode().add(conference).add(
				line("Brewing Agile 2015: Workshop 2", "Avser workshop 16 oktober.\nAvser deltagare: " + participantName, BigDecimal.valueOf(800), BigDecimal.ONE)
			);
			default: throw new IllegalArgumentException("Unknown template: " + invoiceTemplate + ".");
		}
	}

	private static ObjectNode line(String text, String description, BigDecimal price, BigDecimal qty) {
		return JsonNodeFactory.instance.objectNode()
			.put("text", text)
			.put("description", description)
			.put("price", price)
			.put("quantity", qty)
			.put("vatRate", "VAT_25");
	}
}
