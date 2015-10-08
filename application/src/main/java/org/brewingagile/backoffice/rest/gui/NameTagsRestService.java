package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonRootNode;
import fj.Ord;
import fj.data.Set;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.Registration;
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

@Path("/webapp/nametags/")
@NeverCache
public class NameTagsRestService {
	private final DataSource dataSource;
	private final AuthService authService;
	private final RegistrationsSqlMapper registrationsSqlMapper;

	public NameTagsRestService(
		DataSource dataSource,
		AuthService authService,
		RegistrationsSqlMapper registrationsSqlMapper
	) {
		this.dataSource = dataSource;
		this.authService = authService;
		this.registrationsSqlMapper = registrationsSqlMapper;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response invoices(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			return Response.ok(ArgoUtils.format(
				array(registrationsSqlMapper.all(c).map(NameTagsRestService::json))
			)).build();
		}
	}

	private static JsonRootNode json(Registration r) {
		return object(
			field("name", string(r.participantName)),
			field("company", string(r.billingCompany)),
			field("badge", string(r.badge.badge)),
			field("workshop", booleanNode(Set.set(Ord.stringOrd, "conference+workshop", "conference+workshop2").member(r.ticket))),
			field("conference", booleanNode(true)),
			field("burger", booleanNode(true)),
			field("twitter", string(r.twitter)),
			field("diet", booleanNode(!"".equals(r.dietaryRequirements)))
		);
	}
}
