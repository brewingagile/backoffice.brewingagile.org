package org.brewingagile.backoffice.rest.api;

import argo.jdom.JsonRootNode;
import fj.Ord;
import fj.data.*;
import functional.Effect;
import org.brewingagile.backoffice.db.operations.BundlesSql;
import org.brewingagile.backoffice.db.operations.RegistrationState;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.BillingMethod;
import org.brewingagile.backoffice.db.operations.TicketsSql;
import org.brewingagile.backoffice.db.operations.TicketsSql.TicketName;
import org.brewingagile.backoffice.integrations.ConfirmationEmailSender;
import org.brewingagile.backoffice.integrations.MailchimpSubscribeClient;
import org.brewingagile.backoffice.integrations.SlackBotHook;
import org.brewingagile.backoffice.pure.BundleLogic;
import org.brewingagile.backoffice.rest.json.ToJson;
import org.brewingagile.backoffice.types.ParticipantOrganisation;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.Result;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static argo.jdom.JsonNodeFactories.*;

@Path("/registration/1/")
public class RegistrationApiJaxRs {
	private final DataSource dataSource;
	private final RegistrationsSqlMapper registrationsSqlMapper;
	private final ConfirmationEmailSender confirmationEmailSender;
	private final MailchimpSubscribeClient mailchimpSubscribeClient;
	private final BundlesSql bundlesSql;
	private final SlackBotHook slackBotHook;
	private final TicketsSql ticketsSql;

	public RegistrationApiJaxRs(
		DataSource dataSource,
		RegistrationsSqlMapper registrationsSqlMapper,
		ConfirmationEmailSender confirmationEmailSender,
		MailchimpSubscribeClient mailchimpSubscribeClient,
		BundlesSql bundlesSql,
		SlackBotHook slackBotHook,
		TicketsSql ticketsSql
	) {
		this.dataSource = dataSource;
		this.registrationsSqlMapper = registrationsSqlMapper;
		this.confirmationEmailSender = confirmationEmailSender;
		this.mailchimpSubscribeClient = mailchimpSubscribeClient;
		this.bundlesSql = bundlesSql;
		this.slackBotHook = slackBotHook;
		this.ticketsSql = ticketsSql;
	}

	public static final class RegistrationRequest {
		public final String participantName;
		public final String participantEmail;
		public final String dietaryRequirements;
		public final String twitter;
		public final ParticipantOrganisation organisation;
		public final Set<TicketName> tickets;
		public final String invoiceRecipient;
		public final String invoiceAddress;
		public final String billingMethod;

		public RegistrationRequest(
			String participantName,
			String participantEmail,
			String dietaryRequirements,
			String twitter,
			ParticipantOrganisation organisation,
			Set<TicketName> tickets,
			String invoiceRecipient,
			String invoiceAddress,
			String billingMethod
		) {
			this.participantName = participantName;
			this.participantEmail = participantEmail;
			this.organisation = organisation;
			this.invoiceRecipient = invoiceRecipient;
			this.invoiceAddress = invoiceAddress;
			this.billingMethod = billingMethod;
			this.dietaryRequirements = dietaryRequirements;
			this.tickets = tickets;
			this.twitter = twitter;
		}
	}
	
	//curl  -v -X POST -H "Content-Type: application/json" http://localhost:9080/api/registration/1/  --data '{"participantName" : "fel" }'

	@OPTIONS
	public Response options(@Context HttpServletRequest request,  String body) throws  URISyntaxException {
		return accessControlHeaders(request, Response.noContent()).build();
	}

//	curl  -v -X POST -H "Content-Type: application/json" http://localhost:9080/api/registration/1/  --data '{"participantName":"Henrik Test 1","participantEmail":"henrik@sjostrand.at","dietaryRequirements":"Nötter. Mandel, kokos och pinjenöt går bra.","lanyardCompany":"Lanyard Company","twitter":"@hencjo","tickets":["conference","workshop1"],"invoiceRecipient":"Hencjo AB","invoiceAddress":"A\nB\nC","billingMethod":"EMAIL"}'

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(@Context HttpServletRequest request,  String body) throws  URISyntaxException, SQLException {
		try {
			System.out.println(body);

			fj.data.Either<String, JsonRootNode> parseEither = ArgoUtils.parseEither(body);

			if (parseEither.isLeft())
				return Response.status(Response.Status.BAD_REQUEST).build();

			JsonRootNode jrn = parseEither.right().value();

			RegistrationRequest rr = fromJson(jrn);
			System.out.println("============================");
			System.out.println("participantName: " + rr.participantName);
			System.out.println("participantEmail: " + rr.participantEmail);
			System.out.println("dietaryRequirements: " + rr.dietaryRequirements);
			System.out.println("twitter: " + rr.twitter);
			System.out.println("lanyardCompany: " + rr.organisation);
			System.out.println("tickets: " + rr.tickets);
			System.out.println("invoiceRecipient: " + rr.invoiceRecipient);
			System.out.println("invoiceAddress: " + rr.invoiceAddress);
			System.out.println("billingMethod: " + rr.billingMethod);

			if ("fel".equals(rr.participantName)) {
				ResponseBuilder ok = Response.ok(ArgoUtils.format(Result.failure("Registrering misslyckades: Du är inte cool nog.")));
				return accessControl(request, ok).build();
			}

			try (Connection c = dataSource.getConnection()) {
				UUID uuid = UUID.randomUUID();
				registrationsSqlMapper.replace(
					c,
					uuid,
					new RegistrationsSqlMapper.Registration(
						uuid,
						new RegistrationsSqlMapper.RegistrationTuple(
							RegistrationState.RECEIVED,
							rr.participantName,
							rr.participantEmail,
							rr.invoiceRecipient,
							rr.invoiceAddress,
							billingMethod(rr.billingMethod),
							rr.dietaryRequirements,
							new RegistrationsSqlMapper.Badge(""),
							rr.twitter,
							Option.none(),
							rr.organisation
						),
						rr.tickets,
						Option.none()
					)
				);
			}

			Either<String, String> emailResult = confirmationEmailSender.email(rr.participantEmail);
			if (emailResult.isLeft()) {
				System.err.println("We couldn't send an email to " + rr.participantName + "(" + rr.participantEmail + "). Cause: " + emailResult.left().value());
			}

			try {
				String s = rr.tickets.toList().map(x -> x.ticketName).foldLeft1((l, r) -> l + ", " + r);
				slackBotHook.post("*" + rr.participantName + "* just signed up for *" + s +"*");
			} catch (IOException e) {
				e.printStackTrace();
			}

			Either<String, Effect> subscribeResult = mailchimpSubscribeClient.subscribe(rr.participantEmail, "da90a13118");
			if (subscribeResult.isLeft()) {
				System.err.println("We couldn't subscribe " + rr.participantEmail + " to email-list. Cause: " + subscribeResult.left().value());
			}

			ResponseBuilder ok = Response.ok(ArgoUtils.format(Result.success("Registrering klar.")));
			return accessControl(request, ok).build();
		} catch (Exception e) {
			System.out.println("Unexpected: " + e.getMessage());
			e.printStackTrace(System.out);
			return Response.serverError().build();
		}
	}

	//	curl -u admin:password "http://localhost:9080/api/registration/1/tickets"
	@GET
	@Path("tickets")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@Context HttpServletRequest request) throws Exception {
		BundleLogic.Total logic;
		Array<TicketsSql.Ticket> tickets;
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			List<BundlesSql.BucketSummary> bundles = bundlesSql.bundles(c);
			BundlesSql.Individuals individuals = bundlesSql.individuals(c);
			logic = BundleLogic.logic(bundles, individuals);
			tickets = ticketsSql.all(c).toArray();
		}

		BundleLogic.Total2 total = logic.total;
		return Response.ok(ArgoUtils.format(
			object(
				field("tickets",
					array(
						tickets.map(x -> ticketJson(x.ticket, x.productText, usedSeats(total, x.ticket.ticketName) < x.seats, x.price))
					)
				)
			)
		)).build();
	}

	private static int usedSeats(BundleLogic.Total2 total, String ticket) {
		switch (ticket) {
			case "conference": return total.conference;
			case "workshop1": return total.workshop1;
			case "workshop2": return total.workshop2;
			default: return 0;
		}
	}

	private static JsonRootNode ticketJson(TicketName ticketName, String description, boolean value, BigDecimal price) {
		return object(
			field("ticket", ToJson.json(ticketName)),
			field("description", string(description)),
			field("available", booleanNode(value)),
			field("price", number(price))
		);
	}


//{"participantName":"Henrik Test 1","participantEmail":"henrik@sjostrand.at","dietaryRequirements":"Nötter. Mandel, kokos och pinjenöt går bra.","lanyardCompany":"Lanyard Company","twitter":"@hencjo","tickets":["conference","workshop1"],"invoiceRecipient":"Hencjo AB","invoiceAddress":"A\nB\nC","billingMethod":"EMAIL"}

	private RegistrationRequest fromJson(JsonRootNode body) {
		Array<String> a = Array.iterableArray(body.getArrayNode("tickets")).map(x -> x.getStringValue());
		Set<TicketName> tickets = Set.iterableSet(Ord.stringOrd, a).map(Ord.hashEqualsOrd(), x -> TicketName.ticketName(x));
		return new RegistrationRequest(
			ArgoUtils.stringOrEmpty(body, "participantName").trim(),
			ArgoUtils.stringOrEmpty(body, "participantEmail").trim().toLowerCase(),
			ArgoUtils.stringOrEmpty(body, "dietaryRequirements".trim()),
			ArgoUtils.stringOrEmpty(body, "twitter".trim()),
			ParticipantOrganisation.participantOrganisation(ArgoUtils.stringOrEmpty(body, "lanyardCompany").trim()),
			tickets,
			ArgoUtils.stringOrEmpty(body, "invoiceRecipient").trim(),
			ArgoUtils.stringOrEmpty(body, "invoiceAddress").trim(),
			ArgoUtils.stringOrEmpty(body, "billingMethod").trim()
		);
	}

	private static BillingMethod billingMethod(String billingMethod) {
		switch (billingMethod) {
			case "EMAIL": return BillingMethod.EMAIL;
			case "SNAILMAIL": return BillingMethod.SNAILMAIL;
			default: throw new IllegalArgumentException("billingMethod " + billingMethod + " not mapped.");
		}
	}

	private static ResponseBuilder accessControl(HttpServletRequest request, ResponseBuilder response) {
		if (null == request.getHeader("Origin")) return response;
		return accessControlHeaders(request, response);
	}

	private static ResponseBuilder accessControlHeaders(HttpServletRequest request, ResponseBuilder response) {
		return response
				.header("Access-Control-Allow-Origin", "*")
				.header("Access-Control-Allow-Methods", starIfEmpty(request.getHeader("Access-Control-Request-Method")))
				.header("Access-Control-Allow-Headers", starIfEmpty(request.getHeader("Access-Control-Request-Headers")));
	}

	private static String starIfEmpty(String str) {
		if (str == null || str.isEmpty()) return "*";
		return str;
	}
}
