package org.brewingagile.backoffice.db.operations;

import fj.P;
import fj.P5;
import fj.data.List;
import fj.data.Set;
import org.brewingagile.backoffice.instances.PreparedStatements;
import org.brewingagile.backoffice.instances.ResultSets;
import org.brewingagile.backoffice.types.Account;
import org.brewingagile.backoffice.types.AccountPackage;
import org.brewingagile.backoffice.types.TicketName;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AccountsSql {
	public void insert(Connection c, Account account) throws SQLException {
		String sql = "INSERT INTO account (account, billing_recipient, billing_address, billing_email) VALUES (?, '', '', '');";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, account);
			ps.executeUpdate();
		}
	}

	public static final class AccountData {
		public final String billingRecipient;
		public final String billingAddress;
		public final String billingEmail;

		public AccountData(String billingRecipient, String billingAddress, String billingEmail) {
			this.billingRecipient = billingRecipient;
			this.billingAddress = billingAddress;
			this.billingEmail = billingEmail;
		}
	}

	public Set<Account> all(Connection c) throws SQLException {
		String sql = "SELECT account FROM account;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.set(ps, Account.CaseInsensitive, rs -> ResultSets.account(rs, "account"));
		}
	}

	public AccountData accountData(Connection c, Account account) throws SQLException {
		String sql = "SELECT * FROM account WHERE account = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, account);
			return SqlOps.one(ps, rs -> new AccountData(
				rs.getString("billing_recipient"),
				rs.getString("billing_address"),
				rs.getString("billing_email")
			)).some();
		}
	}

	public List<AccountPackage> accountPackages(Connection c, Account account) throws SQLException {
		String sql = "SELECT * FROM account_package JOIN account_package_ticket USING (account, package_number) WHERE account = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, account);

			List<P5<Integer, String, BigDecimal, TicketName, BigInteger>> list = SqlOps.list(ps, rs -> P.p(
				rs.getInt("package_number"),
				rs.getString("description"),
				rs.getBigDecimal("price"),
				ResultSets.ticketName(rs, "ticket"),
				BigInteger.valueOf(rs.getLong("qty"))
			));

			return
				list.groupBy(x1 -> P.p(x1._1(), x1._2(), x1._3()), x1 -> P.p(x1._4(), x1._5()))
					.toList()
					.map(x -> new AccountPackage(
						x._1()._2(),
						x._1()._3(),
						x._2()
					));
		}
	}

	public void update(Connection c, Account account, AccountData accountData) throws SQLException {
		String sql = "UPDATE account SET " +
			"billing_recipient = ?," +
			"billing_address = ?, " +
			"billing_email = ? " +
			"WHERE account = ?";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, accountData.billingRecipient);
			ps.setString(2, accountData.billingAddress);
			ps.setString(3, accountData.billingEmail);
			PreparedStatements.set(ps, 4, account);
			ps.executeUpdate();
		}
	}
}