package org.brewingagile.backoffice.rest.gui;

import fj.Monoid;
import fj.P;
import fj.P2;
import fj.P3;
import fj.data.List;
import fj.data.TreeMap;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.AccountsSql;
import org.brewingagile.backoffice.db.operations.AccountsSql.AccountData;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.TicketsSql;
import org.brewingagile.backoffice.pure.AccountLogic;
import org.brewingagile.backoffice.rest.json.ToJson;
import org.brewingagile.backoffice.types.Account;
import org.brewingagile.backoffice.types.AccountPackage;
import org.brewingagile.backoffice.types.TicketName;
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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;

import static argo.jdom.JsonNodeFactories.*;

@Path("accounts")
@NeverCache
public class AccountsJaxRs {
	private final DataSource dataSource;
	private final AuthService authService;
	private final AccountsSql accountsSql;
	private final TicketsSql ticketsSql;
	private final RegistrationsSqlMapper registrationsSqlMapper;

	public AccountsJaxRs(
		DataSource dataSource,
		AuthService authService,
		AccountsSql accountsSql,
		TicketsSql ticketsSql,
		RegistrationsSqlMapper registrationsSqlMapper
	) {
		this.dataSource = dataSource;
		this.authService = authService;
		this.accountsSql = accountsSql;
		this.ticketsSql = ticketsSql;
		this.registrationsSqlMapper = registrationsSqlMapper;
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
			List<Account> accounts = accountsSql.all(c).toList();


			List.Buffer<P3<Account, AccountData, AccountLogic.Total>> buffer = List.Buffer.empty();
			for (Account account : accounts) {
				AccountData accountData = accountsSql.accountData(c, account);
				List<AccountPackage> packages = accountsSql.accountPackages(c, account);
				List<P2<TicketName, BigInteger>> signups = registrationsSqlMapper.inAccount(c, account)
					.groupBy(x -> x._2(), x -> BigInteger.ONE, Monoid.bigintAdditionMonoid, TicketName.Ord)
					.toList();
				TreeMap<TicketName, BigDecimal> tickets = ticketsSql.all(c).groupBy(x -> x.ticket, x -> x.price).map(x -> x.head());
				AccountLogic.Total total = AccountLogic.logic(
					packages,
					signups,
					tickets
				);
				buffer.snoc(P.p(account, accountData, total));
			}
			List<P3<Account, AccountData, AccountLogic.Total>> data = buffer.toList();

			return Response.ok(ArgoUtils.format(array(data.map(x ->
				object(
					field("account", ToJson.account(x._1())),
					field("billingRecipient", string(x._2().billingRecipient)),
					field("billingAddress", string(x._2().billingAddress)),
					field("totalExVat", number(x._3().totalAmountExVat)),
					field("tickets", array(x._3().tickets.map(y -> object(
						field("ticket", ToJson.ticketName(y._1())),
						field("need", number(y._2())),
						field("signups", number(y._3())),
						field("missing", number(y._4()))
					))))
				)
			)))).build();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
}
