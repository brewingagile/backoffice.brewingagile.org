package org.brewingagile.backoffice.services;

import java.sql.Connection;
import java.util.UUID;

import javax.sql.DataSource;

import org.brewingagile.backoffice.db.operations.RegistrationState;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.Registration;

public class MarkAsCompleteService {
	private final DataSource dataSource;
	private final RegistrationsSqlMapper registrationsSqlMapper;

	public MarkAsCompleteService(DataSource dataSource,
			RegistrationsSqlMapper registrationsSqlMapper) {
		this.dataSource = dataSource;
		this.registrationsSqlMapper = registrationsSqlMapper;
	}

	public void markAsComplete(UUID id) throws Exception {
		Registration registration;
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			registration = registrationsSqlMapper.one(c, id).some();
			if (registration.tuple.state != RegistrationState.RECEIVED) throw new IllegalArgumentException("Registration is not in expected state.");
			registrationsSqlMapper.updateRegistrationState(c, id, RegistrationState.RECEIVED, RegistrationState.PAID);
			c.commit();
		}
	}
}