package org.brewingagile.backoffice.db.operations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import fj.P;
import fj.P2;

import fj.P3;
import fj.data.List;
import fj.data.Option;
import fj.function.Strings;
import fj.function.Try1;

public class RegistrationsSqlMapper {
	public static List<P2<String,String>> participantNameAndEmail(Connection c) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM registration ORDER BY participant_name")) {
			Try1<ResultSet,P2<String,String>,SQLException> f = rs -> P.p(
				rs.getString("participant_name"),
				rs.getString("participant_email")
			);
			return SqlOps.list(ps, f);
		}
	}

	public static List<P3<String,String,String>> diets(Connection c) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("SELECT participant_name, ticket, dietary_requirements FROM registration WHERE dietary_requirements <> ''")) {
			Try1<ResultSet,P3<String,String,String>,SQLException> f = rs -> P.p(
				rs.getString("participant_name"),
				rs.getString("ticket"),
				rs.getString("dietary_requirements")
			);
			return SqlOps.list(ps, f);
		}
	}

	public enum BillingMethod {EMAIL, SNAILMAIL}

	public static final class Badge {
		public final String badge;

		public Badge(String badge) {
			this.badge = Objects.requireNonNull(badge);
		}
	}

	public static final class BillingCompany {
		public final String value;

		public BillingCompany(String value) {
			this.value = Objects.requireNonNull(value);
		}
	}

	public final static class Registration {
		public final UUID id;
		public final RegistrationState state;
		public final String participantName;
		public final String participantEmail;
		public final String billingCompany;
		public final String billingAddress;
		public final BillingMethod billingMethod;
		public final String ticket;
		public final String dietaryRequirements;
		public final Badge badge;
		public final String twitter;
		public final Option<String> bundle;

		public Registration(UUID id, RegistrationState state, String participantName,
				String participantEmail, String billingCompany,
				String billingAddress,
				BillingMethod billingMethod, 
				String ticket, String dietaryRequirements,
				Badge badge,
				String twitter,
				Option<String> bundle
				) {
			this.id = id;
			this.state = state;
			this.participantName = participantName;
			this.participantEmail = participantEmail;
			this.billingCompany = billingCompany;
			this.billingAddress = billingAddress;
			this.billingMethod = billingMethod;
			this.ticket = ticket;
			this.dietaryRequirements = dietaryRequirements;
			this.badge = badge;
			this.twitter = twitter;
			this.bundle = bundle;
		}
	}

	public Option<Registration> one(Connection c, UUID id) throws SQLException {
		String sql = "SELECT *, rb.bucket FROM registration r " +
			"LEFT JOIN registration_bucket rb USING (registration_id) " +
			"WHERE registration_id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, id);
			return SqlOps.one(ps, RegistrationsSqlMapper::toRegistration);
		}
	}
	
	public List<Registration> all(Connection c) throws SQLException {
		String sql = "SELECT *, rb.bucket FROM registration r " +
			"LEFT JOIN registration_bucket rb USING (registration_id) " +
			"ORDER BY participant_name";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.list(ps, RegistrationsSqlMapper::toRegistration);
		}
	}

	public List<Registration> unprintedNametags(Connection c) throws SQLException {
		String sql = "SELECT *, rb.bucket FROM registration r " +
			"LEFT JOIN registration_bucket rb USING (registration_id) " +
			"LEFT JOIN printed_nametags pn USING (registration_id) " +
			"WHERE pn.registration_id IS NULL " +
			"ORDER BY participant_name";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.list(ps, RegistrationsSqlMapper::toRegistration);
		}
	}

	public boolean printedNametag(Connection c, UUID id) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM printed_nametags WHERE registration_id = ?")) {
			ps.setObject(1, id);
			try (ResultSet r = ps.executeQuery()) {
				return r.next();
			}
		}
	}

	public void insertPrintedNametag(Connection c, UUID id) throws SQLException {
		if (printedNametag(c, id)) return;
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO printed_nametags (registration_id) VALUES (?)")) {
			ps.setObject(1, id);
			ps.execute();
		}
	}

	public void updateRegistrationState(Connection c, UUID id, RegistrationState oldState, RegistrationState nextState) throws SQLException {
		String sql = "UPDATE registration SET state = ? WHERE (registration_id = ? AND state = ?);";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, nextState.name());
			ps.setObject(2, id);
			ps.setString(3, oldState.name());
			ps.execute();
		}
	}

	public void insert(Connection c, UUID id, RegistrationState state, String participantName, String participantEmail,
			String billingCompany, String billingAddress, BillingMethod billingMethod, 
			String ticket, String dietaryRequirements, String twitter) throws SQLException {
		String sql = "INSERT INTO registration (registration_id, state, participant_name, participant_email, billing_company, billing_address, billing_method, ticket, dietary_requirements, twitter) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, id);
			ps.setString(2, state.name());
			ps.setString(3, participantName);
			ps.setString(4, participantEmail);
			ps.setString(5, billingCompany);
			ps.setString(6, billingAddress);
			ps.setString(7, billingMethod.name());
			ps.setString(8, ticket);
			ps.setString(9, dietaryRequirements);
			ps.setString(10, twitter);
			ps.execute();
		}
	}
	
	public static Registration toRegistration(ResultSet rs) throws SQLException {
		return new Registration(
			(UUID) rs.getObject("registration_id"),
			RegistrationState.valueOf(rs.getString("state")),
			rs.getString("participant_name"),
			rs.getString("participant_email"),
			rs.getString("billing_company"),
			rs.getString("billing_address"),
			BillingMethod.valueOf(rs.getString("billing_method")),
			rs.getString("ticket"),
			rs.getString("dietary_requirements"),
			new Badge(rs.getString("badge")),
			rs.getString("twitter"),
			Option.fromNull(rs.getString("bucket")).filter(Strings.isNotNullOrEmpty)
		);
	}

	public void insertInvoiceReference(Connection c, UUID registrationId, UUID invoiceReferenceId) throws SQLException {
		String sql = "INSERT INTO registration_invoices (registration_id, invoice_reference_id) VALUES (?, ?)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, registrationId);
			ps.setObject(2, invoiceReferenceId);
			ps.execute();
		}
	}

	public Option<UUID> invoiceReference(Connection c, UUID invoiceId) throws SQLException {
		String sql = "SELECT * FROM registration_invoices WHERE registration_id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, invoiceId);
			Try1<ResultSet, UUID, SQLException> f = r -> {
				try {
					return (UUID) r.getObject("invoice_reference_id");
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
			};
			return SqlOps.one(ps, f);
		}
	}

	public void delete(Connection c, UUID id) throws SQLException {
		String sql = "DELETE FROM registration WHERE registration_id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, id);
			ps.execute();
		}
	}

	public void update(Connection c, UUID id, BillingCompany billingCompany, Badge badge, String diet, Option<String> bundle) throws SQLException {
		replaceRegistrationBundle(c, id, bundle);
		updateRegistration(c, id, billingCompany, badge, diet);
	}

	private static void updateRegistration(
		Connection c,
		UUID id,
		BillingCompany billingCompany,
		Badge badge,
		String diet
	) throws SQLException {
		String sql = "UPDATE registration SET billing_company = ?, badge = ?, dietary_requirements = ? WHERE registration_id = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, billingCompany.value);
			ps.setString(2, badge.badge);
			ps.setString(3, diet);
			ps.setObject(4, id);
			ps.execute();
		}
	}

	private static void replaceRegistrationBundle(Connection c, UUID id, Option<String> bundle) throws SQLException {
		deleteRegistrationBundle(c, id);
		if (bundle.isSome()) insertRegistrationBundle(c, id, bundle.some());
	}

	private static void deleteRegistrationBundle(Connection c, UUID id) throws SQLException {
		String sql = "DELETE FROM registration_bucket WHERE registration_id = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, id);
			ps.execute();
		}
	}

	private static void insertRegistrationBundle(Connection c, UUID registrationId, String bucket) throws SQLException {
		String sql = "INSERT INTO registration_bucket (registration_id, bucket) VALUES (?, ?);";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, registrationId);
			ps.setString(2, bucket);
			ps.execute();
		}
	}
}
