package org.brewingagile.backoffice.io;

import argo.jdom.JsonNode;
import fj.Unit;
import fj.data.Either;
import fj.data.Set;
import org.brewingagile.backoffice.db.operations.RegistrationState;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper.Registration;
import org.brewingagile.backoffice.db.operations.TicketsSql;
import org.brewingagile.backoffice.integrations.OutvoiceInvoiceClient;
import org.brewingagile.backoffice.types.BillingMethod;
import org.brewingagile.backoffice.types.RegistrationId;

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

	public void sendInvoice(RegistrationId registrationId) throws Exception {

		System.out.println("1");
		Registration registration;
		RegistrationsSqlMapper.RegistrationInvoiceMethod registrationInvoiceMethod;
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			registration = registrationsSqlMapper.one(c, registrationId).some();
			registrationInvoiceMethod = registrationsSqlMapper.registrationInvoiceMethod(c, registrationId).some();
			if (registration.tuple.state != RegistrationState.RECEIVED) throw new IllegalArgumentException("Registration is not in expected state.");
		}

		System.out.println("2");
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			if (!registrationsSqlMapper.invoiceReference(c, registrationId).isSome()) {
				Set<TicketsSql.Ticket> tickets = ticketsSql.by(c, registrationId);
				RegistrationsSqlMapper.RegistrationTuple rt = registration.tuple;
				JsonNode jsonRequest = OutvoiceInvoiceClient.mkParticipantRequest(registrationId, BillingMethod.EMAIL, rt.participantEmail, registrationInvoiceMethod.billingCompany, registrationInvoiceMethod.billingAddress, tickets, rt.participantName);
				Either<String, Unit> invoiceReferenceId = outvoiceInvoiceClient.postInvoice(jsonRequest);
				if (invoiceReferenceId.isLeft()) {
					System.err.println(invoiceReferenceId.left().value());
					return;
				}
				registrationsSqlMapper.insertInvoiceReference(c, registrationId, registrationId);
				c.commit();
			}
		}

		System.out.println("3");
		try (Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);
			registrationsSqlMapper.updateRegistrationState(c, registrationId, registration.tuple.state, NextStateHelper.nextState(registration.tuple));
			c.commit();
		}
	}
}