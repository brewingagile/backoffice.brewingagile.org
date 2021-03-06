package org.brewingagile.backoffice.io;

import java.sql.Connection;
import java.util.UUID;

import javax.sql.DataSource;

import org.brewingagile.backoffice.db.operations.RegistrationState;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.Registration;
import org.brewingagile.backoffice.types.RegistrationId;

public class DismissRegistrationService {
	private final DataSource dataSource;
	private final RegistrationsSqlMapper registrationsSqlMapper;

	public DismissRegistrationService(DataSource dataSource,
			RegistrationsSqlMapper registrationsSqlMapper) {
		this.dataSource = dataSource;
		this.registrationsSqlMapper = registrationsSqlMapper;
	}

	public void dismissRegistration(RegistrationId id) throws Exception {
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			Registration registration = registrationsSqlMapper.one(c, id).some();
			if (registration.tuple.state != RegistrationState.RECEIVED) throw new IllegalArgumentException("Registration is not in expected state.");
			registrationsSqlMapper.deleteRegistrationTuple(c, id);
			c.commit();
		}
	}
}