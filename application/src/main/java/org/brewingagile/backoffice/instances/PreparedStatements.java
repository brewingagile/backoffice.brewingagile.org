package org.brewingagile.backoffice.instances;

import org.brewingagile.backoffice.types.Account;
import org.brewingagile.backoffice.types.AccountSecret;
import org.brewingagile.backoffice.types.AccountSignupSecret;
import org.brewingagile.backoffice.types.ParticipantOrganisation;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PreparedStatements {
	public static void set(PreparedStatement ps, int i, AccountSecret x) throws SQLException {
		ps.setObject(i, x.value);
	}

	public static void set(PreparedStatement ps, int i, ParticipantOrganisation organisation) throws SQLException {
		ps.setString(i, organisation.value);
	}

	public static void set(PreparedStatement ps, int i, Account x) throws SQLException {
		ps.setString(i, x.value);
	}

	public static void set(PreparedStatement ps, int i, AccountSignupSecret x) throws SQLException {
		ps.setObject(i, x.value);
	}
}
