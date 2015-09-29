package org.brewingagile.backoffice.services;

import java.sql.Connection;
import java.util.UUID;

import javax.sql.DataSource;

import org.brewingagile.backoffice.db.operations.RegistrationState;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.Registration;

public class DismissRegistrationService {
	private final DataSource dataSource;
	private final RegistrationsSqlMapper registrationsSqlMapper;

	public DismissRegistrationService(DataSource dataSource,
			RegistrationsSqlMapper registrationsSqlMapper) {
		this.dataSource = dataSource;
		this.registrationsSqlMapper = registrationsSqlMapper;
	}

	public void dismissRegistration(UUID id) throws Exception {
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			Registration registration = registrationsSqlMapper.one(c, id).some();
			if (registration.state != RegistrationState.RECEIVED) throw new IllegalArgumentException("Registration is not in expected state.");
			registrationsSqlMapper.delete(c, id);
			c.commit();
		}
	}
}