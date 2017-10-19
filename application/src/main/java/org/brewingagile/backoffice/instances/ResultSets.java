package org.brewingagile.backoffice.instances;

import org.brewingagile.backoffice.db.operations.TicketsSql;
import org.brewingagile.backoffice.types.ChargeId;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultSets {
	public static TicketsSql.TicketName ticketName(ResultSet rs, String field) throws SQLException {
		return TicketsSql.TicketName.ticketName(rs.getString(field));
	}

	public static ChargeId chargeId(ResultSet rs, String x) throws SQLException {
		return ChargeId.chargeId(rs.getString(x));
	}
}
