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
	public static final class AccountData {
		public final String billingRecipient;
		public final String billingAddress;

		public AccountData(String billingRecipient, String billingAddress) {
			this.billingRecipient = billingRecipient;
			this.billingAddress = billingAddress;
		}
	}

	public Set<Account> all(Connection c) throws SQLException {
		String sql = "SELECT account FROM account;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.set(ps, Account.Ord, rs -> ResultSets.account(rs, "account"));
		}
	}

	public AccountData accountData(Connection c, Account account) throws SQLException {
		String sql = "SELECT * FROM account WHERE account = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, account);
			return SqlOps.one(ps, rs -> new AccountData(
				rs.getString("billing_recipient"),
				rs.getString("billing_address")
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
}