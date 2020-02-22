package org.brewingagile.backoffice.io;

import org.brewingagile.backoffice.db.operations.RegistrationState;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.Registration;
import org.brewingagile.backoffice.types.RegistrationId;

import javax.sql.DataSource;
import java.sql.Connection;

public class MarkAsCompleteService {
	private final DataSource dataSource;
	private final RegistrationsSqlMapper registrationsSqlMapper;

	public MarkAsCompleteService(
		DataSource dataSource,
		RegistrationsSqlMapper registrationsSqlMapper
	) {
		this.dataSource = dataSource;
		this.registrationsSqlMapper = registrationsSqlMapper;
	}

	public void markAsComplete(RegistrationId id) throws Exception {
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