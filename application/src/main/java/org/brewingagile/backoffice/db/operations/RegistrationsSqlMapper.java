package org.brewingagile.backoffice.db.operations;

import fj.*;
import fj.data.List;
import fj.data.Option;
import fj.data.Set;
import fj.function.Try1;
import functional.Tuple2;
import org.brewingagile.backoffice.instances.ResultSets;
import org.brewingagile.backoffice.types.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.UUID;

import static org.brewingagile.backoffice.instances.PreparedStatements.set;
import static org.brewingagile.backoffice.types.TicketName.ticketName;

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

	public final static class Registration {
		public final UUID id;
		public final RegistrationTuple tuple;
		public final Set<TicketName> tickets;
		public final Option<PrintedNametag> printedNametag;

		public static Ord<RegistrationsSqlMapper.Registration> byBadge = Ord.ord(l -> r -> Ord.stringOrd.compare(l.tuple.badge.badge, r.tuple.badge.badge));

		public Registration(UUID id, RegistrationTuple tuple, Set<TicketName> tickets, Option<PrintedNametag> printedNametag) {
			this.id = id;
			this.tuple = tuple;
			this.tickets = tickets;
			this.printedNametag = printedNametag;
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
		public final Option<Account> account;
		public final ParticipantOrganisation organisation;

		public RegistrationTuple(
			RegistrationState state,
			String participantName,
			String participantEmail, String billingCompany,
			String billingAddress,
			BillingMethod billingMethod,
			String dietaryRequirements,
			Badge badge,
			String twitter,
			Option<Account> account,
			ParticipantOrganisation organisation
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
			this.account = account;
			this.organisation = organisation;
		}
	}

	public Option<Registration> one(Connection c, UUID id) throws SQLException {
		Option<RegistrationTuple> registration = registration(c, id);
		if (registration.isNone()) return Option.none();

		Set<TicketName> tickets = tickets(c, id);
		return Option.some(
			new Registration(
				id, registration.some(), tickets, printedNametag(c, id)
			)
		);
	}

	public void replace(Connection c, UUID id, Registration r) throws SQLException {
		deleteRegistrationTuple(c, id);
		deleteRegistrationTicket(c, id);
		insertRegistrationTuple(c, id, r.tuple);
		for (TicketName ticket : r.tickets) insertRegistrationTicket(c, id, ticket);
	}

	private void insertRegistrationTicket(Connection c, UUID id, TicketName ticket) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO registration_ticket (registration_id, ticket) VALUES (?, ?)")) {
			ps.setObject(1, id);
			ps.setString(2, ticket.ticketName);
			ps.executeUpdate();
		}
	}

	private Set<TicketName> tickets(Connection c, UUID id) throws SQLException {
		String sql = "SELECT ticket FROM registration_ticket WHERE registration_id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, id);
			return SqlOps.set(ps, Ord.hashEqualsOrd(), rs -> ticketName(rs.getString("ticket")));
		}
	}

	private Option<RegistrationTuple> registration(Connection c, UUID id) throws SQLException {
		String sql = "SELECT *, account FROM registration " +
			"LEFT JOIN registration_account USING (registration_id) " +
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
		String sql = "SELECT *, account FROM registration " +
			"LEFT JOIN registration_account USING (registration_id) " +
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
		String sql = "SELECT registration_id FROM registration " +
			"LEFT JOIN printed_nametags USING (registration_id) " +
			"WHERE printed_nametags.registration_id IS NULL " +
			"ORDER BY participant_name";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.list(ps,  rs -> (UUID) rs.getObject("registration_id"));
		}
	}

	public Option<PrintedNametag> printedNametag(Connection c, UUID id) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM printed_nametags WHERE registration_id = ?")) {
			ps.setObject(1, id);
			return SqlOps.one(ps, rs -> new PrintedNametag());
		}
	}

	public void replacePrintedNametag(Connection c, UUID id, Option<PrintedNametag> v) throws SQLException {
		deletePrintedNametag(c, id);
		if (v.isSome()) insertPrintedNametag(c, id, v.some());
	}

	private void insertPrintedNametag(Connection c, UUID id, PrintedNametag __) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO printed_nametags (registration_id) VALUES (?)")) {
			ps.setObject(1, id);
			ps.execute();
		}
	}

	private void deletePrintedNametag(Connection c, UUID id) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM printed_nametags WHERE registration_id = ?")) {
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
		String sql = "INSERT INTO registration " +
			"(registration_id, state, participant_name, participant_email, billing_company, billing_address, billing_method, dietary_requirements, twitter, organisation) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
			set(ps, 10, rt.organisation);
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
			Option.fromNull(rs.getString("account")).map(Account::account),
			ResultSets.participantOrganisation(rs, "organisation")
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

	public void deleteRegistrationTuple(Connection c, UUID id) throws SQLException {
		String sql = "DELETE FROM registration_ticket WHERE registration_id = ?; DELETE FROM registration WHERE registration_id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, id);
			ps.setObject(2, id);
			ps.execute();
		}
	}

	public void update(Connection c, UUID id, BillingCompany billingCompany, Badge badge, String diet, Option<Account> account) throws SQLException {
		replaceRegistrationAccount(c, id, account);
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

	private void replaceRegistrationAccount(Connection c, UUID id, Option<Account> account) throws SQLException {
		deleteRegistrationAccount(c, id);
		if (account.isSome()) insertRegistrationAccount(c, id, account.some());
	}

	private void deleteRegistrationAccount(Connection c, UUID id) throws SQLException {
		String sql = "DELETE FROM registration_account WHERE registration_id = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, id);
			ps.execute();
		}
	}

	private void insertRegistrationAccount(Connection c, UUID registrationId, Account account) throws SQLException {
		String sql = "INSERT INTO registration_account (registration_id, account) VALUES (?, ?);";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, registrationId);
			set(ps, 2, account);
			ps.execute();
		}
	}

	public static final class PrintedNametag {}

	public List<P4<String, TicketName, BigDecimal, String>> inAccount(Connection c, Account account) throws SQLException {
		String sql = "SELECT " +
			"registration.participant_name " +
			", registration_ticket.ticket " +
			", ticket.price " +
			", ticket.product_text " +
			"FROM registration " +
			"JOIN registration_account USING (registration_id) " +
			"JOIN registration_ticket USING (registration_id) " +
			"JOIN ticket USING (ticket) " +
			"WHERE account = ? " +
			"ORDER BY participant_name, ticket";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			set(ps, 1, account);
			return SqlOps.list(ps,
				rs -> P.p(
					rs.getString("participant_name"),
					ResultSets.ticketName(rs, "ticket"),
					rs.getBigDecimal("price"),
					rs.getString("product_text")
				)
			);
		}
	}

	public List<P2<TicketName, BigInteger>> individuals2(Connection c) throws SQLException {
		String sql = "SELECT ticket, count(1) as c FROM registration " +
			"JOIN registration_ticket USING (registration_id) " +
			"LEFT JOIN registration_account USING (registration_id) " +
			"WHERE registration_account IS NULL " +
			"GROUP BY ticket;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.list(ps, rs -> P.p(
				ResultSets.ticketName(rs, "ticket"),
				rs.getBigDecimal("c").toBigInteger()
			));
		}
	}
}
