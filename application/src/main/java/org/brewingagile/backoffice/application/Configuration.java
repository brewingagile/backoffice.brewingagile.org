package org.brewingagile.backoffice.application;

import lombok.EqualsAndHashCode;
import okhttp3.HttpUrl;
import org.brewingagile.backoffice.integrations.MailchimpSubscribeClient;
import org.brewingagile.backoffice.types.StripePrivateKey;
import org.brewingagile.backoffice.types.StripePublishableKey;
import org.brewingagile.backoffice.utils.EtcPropertyFile;

public class Configuration {
	public final int jettyHttpListenPort;
	public final String dbName;
	public final String dbHost;
	public final int dbPort;
	public final String dbUsername;
	public final String dbPassword;
	public final String guiAdminUsername;
	public final String guiAdminPassword;
	public final String outvoiceInvoicesEndpoint;
	public final String outvoiceInvoicesApikey;
	public final String emailFromEmail;
	public final String emailFromName;
	public final String emailsConfirmationSubject;
	public final Smtp smtp;
	public final String mailchimpEndpoint;
	public final String mailchimpApikey;
	public final HttpUrl slackBotHookUrl;
	public final String slackBotName;
	public final String slackBotChannel;
	public final StripePublishableKey stripePublishableKey;
	public final StripePrivateKey stripePrivateKey;
	public final MailchimpSubscribeClient.ListUniqueId mailchimpList;

	public Configuration(
		int jettyHttpListenPort,
		String dbName,
		String dbHost,
		int dbPort,
		String dbUsername,
		String dbPassword,
		String guiAdminUsername,
		String guiAdminPassword,
		String outvoiceInvoicesEndpoint,
		String outvoiceInvoicesApikey,
		String emailFromEmail,
		String emailFromName,
		String emailsConfirmationSubject,
		Smtp smtp,
		String mailchimpEndpoint,
		String mailchimpApikey,
		HttpUrl slackBotHookUrl,
		String slackBotName,
		String slackBotChannel,
		StripePublishableKey stripePublishableKey,
		StripePrivateKey stripePrivateKey,
		MailchimpSubscribeClient.ListUniqueId mailchimpList
	) {
		this.jettyHttpListenPort = jettyHttpListenPort;
		this.dbName = dbName;
		this.dbHost = dbHost;
		this.dbPort = dbPort;
		this.dbUsername = dbUsername;
		this.dbPassword = dbPassword;
		this.guiAdminUsername = guiAdminUsername;
		this.guiAdminPassword = guiAdminPassword;
		this.outvoiceInvoicesEndpoint = outvoiceInvoicesEndpoint;
		this.outvoiceInvoicesApikey = outvoiceInvoicesApikey;
		this.emailFromEmail = emailFromEmail;
		this.emailFromName = emailFromName;
		this.emailsConfirmationSubject = emailsConfirmationSubject;
		this.smtp = smtp;
		this.mailchimpEndpoint = mailchimpEndpoint;
		this.mailchimpApikey = mailchimpApikey;
		this.slackBotHookUrl = slackBotHookUrl;
		this.slackBotName = slackBotName;
		this.slackBotChannel = slackBotChannel;
		this.stripePublishableKey = stripePublishableKey;
		this.stripePrivateKey = stripePrivateKey;
		this.mailchimpList = mailchimpList;
	}

	public static Configuration from(EtcPropertyFile config, EtcPropertyFile secret) {
		return new Configuration(
			config.integer("jetty.http-listen-port"),
			config.string("db.name"),
			config.string("db.host"),
			config.integer("db.port"),
			config.string("db.username"),
			secret.string("db.password"),
			config.string("gui.admin.username"),
			secret.string("gui.admin.password"),
			config.string("outvoice.invoices.endpoint"),
			secret.string("outvoice.invoices.apikey"),
			config.string("email.from.email"),
			config.string("email.from.name"),
			config.string("emails.confirmation.subject"),
			new Smtp(
				config.string("smtp.server"),
				config.integer("smtp.ssl-port"),
				config.string("smtp.username"),
				secret.string("smtp.password")
			),
			config.string("mailchimp.endpoint"),
			secret.string("mailchimp.apikey"),
			HttpUrl.parse(secret.string("slackbot.hookurl")),
			config.string("slackbot.name"),
			config.string("slackbot.channel"),
			StripePublishableKey.stripePublishableKey(config.string("stripe.key.publishable")),
			StripePrivateKey.stripePrivateKey(secret.string("stripe.key.secret")),
			MailchimpSubscribeClient.ListUniqueId.listUniqueId(config.string("mailchimp.list"))
		);
	}

	@EqualsAndHashCode
	public static final class Smtp {
		public final String server;
		public final int sslPort;
		public final String username;
		public final String password;

		public Smtp(String server, int sslPort, String username, String password) {
			this.server = server;
			this.sslPort = sslPort;
			this.username = username;
			this.password = password;
		}


		@Override
		public String toString() {
			return "Smtp{" +
				"server='" + server + '\'' +
				", port=" + sslPort +
				", username='" + username + '\'' +
				", password='" + "REDACTED" + '\'' +
				'}';
		}
	}
}
