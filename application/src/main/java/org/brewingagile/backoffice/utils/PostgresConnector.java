package org.brewingagile.backoffice.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.postgresql.ds.PGPoolingDataSource;
import org.postgresql.ds.PGSimpleDataSource;

public class PostgresConnector {
	private final String hostname;
	private final int port;
	private final String dbName;
	private final String username;
	private final String password;
	
	public PostgresConnector(String hostname, int port, String dbName, String username, String password) {
		this.hostname = hostname;
		this.port = port;
		this.dbName = dbName;
		this.username = username;
		this.password = password;
	}
	
	public PGPoolingDataSource poolingDatasource() {
		PGPoolingDataSource ds = new PGPoolingDataSource();
		ds.setServerName(hostname);
		ds.setPortNumber(port);
		ds.setDatabaseName(dbName);
		ds.setUser(username);
		ds.setPassword(password);
		return ds;
	}
	
	public PGSimpleDataSource simpleDatasource() {
		PGSimpleDataSource ds = new PGSimpleDataSource();
		ds.setServerName(hostname);
		ds.setPortNumber(port);
		ds.setDatabaseName(dbName);
		ds.setUser(username);
		ds.setPassword(password);
		return ds;
	}

	public void testConnection(PGPoolingDataSource datasource) {
		try (Connection c = datasource.getConnection()) {
			try (PreparedStatement ps = c.prepareStatement("SELECT 1")) {
				try (ResultSet r = ps.executeQuery()) {
					r.next();
					if (1 == r.getInt(1)) return;
					throw new RuntimeException("Could not query DB " + dbName + " (" + hostname + ":" + port + ") as " + username);
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Could not query DB " + dbName + " (" + hostname + ":" + port + ") as " + username, e);
		}
	}
}
