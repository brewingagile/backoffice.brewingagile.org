package org.brewingagile.backoffice.rest.gui;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.brewingagile.backoffice.application.Application;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.BucketsSqlMapper;
import org.brewingagile.backoffice.utils.JsonReaderWriter;
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

import static org.brewingagile.backoffice.db.operations.BucketsSqlMapper.BucketSummary;
import static org.brewingagile.backoffice.db.operations.BucketsSqlMapper.Individuals;

@Path("/reports/")
@NeverCache
public class ReportsRestService {
	private final JsonReaderWriter jsonReaderWriter = new JsonReaderWriter();
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
			ArrayNode collect = bucketsSqlMapper.bundles(c).stream().map(ReportsRestService::json).collect(JsonReaderWriter.toArrayNode());
			return Response.ok(jsonReaderWriter.serialize(collect)).build();
		}
	}

	@GET
	@Path("/individuals")
	@Produces(MediaType.APPLICATION_JSON)
	public Response individuals(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try {
			try (Connection c = dataSource.getConnection()) {
				return Response.ok(jsonReaderWriter.serialize(json(bucketsSqlMapper.individuals(c)))).build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
	}

	public static ObjectNode json(BucketSummary b) {
		ObjectNode planned = JsonNodeFactory.instance.objectNode()
			.put("conference", b.bucket.conference)
			.put("workshop1", b.bucket.workshop1)
			.put("workshop2", b.bucket.workshop2);

		ObjectNode actual = JsonNodeFactory.instance.objectNode()
			.put("conference", b.actualConference)
			.put("workshop1", b.actualWorkshop1)
			.put("workshop2", b.actualWorkshop2);

		return JsonNodeFactory.instance.objectNode()
			.put("bundle", b.bucket.bucket)
			.putPOJO("planned", planned)
			.putPOJO("actual", actual);
	}

	public static ObjectNode json(Individuals i) {
		return JsonNodeFactory.instance.objectNode()
			.put("conference", i.conference)
			.put("workshop1", i.workshop1)
			.put("workshop2", i.workshop2);
	}
}