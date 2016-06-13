package org.brewingagile.backoffice.services;

import org.brewingagile.backoffice.db.operations.RegistrationState;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.Registration;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.RegistrationTuple;

public class NextStateHelper {
	public static RegistrationState nextState(RegistrationTuple rt) {
		switch (rt.state) {
			case RECEIVED: return RegistrationState.INVOICING;
			case INVOICING: return RegistrationState.PAID;
			case PAID: throw new IllegalArgumentException("No state after PAID, yet.");
			default: throw new IllegalArgumentException("Unknown RegistrationState: " + rt.state);
		}
	}
}
