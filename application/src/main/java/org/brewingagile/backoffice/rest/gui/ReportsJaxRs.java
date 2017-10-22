package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonRootNode;
import fj.Ord;
import fj.P2;
import fj.data.List;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.BundlesSql;
import org.brewingagile.backoffice.pure.AccountIO;
import org.brewingagile.backoffice.rest.json.ToJson;
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
import java.math.BigInteger;
import java.sql.Connection;

import static argo.jdom.JsonNodeFactories.*;
import static org.brewingagile.backoffice.db.operations.BundlesSql.BucketSummary;
import static org.brewingagile.backoffice.db.operations.BundlesSql.Individuals;

@Path("/reports/")
@NeverCache
public class ReportsJaxRs {
	private final DataSource dataSource;
	private final AuthService authService;
	private final BundlesSql bundlesSql;
	private final AccountIO accountIO;

	public ReportsJaxRs(
		DataSource dataSource,
		AuthService authService,
		BundlesSql bundlesSql,
		AccountIO accountIO
	) {
		this.dataSource = dataSource;
		this.authService = authService;
		this.bundlesSql = bundlesSql;
		this.accountIO = accountIO;
	}

//curl -u admin:password http://localhost:9080/gui/reports/bundles  | jq .

	@GET
	@Path("/bundles")
	@Produces(MediaType.APPLICATION_JSON)
	public Response bundles(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			return Response.ok(ArgoUtils.format(
				array(bundlesSql.bundles(c).map(ReportsJaxRs::json))
			)).build();
		}
	}

	//curl -u admin:password http://localhost:9080/gui/reports/individuals  | jq .

	@GET
	@Path("/individuals")
	@Produces(MediaType.APPLICATION_JSON)
	public Response individuals(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try {
			try (Connection c = dataSource.getConnection()) {
				List<P2<TicketName, BigInteger>> individuals = bundlesSql.individuals2(c).sort(Ord.p2Ord1(TicketName.Ord));
				return Response.ok(ArgoUtils.format(array(individuals.map(x -> object(
					field("ticket", ToJson.ticketName(x._1())),
					field("qty", number(x._2()))
				))))).build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
	}

	//curl -u admin:password http://localhost:9080/gui/reports/totals  | jq .

	@GET
	@Path("/totals")
	public Response total(@Context HttpServletRequest  request) throws Exception {
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

	public static JsonRootNode json(BucketSummary b) {
		JsonRootNode planned = object(
			field("conference", number(b.bucket.conference)),
			field("workshop1", number(b.bucket.workshop1)),
			field("workshop2", number(b.bucket.workshop2))
		);

		JsonRootNode actual = object(
			field("conference", number(b.actualConference)),
			field("workshop1", number(b.actualWorkshop1)),
			field("workshop2", number(b.actualWorkshop2))
		);

		return object(
			field("bundle", string(b.bucket.bucket)),
			field("planned", planned),
			field("actual", actual)
		);
	}

	public static JsonRootNode json(Individuals i) {
		return object(
			field("conference", number(i.conference)),
			field("workshop1", number(i.workshop1)),
			field("workshop2", number(i.workshop2))
		);
	}
}