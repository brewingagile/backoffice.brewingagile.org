package org.brewingagile.backoffice.application;

import com.hencjo.summer.security.SummerAuthenticatedUser;
import org.brewingagile.backoffice.auth.AuthService;
import org.brewingagile.backoffice.db.operations.BucketsSqlMapper;
import org.brewingagile.backoffice.db.operations.RegistrationsSqlMapper;
import org.brewingagile.backoffice.integrations.MailchimpSubscribeClient;
import org.brewingagile.backoffice.integrations.MandrillEmailClient;
import org.brewingagile.backoffice.integrations.OutvoiceInvoiceClient;
import org.brewingagile.backoffice.services.DismissRegistrationService;
import org.brewingagile.backoffice.services.MarkAsCompleteService;
import org.brewingagile.backoffice.services.MarkAsPaidService;
import org.brewingagile.backoffice.services.SendInvoiceService;
import org.brewingagile.backoffice.utils.GitPropertiesDescribeVersionNumberProvider;

import javax.sql.DataSource;
import javax.ws.rs.client.ClientBuilder;

public enum Application {
	INSTANCE(configurationFromMain(), datasourceFromMain());
	private final DataSource dataSource;
	private final AuthService authService;
	private final GitPropertiesDescribeVersionNumberProvider versionNumberProvider;
	public final RegistrationsSqlMapper registrationsSqlMapper = new RegistrationsSqlMapper();
	public final BucketsSqlMapper bucketsSqlMapper = new BucketsSqlMapper();
	private final OutvoiceInvoiceClient outvoiceInvoiceClient;
	private final SendInvoiceService sendInvoiceService;
	private final DismissRegistrationService dismissRegistrationService;
	private final MarkAsCompleteService markAsCompleteService;
	private final MarkAsPaidService markAsPaidService;
	public final MandrillEmailClient mandrillEmailClient;
	public final MailchimpSubscribeClient mailchimpSubscribeClient;

	Application(Configuration config, DataSource dataSource) {
		this.dataSource = dataSource;
		
		this.versionNumberProvider = new GitPropertiesDescribeVersionNumberProvider(Application.class, "/resources/git.properties");
		this.authService = new AuthService(new SummerAuthenticatedUser());
		this.outvoiceInvoiceClient = new OutvoiceInvoiceClient(ClientBuilder.newClient(), config.outvoiceInvoicesEndpoint, config.outvoiceInvoicesApikey);
		this.sendInvoiceService = new SendInvoiceService(dataSource, registrationsSqlMapper, outvoiceInvoiceClient);
		this.dismissRegistrationService = new DismissRegistrationService(dataSource, registrationsSqlMapper);
		this.markAsCompleteService = new MarkAsCompleteService(dataSource, registrationsSqlMapper);
		this.markAsPaidService = new MarkAsPaidService(dataSource, registrationsSqlMapper);
		this.mandrillEmailClient = new MandrillEmailClient(ClientBuilder.newClient(), config.mandrillEndpoint, config.mandrillApikey);
		this.mailchimpSubscribeClient = new MailchimpSubscribeClient(ClientBuilder.newClient(), config.mailchimpEndpoint, config.mailchimpApikey);
	}

	private static Configuration configurationFromMain() { return Main.getConfiguration(); }
	private static DataSource datasourceFromMain() { return Main.getDatasource(); }

	public DataSource dataSource() { return dataSource; }
	public AuthService authService() { return authService;	}
	public GitPropertiesDescribeVersionNumberProvider versionNumberProvider() { return versionNumberProvider; }
	public RegistrationsSqlMapper registrationsSqlMapper() { return registrationsSqlMapper; }
	public SendInvoiceService sendInvoiceService() { return sendInvoiceService; }
	public DismissRegistrationService dismissRegistrationService() { return dismissRegistrationService; }
	public MarkAsCompleteService markAsCompleteService() { return markAsCompleteService; }
	public MarkAsPaidService markAsPaidService() { return markAsPaidService; }
}
