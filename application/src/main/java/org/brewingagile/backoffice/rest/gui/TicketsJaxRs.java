package org.brewingagile.backoffice.rest.gui;

import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.saj.InvalidSyntaxException;
import fj.data.Collectors;
import fj.data.List;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.TicketsSql;
import org.brewingagile.backoffice.db.operations.TicketsSql.Ticket;
import org.brewingagile.backoffice.rest.json.ToJson;
import org.brewingagile.backoffice.utils.ArgoUtils;
import org.brewingagile.backoffice.utils.jersey.NeverCache;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;

import static argo.jdom.JsonNodeFactories.*;
import static org.brewingagile.backoffice.db.operations.TicketsSql.TicketName.ticketName;

@Path("tickets")
@NeverCache
public class TicketsJaxRs {
	private final DataSource dataSource;
	private final AuthService authService;
	private final TicketsSql ticketsSql;

	public TicketsJaxRs(
		DataSource dataSource,
		AuthService authService,
		TicketsSql ticketsSql
	) {
		this.dataSource = dataSource;
		this.authService = authService;
		this.ticketsSql = ticketsSql;
	}

	//	curl -u admin:password "http://localhost:9080/gui/tickets/"
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@Context HttpServletRequest request) throws Exception {
		authService.guardAuthenticatedUser(request);
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			return Response.ok(ArgoUtils.format(
				array(ticketsSql.all(c).map(TicketsJaxRs::json))
			)).build();
		}
	}

	private static JsonRootNode json(Ticket t) {
		return object(
			field("ticket", ToJson.json(t.ticket)),
			field("productText", string(t.productText)),
			field("price", number(t.price)),
			field("seats", number(t.seats))
		);
	}

	//	curl -u admin:password -X PUT -H "Content-Type: application/json" "http://localhost:9080/gui/tickets/" --data '...'
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response put(@Context HttpServletRequest request, String json) throws InvalidSyntaxException, SQLException {
		authService.guardAuthenticatedUser(request);

		try {

			List<Ticket> bs = unJson(json);
			try (Connection c = dataSource.getConnection()) {
				c.setAutoCommit(false);
				ticketsSql.replace(c, bs);
				c.commit();
			}
			return Response.ok().build();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}

	private static List<Ticket> unJson(String json) throws InvalidSyntaxException {
		return ArgoUtils.parse(json)
			.getArrayNode()
			.stream()
			.map(TicketsJaxRs::ticket)
			.collect(Collectors.toList());
	}

	private static Ticket ticket(JsonNode node) {
		return new Ticket(
			ticketName(node.getStringValue("ticket")),
			node.getStringValue("productText"),
			new BigDecimal(node.getNumberValue("price")),
			new BigInteger(node.getNumberValue("seats")).intValue()
		);
	}
}
