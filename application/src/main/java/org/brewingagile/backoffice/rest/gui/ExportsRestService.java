package org.brewingagile.backoffice.rest.gui;

import fj.F;
import fj.F2;
import fj.Ord;
import fj.Ordering;
import fj.data.Array;
import fj.data.IO;
import fj.data.List;
import fj.data.Option;
import fj.function.Strings;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

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

//	curl -u admin:password http://localhost:9080/gui/exports/emails
	@GET
	@Path("/emails")
	@Produces("text/csv")
	public Response emails(@Context HttpServletRequest request) throws SQLException {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			return Response.ok(Strings.unlines(
				registrationsSqlMapper.participantNameAndEmail(c)
					.map(x -> escaped(x._1()) + "," + escaped(x._2()))
			)).header("content-disposition", "attachment; filename=" + "emails-" + Instant.now().toString() + ".csv").build();
		}
	}

//	curl -u admin:password http://localhost:9080/gui/exports/registrations
	@GET
	@Path("/registrations")
	@Produces("text/csv")
	public Response registrations(@Context HttpServletRequest request) throws SQLException, IOException {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			List<UUID> all = registrationsSqlMapper.all(c).map(x -> x._1);
			List<RegistrationsSqlMapper.Registration> somes = Option.somes(all.traverseIO(ioify(c)).run());
//			List<RegistrationsSqlMapper.Registration> filtered = somes.filter(x -> x.tickets.member(ticket));
			return Response.ok(Strings.unlines(
				somes.sort(RegistrationsSqlMapper.Registration.byBadge)
					.map(reg -> {
						F2<String, String, String> stringStringStringF2 = (String l, String r) -> l + "+" + r;
						RegistrationsSqlMapper.RegistrationTuple rt = reg.tuple;
						return escaped(reg.tuple.badge.badge) + "," + escaped(reg.tickets.toList().foldLeft1(stringStringStringF2)) + "," + escaped(rt.participantName) + "," + escaped(rt.dietaryRequirements);
					})
			)).header("content-disposition", "attachment; filename=" + "registrations-" + Instant.now().toString() + ".csv").build();
		}
	}

	private F<UUID,IO<Option<RegistrationsSqlMapper.Registration>>> ioify(Connection c) throws IOException {
		return registrationId -> () -> {
			try {
				return registrationsSqlMapper.one(c, registrationId);
			} catch (SQLException e) {
				throw new IOException(e);
			}
		};
	}

//	curl -u admin:password http://localhost:9080/gui/exports/diets
	@GET
	@Path("/diets")
	@Produces("text/csv")
	public Response diets(@Context HttpServletRequest request) throws SQLException {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			return Response.ok(Strings.unlines(
				registrationsSqlMapper.diets(c)
					.map(x -> escaped(x._1()) + "," + escaped(x._2()) + "," + escaped(x._3()))
			)).header("content-disposition", "attachment; filename=" + "diets-" + Instant.now().toString() + ".csv").build();
		}
	}

	private static String escaped(String value) {
		return "\"" + value.replace("\"", "\"\"") + "\"";
	}
}
