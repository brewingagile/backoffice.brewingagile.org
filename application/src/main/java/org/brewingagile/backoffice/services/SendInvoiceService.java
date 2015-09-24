package org.brewingagile.backoffice.services;

import java.sql.Connection;
import java.util.UUID;

import javax.sql.DataSource;

import functional.Either;
import org.brewingagile.backoffice.db.operations.RegistrationState;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.Registration;
import org.brewingagile.backoffice.integrations.OutvoiceInvoiceClient;

public class SendInvoiceService {
	private final DataSource dataSource;
	private final RegistrationsSqlMapper registrationsSqlMapper;
	private final OutvoiceInvoiceClient outvoiceInvoiceClient;

	public SendInvoiceService(DataSource dataSource,
			RegistrationsSqlMapper registrationsSqlMapper,
			OutvoiceInvoiceClient outvoiceInvoiceClient) {
		this.dataSource = dataSource;
		this.registrationsSqlMapper = registrationsSqlMapper;
		this.outvoiceInvoiceClient = outvoiceInvoiceClient;
	}

	public void sendInvoice(UUID id) throws Exception {
		Registration registration;
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			registration = registrationsSqlMapper.one(c, id).get();
			if (registration.state != RegistrationState.RECEIVED) throw new IllegalArgumentException("Registration is not in expected state.");
		}
		
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			if (!registrationsSqlMapper.invoiceReference(c, id).isPresent()) {
				Either<String,UUID> invoiceReferenceId = outvoiceInvoiceClient.postInvoice(registration.id, registration.billingMethod, registration.participantEmail, registration.billingCompany, registration.billingAddress, registration.ticket, registration.participantName);
				if (invoiceReferenceId.isLeft()) System.err.println(invoiceReferenceId.left());
				registrationsSqlMapper.insertInvoiceReference(c, registration.id, invoiceReferenceId.right());
				c.commit();
			}
		}
		
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			registrationsSqlMapper.updateRegistrationState(c, id, registration.state, NextStateHelper.nextState(registration));
			c.commit();
		}
	}
}