package org.brewingagile.backoffice.rest.gui;

import java.sql.Connection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableSet;
import org.brewingagile.backoffice.application.Application;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.sqlops.ResultSetMapper;
import org.brewingagile.backoffice.sqlops.SqlOps;
import org.brewingagile.backoffice.utils.JsonReaderWriter;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

@Path("/nametags/")
@NeverCache
public class NameTagsRestService {
	private final JsonReaderWriter jsonReaderWriter = new JsonReaderWriter();
	private final DataSource dataSource = Application.INSTANCE.dataSource();
	private final AuthService authService = Application.INSTANCE.authService();
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response invoices(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		
		try (Connection c = dataSource.getConnection()) {
			ResultSetMapper<ObjectNode> f = rs -> JsonNodeFactory.instance.objectNode()
					.put("name", rs.getString("participant_name"))
					.put("company", rs.getString("billing_company"))
					.put("badge", rs.getString("badge"))
					.put("workshop", (ImmutableSet.of("conference+workshop", "conference+workshop2").contains(rs.getString("ticket"))))
					.put("conference", true)
					.put("burger", true)
					.put("twitter", rs.getString("twitter"))
					.put("diet", (!"".equals(rs.getString("dietary_requirements"))));
			
			List<ObjectNode> tags = SqlOps.map(c, RegistrationsSqlMapper.orderBy_ParticipantName(), f);
			return Response.ok(jsonReaderWriter.serialize(tags)).build();
		}
	}
}
