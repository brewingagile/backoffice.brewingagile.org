package org.brewingagile.backoffice.instances;

import org.brewingagile.backoffice.types.AccountSecret;
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
}
