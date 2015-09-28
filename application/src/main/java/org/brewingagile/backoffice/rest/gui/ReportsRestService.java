package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonRootNode;
import org.brewingagile.backoffice.application.Application;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.BucketsSqlMapper;
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

import static org.brewingagile.backoffice.db.operations.BucketsSqlMapper.BucketSummary;
import static org.brewingagile.backoffice.db.operations.BucketsSqlMapper.Individuals;

@Path("/reports/")
@NeverCache
public class ReportsRestService {
	private final DataSource dataSource = Application.INSTANCE.dataSource();
	private final AuthService authService = Application.INSTANCE.authService();
	private final BucketsSqlMapper bucketsSqlMapper = Application.INSTANCE.bucketsSqlMapper;

	//curl -u admin:password http://localhost:9080/ba-backoffice/gui/reports/bundles  | jq .

	@GET
	@Path("/bundles")
	@Produces(MediaType.APPLICATION_JSON)
	public Response bundless(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			return Response.ok(ArgoUtils.format(
				bucketsSqlMapper.bundles(c).stream().map(ReportsRestService::json).collect(ArgoUtils.toArray())
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
				return Response.ok(ArgoUtils.format(json(bucketsSqlMapper.individuals(c)))).build();
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