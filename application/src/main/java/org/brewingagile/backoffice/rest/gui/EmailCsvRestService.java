package org.brewingagile.backoffice.rest.gui;

import org.brewingagile.backoffice.application.Application;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.sqlops.ResultSetMapper;
import org.brewingagile.backoffice.sqlops.SqlOps;
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
import java.util.List;

@Path("/emails/")
@NeverCache
public class EmailCsvRestService {
	private final DataSource dataSource = Application.INSTANCE.dataSource();
	private final AuthService authService = Application.INSTANCE.authService();
	
	@GET
	@Produces("text/csv")
	public Response invoices(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			ResultSetMapper<String> f2 = rs -> escaped(rs.getString("participant_name")) + "," + escaped(rs.getString("participant_email"));
			List<String> map = SqlOps.map(c, RegistrationsSqlMapper.orderBy_ParticipantName(), f2);
			return Response.ok(unlines(map)).header("content-disposition", "attachment; filename=" + "participants-" + Instant.now().toString() + ".csv").build();
		}
	}

	private static String unlines(List<String> map) {
		return map.stream().reduce((l,r) -> l + "\n" + r).orElse("");
	}

	private static String escaped(String value) {
		return "\"" + value.replace("\"", "\"\"") + "\"";
	}
}
