package org.brewingagile.backoffice.db.operations;

import java.sql.*;
import java.util.Objects;
import java.util.UUID;

import fj.Ord;
import fj.P;
import fj.P2;

import fj.P3;
import fj.data.List;
import fj.data.Option;
import fj.data.Set;
import fj.function.Strings;
import fj.function.Try1;
import functional.Tuple2;

public class RegistrationsSqlMapper {
	public List<P2<String,String>> participantNameAndEmail(Connection c) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM registration ORDER BY participant_name")) {
			Try1<ResultSet,P2<String,String>,SQLException> f = rs -> P.p(
				rs.getString("participant_name"),
				rs.getString("participant_email")
			);
			return SqlOps.list(ps, f);
		}
	}

	public List<P3<String,String,String>> diets(Connection c) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("SELECT ticket, participant_name, dietary_requirements\n" +
			"FROM registration \n" +
			"JOIN registration_ticket USING (registration_id) \n" +
			"WHERE dietary_requirements <> ''\n" +
			"ORDER BY ticket")) {
			Try1<ResultSet,P3<String,String,String>,SQLException> f = rs -> P.p(
				rs.getString("participant_name"),
				rs.getString("ticket"),
				rs.getString("dietary_requirements")
			);
			return SqlOps.list(ps, f);
		}
	}

	public Option<UUID> invoiceReferenceToRegistrationId(Connection c, UUID apiClientReference) throws SQLException {
		String sql = "SELECT * FROM registration_invoices WHERE invoice_reference_id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, apiClientReference);
			return SqlOps.one(ps, rs -> (UUID)rs.getObject("registration_id"));
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
		public final RegistrationTuple tuple;
		public final Set<String> tickets;

		public static Ord<RegistrationsSqlMapper.Registration> byBadge = Ord.ord(l -> r -> Ord.stringOrd.compare(l.tuple.badge.badge, r.tuple.badge.badge));

		public Registration(UUID id, RegistrationTuple tuple, Set<String> tickets) {
			this.id = id;
			this.tuple = tuple;
			this.tickets = tickets;
		}
	}

	public final static class RegistrationTuple {
		public final RegistrationState state;
		public final String participantName;
		public final String participantEmail;
		public final String billingCompany;
		public final String billingAddress;
		public final BillingMethod billingMethod;
		public final String dietaryRequirements;
		public final Badge badge;
		public final String twitter;
		public final Option<String> bundle;

		public RegistrationTuple(
			RegistrationState state,
			String participantName,
			String participantEmail, String billingCompany,
			String billingAddress,
			BillingMethod billingMethod,
			String dietaryRequirements,
			Badge badge,
			String twitter,
			Option<String> bundle
		) {
			this.state = state;
			this.participantName = participantName;
			this.participantEmail = participantEmail;
			this.billingCompany = billingCompany;
			this.billingAddress = billingAddress;
			this.billingMethod = billingMethod;
			this.dietaryRequirements = dietaryRequirements;
			this.badge = badge;
			this.twitter = twitter;
			this.bundle = bundle;
		}
	}

	public Option<Registration> one(Connection c, UUID id) throws SQLException {
		Option<RegistrationTuple> registration = registration(c, id);
		if (registration.isNone()) return Option.none();

		Set<String> tickets = tickets(c, id);
		return Option.some(
			new Registration(
				id, registration.some(), tickets
			)
		);
	}

	public void replace(Connection c, UUID id, Registration r) throws SQLException {
		deleteRegistrationTuple(c, id);
		deleteRegistrationTicket(c, id);
		insertRegistrationTuple(c, id, r.tuple);
		for (String ticket : r.tickets) insertRegistrationTicket(c, id, ticket);

	}

	private void insertRegistrationTicket(Connection c, UUID id, String ticket) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO registration_ticket (registration_id, ticket) VALUES (?, ?)")) {
			ps.setObject(1, id);
			ps.setString(2, ticket);
			ps.executeUpdate();
		}
	}

	private Set<String> tickets(Connection c, UUID id) throws SQLException {
		String sql = "SELECT ticket FROM registration_ticket WHERE registration_id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, id);
			return SqlOps.set(ps, Ord.stringOrd, rs -> rs.getString("ticket"));
		}
	}

	private Option<RegistrationTuple> registration(Connection c, UUID id) throws SQLException {
		String sql = "SELECT *, rb.bucket FROM registration r " +
			"LEFT JOIN registration_bucket rb USING (registration_id) " +
			"WHERE registration_id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, id);
			return SqlOps.one(ps, RegistrationsSqlMapper::toRegistrationTuple);
		}
	}

	private void deleteRegistrationTicket(Connection c, UUID id) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM registration_ticket WHERE registration_id = ?;")) {
			ps.setObject(1, id);
			ps.execute();
		}
	}

	public List<Tuple2<UUID, RegistrationTuple>> all(Connection c) throws SQLException {
		String sql = "SELECT *, rb.bucket FROM registration r " +
			"LEFT JOIN registration_bucket rb USING (registration_id) " +
			"ORDER BY participant_name";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.list(ps,
				rs -> Tuple2.of(
					(UUID) rs.getObject("registration_id"),
					toRegistrationTuple(rs)
				)
			);
		}
	}

	public List<UUID> unprintedNametags(Connection c) throws SQLException {
		String sql = "SELECT *, rb.bucket FROM registration r " +
			"LEFT JOIN registration_bucket rb USING (registration_id) " +
			"LEFT JOIN printed_nametags pn USING (registration_id) " +
			"WHERE pn.registration_id IS NULL " +
			"ORDER BY participant_name";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.list(ps,  rs -> (UUID) rs.getObject("registration_id"));
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

	private void insertRegistrationTuple(Connection c, UUID id, RegistrationTuple rt) throws SQLException {
		String sql = "INSERT INTO registration (registration_id, state, participant_name, participant_email, billing_company, billing_address, billing_method, dietary_requirements, twitter) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, id);
			ps.setString(2, rt.state.name());
			ps.setString(3, rt.participantName);
			ps.setString(4, rt.participantEmail);
			ps.setString(5, rt.billingCompany);
			ps.setString(6, rt.billingAddress);
			ps.setString(7, rt.billingMethod.name());
			ps.setString(8, rt.dietaryRequirements);
			ps.setString(9, rt.twitter);
			ps.execute();
		}
	}
	
	public static RegistrationTuple toRegistrationTuple(ResultSet rs) throws SQLException {
		return new RegistrationTuple(
			RegistrationState.valueOf(rs.getString("state")),
			rs.getString("participant_name"),
			rs.getString("participant_email"),
			rs.getString("billing_company"),
			rs.getString("billing_address"),
			BillingMethod.valueOf(rs.getString("billing_method")),
			rs.getString("dietary_requirements"),
			new Badge(rs.getString("badge")),
			rs.getString("twitter"),
			Option.fromNull(rs.getString("bucket")).filter(Strings.isNotNullOrEmpty)
		);
	}

	private static Set<String> tickets(Array a) throws SQLException {
		ResultSet rs = a.getResultSet();
		List.Buffer<String> buffer = List.Buffer.empty();
		while(rs.next()) buffer.snoc(rs.getString(0));
		return Set.iterableSet(Ord.stringOrd, buffer.toList());
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

	public void deleteRegistrationTuple(Connection c, UUID id) throws SQLException {
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

	private void updateRegistration(
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

	private void replaceRegistrationBundle(Connection c, UUID id, Option<String> bundle) throws SQLException {
		deleteRegistrationBundle(c, id);
		if (bundle.isSome()) insertRegistrationBundle(c, id, bundle.some());
	}

	private void deleteRegistrationBundle(Connection c, UUID id) throws SQLException {
		String sql = "DELETE FROM registration_bucket WHERE registration_id = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, id);
			ps.execute();
		}
	}

	private void insertRegistrationBundle(Connection c, UUID registrationId, String bucket) throws SQLException {
		String sql = "INSERT INTO registration_bucket (registration_id, bucket) VALUES (?, ?);";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, registrationId);
			ps.setString(2, bucket);
			ps.execute();
		}
	}
}
