package org.brewingagile.backoffice.io;

import fj.data.Either;
import fj.data.Set;
import org.brewingagile.backoffice.db.operations.RegistrationState;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.Registration;
import org.brewingagile.backoffice.db.operations.TicketsSql;
import org.brewingagile.backoffice.integrations.OutvoiceInvoiceClient;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.UUID;

public class SendInvoiceService {
	private final DataSource dataSource;
	private final RegistrationsSqlMapper registrationsSqlMapper;
	private final OutvoiceInvoiceClient outvoiceInvoiceClient;
	private final TicketsSql ticketsSql;

	public SendInvoiceService(
		DataSource dataSource,
		RegistrationsSqlMapper registrationsSqlMapper,
		OutvoiceInvoiceClient outvoiceInvoiceClient,
		TicketsSql ticketsSql
	) {
		this.dataSource = dataSource;
		this.registrationsSqlMapper = registrationsSqlMapper;
		this.outvoiceInvoiceClient = outvoiceInvoiceClient;
		this.ticketsSql = ticketsSql;
	}

	public void sendInvoice(UUID id) throws Exception {
		System.out.println("1");
		Registration registration;
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			registration = registrationsSqlMapper.one(c, id).some();
			if (registration.tuple.state != RegistrationState.RECEIVED) throw new IllegalArgumentException("Registration is not in expected state.");
		}

		System.out.println("2");
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			if (!registrationsSqlMapper.invoiceReference(c, id).isSome()) {
				Set<TicketsSql.Ticket> tickets = ticketsSql.by(c, id);
				RegistrationsSqlMapper.RegistrationTuple rt = registration.tuple;
				Either<String,UUID> invoiceReferenceId = outvoiceInvoiceClient.postInvoice(registration.id, rt.billingMethod, rt.participantEmail, rt.billingCompany, rt.billingAddress, tickets, rt.participantName);
				if (invoiceReferenceId.isLeft()) System.err.println(invoiceReferenceId.left().value());
				registrationsSqlMapper.insertInvoiceReference(c, registration.id, invoiceReferenceId.right().value());
				c.commit();
			}
		}

		System.out.println("3");
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			registrationsSqlMapper.updateRegistrationState(c, id, registration.tuple.state, NextStateHelper.nextState(registration.tuple));
			c.commit();
		}
	}
}