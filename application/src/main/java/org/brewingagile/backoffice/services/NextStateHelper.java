package org.brewingagile.backoffice.services;

import org.brewingagile.backoffice.db.operations.RegistrationState;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.Registration;

public class NextStateHelper {
	public static RegistrationState nextState(Registration r) {
		switch (r.state) {
			case RECEIVED: return RegistrationState.INVOICING;
			case INVOICING: return RegistrationState.PAID;
			case PAID: throw new IllegalArgumentException("No state after PAID, yet.");
			default: throw new IllegalArgumentException("Unknown RegistrationState: " + r.state);
		}
	}
}
