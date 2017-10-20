package org.brewingagile.backoffice.rest.api;

import argo.jdom.JsonRootNode;
import fj.Monoid;
import fj.P4;
import fj.data.Either;
import fj.data.List;
import fj.data.Option;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.brewingagile.backoffice.db.operations.*;
import org.brewingagile.backoffice.integrations.StripeChargeClient;
import org.brewingagile.backoffice.rest.json.ToJson;
import org.brewingagile.backoffice.types.AccountSecret;
import org.brewingagile.backoffice.types.StripePublishableKey;
import org.brewingagile.backoffice.types.TicketName;
import org.brewingagile.backoffice.utils.ArgoUtils;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.time.Instant;

import static argo.jdom.JsonNodeFactories.*;

@Path("/stripe/")
public class StripeJaxRs {
	private final DataSource dataSource;
	private final RegistrationsSqlMapper registrationsSqlMapper;
	private final AccountSecretSql accountSecretSql;
	private final StripeChargeClient stripeChargeClient;
	private final StripePublishableKey stripePublishableKey;
	private final StripeChargeSql stripeChargeSql;

	public StripeJaxRs(
		DataSource dataSource,
		RegistrationsSqlMapper registrationsSqlMapper,
		AccountSecretSql accountSecretSql,
		StripeChargeClient stripeChargeClient,
		StripePublishableKey stripePublishableKey,
		StripeChargeSql stripeChargeSql
	) {
		this.dataSource = dataSource;
		this.registrationsSqlMapper = registrationsSqlMapper;
		this.accountSecretSql = accountSecretSql;
		this.stripeChargeClient = stripeChargeClient;
		this.stripePublishableKey = stripePublishableKey;
		this.stripeChargeSql = stripeChargeSql;
	}

	//	curl "http://localhost:9080/api/stripe/account/0f6f369c-b3da-11e7-957f-bbf58cbe212a"
	@GET
	@Path("account/{accountSecret}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(
		@Context HttpServletRequest request,
		@PathParam("accountSecret") String accountSecretRaw
	) throws Exception {
		try {
			Option<AccountSecret> parse = AccountSecret.parse(accountSecretRaw);

			if (parse.isNone())
				return Response.status(Response.Status.BAD_REQUEST).build();

			AccountSecret accountSecret = parse.some();

			List<P4<String, TicketName, BigDecimal, String>> tickets;
			List<StripeChargeSql.Charge> charges;
			try (Connection c = dataSource.getConnection()) {
				c.setAutoCommit(false);

				Option<String> maybeBundle = accountSecretSql.bundle(c, accountSecret);
				if (maybeBundle.isNone())
					return Response.status(Response.Status.NOT_FOUND).build();

				String bundle = maybeBundle.some();
				tickets = registrationsSqlMapper.inBundle(c, bundle);
				charges = stripeChargeSql.byBundle(c, bundle);
			}

			Monoid<BigDecimal> add = Monoid.bigdecimalAdditionMonoid;
			BigDecimal total = tickets.map(P4::_3).foldLeft(add.sum(), add.zero());
			BigDecimal paid = charges.map(x -> x.amount).foldLeft(add.sum(), add.zero());
			BigDecimal due = total.subtract(paid);
			return Response.ok(ArgoUtils.format(
				object(
					field("key", string(stripePublishableKey.value)),
					field("tickets",
						array(
							tickets.map(x -> object(
								field("participantName", string(x._1())),
								field("ticket", ToJson.json(x._2())),
								field("productText", string(x._4())),
								field("price", number(x._3()))
							))
						)
					),
					field("total", number(total)),
					field("amountPaid", number(paid)),
					field("amountDue", number(due)),
					field("amountDueOre", number(due.multiply(BigDecimal.valueOf(100))))
				)
			)).build();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	//	curl -X POST "http://localhost:9080/api/stripe/account/0f6f369c-b3da-11e7-957f-bbf58cbe212a/pay --data '{"token":{"id":"","email":""}, "amount": "123400"}'"
	@POST
	@Path("account/{accountSecret}/pay")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response pay(
		@Context HttpServletRequest request,
		@PathParam("accountSecret") String accountSecretRaw,
		String body
	) throws Exception {
		try {
			Option<AccountSecret> parse = AccountSecret.parse(accountSecretRaw);
			if (parse.isNone()) return Response.status(Response.Status.BAD_REQUEST).build();

			AccountSecret accountSecret = parse.some();
			PayRequest unjson = PayRequest.unjson(ArgoUtils.parse(body));

			System.out.println("pay:");
			System.out.println(accountSecret);
			System.out.println(body);
			System.out.println(unjson);

			Either<String, StripeChargeClient.ChargeResponse> stringChargeEither = stripeChargeClient.postCharge(unjson.tokenId, unjson.amountInOre);
			if (stringChargeEither.isLeft())
				return Response.status(402).entity(errorJson(stringChargeEither.left().value())).build();

			try (Connection c = dataSource.getConnection()) {
				c.setAutoCommit(false);
				String bundle = accountSecretSql.bundle(c, accountSecret).some();
				stripeChargeSql.insertCharge(c, bundle, new StripeChargeSql.Charge(
					stringChargeEither.right().value().id,
					new BigDecimal(unjson.amountInOre).divide(BigDecimal.valueOf(100)),
					Instant.now()
				));
				c.commit();
			}

			return Response.ok().build();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private static String errorJson(String message) {
		return ArgoUtils.format(object(
			field("message", string(message))
		));
	}

	@EqualsAndHashCode
	@ToString
	private static final class PayRequest {
		public final String tokenId;
		public final String tokenEmail;
		public final BigInteger amountInOre;

		public PayRequest(String tokenId, String tokenEmail, BigInteger amountInOre) {
			this.tokenId = tokenId;
			this.tokenEmail = tokenEmail;
			this.amountInOre = amountInOre;
		}

		public static PayRequest unjson(JsonRootNode x) {
			return new PayRequest(
				x.getStringValue("token", "id"),
				x.getStringValue("token", "email"),
				new BigInteger(x.getNumberValue("amount"))
			);
		}
	}
}
