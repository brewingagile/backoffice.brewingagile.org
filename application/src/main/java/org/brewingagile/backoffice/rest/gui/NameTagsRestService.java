package org.brewingagile.backoffice.rest.gui;

import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import argo.jdom.JsonRootNode;
import com.google.common.collect.ImmutableSet;
import org.brewingagile.backoffice.application.Application;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.sqlops.ResultSetMapper;
import org.brewingagile.backoffice.sqlops.SqlOps;
import org.brewingagile.backoffice.utils.ArgoUtils;

import static argo.jdom.JsonNodeFactories.*;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

@Path("/nametags/")
@NeverCache
public class NameTagsRestService {
	private final DataSource dataSource = Application.INSTANCE.dataSource();
	private final AuthService authService = Application.INSTANCE.authService();
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response invoices(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		
		try (Connection c = dataSource.getConnection()) {
			ResultSetMapper<JsonRootNode> f = rs -> object(
					field("name", string(rs.getString("participant_name"))),
					field("company", string(rs.getString("billing_company"))),
					field("badge", string(rs.getString("badge"))),
					field("workshop", booleanNode(ImmutableSet.of("conference+workshop", "conference+workshop2").contains(rs.getString("ticket")))),
					field("conference", booleanNode(true)),
					field("burger", booleanNode(true)),
					field("twitter", string(rs.getString("twitter"))),
					field("diet", booleanNode(!"".equals(rs.getString("dietary_requirements"))))
			);

			JsonRootNode tags = SqlOps.map(c, RegistrationsSqlMapper.orderBy_ParticipantName(), f).stream().collect(ArgoUtils.toArray());
			return Response.ok(ArgoUtils.format(tags)).build();
		}
	}
}
