package org.brewingagile.backoffice.db.operations;

import fj.*;
import fj.data.List;
import fj.data.Option;
import fj.data.Set;
import fj.function.Try1;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.brewingagile.backoffice.instances.PreparedStatements;
import org.brewingagile.backoffice.instances.ResultSets;
import org.brewingagile.backoffice.types.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

	public Option<RegistrationId> invoiceReferenceToRegistrationId(Connection c, UUID apiClientReference) throws SQLException {
		String sql = "SELECT * FROM registration_invoices WHERE invoice_reference_id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, apiClientReference);
			return SqlOps.one(ps, rs -> RegistrationId.registrationId((UUID)rs.getObject("registration_id")));
		}
	}

	public void replaceAccount(Connection c, UUID registrationId, Option<Account> account) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM registration_account WHERE registration_id = ?;")) {
			ps.setObject(1, registrationId);
			ps.execute();
		}
		if (account.isSome()) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO registration_account (registration_id, account) VALUES (?, ?);")) {
				ps.setObject(1, registrationId);
				set(ps, 2, account.some());
				ps.execute();
			}
		}
	}

	public BigDecimal totalTicketsIncVat(Connection c, UUID registrationId) throws SQLException {
		String sql = "SELECT sum(price) AS total FROM registration JOIN registration_ticket USING (registration_id) JOIN ticket USING (ticket) WHERE registration_id = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setObject(1, registrationId);
			return SqlOps.queryExactlyOne(ps, rs -> rs.getBigDecimal("total"));
		}
	}

	public void insertRegistrationInvoiceMethod(Connection c, RegistrationId registrationId, String recipient, String address) throws SQLException {
		String sql = "INSERT INTO registration_invoice_method " +
			"(registration_id, billing_company, billing_address) " +
			"VALUES (?, ?, ?)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			set(ps, 1, registrationId);
			ps.setString(2, recipient);
			ps.setString(3, address);
			ps.execute();
		}
	}

	public void insertRegistrationInvoice2(
		Connection c,
		RegistrationId registrationId,
		String invoiceNumber,
		byte[] pdf
	) throws SQLException {
		String sql = "INSERT INTO registration_invoice_2 (registration_id, invoice_number, pdf) VALUES (?, ?, ?)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			set(ps, 1, registrationId);
			ps.setString(2, invoiceNumber);
			ps.setBytes(3, pdf);
			ps.execute();
		}
	}

	public final static class Registration {
		public final RegistrationId id;
		public final RegistrationTuple tuple;
		public final Set<TicketName> tickets;
		public final Option<PrintedNametag> printedNametag;
		public final Option<Account> account;

		public static Ord<RegistrationsSqlMapper.Registration> byBadge = Ord.ord(l -> r -> Ord.stringOrd.compare(l.tuple.badge.badge, r.tuple.badge.badge));

		public Registration(
			RegistrationId id,
			RegistrationTuple tuple,
			Set<TicketName> tickets,
			Option<PrintedNametag> printedNametag,
			Option<Account> account
		) {
			this.id = id;
			this.tuple = tuple;
			this.tickets = tickets;
			this.printedNametag = printedNametag;
			this.account = account;
		}
	}

	public final static class RegistrationTuple {
		public final RegistrationState state;
		public final ParticipantName participantName;
		public final ParticipantEmail participantEmail;
		public final String dietaryRequirements;
		public final Badge badge;
		public final String twitter;
		public final ParticipantOrganisation organisation;

		public RegistrationTuple(
			RegistrationState state,
			ParticipantName participantName,
			ParticipantEmail participantEmail,
			String dietaryRequirements,
			Badge badge,
			String twitter,
			ParticipantOrganisation organisation
		) {
			this.state = state;
			this.participantName = participantName;
			this.participantEmail = participantEmail;
			this.dietaryRequirements = dietaryRequirements;
			this.badge = badge;
			this.twitter = twitter;
			this.organisation = organisation;
		}
	}

	@EqualsAndHashCode
	@ToString
	public final static class RegistrationInvoiceMethod {
		public final String billingCompany;
		public final String billingAddress;

		public RegistrationInvoiceMethod(String billingCompany, String billingAddress) {
			this.billingCompany = billingCompany;
			this.billingAddress = billingAddress;
		}
	}

	public Option<Registration> one(Connection c, RegistrationId id) throws SQLException {
		Option<RegistrationTuple> registration = registration(c, id);
		if (registration.isNone()) return Option.none();

		Option<Account> account = account(c, id);
		Set<TicketName> tickets = tickets(c, id);
		return Option.some(
			new Registration(
				id, registration.some(), tickets, printedNametag(c, id), account
			)
		);
	}

	public Option<RegistrationInvoiceMethod> registrationInvoiceMethod(Connection c, RegistrationId registrationId) throws SQLException {
		String sql = "SELECT * FROM registration_invoice_method WHERE registration_id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			set(ps, 1, registrationId);
			return SqlOps.one(ps, rs -> new RegistrationInvoiceMethod(
				rs.getString("billing_company"),
				rs.getString("billing_address")
			));
		}
	}

	public Option<Account> account(Connection c, RegistrationId id) throws SQLException {
		String sql = "SELECT account FROM registration_account WHERE registration_id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, id);
			return SqlOps.one(ps, rs -> Account.account(rs.getString("account")));
		}
	}

	public void replace(Connection c, RegistrationId id, Registration r) throws SQLException {
		deleteRegistrationTuple(c, id);
		deleteRegistrationTicket(c, id);
		insertRegistrationTuple(c, id, r.tuple);
		insertTickets(c, id, r.tickets);
	}

	public void insertTickets(Connection c, RegistrationId id, Set<TicketName> tickets) throws SQLException {
		for (TicketName ticket : tickets) insertRegistrationTicket(c, id, ticket);
	}

	private void insertRegistrationTicket(Connection c, RegistrationId id, TicketName ticket) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO registration_ticket (registration_id, ticket) VALUES (?, ?)")) {
			PreparedStatements.set(ps, 1, id);
			ps.setString(2, ticket.ticketName);
			ps.executeUpdate();
		}
	}

	private Set<TicketName> tickets(Connection c, RegistrationId id) throws SQLException {
		String sql = "SELECT ticket FROM registration_ticket WHERE registration_id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, id);
			return SqlOps.set(ps, Ord.hashEqualsOrd(), rs -> ticketName(rs.getString("ticket")));
		}
	}

	private Option<RegistrationTuple> registration(Connection c, RegistrationId id) throws SQLException {
		String sql = "SELECT * FROM registration WHERE registration_id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, id);
			return SqlOps.one(ps, RegistrationsSqlMapper::toRegistrationTuple);
		}
	}

	private void deleteRegistrationTicket(Connection c, RegistrationId id) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM registration_ticket WHERE registration_id = ?;")) {
			PreparedStatements.set(ps, 1, id);
			ps.execute();
		}
	}

	public List<P2<RegistrationId, RegistrationTuple>> all(Connection c) throws SQLException {
		String sql = "SELECT * FROM registration ORDER BY participant_name";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.list(ps,
				rs -> P.p(
					RegistrationId.registrationId((UUID) rs.getObject("registration_id")),
					toRegistrationTuple(rs)
				)
			);
		}
	}

	public List<RegistrationId> unprintedNametags(Connection c) throws SQLException {
		String sql = "SELECT registration_id FROM registration " +
			"LEFT JOIN printed_nametags USING (registration_id) " +
			"WHERE printed_nametags.registration_id IS NULL " +
			"ORDER BY participant_name";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.list(ps,  rs -> RegistrationId.registrationId((UUID) rs.getObject("registration_id")));
		}
	}

	public Option<PrintedNametag> printedNametag(Connection c, RegistrationId id) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM printed_nametags WHERE registration_id = ?")) {
			PreparedStatements.set(ps, 1, id);
			return SqlOps.one(ps, rs -> new PrintedNametag());
		}
	}

	public void replacePrintedNametag(Connection c, RegistrationId id, Option<PrintedNametag> v) throws SQLException {
		deletePrintedNametag(c, id);
		if (v.isSome()) insertPrintedNametag(c, id, v.some());
	}

	private void insertPrintedNametag(Connection c, RegistrationId id, PrintedNametag __) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO printed_nametags (registration_id) VALUES (?)")) {
			PreparedStatements.set(ps, 1, id);
			ps.execute();
		}
	}

	private void deletePrintedNametag(Connection c, RegistrationId id) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM printed_nametags WHERE registration_id = ?")) {
			PreparedStatements.set(ps, 1, id);
			ps.execute();
		}
	}

	public void updateRegistrationState(Connection c, RegistrationId id, RegistrationState oldState, RegistrationState nextState) throws SQLException {
		String sql = "UPDATE registration SET state = ? WHERE (registration_id = ? AND state = ?);";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, nextState.name());
			set(ps, 2, id);
			ps.setString(3, oldState.name());
			ps.execute();
		}
	}

	public void insertRegistrationTuple(Connection c, RegistrationId id, RegistrationTuple rt) throws SQLException {
		String sql = "INSERT INTO registration " +
			"(registration_id, state, participant_name, participant_email, dietary_requirements, twitter, organisation) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, id);
			ps.setString(2, rt.state.name());
			ps.setString(3, rt.participantName.value);
			ps.setString(4, rt.participantEmail.value);
			ps.setString(5, rt.dietaryRequirements);
			ps.setString(6, rt.twitter);
			set(ps, 7, rt.organisation);
			ps.execute();
		}
	}

	public static RegistrationTuple toRegistrationTuple(ResultSet rs) throws SQLException {
		return new RegistrationTuple(
			RegistrationState.valueOf(rs.getString("state")),
			ParticipantName.participantName(rs.getString("participant_name")),
			ParticipantEmail.participantEmail(rs.getString("participant_email")),
			rs.getString("dietary_requirements"),
			new Badge(rs.getString("badge")),
			rs.getString("twitter"),
			ResultSets.participantOrganisation(rs, "organisation")
		);
	}

	public void insertInvoiceReference(Connection c, RegistrationId registrationId, RegistrationId invoiceReferenceId) throws SQLException {
		String sql = "INSERT INTO registration_invoices (registration_id, invoice_reference_id) VALUES (?, ?)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, registrationId);
			PreparedStatements.set(ps, 2, invoiceReferenceId);
			ps.execute();
		}
	}

	public Option<UUID> invoiceReference(Connection c, RegistrationId registrationId) throws SQLException {
		String sql = "SELECT * FROM registration_invoices WHERE registration_id = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, registrationId);
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

	public void deleteRegistrationTuple(Connection c, RegistrationId id) throws SQLException {
		String sql =
			"DELETE FROM registration_ticket WHERE registration_id = ?;" +
			"DELETE FROM registration_account WHERE registration_id = ?;" +
			"DELETE FROM registration_invoice_method WHERE registration_id = ?;" +
			"DELETE FROM registration WHERE registration_id = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, id);
			PreparedStatements.set(ps, 2, id);
			PreparedStatements.set(ps, 3, id);
			PreparedStatements.set(ps, 4, id);
			ps.execute();
		}
	}

	public void update(Connection c, RegistrationId registrationId, Badge badge, String diet, Option<Account> account) throws SQLException {
		replaceRegistrationAccount(c, registrationId.value, account);
		updateRegistration(c, registrationId, badge, diet);
	}

	private void updateRegistration(
		Connection c,
		RegistrationId registrationId,
		Badge badge,
		String diet
	) throws SQLException {
		String sql = "UPDATE registration SET badge = ?, dietary_requirements = ? WHERE registration_id = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			set(ps, 1, badge);
			ps.setString(2, diet);
			set(ps, 3, registrationId);
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

	public List<P2<ParticipantName, Set<TicketName>>> inAccount2(Connection c, Account account) throws SQLException {
		String sql = "SELECT participant_name, ticket FROM registration " +
			"JOIN registration_account USING (registration_id) " +
			"JOIN registration_ticket USING (registration_id) " +
			"WHERE account = ? " +
			"ORDER BY participant_name, ticket";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			set(ps, 1, account);
			List<P2<ParticipantName, TicketName>> list = SqlOps.list(ps,
				rs -> P.p(
					ResultSets.participantName(rs, "participant_name"),
					ResultSets.ticketName(rs, "ticket")
				)
			);
			return list.groupBy(x -> x._1(), x -> x._2(), ParticipantName.Ord).map(x -> Set.iterableSet(TicketName.Ord, x)).toList();
		}
	}

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
