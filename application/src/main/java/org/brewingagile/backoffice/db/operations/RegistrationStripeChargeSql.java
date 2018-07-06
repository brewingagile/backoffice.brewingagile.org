package org.brewingagile.backoffice.db.operations;

import fj.data.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.brewingagile.backoffice.instances.PreparedStatements;
import org.brewingagile.backoffice.instances.ResultSets;
import org.brewingagile.backoffice.types.ChargeId;
import org.brewingagile.backoffice.types.RegistrationId;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class RegistrationStripeChargeSql {

	public void insertCharge(Connection c, RegistrationId registrationId, Charge charge) throws SQLException {
		String sql = "INSERT INTO registration_stripe_charge (registration_id, charge_id, amount, \"when\") VALUES (?, ?, ?, ?);";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, registrationId);
			ps.setString(2, charge.chargeId.value);
			ps.setBigDecimal(3, charge.amount);
			ps.setTimestamp(4, Timestamp.from(charge.when));
			ps.execute();
		}
	}

	public void insertChargeReceipt(Connection c, ChargeId chargeId, byte[] receiptPdfSource) throws SQLException {
		String sql = "INSERT INTO registration_stripe_charge_receipt (charge_id, receipt_pdf_source) VALUES (?, ?);";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, chargeId.value);
			ps.setBytes(2, receiptPdfSource);
			ps.execute();
		}
	}

	public List<Charge> byRegistration(Connection c, RegistrationId registrationId) throws SQLException {
		String sql = "SELECT * FROM stripe_charge WHERE registration_id = ? ORDER BY \"when\";";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, registrationId);
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