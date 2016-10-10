package org.brewingagile.backoffice.application;

import com.hencjo.summer.security.SummerAuthenticatedUser;
import fj.data.List;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.BucketsSqlMapper;
import org.brewingagile.backoffice.db.operations.BudgetSql;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.integrations.ConfirmationEmailSender;
import org.brewingagile.backoffice.integrations.MailchimpSubscribeClient;
import org.brewingagile.backoffice.integrations.OutvoiceInvoiceClient;
import org.brewingagile.backoffice.rest.api.RegistrationApiRestService;
import org.brewingagile.backoffice.rest.gui.*;
import org.brewingagile.backoffice.services.DismissRegistrationService;
import org.brewingagile.backoffice.services.MarkAsCompleteService;
import org.brewingagile.backoffice.services.MarkAsPaidService;
import org.brewingagile.backoffice.services.SendInvoiceService;
import org.brewingagile.backoffice.utils.GitPropertiesDescribeVersionNumberProvider;

import javax.sql.DataSource;
import javax.ws.rs.client.ClientBuilder;

public class Application {
	public final GitPropertiesDescribeVersionNumberProvider versionNumberProvider;
	public final List<Object> apiRestServices;
	public final List<Object> guiRestServices;

	Application(Configuration config, DataSource dataSource) {
		this.versionNumberProvider = new GitPropertiesDescribeVersionNumberProvider(Application.class, "/resources/git.properties");
		AuthService authService = new AuthService(new SummerAuthenticatedUser());
		OutvoiceInvoiceClient outvoiceInvoiceClient = new OutvoiceInvoiceClient(ClientBuilder.newClient(), config.outvoiceInvoicesEndpoint, config.outvoiceInvoicesApikey);
		BudgetSql budgetSql = new BudgetSql();
		RegistrationsSqlMapper registrationsSqlMapper = new RegistrationsSqlMapper();
		SendInvoiceService sendInvoiceService = new SendInvoiceService(dataSource, registrationsSqlMapper, outvoiceInvoiceClient);
		DismissRegistrationService dismissRegistrationService = new DismissRegistrationService(dataSource, registrationsSqlMapper);
		MarkAsCompleteService markAsCompleteService = new MarkAsCompleteService(dataSource, registrationsSqlMapper);
		MarkAsPaidService markAsPaidService = new MarkAsPaidService(dataSource, registrationsSqlMapper);
		ConfirmationEmailSender confirmationEmailSender = new ConfirmationEmailSender(config);
		MailchimpSubscribeClient mailchimpSubscribeClient = new MailchimpSubscribeClient(ClientBuilder.newClient(), config.mailchimpEndpoint, config.mailchimpApikey);

		this.apiRestServices = List.list(
			new RegistrationApiRestService(dataSource, registrationsSqlMapper, confirmationEmailSender, mailchimpSubscribeClient)
		);

		BucketsSqlMapper bucketsSqlMapper = new BucketsSqlMapper();
		this.guiRestServices = List.list(
			new LoggedInRestService(authService),
			new VersionNumberRestService(versionNumberProvider),
			new RegistrationsRestService(dataSource, authService, registrationsSqlMapper, sendInvoiceService, dismissRegistrationService, markAsCompleteService, markAsPaidService),
			new NameTagsRestService(dataSource, authService, registrationsSqlMapper),
			new BucketsRestService(dataSource, authService, bucketsSqlMapper),
			new BudgetJaxRs(dataSource, authService, budgetSql, registrationsSqlMapper, bucketsSqlMapper),
			new ReportsRestService(dataSource, authService, bucketsSqlMapper),
			new ExportsRestService(dataSource, authService,registrationsSqlMapper)
		);
	}
}
