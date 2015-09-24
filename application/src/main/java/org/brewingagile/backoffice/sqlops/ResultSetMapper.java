package org.brewingagile.backoffice.sqlops;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultSetMapper<T> {
	public T apply(ResultSet r) throws SQLException;
}
