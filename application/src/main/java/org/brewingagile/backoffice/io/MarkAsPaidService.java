package org.brewingagile.backoffice.io;

import java.sql.Connection;
import java.util.UUID;

import javax.sql.DataSource;

import org.brewingagile.backoffice.db.operations.RegistrationState;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.Registration;
import org.brewingagile.backoffice.types.RegistrationId;

public class MarkAsPaidService {
	private final DataSource dataSource;
	private final RegistrationsSqlMapper registrationsSqlMapper;

	public MarkAsPaidService(DataSource dataSource,
			RegistrationsSqlMapper registrationsSqlMapper) {
		this.dataSource = dataSource;
		this.registrationsSqlMapper = registrationsSqlMapper;
	}

	public void markAsPaid(RegistrationId id) throws Exception {
		Registration registration;
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			registration = registrationsSqlMapper.one(c, id).some();
			if (registration.tuple.state != RegistrationState.INVOICING) throw new IllegalArgumentException("Registration is not in expected state.");
			registrationsSqlMapper.updateRegistrationState(c, id, RegistrationState.INVOICING, RegistrationState.PAID);
			c.commit();
		}
	}
}