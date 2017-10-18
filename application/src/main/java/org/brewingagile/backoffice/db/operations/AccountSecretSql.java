package org.brewingagile.backoffice.db.operations;

import fj.data.Option;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import static org.brewingagile.backoffice.instances.PreparedStatements.set;

public class AccountSecretSql {
	@EqualsAndHashCode
	@ToString(includeFieldNames = false)
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
		String sql = "SELECT bundle FROM account_secret WHERE account_secret.secret_id = ?;";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			set(ps, 1, x);
			return SqlOps.one(ps, rs -> rs.getString("bundle"));
		}
	}

}