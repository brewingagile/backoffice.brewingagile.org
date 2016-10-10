package org.brewingagile.backoffice.db.operations;

import fj.data.List;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BudgetSql {
	public final static class FixedCost {
		public final String cost;
		public final BigDecimal amount;

		public FixedCost(
			String cost,
			BigDecimal amount
		) {
			this.cost = cost;
			this.amount = amount;
		}
	}

	public List<FixedCost> fixedCosts(Connection c) throws SQLException {
		String sql = "SELECT * FROM budget_fixed_costs ORDER BY cost;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.list(ps, rs -> new FixedCost(
				rs.getString("cost"),
				rs.getBigDecimal("amount")
			));
		}
	}

	public void replace(Connection c, List<FixedCost> bs) throws SQLException {
		delete(c); for (FixedCost b : bs) insert(c, b);
	}

	private void delete(Connection connection) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM budget_fixed_costs;")) {
			statement.executeUpdate();
		}
	}

	private void insert(Connection connection, FixedCost fixedCost) throws SQLException {
		String sql = "INSERT INTO budget_fixed_costs (cost, amount) VALUES (?,?);";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, fixedCost.cost);
			statement.setBigDecimal(2, fixedCost.amount);
			statement.executeUpdate();
		}
	}
}