package org.brewingagile.backoffice.rest.gui;

import fj.P2;
import fj.data.List;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.pure.AccountIO;
import org.brewingagile.backoffice.rest.json.ToJson;
import org.brewingagile.backoffice.types.TicketName;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.sql.Connection;

import static argo.jdom.JsonNodeFactories.*;

@Path("/reports/")
@NeverCache
public class ReportsJaxRs {
	private final DataSource dataSource;
	private final AuthService authService;
	private final AccountIO accountIO;

	public ReportsJaxRs(
		DataSource dataSource,
		AuthService authService,
		AccountIO accountIO
	) {
		this.dataSource = dataSource;
		this.authService = authService;
		this.accountIO = accountIO;
	}


	//curl -u admin:password http://localhost:9080/gui/reports/totals  | jq .

	@GET
	@Path("/totals")
	public Response total(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try {
			try (Connection c = dataSource.getConnection()) {
				List<P2<TicketName, AccountIO.TicketSales>> map = accountIO.ticketSales(c);
				return Response.ok(ArgoUtils.format(array(map.map(x -> object(
					field("ticket", ToJson.ticketName(x._1())),
					field("individuals", number(x._2().individuals)),
					field("accounts", number(x._2().accounts)),
					field("total", number(x._2().total))
				))))).build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
	}
}

