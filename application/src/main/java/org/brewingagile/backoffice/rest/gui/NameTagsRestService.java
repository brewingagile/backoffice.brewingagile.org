package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonRootNode;
import fj.F;
import fj.data.IO;
import fj.data.List;
import fj.data.Option;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.Registration;
import org.brewingagile.backoffice.db.operations.TicketsSql;
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
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static argo.jdom.JsonNodeFactories.*;
import static org.brewingagile.backoffice.db.operations.TicketsSql.TicketName.ticketName;

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
			List<UUID> registrationTuples = registrationsSqlMapper.unprintedNametags(c);
			List<Registration> somes = Option.somes(registrationTuples.traverseIO(ioify(c)).run());
			return Response.ok(ArgoUtils.format(array(somes.map(NameTagsRestService::json)))).build();
		}
	}

	private F<UUID,IO<Option<Registration>>> ioify(Connection c) throws IOException {
		return registrationId -> () -> {
			try {
				return registrationsSqlMapper.one(c, registrationId);
			} catch (SQLException e) {
				throw new IOException(e);
			}
		};
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
			field("name", string(r.tuple.participantName)),
			field("company", string(r.tuple.billingCompany)),
			field("badge", string(r.tuple.badge.badge)),
			field("workshop", booleanNode(false)),
			field("conference", booleanNode(r.tickets.member(ticketName("conference")))),
			field("burger", booleanNode(true)),
			field("twitter", string(r.tuple.twitter)),
			field("diet", booleanNode(!"".equals(r.tuple.dietaryRequirements)))
		);
	}
}
