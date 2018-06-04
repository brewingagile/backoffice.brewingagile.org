package org.brewingagile.backoffice.rest.gui;

import fj.F;
import fj.data.IO;
import fj.data.List;
import fj.data.Option;
import fj.function.Strings;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.types.TicketName;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

@Path("/exports")
@NeverCache
public class ExportsJaxRs {
	private final DataSource dataSource;
	private final AuthService authService;
	private final RegistrationsSqlMapper registrationsSqlMapper;

	public ExportsJaxRs(
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

//	curl -u admin:password http://localhost:9080/gui/exports/registrations?ticket=workshop1
	@GET
	@Path("/registrations")
	@Produces("text/csv")
	public Response registrations(@Context HttpServletRequest request, @QueryParam("ticket") String ticket0) throws SQLException, IOException {
		authService.guardAuthenticatedUser(request);
		Option<TicketName> ticket1 = Option.fromNull(ticket0).map(TicketName::ticketName);
		try (Connection c = dataSource.getConnection()) {
			List<UUID> all = registrationsSqlMapper.all(c).map(x -> x._1());
			List<RegistrationsSqlMapper.Registration> somes = Option.somes(all.traverseIO(ioify(c)).run());
			List<RegistrationsSqlMapper.Registration> filtered = somes.filter(x -> ticket1.map(t -> x.tickets.member(t)).orSome(true));
			return Response.ok(Strings.unlines(
				filtered.sort(RegistrationsSqlMapper.Registration.byBadge)
					.map(reg -> {
						RegistrationsSqlMapper.RegistrationTuple rt = reg.tuple;
						String ticketName = reg.tickets.toList().map(x -> x.ticketName).foldLeft1((l,r) -> l + "+" + r);
						return escaped(rt.badge.badge) + "," + escaped(ticketName) + "," + escaped(rt.participantName) + "," + escaped(rt.dietaryRequirements) + "," + escaped(rt.participantEmail);
					})
			)).header("content-disposition", "attachment; filename=" + ticket1.map(x -> x.ticketName).orSome("registrations") + "-" + Instant.now().toString() + ".csv").build();
		}
	}

	private F<UUID,IO<Option<RegistrationsSqlMapper.Registration>>> ioify(Connection c) {
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
