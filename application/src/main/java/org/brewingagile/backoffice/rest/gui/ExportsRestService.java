package org.brewingagile.backoffice.rest.gui;

import fj.function.Strings;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.time.Instant;

@Path("/exports")
@NeverCache
public class ExportsRestService {
	private final DataSource dataSource;
	private final AuthService authService;
	private final RegistrationsSqlMapper registrationsSqlMapper;

	public ExportsRestService(
		DataSource dataSource,
		AuthService authService,
		RegistrationsSqlMapper registrationsSqlMapper
	) {
		this.dataSource = dataSource;
		this.authService = authService;
		this.registrationsSqlMapper = registrationsSqlMapper;
	}

	@GET
	@Path("/emails")
	@Produces("text/csv")
	public Response emails(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			return Response.ok(Strings.unlines(
				RegistrationsSqlMapper.participantNameAndEmail(c)
					.map(x -> escaped(x._1()) + "," + escaped(x._2()))
			)).header("content-disposition", "attachment; filename=" + "emails-" + Instant.now().toString() + ".csv").build();
		}
	}

	@GET
	@Path("/registrations")
	@Produces("text/csv")
	public Response registrations(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			return Response.ok(Strings.unlines(
				registrationsSqlMapper.all(c)
					.map(r -> escaped(r.badge.badge) + "," + escaped(r.ticket) + "," + escaped(r.participantName) + "," + escaped(r.dietaryRequirements))
			)).header("content-disposition", "attachment; filename=" + "registrations-" + Instant.now().toString() + ".csv").build();
		}
	}

	@GET
	@Path("/diets")
	@Produces("text/csv")
	public Response diets(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			return Response.ok(Strings.unlines(
				RegistrationsSqlMapper.diets(c)
					.map(x -> escaped(x._1()) + "," + escaped(x._2()) + "," + escaped(x._3()))
			)).header("content-disposition", "attachment; filename=" + "diets-" + Instant.now().toString() + ".csv").build();
		}
	}

	private static String escaped(String value) {
		return "\"" + value.replace("\"", "\"\"") + "\"";
	}
}
