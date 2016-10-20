package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.saj.InvalidSyntaxException;
import fj.data.Collectors;
import fj.data.List;
import fj.data.Option;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.BundlesSql;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;

import static argo.jdom.JsonNodeFactories.*;

import static org.brewingagile.backoffice.db.operations.BundlesSql.Bucket;
import static org.brewingagile.backoffice.rest.gui.ToJson.nullable;

@Path("buckets")
@NeverCache
public class BundlesJaxRs {
	private final DataSource dataSource;
	private final AuthService authService;
	private final BundlesSql bundlesSql;

	public BundlesJaxRs(
		DataSource dataSource,
		AuthService authService,
		BundlesSql bundlesSql
	) {
		this.dataSource = dataSource;
		this.authService = authService;
		this.bundlesSql = bundlesSql;
	}

	//	curl -u admin:password "http://localhost:9080/ba-backoffice/gui/buckets/"
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			return Response.ok(ArgoUtils.format(
				array(bundlesSql.all(c).map(BundlesJaxRs::json))
			)).build();
		}
	}

	private static JsonRootNode json(Bucket b) {
		return object(
			field("bucket" ,string(b.bucket)),
			field("conference", number(b.conference)),
			field("workshop1", number(b.workshop1)),
			field("workshop2", number(b.workshop2)),
			field("deal", nullable(b.deal, x ->
				object(field("price", number(x.price)))
			))
		);
	}

	//	curl -u admin:password -X PUT -H "Content-Type: application/json" "http://localhost:9080/ba-backoffice/gui/buckets/" --data '...'
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response put(@Context HttpServletRequest request, String json) throws InvalidSyntaxException, SQLException {
		authService.guardAuthenticatedUser(request);

		List<Bucket> bs = unJson(json);
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			bundlesSql.replace(c, bs);
			c.commit();
		}
		return Response.ok().build();
	}

	private static List<Bucket> unJson(String json) throws InvalidSyntaxException {
		return ArgoUtils.parse(json)
			.getArrayNode()
			.stream()
			.map(BundlesJaxRs::unbucket)
			.collect(Collectors.toList());
	}

	private static Bucket unbucket(JsonNode node) {
		return new BundlesSql.Bucket(
			node.getStringValue("bucket"),
			new BigInteger(node.getNumberValue("conference")).intValue(),
			new BigInteger(node.getNumberValue("workshop1")).intValue(),
			new BigInteger(node.getNumberValue("workshop2")).intValue(),
			Option.fromNull(getNullableNode(node, "deal")).map(x -> new BundlesSql.Deal(
				new BigDecimal(x.getNumberValue("price"))
			))
		);
	}

	private static JsonNode getNullableNode(JsonNode node, String f) {
		if (node.isNullNode("deal")) return null;
		return node.getNode(f);
	}
}
