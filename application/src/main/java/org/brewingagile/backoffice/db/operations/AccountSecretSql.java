package org.brewingagile.backoffice.db.operations;

import fj.data.Option;
import org.brewingagile.backoffice.instances.ResultSets;
import org.brewingagile.backoffice.types.Account;
import org.brewingagile.backoffice.types.AccountSecret;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.brewingagile.backoffice.instances.PreparedStatements.set;

public class AccountSecretSql {
	public Option<Account> bundle(Connection c, AccountSecret x) throws SQLException {
		String sql = "SELECT account FROM account_secret WHERE account_secret.secret_id = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			set(ps, 1, x);
			return SqlOps.one(ps, rs -> ResultSets.account(rs, "account"));
		}
	}
}