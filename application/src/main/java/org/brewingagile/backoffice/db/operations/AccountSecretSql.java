package org.brewingagile.backoffice.db.operations;

import fj.data.Option;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

public class AccountSecretSql {
	@EqualsAndHashCode
	@ToString
	public final static class AccountSecret {
		public final UUID value;

		private AccountSecret(UUID value) {
			this.value = Objects.requireNonNull(value);
		}

		public static AccountSecret accountSecret(UUID value) {
			return new AccountSecret(value);
		}

		public static Option<AccountSecret> parse(String x) {
			try {
				return Option.some(UUID.fromString(x)).map(AccountSecret::accountSecret);
			} catch (IllegalArgumentException e) {
				return Option.none();
			}
		}
	}

	public Option<String> bundle(Connection c, AccountSecret x) throws SQLException {
		String sql = "SELECT bundle FROM account_secret WHERE account_secret = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			return SqlOps.one(ps, rs -> rs.getString("bundle"));
		}
	}
}