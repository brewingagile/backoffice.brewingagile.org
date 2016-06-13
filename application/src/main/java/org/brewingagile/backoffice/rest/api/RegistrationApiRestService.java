package org.brewingagile.backoffice.rest.api;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import argo.jdom.JsonRootNode;
import fj.Ord;
import fj.data.*;
import functional.Effect;
import org.brewingagile.backoffice.db.operations.RegistrationState;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.BillingMethod;
import org.brewingagile.backoffice.integrations.ConfirmationEmailSender;
import org.brewingagile.backoffice.integrations.MailchimpSubscribeClient;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.Result;

@Path("/registration/1/")
public class RegistrationApiRestService {
	private final DataSource dataSource;
	private final RegistrationsSqlMapper registrationsSqlMapper;
	private final ConfirmationEmailSender confirmationEmailSender;
	private final MailchimpSubscribeClient mailchimpSubscribeClient;

	public RegistrationApiRestService(
		DataSource dataSource,
		RegistrationsSqlMapper registrationsSqlMapper,
		ConfirmationEmailSender confirmationEmailSender,
		MailchimpSubscribeClient mailchimpSubscribeClient
	) {
		this.dataSource = dataSource;
		this.registrationsSqlMapper = registrationsSqlMapper;
		this.confirmationEmailSender = confirmationEmailSender;
		this.mailchimpSubscribeClient = mailchimpSubscribeClient;
	}

	public static final class RegistrationRequest {
		public final String participantName;
		public final String participantEmail;
		public final String billingCompany;
		public final String billingAddress;
		public final String billingMethod;
		public final String dietaryRequirements;
		public final Set<String> tickets;
		public final String twitter;

		public RegistrationRequest(
				String participantName,
				String participantEmail,
				String billingCompany,
				String billingAddress,
				String billingMethod,
				String dietaryRequirements,
				Set<String> tickets,
				String twitter
		) {
			this.participantName = participantName;
			this.participantEmail = participantEmail;
			this.billingCompany = billingCompany;
			this.billingAddress = billingAddress;
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
	
//	curl  -v -X POST -H "Content-Type: application/json" http://localhost:9080/api/registration/1/  --data '{"participantName" : "participant name", "participantEmail":"participant@email", "billingCompany": "billing-company", "billingAddress": "billing address", "billingMethod": "EMAIL", "tickets": "conference", "dietaryRequirements": "", "twitter": "@meow" }'
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(@Context HttpServletRequest request,  String body) throws  URISyntaxException, SQLException {
		try {
			fj.data.Either<String, JsonRootNode> parseEither = ArgoUtils.parseEither(body);

			if (parseEither.isLeft())
				return Response.status(Response.Status.BAD_REQUEST).build();

			JsonRootNode jrn = parseEither.right().value();

			RegistrationRequest rr = fromJson(jrn);
			System.out.println("============================");
			System.out.println("participantName: " + rr.participantName);
			System.out.println("participantEmail: " + rr.participantEmail);
			System.out.println("billingCompany: " + rr.billingCompany);
			System.out.println("billingAddress: " + rr.billingAddress);
			System.out.println("billingMethod: " + rr.billingMethod);
			System.out.println("tickets: " + rr.tickets);
			System.out.println("dietaryRequirements: " + rr.dietaryRequirements);
			System.out.println("twitter: " + rr.twitter);

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
							rr.billingCompany,
							rr.billingAddress,
							billingMethod(rr.billingMethod),
							rr.dietaryRequirements,
							new RegistrationsSqlMapper.Badge(""),
							rr.twitter,
							Option.none()
						),
						rr.tickets
					)
				);
			}

			Either<String, String> emailResult = confirmationEmailSender.email(rr.participantEmail);
			if (emailResult.isLeft()) {
				System.err.println("We couldn't send an email to " + rr.participantName + "(" + rr.participantEmail + "). Cause: " + emailResult.left().value());
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

	private RegistrationRequest fromJson(JsonRootNode body) {
		return new RegistrationRequest(
			ArgoUtils.stringOrEmpty(body, "participantName").trim(),
			ArgoUtils.stringOrEmpty(body, "participantEmail").trim().toLowerCase(),
			ArgoUtils.stringOrEmpty(body, "billingCompany").trim(),
			ArgoUtils.stringOrEmpty(body, "billingAddress").trim(),
			ArgoUtils.stringOrEmpty(body, "billingMethod").trim(),
			ArgoUtils.stringOrEmpty(body, "dietaryRequirements".trim()),
			tickets(body),
			ArgoUtils.stringOrEmpty(body, "twitter".trim())
		);
	}

	private static Set<String> tickets(JsonRootNode body) {
		Option<String> conference1 = Option.iif(body.getBooleanValue("tickets", "conference"), "conference");
		Option<String> workshop1 = Option.iif(body.getBooleanValue("tickets", "workshop1"), "workshop1");
		Option<String> workshop2 = Option.iif(body.getBooleanValue("tickets", "workshop2"), "workshop2");
		return Set.iterableSet(Ord.stringOrd, Option.somes(List.list(conference1, workshop1, workshop2)));
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
