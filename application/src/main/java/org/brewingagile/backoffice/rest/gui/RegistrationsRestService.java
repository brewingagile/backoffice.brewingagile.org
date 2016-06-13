package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.saj.InvalidSyntaxException;
import fj.F;
import fj.Function;
import fj.data.*;
import fj.function.Strings;
import functional.Tuple2;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.RegistrationState;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.Badge;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.BillingCompany;
import org.brewingagile.backoffice.services.DismissRegistrationService;
import org.brewingagile.backoffice.services.MarkAsCompleteService;
import org.brewingagile.backoffice.services.MarkAsPaidService;
import org.brewingagile.backoffice.services.SendInvoiceService;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.Result;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static argo.jdom.JsonNodeFactories.*;

@Path("/registrations/")
@NeverCache
public class RegistrationsRestService {
	private final DataSource dataSource;
	private final AuthService authService;
	private final RegistrationsSqlMapper registrationsSqlMapper;
	private final SendInvoiceService sendInvoiceService;
	private final DismissRegistrationService dismissRegistrationService;
	private final MarkAsCompleteService markAsCompleteService;
	private final MarkAsPaidService markAsPaidService;

	public RegistrationsRestService(
		DataSource dataSource,
		AuthService authService,
		RegistrationsSqlMapper registrationsSqlMapper,
		SendInvoiceService sendInvoiceService,
		DismissRegistrationService dismissRegistrationService,
		MarkAsCompleteService markAsCompleteService,
		MarkAsPaidService markAsPaidService
	) {
		this.dataSource = dataSource;
		this.authService = authService;
		this.registrationsSqlMapper = registrationsSqlMapper;
		this.sendInvoiceService = sendInvoiceService;
		this.dismissRegistrationService = dismissRegistrationService;
		this.markAsCompleteService = markAsCompleteService;
		this.markAsPaidService = markAsPaidService;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response invoices(@Context HttpServletRequest request) throws SQLException, IOException {
		authService.guardAuthenticatedUser(request);
		
		try (Connection c = dataSource.getConnection()) {
			List<UUID> ids = registrationsSqlMapper.all(c).map(x -> x.fst());
			List<RegistrationsSqlMapper.Registration> all = Option.somes(ids.traverseIO(ioify(c)).run());
			JsonRootNode overview = object(
				field("received", array(all.filter(x -> x.tuple.state == RegistrationState.RECEIVED).map(RegistrationsRestService::json))),
				field("invoicing", array(all.filter(x -> x.tuple.state == RegistrationState.INVOICING).map(RegistrationsRestService::json))),
				field("paid", array(all.filter(x -> x.tuple.state == RegistrationState.PAID).map(RegistrationsRestService::json)))
			);
			return Response.ok(ArgoUtils.format(overview)).build();
		}
	}

	private F<UUID,IO<Option<RegistrationsSqlMapper.Registration>>> ioify(Connection c) {
		return registrationId -> () -> {
			try {
				return registrationsSqlMapper.one(c, registrationId);
			} catch (SQLException e) {
				throw new IOException(e);
			}
		};
	}

	@GET
	@Path("/{registrationId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRegistration(@Context HttpServletRequest request, @PathParam("registrationId") UUID id) {
		authService.guardAuthenticatedUser(request);
		try {
			try (Connection c = dataSource.getConnection()) {
				return registrationsSqlMapper.one(c, id)
					.map(RegistrationsRestService::json)
					.map(ArgoUtils::format)
					.map(Response::ok)
					.orSome(Response.status(Status.NOT_FOUND))
					.build();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
	}

	public static argo.jdom.JsonRootNode json(Tuple2<UUID, RegistrationsSqlMapper.RegistrationTuple> t) {
		RegistrationsSqlMapper.RegistrationTuple r = t._2;
		return object(
			field("id", string(t._1.toString())),
			field("tuple", json(r))
		);
	}

	private static JsonRootNode json(RegistrationsSqlMapper.Registration r) {
		return object(
			field("id", string(r.id.toString())),
			field("tuple", json(r.tuple)),
			field("tickets", r.tickets.toJavaSet().stream().collect(ArgoUtils.toStringArray()))
		);
	}

	private static JsonRootNode json(RegistrationsSqlMapper.RegistrationTuple r) {
		return object(
			field("participantName", string(r.participantName)),
			field("participantEmail", string(r.participantEmail)),
			field("billingCompany", string(r.billingCompany)),
			field("billingAddress", string(r.billingAddress)),
			field("billingMethod", string(r.billingMethod.name())),
			field("twitter", string(r.twitter)),
			field("dietaryRequirements", string(r.dietaryRequirements)),
			field("badge", string(r.badge.badge)),
			field("bundle", string(r.bundle.orSome("")))
		);
	}

	public static final class RegistrationsUpdate {
		public final BillingCompany billingCompany;
		public final Badge badge;
		public final String dietaryRequirements;
		public final Option<String> bundle;

		public RegistrationsUpdate(
			BillingCompany billingCompany,
			Badge badge,
			String dietaryRequirements,
			Option<String> bundle
		) {
			this.billingCompany = billingCompany;
			this.badge = badge;
			this.dietaryRequirements = dietaryRequirements;
			this.bundle = bundle;
		}
	}

	private static Either<String, RegistrationsUpdate> registrationsUpdate(JsonNode jsonNode) {
		Either<String, BillingCompany> billingCompany = ArgoUtils.stringValue(jsonNode, "billingCompany").right().map(BillingCompany::new);
		Either<String, Badge> badge = ArgoUtils.stringValue(jsonNode, "badge").right().map(Badge::new);
		Either<String, String> dietaryRequirements = ArgoUtils.stringValue(jsonNode, "dietaryRequirements");
		Either<String, Option<String>> bundle = ArgoUtils.stringValue(jsonNode, "bundle")
			.right().map(Option::fromNull)
			.right().map(r -> r.filter(Strings.isNotNullOrEmpty));

		return bundle.right()
			.apply(dietaryRequirements.right()
				.apply(badge.right()
					.apply(billingCompany.right()
						.apply(Either.right(Function.curry(RegistrationsUpdate::new))))));
	}

	@POST
	@Path("/{registrationId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postUpdate(@Context HttpServletRequest request, @PathParam("registrationId") UUID id, String body) throws Exception {
		authService.guardAuthenticatedUser(request);
		Either<String, RegistrationsUpdate> map = ArgoUtils.parseEither(body)
			.right()
			.bind(RegistrationsRestService::registrationsUpdate);

		if (map.isLeft()) return Response.serverError().build();
		RegistrationsUpdate ru = map.right().value();
		
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			if (!registrationsSqlMapper.one(c, id).isSome()) return Response.status(Status.NOT_FOUND).build();
			registrationsSqlMapper.update(c, id, ru.billingCompany, ru.badge, ru.dietaryRequirements, ru.bundle);
			c.commit();
		}
		return Response.ok().build();
	}

	@POST
	@Path("/send-invoices")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postSendInvoices(@Context HttpServletRequest request, String body) throws Exception {
		authService.guardAuthenticatedUser(request);

		int invoicesSent = 0;
		for (UUID uuid : registrationListRequest(body)) {
			sendInvoiceService.sendInvoice(uuid);
			invoicesSent++;
		}

		return Response.ok(ArgoUtils.format(Result.success(String.format("Skickade %s fakturor.", invoicesSent)))).build();
	}

//		 curl -u admin:password -X POST -H "Content-Type: application/json" -H "Accept: application/json" http://localhost:9080/gui/registrations/mark-as-printed --data '{"registrations": ["f29651e0-c638-43d7-ace5-9dd7a07b1b78"]}'

	@POST
	@Path("/mark-as-printed")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response markAsPrinted(@Context HttpServletRequest request, String body) throws Exception {
		authService.guardAuthenticatedUser(request);

		int i = 0;
		for (UUID uuid : registrationListRequest(body)) {
			try (Connection c = dataSource.getConnection()) {
				registrationsSqlMapper.insertPrintedNametag(c, uuid);
			}
			i++;
		}

		return Response.ok(ArgoUtils.format(Result.success(String.format("Markerade %s namnbrickor som utskrivna.", i)))).build();
	}



	@POST
	@Path("/dismiss-registrations")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postDismissRegistrations(@Context HttpServletRequest httpRequest, String body) throws Exception {
		authService.guardAuthenticatedUser(httpRequest);

		int invoicesDismissed = 0;
		for (UUID uuid : registrationListRequest(body)) {
			dismissRegistrationService.dismissRegistration(uuid);
			invoicesDismissed++;
		}

		return Response.ok(ArgoUtils.format(Result.success(String.format("Avfärdade %s registreringar.", invoicesDismissed)))).build();
	}
	
	@POST
	@Path("/mark-as-complete")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postMarkAsComplete(@Context HttpServletRequest httpRequest, String body) throws Exception {
		authService.guardAuthenticatedUser(httpRequest);

		int i = 0;
		for (UUID uuid : registrationListRequest(body)) {
			markAsCompleteService.markAsComplete(uuid);
			i++;
		}

		return Response.ok(ArgoUtils.format(Result.success(String.format("Flyttade %s registreringar.", i)))).build();
	}

	@POST
	@Path("/mark-as-paid")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postMarkAsPaid(@Context HttpServletRequest httpRequest, String body) throws Exception {
		authService.guardAuthenticatedUser(httpRequest);

		int i = 0;
		for (UUID uuid : registrationListRequest(body)) {
			markAsPaidService.markAsPaid(uuid);
			i++;
		}

		return Response.ok(ArgoUtils.format(Result.success(String.format("%s registreringar markerade som betalda.", i)))).build();
	}

	private static List<UUID> registrationListRequest(String body) throws InvalidSyntaxException {
		return ArgoUtils.parse(body)
			.getArrayNode("registrations")
			.stream()
			.map(RegistrationsRestService::uuid)
			.collect(Collectors.toList());
	}

	private static UUID uuid(JsonNode jsonNode) {
		return UUID.fromString(jsonNode.getStringValue());
	}
}
