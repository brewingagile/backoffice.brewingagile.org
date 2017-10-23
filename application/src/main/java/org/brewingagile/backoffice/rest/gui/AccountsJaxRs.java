package org.brewingagile.backoffice.rest.gui;

import fj.P3;
import fj.data.List;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.AccountsSql;
import org.brewingagile.backoffice.db.operations.AccountsSql.AccountData;
import org.brewingagile.backoffice.pure.AccountIO;
import org.brewingagile.backoffice.pure.AccountLogic;
import org.brewingagile.backoffice.rest.json.ToJson;
import org.brewingagile.backoffice.types.Account;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;

import static argo.jdom.JsonNodeFactories.*;

@Path("accounts")
@NeverCache
public class AccountsJaxRs {
	private final DataSource dataSource;
	private final AuthService authService;
	private final AccountsSql accountsSql;
	private final AccountIO accountIO;

	public AccountsJaxRs(
		DataSource dataSource,
		AuthService authService,
		AccountsSql accountsSql,
		AccountIO accountIO
	) {
		this.dataSource = dataSource;
		this.authService = authService;
		this.accountsSql = accountsSql;
		this.accountIO = accountIO;
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


}
