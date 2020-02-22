package org.brewingagile.backoffice.application;

import com.hencjo.summer.security.SummerAuthenticatedUser;
import fj.data.List;
import okhttp3.OkHttpClient;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.*;
import org.brewingagile.backoffice.integrations.*;
import org.brewingagile.backoffice.io.DismissRegistrationService;
import org.brewingagile.backoffice.io.MarkAsCompleteService;
import org.brewingagile.backoffice.io.MarkAsPaidService;
import org.brewingagile.backoffice.io.SendInvoiceService;
import org.brewingagile.backoffice.pure.AccountIO;
import org.brewingagile.backoffice.rest.api.RegistrationApiJaxRs;
import org.brewingagile.backoffice.rest.gui.*;

import javax.sql.DataSource;
import javax.ws.rs.client.ClientBuilder;

public class Application {
	public final String version;
	public final List<Object> apiRestServices;
	public final List<Object> guiRestServices;

	Application(Configuration config, DataSource dataSource, String version) {
		this.version = version;
		AuthService authService = new AuthService(new SummerAuthenticatedUser());
		OkHttpClient okHttpClient = new OkHttpClient();
		OkHttpClient unsafe = OkHttpHelper.getUnsafeOkHttpClient();

		OutvoiceInvoiceClient outvoiceInvoiceClient = new OutvoiceInvoiceClient(unsafe, config.outvoiceInvoicesEndpoint, config.outvoiceInvoicesApikey);
		OutvoicePaidClient outvoicePaidClient = new OutvoicePaidClient(okHttpClient, config.outvoiceInvoicesEndpoint, config.outvoiceInvoicesApikey);
		OutvoiceAccountClient outvoiceAccountClient = new OutvoiceAccountClient(okHttpClient, config.outvoiceInvoicesEndpoint, config.outvoiceInvoicesApikey);
		OutvoiceReceiptClient outvoiceReceiptClient = new OutvoiceReceiptClient(okHttpClient, config.outvoiceInvoicesEndpoint, config.outvoiceInvoicesApikey);
		OutvoiceInvoice3Client outvoiceInvoice3Client = new OutvoiceInvoice3Client(unsafe, config.outvoiceInvoicesEndpoint, config.outvoiceInvoicesApikey);

		AccountsSql accountsSql = new AccountsSql();
		AccountSecretSql accountSecretSql = new AccountSecretSql();
		AccountSignupSecretSql accountSignupSecretSql = new AccountSignupSecretSql();
		BudgetSql budgetSql = new BudgetSql();
		RegistrationsSqlMapper registrationsSqlMapper = new RegistrationsSqlMapper();
		RegistrationStripeChargeSql registrationStripeChargeSql = new RegistrationStripeChargeSql();
		StripeChargeSql stripeChargeSql = new StripeChargeSql();
		TicketsSql ticketsSql = new TicketsSql();

		SendInvoiceService sendInvoiceService = new SendInvoiceService(dataSource, registrationsSqlMapper, outvoiceInvoiceClient, ticketsSql);

		DismissRegistrationService dismissRegistrationService = new DismissRegistrationService(dataSource, registrationsSqlMapper);
		MarkAsCompleteService markAsCompleteService = new MarkAsCompleteService(dataSource, registrationsSqlMapper);
		MarkAsPaidService markAsPaidService = new MarkAsPaidService(dataSource, registrationsSqlMapper);
		ConfirmationEmailSender confirmationEmailSender = new ConfirmationEmailSender(config);
		MailchimpSubscribeClient mailchimpSubscribeClient = new MailchimpSubscribeClient(ClientBuilder.newClient(), config.mailchimpEndpoint, config.mailchimpApikey);

		SlackBotHook slackBotHook = new SlackBotHook(okHttpClient, config.slackBotHookUrl, config.slackBotName, config.slackBotChannel);
		StripeChargeClient stripeChargeClient = new StripeChargeClient(okHttpClient, config.stripePrivateKey);

		AccountIO accountIO = new AccountIO(accountsSql, registrationsSqlMapper, ticketsSql);

		this.apiRestServices = List.list(
			new RegistrationApiJaxRs(dataSource, registrationsSqlMapper, confirmationEmailSender, mailchimpSubscribeClient, slackBotHook, ticketsSql, accountIO, accountSignupSecretSql, config.stripePublishableKey, stripeChargeClient, registrationStripeChargeSql, config.mailchimpList, outvoiceReceiptClient, outvoiceInvoice3Client)
		);

		this.guiRestServices = List.list(
			new LoggedInJaxRs(authService),
			new VersionNumberJaxRs(this.version),
			new AccountsJaxRs(dataSource, authService, accountsSql, accountIO, accountSignupSecretSql, outvoiceInvoiceClient, outvoiceAccountClient),
			new RegistrationsJaxRs(dataSource, authService, registrationsSqlMapper, sendInvoiceService, dismissRegistrationService, markAsCompleteService, markAsPaidService, outvoicePaidClient),
			new NameTagsJaxRs(dataSource, authService, registrationsSqlMapper),
			new BudgetJaxRs(dataSource, authService, budgetSql, registrationsSqlMapper, accountIO, ticketsSql),
			new TicketsJaxRs(dataSource, authService, ticketsSql),
			new DashJaxRs(dataSource, authService, accountIO),
			new ExportsJaxRs(dataSource, authService,registrationsSqlMapper)
		);
	}
}
