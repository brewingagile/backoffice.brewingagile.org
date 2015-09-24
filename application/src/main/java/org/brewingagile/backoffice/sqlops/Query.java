package org.brewingagile.backoffice.sqlops;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface Query {
	public PreparedStatement apply(Connection c) throws SQLException;
}