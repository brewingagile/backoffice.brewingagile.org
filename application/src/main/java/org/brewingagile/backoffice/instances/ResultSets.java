package org.brewingagile.backoffice.instances;

import org.brewingagile.backoffice.types.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ResultSets {
	public static TicketName ticketName(ResultSet rs, String field) throws SQLException {
		return TicketName.ticketName(rs.getString(field));
	}

	public static ChargeId chargeId(ResultSet rs, String x) throws SQLException {
		return ChargeId.chargeId(rs.getString(x));
	}

	public static ParticipantOrganisation participantOrganisation(ResultSet rs, String organisation) throws SQLException {
		return ParticipantOrganisation.participantOrganisation(rs.getString(organisation));
	}

	public static Account account(ResultSet rs, String x) throws SQLException {
		return Account.account(rs.getString(x));
	}

	public static AccountSignupSecret accountSignupSecret(ResultSet rs, String f) throws SQLException {
		return AccountSignupSecret.accountSignupSecret((UUID) rs.getObject(f));
	}

	public static ParticipantName participantName(ResultSet rs, String f) throws SQLException {
		return ParticipantName.participantName(rs.getString(f));
	}
}
