package org.brewingagile.backoffice.rest.api;

import argo.jdom.JsonNode;
import fj.Monoid;
import fj.Ord;
import fj.data.*;
import functional.Effect;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.brewingagile.backoffice.db.operations.*;
import org.brewingagile.backoffice.integrations.*;
import org.brewingagile.backoffice.pure.AccountIO;
import org.brewingagile.backoffice.rest.json.ToJson;
import org.brewingagile.backoffice.types.*;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.BigDecimals;
import org.brewingagile.backoffice.utils.Result;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.time.Instant;
import java.util.UUID;

import static argo.jdom.JsonNodeFactories.*;
import static java.util.Objects.requireNonNull;

@NeverCache
@Path("/registration/1/")
public class RegistrationApiJaxRs {
	private final DataSource dataSource;
	private final RegistrationsSqlMapper registrationsSqlMapper;
	private final ConfirmationEmailSender confirmationEmailSender;
	private final MailchimpSubscribeClient mailchimpSubscribeClient;
	private final SlackBotHook slackBotHook;
	private final TicketsSql ticketsSql;
	private final AccountIO accountIO;
	private final AccountSignupSecretSql accountSignupSecretSql;
	private final StripePublishableKey stripePublishableKey;
	private final StripeChargeClient stripeChargeClient;
	private final RegistrationStripeChargeSql registrationStripeChargeSql;
	private final MailchimpSubscribeClient.ListUniqueId newsletterList;
	private final OutvoiceReceiptClient outvoiceReceiptClient;
	private final OutvoiceInvoice3Client outvoiceInvoice3Client;

	public RegistrationApiJaxRs(
		DataSource dataSource,
		RegistrationsSqlMapper registrationsSqlMapper,
		ConfirmationEmailSender confirmationEmailSender,
		MailchimpSubscribeClient mailchimpSubscribeClient,
		SlackBotHook slackBotHook,
		TicketsSql ticketsSql,
		AccountIO accountIO,
		AccountSignupSecretSql accountSignupSecretSql,
		StripePublishableKey stripePublishableKey,
		StripeChargeClient stripeChargeClient,
		RegistrationStripeChargeSql registrationStripeChargeSql,
		MailchimpSubscribeClient.ListUniqueId newsletterList,
		OutvoiceReceiptClient outvoiceReceiptClient,
		OutvoiceInvoice3Client outvoiceInvoice3Client
	) {
		this.dataSource = dataSource;
		this.registrationsSqlMapper = registrationsSqlMapper;
		this.confirmationEmailSender = confirmationEmailSender;
		this.mailchimpSubscribeClient = mailchimpSubscribeClient;
		this.slackBotHook = slackBotHook;
		this.ticketsSql = ticketsSql;
		this.accountIO = accountIO;
		this.accountSignupSecretSql = accountSignupSecretSql;
		this.stripePublishableKey = stripePublishableKey;
		this.stripeChargeClient = stripeChargeClient;
		this.registrationStripeChargeSql = registrationStripeChargeSql;
		this.newsletterList = newsletterList;
		this.outvoiceReceiptClient = outvoiceReceiptClient;
		this.outvoiceInvoice3Client = outvoiceInvoice3Client;
	}

	@EqualsAndHashCode
	@ToString
	public static final class InvoicingR {
		public final String recipient;
		public final String address;

		public InvoicingR(
			String recipient,
			String address
		) {
			this.recipient = recipient;
			this.address = address;
		}
	}

	@EqualsAndHashCode
	@ToString
	public static final class StripeTokenR {
		public final String id;
		public final String email;

		public StripeTokenR(String id, String email) {
			this.id = id;
			this.email = email;
		}
	}

	@EqualsAndHashCode
	@ToString
	public static final class RegistrationR {
		public final ParticipantR participantR;
		public final Option<AccountSignupSecret> accountSignupSecret;
		public final Option<InvoicingR> invoicingR;
		public final Option<StripeTokenR> stripeTokenR;

		public RegistrationR(
			ParticipantR participantR,
			Option<AccountSignupSecret> accountSignupSecret,
			Option<InvoicingR> invoicingR,
			Option<StripeTokenR> stripeTokenR
		) {
			this.participantR = participantR;
			this.accountSignupSecret = accountSignupSecret;
			this.invoicingR = invoicingR;
			this.stripeTokenR = stripeTokenR;
		}
	}

	@EqualsAndHashCode
	@ToString
	public static final class ParticipantR {
		public final ParticipantName name;
		public final ParticipantEmail email;
		public final String dietaryRequirements;
		public final String twitter;
		public final ParticipantOrganisation organisation;
		public final Set<TicketName> tickets;

		public ParticipantR(
			ParticipantName name,
			ParticipantEmail participantEmail,
			String dietaryRequirements,
			String twitter,
			ParticipantOrganisation organisation,
			Set<TicketName> tickets
		) {
			this.name = requireNonNull(name);
			this.email = requireNonNull(participantEmail);
			this.dietaryRequirements = dietaryRequirements;
			this.twitter = twitter;
			this.organisation = organisation;
			this.tickets = tickets;
		}
	}

/*
INVOICE

curl -X POST -H "Content-Type: application/json" 'http://localhost:9080/api/registration/1/' --data '{
	"registration": {"name":"Henrik Test 1","email":"henrik@sjostrand.at","dietaryRequirements":"Nötter. Mandel, kokos och pinjenöt går bra.","lanyardCompany":"Lanyard Company","twitter":"@hencjo","tickets":["conference","workshop1"]},
	"invoice": {"recipient": "Hencjo AB", "address":"A\nB\nC"},
	"accountSignupSecret": null,
	"stripe": null
}'

curl -X POST -H "Content-Type: application/json" 'http://localhost:9080/api/registration/1/' --data '{
	"registration": {"name":"Henrik Test 1","email":"henrik@sjostrand.at","dietaryRequirements":"Nötter. Mandel, kokos och pinjenöt går bra.","lanyardCompany":"Lanyard Company","twitter":"@hencjo","tickets":["conference","workshop1"]},
	"invoice": null,
	"accountSignupSecret": null,
	"stripe": {"id": "tok_amex", "email": "henrik@sjostrand.at"}
}'

curl -X POST -H "Content-Type: application/json" 'http://localhost:9080/api/registration/1/' --data '{
	"registration": {"name":"Henrik Test 1","email":"henrik@sjostrand.at","dietaryRequirements":"Nötter. Mandel, kokos och pinjenöt går bra.","lanyardCompany":"Lanyard Company","twitter":"@hencjo","tickets":["conference","workshop1"]},
	"invoice": null,
	"accountSignupSecret": "858e8f44-be34-11e7-8158-4b6caac9b1c0",
	"stripe": null
}'
*/

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(@Context HttpServletRequest request,  String body) {
		try {
			System.out.println(body);

			JsonNode jrn = ArgoUtils.parse(body);
			RegistrationR rr = unjson(jrn);
			System.out.println("============================");
			System.out.println(rr);

			if ("fel".equals(rr.participantR.name)) {
				return Response
					.status(Response.Status.BAD_REQUEST)
					.entity(ArgoUtils.format(Result.failure("Registrering misslyckades: Du är inte cool nog.")))
					.build();
			}

			BigDecimal totalTicketIncsVat;
			RegistrationId registrationId = RegistrationId.registrationId(UUID.randomUUID());
			Option<Account> account = Option.none();
			try (Connection c = dataSource.getConnection()) {
				c.setAutoCommit(false);

				registrationsSqlMapper.insertRegistrationTuple(c, registrationId, new RegistrationsSqlMapper.RegistrationTuple(
					RegistrationState.RECEIVED,
					rr.participantR.name,
					rr.participantR.email,
					rr.participantR.dietaryRequirements,
					new Badge(""),
					rr.participantR.twitter,
					rr.participantR.organisation
				));
				registrationsSqlMapper.insertTickets(c, registrationId, rr.participantR.tickets);

				if (rr.accountSignupSecret.isSome()) {
					account = accountSignupSecretSql.account(c, rr.accountSignupSecret.some());
					registrationsSqlMapper.replaceAccount(c, registrationId.value, account);
					registrationsSqlMapper.updateRegistrationState(c, registrationId, RegistrationState.RECEIVED, RegistrationState.PAID);
				} else if (rr.invoicingR.isSome()) {
					InvoicingR invoicingR = rr.invoicingR.some();
					registrationsSqlMapper.insertRegistrationInvoiceMethod(c, registrationId, invoicingR.recipient, invoicingR.address);
				}

				totalTicketIncsVat = registrationsSqlMapper.totalTicketsIncVat(c, registrationId.value);

				c.commit();
			}

			Option<byte[]> receiptPdf = Option.none();
			if (rr.stripeTokenR.isSome()) {
				StripeTokenR some = rr.stripeTokenR.some();
				BigInteger amountInOre = BigDecimals.inOre(totalTicketIncsVat);
				Either<String, StripeChargeClient.ChargeResponse> stringChargeEither = stripeChargeClient.postCharge(some.id, amountInOre, rr.participantR.name, rr.participantR.email, registrationId);
				if (stringChargeEither.isLeft())
					return Response.status(402).entity(errorJson(stringChargeEither.left().value())).build();

				Instant stripeTxTimestamp = Instant.now();
				ChargeId chargeId = stringChargeEither.right().value().id;
				try (Connection c = dataSource.getConnection()) {
					c.setAutoCommit(false);
					registrationStripeChargeSql.insertCharge(c, registrationId, new RegistrationStripeChargeSql.Charge(
						chargeId,
						new BigDecimal(amountInOre).divide(BigDecimal.valueOf(100)),
						stripeTxTimestamp
					));
					c.commit();
				}

				try (Connection c = dataSource.getConnection()) {
					c.setAutoCommit(false);
					TreeMap<TicketName, TicketsSql.Ticket> p2s = ticketsSql.all(c).groupBy(x -> x.ticket).map(x -> x.head());
					List<TicketsSql.Ticket> map = rr.participantR.tickets.toList().map(x -> p2s.get(x).some());
					JsonNode jsonRootNode = OutvoiceReceiptClient.mkParticipantRequest(stringChargeEither.right().value().id, stripeTxTimestamp, rr.participantR.name, rr.participantR.email, map);
					Either<String, OutvoiceReceiptClient.ReceiptResponse> post = outvoiceReceiptClient.post(jsonRootNode);
					receiptPdf = post.right().toOption().map(x -> x.pdfSource);

					if (post.isLeft()) {
						System.err.println("We could not create a receipt for ChargeId " + chargeId + ". This is really bad.");
					} else {
						registrationStripeChargeSql.insertChargeReceipt(c, chargeId, post.right().value().pdfSource);
					}

					c.commit();
				}
			}

			Option<OutvoiceInvoice3Client.PostInvoiceResponse> postInvoiceResponse = Option.none();
			if (rr.invoicingR.isSome()) {
				InvoicingR some = rr.invoicingR.some();
				Set<TicketsSql.Ticket> tickets;
				try (Connection c = dataSource.getConnection()) {
					c.setAutoCommit(false);
					tickets = ticketsSql.by(c, registrationId);
				}
				postInvoiceResponse = Option.some(outvoiceInvoice3Client.postInvoice(OutvoiceInvoice3Client.mkParticipantRequest(
					registrationId,
					some.recipient,
					some.address,
					tickets,
					rr.participantR.name
				)));
				try (Connection c = dataSource.getConnection()) {
					c.setAutoCommit(false);
					OutvoiceInvoice3Client.PostInvoiceResponse por = postInvoiceResponse.some();
					registrationsSqlMapper.insertRegistrationInvoice2(c, registrationId, por.invoiceNumber, por.pdf);
					registrationsSqlMapper.updateRegistrationState(c, registrationId, RegistrationState.RECEIVED, RegistrationState.INVOICING);
					c.commit();
				}
			}

			Array<ConfirmationEmailSender.Attachment> attachments = Array.<ConfirmationEmailSender.Attachment>empty()
				.append(receiptPdf.toArray()
					.map(x -> new ConfirmationEmailSender.Attachment("Brewing Agile 2020 kvitto (" + rr.participantR.name.value + ").pdf", "application/pdf", x)))
				.append(postInvoiceResponse.toArray()
					.map(x -> new ConfirmationEmailSender.Attachment("Brewing Agile 2020 faktura (" + x.invoiceNumber + ").pdf", "application/pdf", x.pdf)));

			Either<String, String> emailResult = confirmationEmailSender.email(rr.participantR.email, attachments);
			if (emailResult.isLeft()) {
				System.err.println("We couldn't send an email to " + rr.participantR.name + "(" + rr.participantR.email + "). Cause: " + emailResult.left().value());
			}

			try {
				String s = rr.participantR.tickets.toList().map(x -> x.ticketName).foldLeft1((l, r) -> l + ", " + r);
				String slackPaymentText = slackPaymentText(rr.invoicingR, account, rr.stripeTokenR);
				slackBotHook.post("*" + rr.participantR.name.value + "* just signed up for *" + s +"* (" + slackPaymentText + ")");
			} catch (IOException e) {
				System.err.println("Couldn't post signup to Slack: " + e.getMessage());
				e.printStackTrace();
			}

			Either<String, Effect> subscribeResult = mailchimpSubscribeClient.subscribe(rr.participantR.email, newsletterList);
			if (subscribeResult.isLeft()) {
				System.err.println("We couldn't subscribe " + rr.participantR.email + " to email-list. Cause: " + subscribeResult.left().value());
			}

			return Response.ok(ArgoUtils.format(object(
				field("success", booleanNode(true))
			))).build();
		} catch (Exception e) {
			System.out.println("Unexpected: " + e.getMessage());
			e.printStackTrace(System.out);
			return Response.serverError().build();
		}
	}

	private static String slackPaymentText(
		Option<InvoicingR> invoicingR,
		Option<Account> account,
		Option<StripeTokenR> stripeTokenR
	) {
		if (invoicingR.isSome()) return "INVOICE";
		if (account.isSome()) return "Account: " + account.some().value;
		if (stripeTokenR.isSome()) return "CREDIT CARD";
		return "UNKNOWN";
	}

	private static String errorJson(String message) {
		return ArgoUtils.format(object(
			field("message", string(message))
		));
	}

	//	curl -u admin:password "http://localhost:9080/api/registration/1/config"
	@GET
	@Path("config")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConfig(@Context HttpServletRequest request) {
		return Response.ok(ArgoUtils.format(object(
			field("stripePublicKey", string(stripePublishableKey.value))
		))).build();
	}

	//	curl -u admin:password "http://localhost:9080/api/registration/1/tickets"
	@GET
	@Path("tickets")
	@Produces(MediaType.APPLICATION_JSON)
	public Response tickets(@Context HttpServletRequest request) throws Exception {
		Array<TicketsSql.Ticket> tickets;
		TreeMap<TicketName, BigInteger> sold;
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			sold = accountIO.ticketSales(c)
				.groupBy(x -> x._1(), x -> x._2().total, Monoid.bigintAdditionMonoid, TicketName.Ord);
			tickets = ticketsSql.all(c).toArray();
		}

		return Response.ok(ArgoUtils.format(
			object(
				field("tickets",
					array(
						tickets.map(x -> {
							BigInteger allTickets = BigInteger.valueOf(x.seats);
							BigInteger usedTickets = sold.get(x.ticket).orSome(BigInteger.ZERO);
							BigInteger availableTickets = allTickets.subtract(usedTickets);
							return ticketJson(x.ticket, x.productText, availableTickets.longValue() > 0, BigDecimals.inOre(x.price));
						})
					)
				)
			)
		)).build();
	}

	//	curl "http://localhost:9080/api/registration/1/account/0f6f369c-b3da-11e7-957f-bbf58cbe212a"
	@GET
	@Path("account/{accountSignupSecret}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAccount(
		@Context HttpServletRequest request,
		@PathParam("accountSignupSecret") String a
	) throws Exception {
		try {
			Option<AccountSignupSecret> parse = AccountSignupSecret.parse(a);

			if (parse.isNone())
				return Response.status(Response.Status.BAD_REQUEST).build();

			AccountSignupSecret accountSecret = parse.some();

			Account account;
			try (Connection c = dataSource.getConnection()) {
				c.setAutoCommit(false);

				Option<Account> maybeBundle = accountSignupSecretSql.account(c, accountSecret);
				if (maybeBundle.isNone())
					return Response.status(Response.Status.NOT_FOUND).build();

				account = maybeBundle.some();
			}

			return Response.ok(ArgoUtils.format(
				object(
					field("account", ToJson.account(account)),
					field("accountSignupSecret", ToJson.accountSignupSecret(accountSecret))
				)
			)).build();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private static JsonNode ticketJson(TicketName ticketName, String description, boolean value, BigInteger priceInOre) {
		return object(
			field("ticket", ToJson.json(ticketName)),
			field("description", string(description)),
			field("available", booleanNode(value)),
			field("price", number(priceInOre))
		);
	}

	private static RegistrationR unjson(JsonNode body) {
		return new RegistrationR(
			participant(body.getNode("registration")),
			optional(body, "accountSignupSecret").bind(j -> AccountSignupSecret.parse(j.getStringValue())),
			optional(body, "invoice").map(j ->
				new InvoicingR(
					j.getStringValue("recipient"),
					j.getStringValue("address")
				)
			),
			optional(body, "stripe").map(j ->
				new StripeTokenR(
					j.getStringValue("id"),
					j.getStringValue("email")
				)
			)
		);
	}

	private static Option<JsonNode> optional(JsonNode body, String f) {
		if (!body.isNode(f)) return Option.none();
		if (body.isNullNode(f)) return Option.none();
		return Option.some(body.getNode(f));
	}

	private static ParticipantR participant(JsonNode body) {
		Array<String> a = Array.iterableArray(body.getArrayNode("tickets")).map(x -> x.getStringValue());
		Set<TicketName> tickets = Set.iterableSet(Ord.stringOrd, a).map(Ord.hashEqualsOrd(), TicketName::ticketName);
		return new ParticipantR(
			ParticipantName.participantName(ArgoUtils.stringOrEmpty(body, "name").trim()),
			ParticipantEmail.participantEmail(ArgoUtils.stringOrEmpty(body, "email").trim()),
			ArgoUtils.stringOrEmpty(body, "dietaryRequirements".trim()),
			ArgoUtils.stringOrEmpty(body, "twitter".trim()),
			ParticipantOrganisation.participantOrganisation(ArgoUtils.stringOrEmpty(body, "lanyardCompany").trim()),
			tickets
		);
	}
}
