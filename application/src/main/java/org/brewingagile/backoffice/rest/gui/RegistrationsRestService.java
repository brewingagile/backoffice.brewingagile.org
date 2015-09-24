package org.brewingagile.backoffice.rest.gui;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import functional.Either;
import org.brewingagile.backoffice.application.Application;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.RegistrationState;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.Badge;
import org.brewingagile.backoffice.services.DismissRegistrationService;
import org.brewingagile.backoffice.services.MarkAsCompleteService;
import org.brewingagile.backoffice.services.MarkAsPaidService;
import org.brewingagile.backoffice.services.SendInvoiceService;
import org.brewingagile.backoffice.utils.JsonReaderWriter;
import org.brewingagile.backoffice.utils.Responses;
import org.brewingagile.backoffice.utils.Result;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

@Path("/registrations/")
@NeverCache
public class RegistrationsRestService {
	private final JsonReaderWriter jsonReaderWriter = new JsonReaderWriter();
	private final DataSource dataSource = Application.INSTANCE.dataSource();
	private final AuthService authService = Application.INSTANCE.authService();
	private final RegistrationsSqlMapper registrationsSqlMapper = Application.INSTANCE.registrationsSqlMapper();
	private final SendInvoiceService sendInvoiceService = Application.INSTANCE.sendInvoiceService();
	private final DismissRegistrationService dismissRegistrationService = Application.INSTANCE.dismissRegistrationService();
	private final MarkAsCompleteService markAsCompleteService = Application.INSTANCE.markAsCompleteService();
	private final MarkAsPaidService markAsPaidService = Application.INSTANCE.markAsPaidService();
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response invoices(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		
		try (Connection c = dataSource.getConnection()) {
			List<RegistrationsSqlMapper.Registration> all = registrationsSqlMapper.all(c);
			ObjectNode overview = JsonNodeFactory.instance.objectNode();
			overview.put("received", all.stream().filter(r -> r.state == RegistrationState.RECEIVED).map(RegistrationsRestService::json).collect(JsonReaderWriter.toArrayNode()));
			overview.put("invoicing", all.stream().filter(r -> r.state == RegistrationState.INVOICING).map(RegistrationsRestService::json).collect(JsonReaderWriter.toArrayNode()));
			overview.put("paid", all.stream().filter(r -> r.state == RegistrationState.PAID).map(RegistrationsRestService::json).collect(JsonReaderWriter.toArrayNode()));
			return Response.ok(jsonReaderWriter.serialize(overview)).build();
		}
	}

	@GET
	@Path("/{registrationId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRegistration(@Context HttpServletRequest request, @PathParam("registrationId") UUID id) {
		authService.guardAuthenticatedUser(request);
		try {
			try (Connection c = dataSource.getConnection()) {
				Optional<JsonNode> jn = registrationsSqlMapper.one(c, id).transform(RegistrationsRestService::json);
				return Responses.from(jsonReaderWriter, jn).build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
	}

	public static ObjectNode json(RegistrationsSqlMapper.Registration r) {
		return JsonNodeFactory.instance.objectNode()
			.put("id", r.id.toString())
			.put("participantName", r.participantName)
			.put("participantEmail", r.participantEmail)
			.put("billingCompany", r.billingCompany)
			.put("billingAddress", r.billingAddress)
			.put("billingMethod", r.billingMethod.name())
			.put("twitter", r.twitter)
			.put("ticket", r.ticket)
			.put("dietaryRequirements", r.dietaryRequirements)
			.put("badge", r.badge.badge)
			.put("bundle", r.bundle.or(""));
	}
	
	public static final class RegistrationsUpdate {
		public final Badge badge;
		public final String dietaryRequirements;
		public final Optional<String> bundle;

		public RegistrationsUpdate(
			Badge badge,
			String dietaryRequirements,
			Optional<String> bundle
		) {
			this.badge = badge;
			this.dietaryRequirements = dietaryRequirements;
			this.bundle = bundle;
		}
	}

	private static RegistrationsUpdate registrationsUpdate(JsonNode jsonNode) {
		return new RegistrationsUpdate(
			new Badge(jsonNode.get("badge").asText()),
			jsonNode.get("dietaryRequirements").asText(),
			Optional.fromNullable(Strings.emptyToNull(jsonNode.get("bundle").asText()))
		);
	}

	@POST
	@Path("/{registrationId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postUpdate(@Context HttpServletRequest request, @PathParam("registrationId") UUID id, String body) throws Exception {
		authService.guardAuthenticatedUser(request);
		Either<String, RegistrationsUpdate> transform = jsonReaderWriter.jsonNodeEither(body)
			.transform(RegistrationsRestService::registrationsUpdate);
		if (transform.isLeft()) return Response.serverError().build();

		RegistrationsUpdate ru = transform.right();
		
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			if (!registrationsSqlMapper.one(c, id).isPresent()) return Response.status(Status.NOT_FOUND).build();
			registrationsSqlMapper.update(c, id, ru.badge, ru.dietaryRequirements, ru.bundle);
			c.commit();
		}
		return Response.ok().build();
	}

	public static final class RegistrationsListRequest {
		public List<UUID> registrations;
	}
	
	@POST
	@Path("/send-invoices")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postSendInvoices(@Context HttpServletRequest request, String body) throws Exception {
		authService.guardAuthenticatedUser(request);
		RegistrationsListRequest sir = jsonReaderWriter.deserialize(body, RegistrationsListRequest.class);

		int invoicesSent = 0;
		for (UUID uuid : sir.registrations) {
			sendInvoiceService.sendInvoice(uuid);
			invoicesSent++;
		}

		return Responses.from(jsonReaderWriter, Result.success(String.format("Skickade %s fakturor.", invoicesSent))).build();
	}
		
	@POST
	@Path("/dismiss-registrations")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postDismissRegistrations(@Context HttpServletRequest httpRequest, String body) throws Exception {
		authService.guardAuthenticatedUser(httpRequest);
		RegistrationsListRequest request = jsonReaderWriter.deserialize(body, RegistrationsListRequest.class);

		int invoicesDismissed = 0;
		for (UUID uuid : request.registrations) {
			dismissRegistrationService.dismissRegistration(uuid);
			invoicesDismissed++;
		}

		return Responses.from(jsonReaderWriter, Result.success(String.format("Avfärdade %s registreringar.", invoicesDismissed))).build();
	}
	
	@POST
	@Path("/mark-as-complete")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postMarkAsComplete(@Context HttpServletRequest httpRequest, String body) throws Exception {
		authService.guardAuthenticatedUser(httpRequest);
		RegistrationsListRequest request = jsonReaderWriter.deserialize(body, RegistrationsListRequest.class);

		int i = 0;
		for (UUID uuid : request.registrations) {
			markAsCompleteService.markAsComplete(uuid);
			i++;
		}

		return Responses.from(jsonReaderWriter, Result.success(String.format("Flyttade %s registreringar.", i))).build();
	}

	@POST
	@Path("/mark-as-paid")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postMarkAsPaid(@Context HttpServletRequest httpRequest, String body) throws Exception {
		authService.guardAuthenticatedUser(httpRequest);
		RegistrationsListRequest request = jsonReaderWriter.deserialize(body, RegistrationsListRequest.class);

		int i = 0;
		for (UUID uuid : request.registrations) {
			markAsPaidService.markAsPaid(uuid);
			i++;
		}

		return Responses.from(jsonReaderWriter, Result.success(String.format("%s registreringar markerade som betalda.", i))).build();
	}
}
