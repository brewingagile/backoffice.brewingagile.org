package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonRootNode;
import fj.Monoid;
import fj.P3;
import fj.Unit;
import fj.data.Either;
import fj.data.List;
import fj.data.Option;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.AccountSignupSecretSql;
import org.brewingagile.backoffice.db.operations.AccountsSql;
import org.brewingagile.backoffice.db.operations.AccountsSql.AccountData;
import org.brewingagile.backoffice.integrations.OutvoiceInvoiceClient;
import org.brewingagile.backoffice.pure.AccountIO;
import org.brewingagile.backoffice.pure.AccountLogic;
import org.brewingagile.backoffice.rest.json.ToJson;
import org.brewingagile.backoffice.types.Account;
import org.brewingagile.backoffice.types.AccountSignupSecret;
import org.brewingagile.backoffice.types.BillingMethod;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.Result;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.sql.Connection;

import static argo.jdom.JsonNodeFactories.*;

@Path("accounts")
@NeverCache
public class AccountsJaxRs {
	private final DataSource dataSource;
	private final AuthService authService;
	private final AccountsSql accountsSql;
	private final AccountIO accountIO;
	private final AccountSignupSecretSql accountSignupSecretSql;
	private final OutvoiceInvoiceClient outvoiceInvoiceClient;

	public AccountsJaxRs(
		DataSource dataSource,
		AuthService authService,
		AccountsSql accountsSql,
		AccountIO accountIO,
		AccountSignupSecretSql accountSignupSecretSql,
		OutvoiceInvoiceClient outvoiceInvoiceClient
	) {
		this.dataSource = dataSource;
		this.authService = authService;
		this.accountsSql = accountsSql;
		this.accountIO = accountIO;
		this.accountSignupSecretSql = accountSignupSecretSql;
		this.outvoiceInvoiceClient = outvoiceInvoiceClient;
	}

	//	curl -u admin:password "http://localhost:9080/gui/accounts/"
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response namesOnly(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			List<Account> accounts = accountsSql.all(c).toList();
			return Response.ok(ArgoUtils.format(array(accounts.map(ToJson::account)))).build();
		}
	}

//	curl -u admin:password "http://localhost:9080/gui/accounts/2/"
	@GET
	@Path("2/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response withTotals(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			List<P3<Account, AccountData, AccountLogic.Total>> data = accountIO.allAccountTotals(c);

			return Response.ok(ArgoUtils.format(array(data.map(x ->
				object(
					field("account", ToJson.account(x._1())),
					field("billingRecipient", string(x._2().billingRecipient)),
					field("billingAddress", string(x._2().billingAddress)),
					field("totalExVat", number(x._3().totalAmountExVat)),
					field("tickets", array(x._3().tickets.map(y -> object(
						field("ticket", ToJson.ticketName(y._1())),
						field("need", number(y._2().signupsNotPartOfPackage)),
						field("signups", number(y._2().signups)),
						field("missing", number(y._2().missingSignups)),
						field("totalReserved", number(y._2().totalReserved))
					))))
				)
			)))).build();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

//	curl -u admin:password "http://localhost:9080/gui/accounts/Uptive"
	@GET
	@Path("{account}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response accountStatement(@Context HttpServletRequest request, @PathParam("account") String aAccount) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			Account account = Account.account(aAccount);
			AccountLogic.AccountStatement accountStatement = accountIO.accountStatement(c, account);
			AccountData accountData = accountsSql.accountData(c, account);
			Option<AccountSignupSecret> accountSignupSecrets = accountSignupSecretSql.accountSignupSecret(c, account);
			return Response.ok(ArgoUtils.format(
				object(
					field("billingRecipient", string(accountData.billingRecipient)),
					field("billingAddress", string(accountData.billingAddress)),
					field("accountSignupSecret", ToJson.nullable(accountSignupSecrets, x -> string("/form.html?account_signup_secret=" + x.value))),
					field("lines", array(accountStatement.lines.map(x -> object(
						field("description", string(x.description)),
						field("price", number(x.price)),
						field("qty", number(x.qty)),
						field("total", number(total(x)))
					)))),
					field("total", number(accountStatement.lines.map(AccountsJaxRs::total).foldLeft(Monoid.bigdecimalAdditionMonoid.sum(), Monoid.bigdecimalAdditionMonoid.zero())))
				)
			)).build();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private static BigDecimal total(AccountLogic.Line x) {
		return x.price.multiply(new BigDecimal(x.qty));
	}

//	curl -v -u admin:password -X POST 'http://localhost:9080/gui/accounts/Uptive/invoice'

	@POST
	@Path("{account}/invoice")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postSendInvoices(@Context HttpServletRequest request, @PathParam("account") String aAccount) throws Exception {
		authService.guardAuthenticatedUser(request);

		try {
			Account account = Account.account(aAccount);
			JsonRootNode jsonRequest;
			try (Connection c = dataSource.getConnection()) {
				c.setAutoCommit(false);
				AccountLogic.AccountStatement accountStatement = accountIO.accountStatement(c, account);
				AccountData ad = accountsSql.accountData(c, account);
				jsonRequest = OutvoiceInvoiceClient.mkAccountRequest(BillingMethod.SNAILMAIL, "", ad.billingRecipient, ad.billingAddress, account, accountStatement);
			}
			return outvoiceInvoiceClient.postInvoice(jsonRequest).either(
				x -> Response.serverError().entity(ArgoUtils.format(Result.warning(x))).build(),
				x -> Response.ok(ArgoUtils.format(Result.success2("Invoice Successfully Sent"))).build()
			);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return Response.serverError().entity(ArgoUtils.format(Result.warning(e.getMessage()))).build();
		}
	}

}
