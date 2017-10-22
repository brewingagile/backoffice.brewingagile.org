package org.brewingagile.backoffice.db.operations;

import fj.data.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.brewingagile.backoffice.instances.PreparedStatements;
import org.brewingagile.backoffice.instances.ResultSets;
import org.brewingagile.backoffice.types.Account;
import org.brewingagile.backoffice.types.ChargeId;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class StripeChargeSql {

	public void insertCharge(Connection c, Account account, Charge charge) throws SQLException {
		String sql = "INSERT INTO stripe_charge (account, charge_id, amount, \"when\") VALUES (?, ?, ?, ?);";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, account);
			ps.setString(2, charge.chargeId.value);
			ps.setBigDecimal(3, charge.amount);
			ps.setTimestamp(4, Timestamp.from(charge.when));
			ps.execute();
		}
	}

	public List<Charge> byAccount(Connection c, Account account) throws SQLException {
		String sql = "SELECT * FROM stripe_charge WHERE account = ? ORDER BY \"when\";";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, account);
			return SqlOps.list(ps, rs -> new Charge(
				ResultSets.chargeId(rs, "charge_id"),
				rs.getBigDecimal("amount"),
				rs.getTimestamp("when").toInstant()
			));
		}
	}

	@ToString
	@EqualsAndHashCode
	public static final class Charge {
		public final ChargeId chargeId;
		public final BigDecimal amount;
		public final Instant when;

		public Charge(ChargeId chargeId, BigDecimal amount, Instant when) {
			this.chargeId = chargeId;
			this.amount = amount;
			this.when = when;
		}
	}
}