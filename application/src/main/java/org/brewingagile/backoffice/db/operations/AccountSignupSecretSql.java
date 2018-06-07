package org.brewingagile.backoffice.db.operations;

import fj.data.Option;
import org.brewingagile.backoffice.instances.PreparedStatements;
import org.brewingagile.backoffice.instances.ResultSets;
import org.brewingagile.backoffice.types.Account;
import org.brewingagile.backoffice.types.AccountSignupSecret;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.brewingagile.backoffice.instances.PreparedStatements.set;

public class AccountSignupSecretSql {
	public Option<Account> account(Connection c, AccountSignupSecret x) throws SQLException {
		String sql = "SELECT account FROM account_signup_secret WHERE account_signup_secret.account_signup_secret = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			set(ps, 1, x);
			return SqlOps.one(ps, rs -> ResultSets.account(rs, "account"));
		}
	}

	public Option<AccountSignupSecret> accountSignupSecret(Connection c, Account a) throws SQLException {
		String sql = "SELECT account_signup_secret FROM account_signup_secret WHERE account = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			set(ps, 1, a);
			return SqlOps.one(ps, rs -> ResultSets.accountSignupSecret(rs, "account_signup_secret"));
		}
	}

	public void insert(Connection c, Account account, AccountSignupSecret x) throws SQLException {
		String sql = "INSERT INTO account_signup_secret (account, account_signup_secret) VALUES (?, ?);";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			PreparedStatements.set(ps, 1, account);
			PreparedStatements.set(ps, 2, x);
			ps.executeUpdate();
		}
	}
}