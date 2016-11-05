package org.brewingagile.backoffice.application;

import com.hencjo.summer.security.SummerAuthenticatedUser;
import com.squareup.okhttp.OkHttpClient;
import fj.data.List;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.BundlesSql;
import org.brewingagile.backoffice.db.operations.BudgetSql;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.db.operations.TicketsSql;
import org.brewingagile.backoffice.integrations.*;
import org.brewingagile.backoffice.rest.api.RegistrationApiJaxRs;
import org.brewingagile.backoffice.rest.gui.*;
import org.brewingagile.backoffice.io.DismissRegistrationService;
import org.brewingagile.backoffice.io.MarkAsCompleteService;
import org.brewingagile.backoffice.io.MarkAsPaidService;
import org.brewingagile.backoffice.io.SendInvoiceService;
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
		OutvoicePaidClient outvoicePaidClient = new OutvoicePaidClient(new OkHttpClient(), config.outvoiceInvoicesEndpoint, config.outvoiceInvoicesApikey);

		BudgetSql budgetSql = new BudgetSql();
		BundlesSql bundlesSql = new BundlesSql();
		RegistrationsSqlMapper registrationsSqlMapper = new RegistrationsSqlMapper();
		TicketsSql ticketsSql = new TicketsSql();

		SendInvoiceService sendInvoiceService = new SendInvoiceService(dataSource, registrationsSqlMapper, outvoiceInvoiceClient);

		DismissRegistrationService dismissRegistrationService = new DismissRegistrationService(dataSource, registrationsSqlMapper);
		MarkAsCompleteService markAsCompleteService = new MarkAsCompleteService(dataSource, registrationsSqlMapper);
		MarkAsPaidService markAsPaidService = new MarkAsPaidService(dataSource, registrationsSqlMapper);
		ConfirmationEmailSender confirmationEmailSender = new ConfirmationEmailSender(config);
		MailchimpSubscribeClient mailchimpSubscribeClient = new MailchimpSubscribeClient(ClientBuilder.newClient(), config.mailchimpEndpoint, config.mailchimpApikey);

		SlackBotHook slackBotHook = new SlackBotHook(new OkHttpClient(), config.slackBotHookUrl, config.slackBotName, config.slackBotChannel);

		this.apiRestServices = List.list(
			new RegistrationApiJaxRs(dataSource, registrationsSqlMapper, confirmationEmailSender, mailchimpSubscribeClient, bundlesSql, slackBotHook, ticketsSql)
		);

		this.guiRestServices = List.list(
			new LoggedInJaxRs(authService),
			new VersionNumberJaxRs(versionNumberProvider),
			new RegistrationsJaxRs(dataSource, authService, registrationsSqlMapper, sendInvoiceService, dismissRegistrationService, markAsCompleteService, markAsPaidService, outvoicePaidClient),
			new NameTagsJaxRs(dataSource, authService, registrationsSqlMapper),
			new BundlesJaxRs(dataSource, authService, bundlesSql),
			new BudgetJaxRs(dataSource, authService, budgetSql, registrationsSqlMapper, bundlesSql),
			new TicketsJaxRs(dataSource, authService, ticketsSql),
			new ReportsJaxRs(dataSource, authService, bundlesSql),
			new ExportsJaxRs(dataSource, authService,registrationsSqlMapper)
		);
	}
}
