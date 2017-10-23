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
			PreparedStatements.set(ps, 1, x);
			return SqlOps.one(ps, rs -> ResultSets.account(rs, "account"));
		}
	}
}