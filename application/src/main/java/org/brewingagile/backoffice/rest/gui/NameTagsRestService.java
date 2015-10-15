package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonRootNode;
import fj.Ord;
import fj.data.Option;
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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.util.UUID;

import static argo.jdom.JsonNodeFactories.*;

@Path("/nametags/")
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

//	curl -u admin:password -v http://localhost:9080/gui/nametags/

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response all(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			return Response.ok(ArgoUtils.format(
				array(registrationsSqlMapper.all(c).map(NameTagsRestService::json))
			)).build();
		}
	}

//	curl -u admin:password -v http://localhost:9080/gui/nametags/8e57b3d5-2a50-47a5-853f-bdb76cdbbb62

	@GET
	@Path("{registration_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response one(@Context HttpServletRequest request, @PathParam("registration_id") UUID registrationId) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			return registrationsSqlMapper.one(c, registrationId)
				.map(r -> Response.ok(ArgoUtils.format(array(json(r)))))
				.orSome(Response.status(Response.Status.NOT_FOUND))
				.build();
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
