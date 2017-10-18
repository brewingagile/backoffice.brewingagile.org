package org.brewingagile.backoffice.instances;

import org.brewingagile.backoffice.db.operations.AccountSecretSql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PreparedStatements {
	public static void set(PreparedStatement ps, int i, AccountSecretSql.AccountSecret x) throws SQLException {
		ps.setObject(i, x.value);
	}
}
