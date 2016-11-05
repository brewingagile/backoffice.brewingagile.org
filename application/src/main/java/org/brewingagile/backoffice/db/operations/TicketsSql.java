package org.brewingagile.backoffice.db.operations;

import fj.data.List;
import org.brewingagile.backoffice.utils.Strings;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class TicketsSql {
	public final static class TicketName {
		public final String ticketName;

		private TicketName(String ticketName) {
			this.ticketName = Objects.requireNonNull(Strings.emptyToNull(ticketName));
		}

		public static TicketName ticketName(String s) {
			return new TicketName(s);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			TicketName that = (TicketName) o;

			return ticketName.equals(that.ticketName);

		}

		@Override
		public int hashCode() {
			return ticketName.hashCode();
		}

		@Override
		public String toString() {
			return "(TicketName " + ticketName + ")";
		}
	}

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
}