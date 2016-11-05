package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonRootNode;
import fj.data.List;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.BundlesSql;
import org.brewingagile.backoffice.pure.BundleLogic;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.jersey.NeverCache;


import static argo.jdom.JsonNodeFactories.*;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;

import static org.brewingagile.backoffice.db.operations.BundlesSql.BucketSummary;
import static org.brewingagile.backoffice.db.operations.BundlesSql.Individuals;

@Path("/reports/")
@NeverCache
public class ReportsJaxRs {
	private final DataSource dataSource;
	private final AuthService authService;
	private final BundlesSql bundlesSql;

	public ReportsJaxRs(DataSource dataSource, AuthService authService, BundlesSql bundlesSql) {
		this.dataSource = dataSource;
		this.authService = authService;
		this.bundlesSql = bundlesSql;
	}

	//curl -u admin:password http://localhost:9080/gui/reports/bundles  | jq .

	@GET
	@Path("/bundles")
	@Produces(MediaType.APPLICATION_JSON)
	public Response bundless(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			return Response.ok(ArgoUtils.format(
				array(bundlesSql.bundles(c).map(ReportsJaxRs::json))
			)).build();
		}
	}

	@GET
	@Path("/individuals")
	@Produces(MediaType.APPLICATION_JSON)
	public Response individuals(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try {
			try (Connection c = dataSource.getConnection()) {
				return Response.ok(ArgoUtils.format(json(bundlesSql.individuals(c)))).build();
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
				List<BucketSummary> bundles = bundlesSql.bundles(c);
				Individuals individuals = bundlesSql.individuals(c);
				return Response.ok(ArgoUtils.format(json(
					BundleLogic.logic(bundles, individuals)))).build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
	}

	private static JsonRootNode json(BundleLogic.Total total) {
		return object(
			field("actual", row(total.bundlesActual)),
			field("planned", row(total.bundlesPlanned)),
			field("individuals", row(total.individuals)),
			field("totals", row(total.total))
		);
	}

	private static JsonRootNode row(BundleLogic.Total2 actual) {
		return object(
			field("conference", number(actual.conference)),
			field("workshop1", number(actual.workshop1)),
			field("workshop2", number(actual.workshop2))
		);
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