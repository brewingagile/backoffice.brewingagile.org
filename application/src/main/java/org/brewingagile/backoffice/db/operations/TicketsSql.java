package org.brewingagile.backoffice.db.operations;

import fj.Ord;
import fj.data.List;
import fj.data.Set;
import org.brewingagile.backoffice.instances.PreparedStatements;
import org.brewingagile.backoffice.types.RegistrationId;
import org.brewingagile.backoffice.types.TicketName;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

public class TicketsSql {

	public final static class Ticket {
		public final TicketName ticket;
		public final String productText;
		public final BigDecimal price;
		public final int seats;

		public Ticket(
			TicketName ticket,
			String productText,
			BigDecimal price,
			int seats
		) {
			this.ticket = requireNonNull(ticket);
			this.productText = requireNonNull(productText);
			this.price = requireNonNull(price);
			this.seats = seats;
		}
	}

	public List<Ticket> all(Connection c) throws SQLException {
		String sql = "SELECT * FROM ticket ORDER BY ticket;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.list(ps, TicketsSql::rsTicket);
		}
	}

	private static Ticket rsTicket(ResultSet rs) throws SQLException {
		return new Ticket(
			TicketName.ticketName(rs.getString("ticket")),
			rs.getString("product_text"),
			rs.getBigDecimal("price"),
			rs.getInt("seats")
		);
	}

	public void replace(Connection c, List<Ticket> xs) throws SQLException {
		SqlOps.deferAll(c); delete(c);
		for (Ticket x : xs) {
			insert(c, x);
		}
	}

	private void delete(Connection connection) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM ticket;")) {
			statement.executeUpdate();
		}
	}

	private void insert(Connection connection, Ticket ticket) throws SQLException {
		String sql = "INSERT INTO ticket (ticket, product_text, price, seats) VALUES (?,?,?,?);";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, ticket.ticket.ticketName);
			ps.setString(2, ticket.productText);
			ps.setBigDecimal(3, ticket.price);
			ps.setInt(4, ticket.seats);
			ps.executeUpdate();
		}
	}

	public Set<Ticket> by(Connection c, RegistrationId registrationId) throws SQLException {
		String sql = "SELECT ticket.* FROM registration_ticket JOIN ticket USING (ticket) WHERE registration_id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, registrationId);
			return SqlOps.set(ps, Ord.hashEqualsOrd(), TicketsSql::rsTicket);
		}
	}
}