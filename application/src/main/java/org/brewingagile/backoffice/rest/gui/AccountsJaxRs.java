package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonRootNode;
import fj.P3;
import fj.data.Either;
import fj.data.List;
import fj.data.Option;
import fj.function.Strings;
import jdk.nashorn.internal.runtime.regexp.joni.constants.Arguments;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.AccountSignupSecretSql;
import org.brewingagile.backoffice.db.operations.AccountsSql;
import org.brewingagile.backoffice.db.operations.AccountsSql.AccountData;
import org.brewingagile.backoffice.integrations.OutvoiceAccountClient;
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
	private final OutvoiceAccountClient outvoiceAccountClient;

	public AccountsJaxRs(
		DataSource dataSource,
		AuthService authService,
		AccountsSql accountsSql,
		AccountIO accountIO,
		AccountSignupSecretSql accountSignupSecretSql,
		OutvoiceInvoiceClient outvoiceInvoiceClient,
		OutvoiceAccountClient outvoiceAccountClient
	) {
		this.dataSource = dataSource;
		this.authService = authService;
		this.accountsSql = accountsSql;
		this.accountIO = accountIO;
		this.accountSignupSecretSql = accountSignupSecretSql;
		this.outvoiceInvoiceClient = outvoiceInvoiceClient;
		this.outvoiceAccountClient = outvoiceAccountClient;
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
			BigDecimal value = AccountLogic.total(accountStatement.lines);
			return Response.ok(ArgoUtils.format(
				object(
					field("billingRecipient", string(accountData.billingRecipient)),
					field("billingAddress", string(accountData.billingAddress)),
					field("billingEmail", string(accountData.billingEmail)),
					field("accountSignupSecret", ToJson.nullable(accountSignupSecrets, x -> string("/form.html?account_signup_secret=" + x.value))),
					field("lines", array(accountStatement.lines.map(x -> object(
						field("description", string(x.description)),
						field("price", number(x.price)),
						field("qty", number(x.qty)),
						field("total", number(AccountLogic.total(x)))
					)))),
					field("total", number(value))
				)
			)).build();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	//	curl -u admin:password "http://localhost:9080/gui/accounts/Uptive/invoices"
	@GET
	@Path("{account}/invoices")
	@Produces(MediaType.APPLICATION_JSON)
	public Response accountInvoices(@Context HttpServletRequest request, @PathParam("account") String aAccount) throws Exception {
		authService.guardAuthenticatedUser(request);
		try {
			Account account = Account.account(aAccount);
			return Response.ok(outvoiceAccountClient.get("brewingagile-" + account.value)).build();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return Response.serverError().entity(ArgoUtils.format(Result.warning(e.getMessage()))).build();
		}
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
				AccountLogic.AccountStatement2 accountStatement = accountIO.accountStatement2(c, account);
				AccountData ad = accountsSql.accountData(c, account);
				String invoiceAccountKey = "brewingagile-" + account.value;
				BigDecimal alreadyInvoicedAmountExVat = OutvoiceAccountClient.invoiceAmountExVat(outvoiceAccountClient.get(invoiceAccountKey));
				Option<JsonRootNode> jsonRootNodes = OutvoiceInvoiceClient.mkAccountRequest(invoiceAccountKey, ad.billingEmail, ad.billingRecipient, ad.billingAddress, accountStatement, alreadyInvoicedAmountExVat);
				if (jsonRootNodes.isNone())
					return Response.ok(ArgoUtils.format(Result.success2("Balance is Zero. Nothing to invoice."))).build();
				jsonRequest = jsonRootNodes.some();
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


	@POST
	@Path("{account}/billing")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postBillingInfo(@Context HttpServletRequest request, @PathParam("account") String aAccount, String body) throws Exception {
		authService.guardAuthenticatedUser(request);

		try {
			Account account = Account.account(aAccount);
			JsonRootNode parse = ArgoUtils.parse(body);
			String billingRecipient = parse.getStringValue("billingRecipient");
			String billingAddress = parse.getStringValue("billingAddress");
			String billingEmail = parse.getStringValue("billingEmail");

			AccountData accountData = new AccountData(billingRecipient, billingAddress, billingEmail);

			try (Connection c = dataSource.getConnection()) {
				c.setAutoCommit(false);
				accountsSql.update(c, account, accountData);
				c.commit();
			}
			return Response.ok(ArgoUtils.format(Result.success2("Account Data Saved"))).build();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return Response.serverError().entity(ArgoUtils.format(Result.warning(e.getMessage()))).build();
		}
	}
}
